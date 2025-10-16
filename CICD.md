# WedSnap CI/CD 가이드

## 📋 문서 정보

- **프로젝트**: WedSnap
- **작성일**: 2025-10-12
- **목적**: 프로젝트 온보딩 사용자를 위한 CI/CD 파이프라인 가이드
- **대상**: 신규 개발자, DevOps 담당자

---

## 🎯 개요

WedSnap 프로젝트는 **GitHub Actions**를 기반으로 한 자동화된 CI/CD 파이프라인을 구축하여 코드 변경사항이 발생할 때마다 자동으로 테스트, 빌드, 배포가 수행됩니다.

### 주요 기술 스택

| 구성 요소        | 기술                                     |
|--------------|----------------------------------------|
| **CI 플랫폼**   | GitHub Actions                         |
| **빌드 도구**    | Gradle 8.x                             |
| **컨테이너화**    | Docker (Jib 플러그인)                      |
| **이미지 저장소**  | Private Docker Registry (Synology NAS) |
| **배포 대상**    | Synology NAS (Docker 컨테이너)             |
| **테스트 커버리지** | JaCoCo                                 |
| **배포 방식**    | SSH 기반 원격 배포                           |

---

## 🏗️ 시스템 아키텍처

```
┌─────────────┐
│  개발자 PC   │
│  코드 작성   │
└──────┬──────┘
       │ git push
       ↓
┌─────────────────────────────────────────────────────┐
│              GitHub Repository                       │
└──────────────────┬──────────────────────────────────┘
                   │ Webhook Trigger
                   ↓
┌─────────────────────────────────────────────────────┐
│           GitHub Actions Runner                      │
│  ┌───────────────────────────────────────────────┐  │
│  │  1. set-environment (브랜치별 환경 결정)       │  │
│  └───────────────┬───────────────────────────────┘  │
│                  ↓                                   │
│  ┌───────────────────────────────────────────────┐  │
│  │  2. CI Job                                    │  │
│  │  ├─ Checkout 코드                             │  │
│  │  ├─ JDK 17 설정                              │  │
│  │  ├─ Gradle 캐시 복원                         │  │
│  │  ├─ 테스트 실행 (JUnit + JaCoCo)            │  │
│  │  ├─ 커버리지 검증 (최소 30%)                 │  │
│  │  ├─ JAR 빌드 (main/dev)                     │  │
│  │  └─ Docker 이미지 빌드 (Jib) & 푸시 (main/dev) │
│  └───────────────┬───────────────────────────────┘  │
│                  │                                   │
│                  ↓                                   │
│  ┌───────────────────────────────────────────────┐  │
│  │  3. CD Job (main/dev 브랜치)                 │  │
│  │  ├─ SSH 접속 (Synology NAS)                 │  │
│  │  ├─ Docker 이미지 Pull                       │  │
│  │  ├─ 기존 컨테이너 중지/삭제                  │  │
│  │  └─ 새 컨테이너 시작 (환경별 포트)           │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────────────┐
│        Private Docker Registry                       │
│        (Synology NAS:5050)                          │
│                                                      │
│  wedsnap:prod     ← main 브랜치                     │
│  wedsnap:dev      ← dev 브랜치                      │
│  wedsnap:feature  ← feature/* 브랜치                │
└──────────────────┬──────────────────────────────────┘
                   │ docker pull
                   ↓
┌─────────────────────────────────────────────────────┐
│           Synology NAS                               │
│  ┌───────────────────────────────────────────────┐  │
│  │  Synology Reverse Proxy (443 포트)           │  │
│  │  ├─ wedsnap.agfe2.synology.me → 8080        │  │
│  │  └─ wedsnap-dev.agfe2.synology.me → 8081    │  │
│  └────────┬──────────────────────────────────────┘  │
│           │                                          │
│           ├─────────────┬────────────────────────────┤
│           ↓             ↓                            │
│  ┌─────────────────┐  ┌──────────────────────────┐  │
│  │ wedsnap-prod    │  │ wedsnap-dev              │  │
│  │ Port: 8080      │  │ Port: 8081               │  │
│  │ Image: prod     │  │ Image: dev               │  │
│  │ Restart: unless │  │ Restart: unless-stopped  │  │
│  │ -stopped        │  │                          │  │
│  └─────────────────┘  └──────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 🔄 CI (Continuous Integration) 프로세스

### 트리거 조건

CI 파이프라인은 다음 조건에서 자동 실행됩니다:

| 이벤트              | 브랜치         | 설명                |
|------------------|-------------|-------------------|
| **push**         | `main`      | 프로덕션 배포 트리거       |
| **push**         | `dev`       | 개발 환경 빌드          |
| **push**         | `feature/*` | 기능 브랜치 빌드 (배포 없음) |
| **pull_request** | `main`      | PR 생성 시 검증        |

### CI 단계별 설명

#### 1. 환경 결정 (set-environment)

브랜치명을 기반으로 빌드 환경을 결정합니다.

```yaml
main       → BUILD_ENV=prod
dev        → BUILD_ENV=dev
feature/*  → BUILD_ENV=feature
```

#### 2. 코드 체크아웃 및 환경 설정

```bash
# 소스 코드 체크아웃
actions/checkout@v4

# JDK 17 설정 (Eclipse Temurin)
actions/setup-java@v4

# Gradle 의존성 캐싱 (빌드 속도 향상)
actions/cache@v4
```

#### 3. 테스트 및 커버리지 측정

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification
```

- **JUnit 5**를 이용한 단위 테스트 실행
- **JaCoCo**로 코드 커버리지 측정
- **최소 커버리지**: 30% (LINE 기준)
- 커버리지 미달 시 빌드 실패

#### 4. JAR 빌드

```bash
./gradlew build
```

- `main` 및 `dev` 브랜치에서만 실행
- 테스트가 성공한 경우에만 진행

#### 5. Docker 이미지 빌드 및 푸시 (Jib)

```bash
./gradlew jib
```

- **main 및 dev 브랜치에서 실행**
- **Jib 플러그인**을 사용하여 Docker 이미지 빌드
- Dockerfile 없이 최적화된 레이어 구조로 이미지 생성
- Private Docker Registry로 자동 푸시

**이미지 태그 규칙:**

```
${DOCKER_REGISTRY_URL}/wedsnap:${BUILD_ENV}

예시:
- wedsnap:prod     (main 브랜치)
- wedsnap:dev      (dev 브랜치)
- wedsnap:feature  (feature/* 브랜치)
```

---

## 🚀 CD (Continuous Deployment) 프로세스

### 배포 조건

- **브랜치**: `main` 및 `dev` 배포
- **선행 조건**: CI Job 성공
- **배포 대상**: Synology NAS (Docker 컨테이너)

### 환경별 배포 설정

| 환경              | 브랜치    | 컨테이너 이름        | 호스트 포트 | 도메인                             | 이미지 태그         |
|-----------------|--------|----------------|--------|---------------------------------|----------------|
| **Production**  | `main` | `wedsnap-prod` | 8080   | `wedsnap.agfe2.synology.me`     | `wedsnap:prod` |
| **Development** | `dev`  | `wedsnap-dev`  | 8081   | `wedsnap-dev.agfe2.synology.me` | `wedsnap:dev`  |

### CD 단계별 설명

#### 1. SSH 연결

```yaml
appleboy/ssh-action@v1.0.3
```

- **SSH 키 기반 인증** (비밀번호 없음)
- GitHub Secrets에 저장된 SSH 개인키 사용
- 사용자 지정 포트로 연결

#### 2. 배포 스크립트 실행

```bash
# PATH 설정 (Docker 명령어 접근)
export PATH=$PATH:/usr/local/bin

# 환경별 설정 (prod: 8080, dev: 8081)
if [ "${BUILD_ENV}" = "prod" ]; then
  CONTAINER_NAME="wedsnap-prod"
  HOST_PORT="8080"
elif [ "${BUILD_ENV}" = "dev" ]; then
  CONTAINER_NAME="wedsnap-dev"
  HOST_PORT="8081"
fi

# 최신 이미지 Pull
docker pull ${DOCKER_REGISTRY_URL}/wedsnap:${BUILD_ENV}

# 기존 컨테이너 중지 및 삭제
docker stop ${CONTAINER_NAME} || true
docker rm ${CONTAINER_NAME} || true

# 새 컨테이너 시작
docker run -d \
  --name ${CONTAINER_NAME} \
  --restart=unless-stopped \
  -p ${HOST_PORT}:8080 \
  ${DOCKER_REGISTRY_URL}/wedsnap:${BUILD_ENV}
```

**주요 특징:**

- 환경별 컨테이너 이름 및 포트 자동 설정
- 기존 컨테이너 안전 삭제 (`|| true`로 에러 무시)
- 환경별 호스트 포트 분리 (prod: 8080, dev: 8081)
- 자동 재시작 정책 (`--restart=unless-stopped`)
- Detached 모드로 백그라운드 실행

#### 3. Docker Registry 접근

배포 대상 서버(Synology NAS)에서는 **사전에 Docker 로그인**을 수행해 두었습니다:

```bash
docker login ${DOCKER_REGISTRY_URL} -u ${USERNAME}
```

이로 인해 배포 스크립트에서는 별도의 인증 없이 `docker pull`이 가능합니다.

---

## 🌐 Reverse Proxy 및 HTTPS 설정

WedSnap은 Synology NAS의 Reverse Proxy 기능을 사용하여 포트 번호 없이 HTTPS로 접속할 수 있습니다.

### 접속 URL

| 환경              | HTTPS URL (포트 생략)                       | 내부 포트 |
|-----------------|-----------------------------------------|-------|
| **Production**  | `https://wedsnap.agfe2.synology.me`     | 8080  |
| **Development** | `https://wedsnap-dev.agfe2.synology.me` | 8081  |

### 동작 원리

```
사용자 브라우저
    ↓
https://wedsnap.agfe2.synology.me (포트 443, 자동)
    ↓
인터넷 → 라우터 (443 포트 포워딩)
    ↓
Synology NAS (Reverse Proxy: 443 포트)
    ↓
내부 라우팅:
  - wedsnap.agfe2.synology.me → localhost:8080 (wedsnap-prod)
  - wedsnap-dev.agfe2.synology.me → localhost:8081 (wedsnap-dev)
    ↓
Docker 컨테이너
```

### Synology Reverse Proxy 설정

#### 1. DSM 접속 및 역방향 프록시 설정

1. **Synology DSM 로그인**
2. **제어판** → **로그인 포털** → **고급** 탭
3. **역방향 프록시** 선택
4. **생성** 버튼 클릭

#### 2. Production 환경 규칙 생성

**규칙 이름**: `WedSnap Production`

**소스 (외부 접속):**

- 프로토콜: `HTTPS`
- 호스트 이름: `wedsnap.agfe2.synology.me`
- 포트: `443`
- HSTS 활성화: ✅ (권장)

**대상 (내부 서버):**

- 프로토콜: `HTTP`
- 호스트 이름: `localhost`
- 포트: `8080`

#### 3. Development 환경 규칙 생성

**규칙 이름**: `WedSnap Development`

**소스 (외부 접속):**

- 프로토콜: `HTTPS`
- 호스트 이름: `wedsnap-dev.agfe2.synology.me`
- 포트: `443`
- HSTS 활성화: ✅ (권장)

**대상 (내부 서버):**

- 프로토콜: `HTTP`
- 호스트 이름: `localhost`
- 포트: `8081`

#### 4. WebSocket 지원 (선택 사항)

만약 WebSocket이 필요한 경우, **사용자 정의 헤더** 탭에서 다음 헤더를 추가합니다:

```
Upgrade: $http_upgrade
Connection: $connection_upgrade
```

### 인증서 설정

#### 1. 와일드카드 인증서 설정 (권장)

**Synology DSM에서 인증서 설정:**

1. **제어판** → **보안** → **인증서**
2. **추가** 버튼 클릭
3. **새 인증서 추가** 선택

**옵션 A: Let's Encrypt (자동 갱신)**

- 도메인 이름: `*.agfe2.synology.me`
- 이메일 주소 입력
- DNS 공급자 설정 (Synology DDNS 사용 시 자동)

**옵션 B: 기존 인증서 가져오기**

- 개인 키, 인증서, 중간 인증서 업로드

#### 2. 인증서 서비스별 할당

1. **설정** → **인증서 설정**
2. 다음 서비스에 인증서 할당:
    - `wedsnap.agfe2.synology.me` → 와일드카드 인증서
    - `wedsnap-dev.agfe2.synology.me` → 와일드카드 인증서
    - 또는 각 서브도메인별 개별 인증서

### 네트워크 설정

#### 1. 라우터 포트 포워딩

라우터 관리 페이지에서 다음 포트를 포워딩합니다:

| 외부 포트   | 내부 IP           | 내부 포트 | 프로토콜 | 설명                      |
|---------|-----------------|-------|------|-------------------------|
| **443** | Synology NAS IP | 443   | TCP  | HTTPS (필수)              |
| **80**  | Synology NAS IP | 80    | TCP  | HTTP → HTTPS 리다이렉트 (권장) |

**중요:** 기존에 8080 포트를 외부에 오픈했다면, 이제는 **제거**할 수 있습니다. Reverse Proxy가 내부적으로 처리하기 때문입니다.

#### 2. 방화벽 설정

Synology DSM 방화벽 설정:

1. **제어판** → **보안** → **방화벽**
2. 다음 규칙 허용:
    - 포트 443 (HTTPS) - 모든 IP
    - 포트 80 (HTTP) - 모든 IP
3. 다음 규칙 차단 (선택 사항, 보안 강화):
    - 포트 8080 - 외부 접근 차단 (Reverse Proxy만 접근)
    - 포트 8081 - 외부 접근 차단 (Reverse Proxy만 접근)

### Synology DDNS 설정

#### 1. DDNS 서비스 활성화

1. **제어판** → **외부 액세스** → **DDNS**
2. **추가** 버튼 클릭
3. 서비스 공급자: `Synology`
4. 호스트 이름: `agfe2.synology.me`
5. 사용자 이름/비밀번호 입력

#### 2. 서브도메인 자동 지원

Synology DDNS는 와일드카드 서브도메인을 자동으로 지원합니다:

- `agfe2.synology.me` 등록 시
- `*.agfe2.synology.me` 모두 동일한 IP로 라우팅됨
- 따라서 `wedsnap.agfe2.synology.me`, `wedsnap-dev.agfe2.synology.me` 별도 등록 불필요

### 접속 테스트

#### 1. 로컬 테스트

```bash
# Production 접속 테스트
curl -I https://wedsnap.agfe2.synology.me

# Development 접속 테스트
curl -I https://wedsnap-dev.agfe2.synology.me

# 예상 응답: HTTP/2 200
```

#### 2. 브라우저 테스트

- **Production**: `https://wedsnap.agfe2.synology.me` 접속
- **Development**: `https://wedsnap-dev.agfe2.synology.me` 접속
- 브라우저 주소창에 🔒 (자물쇠) 아이콘 확인
- 인증서 유효성 확인

#### 3. SSL 인증서 확인

```bash
# 인증서 정보 확인
openssl s_client -connect wedsnap.agfe2.synology.me:443 -servername wedsnap.agfe2.synology.me

# 인증서 만료일 확인
echo | openssl s_client -connect wedsnap.agfe2.synology.me:443 -servername wedsnap.agfe2.synology.me 2>/dev/null | openssl x509 -noout -dates
```

### 트러블슈팅

#### 1. "사이트에 연결할 수 없음" 오류

**원인:**

- 포트 포워딩 미설정
- DDNS 미갱신
- 방화벽 차단

**해결 방법:**

```bash
# 1. 라우터 포트 포워딩 확인 (443 포트)
# 2. Synology DDNS 상태 확인
# 3. 외부에서 포트 접근 테스트
telnet agfe2.synology.me 443
```

#### 2. "인증서 오류" 경고

**원인:**

- 인증서 만료
- 도메인 불일치
- 중간 인증서 누락

**해결 방법:**

1. **제어판** → **보안** → **인증서** 확인
2. Let's Encrypt 인증서 자동 갱신 활성화
3. 인증서 재발급

#### 3. "502 Bad Gateway" 오류

**원인:**

- Docker 컨테이너 중지됨
- 포트 매핑 오류

**해결 방법:**

```bash
# 컨테이너 상태 확인
docker ps | grep wedsnap

# 컨테이너 재시작
docker restart wedsnap-prod
docker restart wedsnap-dev

# 로그 확인
docker logs wedsnap-prod --tail 50
docker logs wedsnap-dev --tail 50
```

#### 4. HTTP로 접속되는 문제

**원인:**

- HTTPS 리다이렉트 미설정

**해결 방법:**

1. Reverse Proxy 규칙에서 HSTS 활성화
2. 또는 별도 HTTP → HTTPS 리다이렉트 규칙 생성:
    - 소스: HTTP, 포트 80
    - 대상: HTTPS, 포트 443

### 보안 권장사항

1. **HSTS (HTTP Strict Transport Security) 활성화**
    - Reverse Proxy 규칙에서 HSTS 옵션 체크
    - 브라우저가 항상 HTTPS로 접속하도록 강제

2. **포트 8080, 8081 외부 접근 차단**
    - 방화벽에서 차단
    - Reverse Proxy를 통해서만 접근 가능

3. **Let's Encrypt 자동 갱신**
    - Synology DSM의 자동 갱신 기능 활성화
    - 인증서 만료 90일 전 자동 갱신

4. **로그 모니터링**
    - DSM 로그 센터에서 Reverse Proxy 접속 로그 확인
    - 비정상적인 접근 패턴 모니터링

---

## 🔐 GitHub Secrets 설정

CI/CD 파이프라인 실행에 필요한 민감한 정보는 GitHub Secrets에 저장됩니다.

### 필수 Secrets 목록

#### Docker Registry 관련

| Secret 이름             | 설명                  | 예시 값                     |
|-----------------------|---------------------|--------------------------|
| `DOCKER_REGISTRY_URL` | Private Registry 주소 | `your-registry.com:5050` |
| `DOCKER_USERNAME`     | Registry 사용자명       | `registryuser`           |
| `DOCKER_PASSWORD`     | Registry 비밀번호       | `********` (보안상 기록하지 않음) |

#### SSH 접속 관련

| Secret 이름         | 설명               | 예시 값                                   |
|-------------------|------------------|----------------------------------------|
| `SSH_HOST`        | Synology NAS 호스트 | `your-nas.com`                         |
| `SSH_PORT`        | SSH 포트           | `31422`                                |
| `SSH_USER`        | SSH 접속 사용자       | `deployuser`                           |
| `SSH_PRIVATE_KEY` | SSH 개인키 전체 내용    | `-----BEGIN RSA PRIVATE KEY-----\n...` |

### Secrets 등록 방법

1. GitHub Repository 페이지 접속
2. **Settings** → **Secrets and variables** → **Actions**
3. **New repository secret** 클릭
4. Secret 이름 및 값 입력
5. **Add secret** 클릭

**중요 사항:**

- `SSH_PRIVATE_KEY`는 개행 문자를 포함한 **전체 내용**을 그대로 복사
- SSH 키 파일 (`wedsnap_deploy_rsa`, `wedsnap_deploy_rsa.pub`)은 별도로 안전하게 공유
- 비밀번호 및 키는 절대 코드에 하드코딩하지 않음

---

## 💻 로컬 개발 가이드

### Jib 이미지 빌드 테스트

로컬 환경에서 Jib을 사용하여 Docker 이미지를 빌드할 수 있습니다.

#### 1. 환경 변수 설정

**Linux / macOS:**

```bash
export DOCKER_REGISTRY_URL="your-registry.com:5050"
export DOCKER_USERNAME="your-username"
export DOCKER_PASSWORD="your-password"
export BUILD_ENV="dev"
```

**Windows (PowerShell):**

```powershell
$env:DOCKER_REGISTRY_URL="your-registry.com:5050"
$env:DOCKER_USERNAME="your-username"
$env:DOCKER_PASSWORD="your-password"
$env:BUILD_ENV="dev"
```

#### 2. Docker Registry 로그인

```bash
docker login ${DOCKER_REGISTRY_URL}
# Username: your-username
# Password: your-password
```

#### 3. Jib 빌드 실행

**Registry로 푸시:**

```bash
./gradlew jib
```

**로컬 Docker 데몬에 빌드 (Registry 푸시 없음):**

```bash
./gradlew jibDockerBuild
```

#### 4. 빌드된 이미지 확인

```bash
docker images | grep wedsnap
```

#### 5. 로컬 컨테이너 실행 테스트

```bash
docker run -p 8080:8080 wedsnap:dev

# 브라우저에서 접속
# http://localhost:8080
```

---

## 🌿 브랜치 전략

### 브랜치별 동작

| 브랜치         | BUILD_ENV | CI 실행 | 이미지 빌드 | CD 실행 | 배포 대상               | 컨테이너 이름      | 포트   | 도메인                           |
|-------------|-----------|-------|--------|-------|---------------------|--------------|------|-------------------------------|
| `main`      | `prod`    | ✅     | ✅      | ✅     | Synology NAS (프로덕션) | wedsnap-prod | 8080 | wedsnap.agfe2.synology.me     |
| `dev`       | `dev`     | ✅     | ✅      | ✅     | Synology NAS (개발)   | wedsnap-dev  | 8081 | wedsnap-dev.agfe2.synology.me |
| `feature/*` | `feature` | ✅     | ❌      | ❌     | 없음                  | -            | -    | -                             |

### 권장 워크플로우

```
1. feature 브랜치 생성
   git checkout -b feature/new-feature

2. 기능 개발 및 테스트
   git add .
   git commit -m "feat: 새 기능 추가"
   git push origin feature/new-feature

3. Pull Request 생성
   - main 브랜치로 PR 생성
   - CI 자동 실행 (테스트 통과 확인)

4. 코드 리뷰 및 머지
   - 리뷰 완료 후 main에 머지
   - CI + CD 자동 실행
   - Synology NAS에 자동 배포
```

---

## 🔧 트러블슈팅

### 1. 테스트 실패

**증상:**

```
❌ Task :test FAILED
```

**해결 방법:**

1. 로컬에서 테스트 실행
   ```bash
   ./gradlew test --info
   ```
2. 실패한 테스트 케이스 확인
3. 테스트 코드 또는 소스 코드 수정
4. 재푸시

### 2. 커버리지 미달

**증상:**

```
❌ Task :jacocoTestCoverageVerification FAILED
Rule violated for bundle: LINE covered ratio is 0.25, but expected minimum is 0.30
```

**해결 방법:**

1. 커버리지 리포트 확인
   ```bash
   ./gradlew jacocoTestReport
   open build/reports/jacoco/test/html/index.html
   ```
2. 테스트 코드 추가 작성
3. 최소 30% 커버리지 달성

### 3. Jib 빌드 실패

**증상:**

```
❌ Task :jib FAILED
Unauthorized: authentication required
```

**해결 방법:**

1. GitHub Secrets 확인
    - `DOCKER_REGISTRY_URL`
    - `DOCKER_USERNAME`
    - `DOCKER_PASSWORD`
2. Registry 로그인 정보 유효성 확인
3. Registry 서버 상태 확인

### 4. SSH 배포 실패

**증상:**

```
❌ Deploy to Synology NAS FAILED
Permission denied (publickey)
```

**해결 방법:**

1. GitHub Secrets의 `SSH_PRIVATE_KEY` 확인
    - 개행 문자 포함 여부
    - `-----BEGIN ... END-----` 포함 여부
2. Synology NAS의 공개키 등록 확인
3. SSH 포트 및 방화벽 설정 확인

### 5. Docker Pull 실패

**증상:**

```
Error response from daemon: pull access denied
```

**해결 방법:**

1. Synology NAS에 SSH 접속
   ```bash
   ssh deployuser@your-nas.com -p 31422
   ```
2. Docker 로그인 재시도
   ```bash
   docker login your-registry.com:5050
   ```
3. 로그인 정보 저장 확인

---

## 📊 모니터링 및 로그

### GitHub Actions 로그 확인

1. GitHub Repository → **Actions** 탭
2. 워크플로우 실행 목록에서 해당 실행 클릭
3. Job별 로그 확인:
    - `set-environment`: 환경 변수 설정
    - `ci`: 테스트, 빌드, 이미지 푸시
    - `cd`: 배포 (main 및 dev 브랜치)

**유용한 로그 검색:**

```
# 커버리지 정보
=== Jacoco Coverage Summary ===

# Docker 이미지 태그
🐳 Building and pushing Docker image with tag: prod

# 배포 시작/완료
🛰️  배포 시작...
✅ 배포 완료!
```

### Synology NAS 컨테이너 로그

#### 1. SSH 접속

```bash
ssh deployuser@your-nas.com -p 31422
```

#### 2. 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker ps

# 출력 예시:
# CONTAINER ID   IMAGE                           STATUS
# abc123def456   your-registry:5050/wedsnap:prod Up 2 hours
```

#### 3. 컨테이너 로그 확인

```bash
# Production 로그
docker logs wedsnap-prod
docker logs wedsnap-prod --tail 100    # 최근 100줄
docker logs wedsnap-prod --follow      # 실시간 스트리밍
docker logs wedsnap-prod --timestamps  # 타임스탬프 포함

# Development 로그
docker logs wedsnap-dev
docker logs wedsnap-dev --tail 100
docker logs wedsnap-dev --follow
docker logs wedsnap-dev --timestamps
```

#### 4. 컨테이너 리소스 확인

```bash
# 모든 WedSnap 컨테이너 리소스 확인
docker stats wedsnap-prod wedsnap-dev

# 한 번만 출력
docker stats wedsnap-prod wedsnap-dev --no-stream

# 개별 확인
docker stats wedsnap-prod --no-stream
docker stats wedsnap-dev --no-stream
```

---

## 📚 추가 참고 자료

### Gradle 명령어

```bash
# 전체 빌드
./gradlew build

# 테스트만 실행
./gradlew test

# 커버리지 리포트 생성
./gradlew jacocoTestReport

# 커버리지 검증
./gradlew jacocoTestCoverageVerification

# Jib 빌드 (로컬 Docker)
./gradlew jibDockerBuild

# Jib 빌드 (Registry 푸시)
./gradlew jib

# 빌드 산출물 정리
./gradlew clean
```

### Private Docker Registry 관리

WedSnap 프로젝트는 **Private Docker Registry**를 사용하여 빌드된 이미지를 저장하고 관리합니다. Synology NAS에서 Docker Hub의 공식 registry 이미지를 사용하여 구축되었습니다.

#### Registry 구성

| 항목               | 값                              |
|------------------|--------------------------------|
| **이미지**          | `registry:latest` (Docker Hub) |
| **프로토콜**         | HTTPS (Synology 인증서 사용)        |
| **인증 방식**        | htpasswd (Basic Auth)          |
| **호스트 포트**       | 5050                           |
| **컨테이너 포트**      | 5000                           |
| **Registry URL** | `${DOCKER_REGISTRY_URL}:5050`  |

#### 디렉토리 구조

```
docker/registry/
├── (루트)          # 이미지 데이터 저장소 (/var/lib/registry)
├── auth/           # htpasswd 인증 파일
│   └── htpasswd    # 사용자 인증 정보
└── certs/          # SSL/TLS 인증서
    ├── domain.crt  # 공개 인증서
    └── domain.key  # 개인 키
```

#### Registry 컨테이너 실행 명령어

Registry 컨테이너가 중지되었거나 재시작이 필요한 경우 다음 명령어를 사용합니다:

```bash
docker run -d \
  --name registry \
  --restart=always \
  -p 5050:5000 \
  -v /volume1/docker/registry:/var/lib/registry \
  -v /volume1/docker/registry/auth:/auth \
  -v /volume1/docker/registry/certs:/certs \
  -e REGISTRY_AUTH=htpasswd \
  -e REGISTRY_AUTH_HTPASSWD_REALM="Registry Realm" \
  -e REGISTRY_AUTH_HTPASSWD_PATH=/auth/htpasswd \
  -e REGISTRY_STORAGE_DELETE_ENABLED=true \
  -e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/domain.crt \
  -e REGISTRY_HTTP_TLS_KEY=/certs/domain.key \
  registry:latest
```

**옵션 설명:**

| 옵션                                | 설명                            |
|-----------------------------------|-------------------------------|
| `--name registry`                 | 컨테이너 이름 지정                    |
| `--restart=always`                | 시스템 재부팅 시 자동 시작               |
| `-p 5050:5000`                    | 호스트 포트 5050을 컨테이너 포트 5000에 매핑 |
| `-v` (데이터)                        | 이미지 저장소 볼륨 마운트                |
| `-v` (auth)                       | 인증 파일 볼륨 마운트                  |
| `-v` (certs)                      | 인증서 볼륨 마운트                    |
| `REGISTRY_AUTH`                   | 인증 방식 (htpasswd)              |
| `REGISTRY_AUTH_HTPASSWD_REALM`    | 인증 영역 이름                      |
| `REGISTRY_AUTH_HTPASSWD_PATH`     | htpasswd 파일 경로                |
| `REGISTRY_STORAGE_DELETE_ENABLED` | 이미지 삭제 허용                     |
| `REGISTRY_HTTP_TLS_CERTIFICATE`   | SSL 인증서 경로                    |
| `REGISTRY_HTTP_TLS_KEY`           | SSL 개인키 경로                    |

#### Registry 관리 명령어

```bash
# Registry 컨테이너 상태 확인
docker ps | grep registry

# Registry 컨테이너 중지
docker stop registry

# Registry 컨테이너 시작
docker start registry

# Registry 컨테이너 재시작
docker restart registry

# Registry 컨테이너 로그 확인
docker logs registry

# Registry 컨테이너 실시간 로그
docker logs registry --follow

# 기존 컨테이너 삭제 (재생성 필요 시)
docker stop registry
docker rm registry
# 위의 실행 명령어 재실행
```

#### 인증서 관리

**인증서 위치:**

```
/volume1/docker/registry/certs/
├── domain.crt  # 공개 인증서
└── domain.key  # 개인 키
```

**인증서 갱신 절차:**

1. Synology DSM에서 새 인증서 발급/갱신
2. 인증서 파일을 `docker/registry/certs/`에 복사
   ```bash
   cp /path/to/new/cert.crt /volume1/docker/registry/certs/domain.crt
   cp /path/to/new/key.key /volume1/docker/registry/certs/domain.key
   ```
3. 파일 권한 설정
   ```bash
   chmod 644 /volume1/docker/registry/certs/domain.crt
   chmod 600 /volume1/docker/registry/certs/domain.key
   ```
4. Registry 컨테이너 재시작
   ```bash
   docker restart registry
   ```

**인증서 확인:**

```bash
# 인증서 유효기간 확인
openssl x509 -in /volume1/docker/registry/certs/domain.crt -noout -dates

# 인증서 정보 확인
openssl x509 -in /volume1/docker/registry/certs/domain.crt -noout -text
```

#### 사용자 인증 관리 (htpasswd)

**htpasswd 파일 위치:**

```
/volume1/docker/registry/auth/htpasswd
```

**새 사용자 추가:**

```bash
# 첫 번째 사용자 생성 (파일 생성)
docker run --rm --entrypoint htpasswd \
  httpd:2 -Bbn username password > /volume1/docker/registry/auth/htpasswd

# 추가 사용자 생성 (파일에 추가)
docker run --rm --entrypoint htpasswd \
  httpd:2 -Bbn newuser newpassword >> /volume1/docker/registry/auth/htpasswd
```

**사용자 삭제:**

```bash
# htpasswd 파일 편집 (해당 사용자 라인 삭제)
vi /volume1/docker/registry/auth/htpasswd
# 또는
nano /volume1/docker/registry/auth/htpasswd
```

**비밀번호 변경:**

```bash
# 1. 기존 사용자 라인 삭제
# 2. 새 비밀번호로 사용자 추가
docker run --rm --entrypoint htpasswd \
  httpd:2 -Bbn username newpassword >> /volume1/docker/registry/auth/htpasswd
```

**변경 사항 적용:**

```bash
# 인증 정보 변경 후 Registry 재시작
docker restart registry
```

#### Registry 접근 및 사용

**Docker 로그인:**

```bash
# Registry에 로그인
docker login ${DOCKER_REGISTRY_URL}:5050
# Username: your-username
# Password: your-password

# 로그인 확인
cat ~/.docker/config.json
```

**이미지 푸시:**

```bash
# 이미지 태그 지정
docker tag wedsnap:prod ${DOCKER_REGISTRY_URL}:5050/wedsnap:prod

# Registry로 푸시
docker push ${DOCKER_REGISTRY_URL}:5050/wedsnap:prod
```

**이미지 풀:**

```bash
# Registry에서 이미지 가져오기
docker pull ${DOCKER_REGISTRY_URL}:5050/wedsnap:prod
```

**저장된 이미지 목록 확인 (API):**

```bash
# 모든 저장소 목록
curl -u username:password https://${DOCKER_REGISTRY_URL}:5050/v2/_catalog

# 특정 이미지의 태그 목록
curl -u username:password https://${DOCKER_REGISTRY_URL}:5050/v2/wedsnap/tags/list
```

#### Registry 트러블슈팅

##### 1. HTTPS 인증서 오류

**증상:**

```
x509: certificate signed by unknown authority
```

**해결 방법:**

**방법 1: Docker에 인증서 신뢰 추가 (권장)**

```bash
# Linux
sudo mkdir -p /etc/docker/certs.d/${DOCKER_REGISTRY_URL}:5050
sudo cp domain.crt /etc/docker/certs.d/${DOCKER_REGISTRY_URL}:5050/ca.crt
sudo systemctl restart docker

# macOS
security add-trusted-cert -d -r trustRoot -k ~/Library/Keychains/login.keychain domain.crt

# Windows
# 인증서 더블클릭 → 인증서 설치 → 신뢰할 수 있는 루트 인증 기관
```

**방법 2: Insecure Registry 설정 (개발 환경만)**

```json
// /etc/docker/daemon.json
{
  "insecure-registries": [ "${DOCKER_REGISTRY_URL}:5050" ]
}
```

##### 2. 인증 실패

**증상:**

```
unauthorized: authentication required
```

**해결 방법:**

1. 로그인 정보 확인
   ```bash
   docker login ${DOCKER_REGISTRY_URL}:5050
   ```
2. htpasswd 파일 확인
   ```bash
   cat /volume1/docker/registry/auth/htpasswd
   ```
3. Registry 컨테이너 로그 확인
   ```bash
   docker logs registry --tail 50
   ```

##### 3. Registry 컨테이너 시작 실패

**증상:**

```
Error starting userland proxy: listen tcp 0.0.0.0:5050: bind: address already in use
```

**해결 방법:**

```bash
# 포트 사용 중인 프로세스 확인
netstat -tulpn | grep 5050

# 기존 Registry 컨테이너 확인 및 삭제
docker ps -a | grep registry
docker stop registry
docker rm registry

# 컨테이너 재생성
# (위의 실행 명령어 사용)
```

##### 4. 이미지 Pull 실패

**증상:**

```
Error response from daemon: manifest for image not found
```

**해결 방법:**

```bash
# Registry에 저장된 이미지 확인
curl -u username:password https://${DOCKER_REGISTRY_URL}:5050/v2/_catalog

# 태그 확인
curl -u username:password https://${DOCKER_REGISTRY_URL}:5050/v2/wedsnap/tags/list

# 올바른 이미지 이름 및 태그 사용
docker pull ${DOCKER_REGISTRY_URL}:5050/wedsnap:prod
```

#### Registry 백업 및 복구

**데이터 백업:**

```bash
# Registry 데이터 백업
tar -czvf registry-backup-$(date +%Y%m%d).tar.gz /volume1/docker/registry/

# 또는 특정 디렉토리만 백업
tar -czvf registry-data-$(date +%Y%m%d).tar.gz /volume1/docker/registry/docker/
tar -czvf registry-auth-$(date +%Y%m%d).tar.gz /volume1/docker/registry/auth/
tar -czvf registry-certs-$(date +%Y%m%d).tar.gz /volume1/docker/registry/certs/
```

**데이터 복구:**

```bash
# Registry 컨테이너 중지
docker stop registry

# 백업 복구
tar -xzvf registry-backup-20250112.tar.gz -C /

# Registry 컨테이너 시작
docker start registry
```

### Docker 명령어

```bash
# 이미지 목록 확인
docker images | grep wedsnap

# 컨테이너 목록 확인
docker ps -a | grep wedsnap

# 컨테이너 중지
docker stop wedsnap-prod  # Production
docker stop wedsnap-dev   # Development

# 컨테이너 삭제
docker rm wedsnap-prod
docker rm wedsnap-dev

# 컨테이너 재시작
docker restart wedsnap-prod
docker restart wedsnap-dev

# 이미지 삭제
docker rmi wedsnap:prod
docker rmi wedsnap:dev

# Registry 로그인
docker login your-registry.com:5050

# 이미지 Pull
docker pull your-registry.com:5050/wedsnap:prod  # Production
docker pull your-registry.com:5050/wedsnap:dev   # Development

# 수동 컨테이너 실행
docker run -d --name wedsnap-prod --restart=unless-stopped -p 8080:8080 wedsnap:prod
docker run -d --name wedsnap-dev --restart=unless-stopped -p 8081:8080 wedsnap:dev
```

---

## ⚠️ 보안 주의사항

1. **SSH 키 관리**
    - SSH 개인키 파일은 절대 Git에 커밋하지 않음
    - 파일 권한: `chmod 600 ~/.ssh/wedsnap_deploy_rsa`
    - 주기적으로 키 로테이션 (6-12개월)

2. **Docker Registry 인증**
    - 비밀번호는 GitHub Secrets에만 저장
    - 주기적으로 비밀번호 변경 (3-6개월)
    - Registry 접근 로그 모니터링

3. **환경 변수**
    - 민감한 정보는 코드에 하드코딩 금지
    - `.env` 파일은 `.gitignore`에 추가
    - GitHub Secrets 사용 권장

4. **배포 권한**
    - `main` 브랜치는 보호 브랜치로 설정
    - PR 리뷰 필수화
    - 직접 푸시 제한

---

## 📞 문의 및 지원

- **문제 발생 시**: GitHub Issues에 등록
- **긴급 문의**: 프로젝트 관리자에게 직접 연락
- **문서 개선 제안**: Pull Request 환영

---

**문서 버전**: 2.0
**최종 업데이트**: 2025-10-12
**주요 변경사항**:

- dev 브랜치 배포 로직 추가
- Reverse Proxy 및 HTTPS 설정 가이드 추가
- 환경별 포트 분리 (prod: 8080, dev: 8081)
- 서브도메인 기반 접속 설정
