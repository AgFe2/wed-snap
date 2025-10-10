# WedSnap CI/CD 파이프라인 구축 계획서

## 📋 문서 정보

- **프로젝트**: WedSnap
- **작성일**: 2025-10-10
- **목적**: Gitea Actions 기반 CI/CD 자동화 파이프라인 구축

---

## 🎯 개요

본 문서는 WedSnap 프로젝트의 CI/CD 파이프라인을 Gitea Actions를 이용하여 구축하는 계획서입니다.

### 주요 목표

- **자동화된 테스트 실행**: 코드 푸시 시 자동으로 테스트 수행
- **자동 빌드 및 배포**: Gradle을 통한 빌드 및 Docker 이미지 생성
- **Synology NAS 배포**: Docker 컨테이너를 Synology NAS에 자동 배포
- **무중단 배포**: 컨테이너 재시작을 통한 서비스 업데이트

### 선택한 도구: Gitea Actions

**선택 이유:**
- ✅ Gitea 내장 기능으로 별도 도구 설치 불필요
- ✅ GitHub Actions와 높은 호환성
- ✅ YAML 기반의 직관적인 설정
- ✅ 경량화되어 리소스 효율적

**대안 도구:**
- Drone CI: 별도 서버 설치 필요
- Jenkins: 무겁고 복잡한 설정

---

## 🏗️ 시스템 아키텍처

```
┌─────────┐     Git Push      ┌──────────────────┐
│  개발자  │ ───────────────> │ Gitea Repository │
└─────────┘                   └────────┬─────────┘
                                       │
                                       │ Trigger
                                       ↓
                              ┌────────────────────┐
                              │  Gitea Actions     │
                              │  (Workflow)        │
                              └────────┬───────────┘
                                       │
                       ┌───────────────┴────────────────┐
                       │                                │
                       ↓                                ↓
              ┌────────────────┐              ┌────────────────┐
              │   CI 단계      │              │   CD 단계      │
              │   (Runner)     │              │   (Runner)     │
              ├────────────────┤              ├────────────────┤
              │ 1. 테스트 실행 │              │ 1. SSH 접속    │
              │ 2. Gradle 빌드 │              │ 2. Docker Pull │
              │ 3. Jib 이미지  │              │ 3. 컨테이너    │
              │    빌드        │              │    재시작      │
              │ 4. Registry    │              │                │
              │    푸시        │              │                │
              └────────┬───────┘              └────────┬───────┘
                       │                               │
                       ↓                               │
              ┌────────────────────┐                   │
              │ Synology Docker    │ <─────────────────┘
              │ Registry           │
              └─────────┬──────────┘
                        │
                        ↓
              ┌────────────────────┐
              │ Synology Docker    │
              │ Container          │
              │ (WedSnap App)      │
              └────────────────────┘
```

---

## 🔧 구현 단계

### 1단계: Jib Gradle 플러그인 설정

Jib는 Docker 데몬 없이 Java 애플리케이션을 컨테이너화할 수 있는 Gradle 플러그인입니다.

**주요 작업:**
- `build.gradle`에 Jib 플러그인 추가
- Synology Docker Registry 연결 정보 설정
- 이미지 이름 및 태그 전략 설정
- 베이스 이미지 및 포트 설정

**설정 예시:**
```gradle
plugins {
    id 'com.google.cloud.tools.jib' version '3.4.5'
}

jib {
    from {
        image = 'eclipse-temurin:17-jre'
    }
    to {
        image = "${DOCKER_REGISTRY_URL}/wedsnap"
        tags = ['latest', "${GIT_COMMIT_SHA}"]
        auth {
            username = "${DOCKER_USERNAME}"
            password = "${DOCKER_PASSWORD}"
        }
    }
    container {
        jvmFlags = ['-Xms512m', '-Xmx1024m']
        ports = ['8080']
        creationTime = 'USE_CURRENT_TIMESTAMP'
    }
}
```

### 2단계: Gitea Workflow 파일 작성

Gitea Actions의 워크플로우를 정의하는 YAML 파일을 작성합니다.

**파일 위치:** `.gitea/workflows/ci-cd.yaml`

**주요 구성:**
- **트리거 조건**: main 브랜치 푸시 시
- **CI Job**: 테스트 → 빌드 → 이미지 생성 및 푸시
- **CD Job**: SSH를 통한 원격 배포

**워크플로우 구조:**
```yaml
name: CI/CD Pipeline
on:
  push:
    branches: [main]
jobs:
  ci:
    # 테스트, 빌드, 이미지 푸시
  cd:
    # SSH 접속 후 컨테이너 재시작
```

### 3단계: Gitea Secrets 설정

민감한 정보는 Gitea Repository의 Secrets로 관리합니다.

**설정 위치:** Repository Settings → Secrets

**필요한 Secrets:**
- `DOCKER_REGISTRY_URL`: Synology Docker Registry 주소 (예: `registry.nas.local:5000`)
- `DOCKER_USERNAME`: Registry 인증 사용자명
- `DOCKER_PASSWORD`: Registry 인증 비밀번호
- `SSH_PRIVATE_KEY`: Synology SSH 접속용 Private Key
- `SSH_HOST`: Synology NAS IP 또는 호스트명
- `SSH_USER`: SSH 접속 사용자명

### 4단계: Synology 환경 준비

Synology NAS를 배포 타겟으로 준비합니다.

**필요한 설정:**
- Container Manager 설치 및 실행
- Docker Registry 활성화 (포트 5000)
- SSH 서비스 활성화
- Docker CLI 접근 권한 확인
- 배포 스크립트 준비 (선택 사항)

---

## 📋 필요한 구성 요소

### Gradle 의존성

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.6'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'com.google.cloud.tools.jib' version '3.4.5'  // 추가
}
```

### Gitea Secrets

| Secret 이름 | 설명 | 예시 |
|------------|------|------|
| `DOCKER_REGISTRY_URL` | Docker Registry 주소 | `192.168.1.100:5000` |
| `DOCKER_USERNAME` | Registry 사용자명 | `admin` |
| `DOCKER_PASSWORD` | Registry 비밀번호 | `********` |
| `SSH_PRIVATE_KEY` | SSH Private Key | `-----BEGIN RSA...` |
| `SSH_HOST` | Synology NAS 주소 | `192.168.1.100` |
| `SSH_USER` | SSH 사용자명 | `admin` |

### 파일 구조

```
WedSnap/
├── .gitea/
│   └── workflows/
│       └── ci-cd.yaml          # Gitea Actions 워크플로우
├── build.gradle                # Jib 설정 포함
├── docker-compose.yml          # (선택) 컨테이너 배포 설정
├── deploy/
│   └── deploy.sh              # (선택) Synology 배포 스크립트
└── CICD_PLAN.md               # 본 문서
```

---

## 🔄 배포 흐름 상세

### CI 단계 (Continuous Integration)

```
1. 코드 체크아웃
   ↓
2. Java 17 설정
   ↓
3. Gradle 캐시 설정
   ↓
4. 테스트 실행: ./gradlew test
   ↓
5. 빌드 실행: ./gradlew build
   ↓
6. Jib 이미지 빌드 및 푸시: ./gradlew jib
   ↓
   [Synology Docker Registry에 이미지 저장]
```

### CD 단계 (Continuous Deployment)

```
1. SSH Private Key 설정
   ↓
2. Synology NAS SSH 접속
   ↓
3. 최신 이미지 풀: docker pull [registry]/wedsnap:latest
   ↓
4. 기존 컨테이너 중지 및 삭제
   ↓
5. 새 이미지로 컨테이너 실행
   ↓
   [또는 docker-compose up -d --force-recreate]
   ↓
6. 배포 완료
```

---

## ✅ 작업 체크리스트

### Phase 1: 기본 설정 및 준비 (1-5)

- [x] **1. Jib 플러그인 추가 및 기본 설정**
  - [x] `build.gradle`에 Jib 플러그인 추가
  - [x] 베이스 이미지 설정 (eclipse-temurin:17-jre)
  - [x] 컨테이너 포트 설정 (8080)
  - [x] JVM 플래그 설정
  - [ ] 로컬에서 Jib 빌드 테스트

- [ ] **2. Synology Docker Registry 설정**
  - [ ] Synology Container Manager 설치 확인
  - [ ] Docker Registry 서비스 활성화
  - [ ] Registry 포트 설정 (기본 5000)
  - [ ] 인증 정보 생성 (사용자명/비밀번호)
  - [ ] 로컬에서 Registry 접속 테스트

- [ ] **3. Synology SSH 설정**
  - [ ] SSH 서비스 활성화
  - [ ] SSH 키 페어 생성 (로컬)
  - [ ] 공개키를 Synology에 등록
  - [ ] SSH 접속 테스트
  - [ ] Docker 명령어 실행 권한 확인

- [ ] **4. Gitea Secrets 등록**
  - [ ] Repository Settings 접속
  - [ ] `DOCKER_REGISTRY_URL` 등록
  - [ ] `DOCKER_USERNAME` 등록
  - [ ] `DOCKER_PASSWORD` 등록
  - [ ] `SSH_PRIVATE_KEY` 등록
  - [ ] `SSH_HOST` 등록
  - [ ] `SSH_USER` 등록

- [ ] **5. build.gradle에 Registry 연동 설정**
  - [ ] Jib `to.image` 설정 (Registry URL 포함)
  - [ ] 태그 전략 설정 (latest, commit SHA)
  - [ ] 인증 정보 설정 (환경 변수 참조)
  - [ ] 로컬에서 Registry 푸시 테스트

### Phase 2: CI/CD 워크플로우 작성 (6-10)

- [ ] **6. .gitea/workflows 디렉토리 생성**
  - [ ] `.gitea/workflows/` 디렉토리 생성
  - [ ] `ci-cd.yaml` 파일 생성

- [ ] **7. CI Job 작성**
  - [ ] 워크플로우 트리거 설정 (main 브랜치 push)
  - [ ] Job 이름 및 Runner 설정
  - [ ] 코드 체크아웃 Step
  - [ ] Java 17 설정 Step
  - [ ] Gradle 캐시 설정 Step
  - [ ] 테스트 실행 Step
  - [ ] 빌드 실행 Step
  - [ ] Jib 이미지 푸시 Step (Secrets 사용)

- [ ] **8. CD Job 작성**
  - [ ] CI Job 의존성 설정 (needs: ci)
  - [ ] SSH Key 설정 Step
  - [ ] Synology SSH 접속 및 배포 Step
  - [ ] Docker Pull Step
  - [ ] 컨테이너 재시작 Step

- [ ] **9. 워크플로우 로컬 검증**
  - [ ] YAML 문법 검증
  - [ ] 환경 변수 및 Secrets 참조 확인
  - [ ] Job 의존성 관계 확인

- [ ] **10. 워크플로우 커밋 및 푸시**
  - [ ] Git add `.gitea/workflows/ci-cd.yaml`
  - [ ] Git commit
  - [ ] Git push origin main

### Phase 3: 배포 스크립트 및 부가 설정 (11-15)

- [ ] **11. docker-compose.yml 작성 (선택)**
  - [ ] 서비스 정의 (wedsnap)
  - [ ] 이미지 설정 (Registry 이미지 참조)
  - [ ] 포트 매핑 (8080:8080)
  - [ ] 환경 변수 설정
  - [ ] 볼륨 마운트 설정 (필요 시)
  - [ ] 재시작 정책 설정

- [ ] **12. Synology 배포 스크립트 작성 (선택)**
  - [ ] `deploy/deploy.sh` 파일 생성
  - [ ] Docker Pull 명령 추가
  - [ ] 컨테이너 중지 및 삭제 로직
  - [ ] 새 컨테이너 실행 로직
  - [ ] 에러 핸들링 추가
  - [ ] 실행 권한 부여 (chmod +x)

- [ ] **13. 롤백 전략 수립**
  - [ ] 이전 이미지 태그 보관 정책
  - [ ] 롤백 스크립트 작성
  - [ ] 롤백 절차 문서화

- [ ] **14. 로깅 및 모니터링 설정**
  - [ ] 컨테이너 로그 설정
  - [ ] 애플리케이션 로그 볼륨 마운트
  - [ ] 헬스체크 엔드포인트 구현

- [ ] **15. 문서 작성**
  - [ ] CI/CD 구축 가이드 작성
  - [ ] 트러블슈팅 가이드 작성
  - [ ] README 업데이트

### Phase 4: 테스트 및 검증 (16-20)

- [ ] **16. CI 파이프라인 테스트**
  - [ ] 테스트 코드 작성 (간단한 테스트라도)
  - [ ] main 브랜치에 푸시하여 CI 트리거
  - [ ] Gitea Actions 로그 확인
  - [ ] 테스트 실행 결과 확인
  - [ ] 빌드 성공 확인
  - [ ] Registry에 이미지 푸시 확인

- [ ] **17. CD 파이프라인 테스트**
  - [ ] SSH 접속 성공 확인
  - [ ] Docker Pull 성공 확인
  - [ ] 컨테이너 재시작 확인
  - [ ] 애플리케이션 정상 동작 확인
  - [ ] 포트 접근 확인

- [ ] **18. 전체 플로우 통합 테스트**
  - [ ] 코드 수정 후 푸시
  - [ ] CI/CD 자동 실행 확인
  - [ ] 배포 완료까지 전 과정 모니터링
  - [ ] 배포된 애플리케이션 동작 확인

- [ ] **19. 에러 시나리오 테스트**
  - [ ] 테스트 실패 시 파이프라인 중단 확인
  - [ ] 빌드 실패 시 처리 확인
  - [ ] Registry 접속 실패 시 처리 확인
  - [ ] SSH 접속 실패 시 처리 확인

- [ ] **20. 최종 검증 및 문서화**
  - [ ] 모든 단계 정상 동작 확인
  - [ ] 파이프라인 실행 시간 측정
  - [ ] 발견된 이슈 문서화
  - [ ] 개선 사항 정리

### Phase 5: 최적화 및 고도화 (21-25)

- [ ] **21. 캐시 최적화**
  - [ ] Gradle 캐시 효율 확인
  - [ ] Docker 레이어 캐시 활용
  - [ ] 빌드 시간 최적화

- [ ] **22. 알림 설정 (선택)**
  - [ ] 빌드 실패 시 알림 설정
  - [ ] 배포 완료 시 알림 설정
  - [ ] Slack/Discord 웹훅 연동

- [ ] **23. 멀티 환경 지원 (선택)**
  - [ ] 개발/스테이징/프로덕션 환경 분리
  - [ ] 브랜치별 배포 전략 수립
  - [ ] 환경별 설정 관리

- [ ] **24. 보안 강화**
  - [ ] Secrets 로테이션 정책 수립
  - [ ] 이미지 스캔 추가 (Trivy 등)
  - [ ] 취약점 자동 점검

- [ ] **25. 백업 및 복구 전략**
  - [ ] 이미지 백업 정책
  - [ ] 설정 파일 백업
  - [ ] 재해 복구 절차 수립

---

## ⚠️ 주의사항

### 보안

1. **Secrets 관리**
   - SSH Private Key와 Registry 비밀번호는 반드시 Gitea Secrets로 관리
   - 절대 코드에 하드코딩하지 않기
   - .env 파일도 .gitignore에 추가

2. **네트워크 보안**
   - Gitea Runner가 Synology NAS에 접근 가능한 네트워크 구성 확인
   - 필요시 방화벽 규칙 추가
   - SSH는 키 기반 인증 사용

3. **권한 관리**
   - SSH 사용자가 Docker 명령 실행 권한 보유 확인
   - 최소 권한 원칙 적용
   - sudo 없이 Docker 사용 설정 (docker group)

### 성능

1. **빌드 시간**
   - Gradle 캐시 활용으로 빌드 시간 단축
   - 멀티 스테이지 빌드 고려
   - 불필요한 의존성 제거

2. **이미지 크기**
   - JRE 이미지 사용 (JDK 대신)
   - 불필요한 파일 제외
   - 레이어 최적화

### 롤백

1. **이미지 태그 전략**
   - latest 외에 commit SHA 태그도 유지
   - 문제 발생 시 이전 태그로 롤백 가능
   - 최소 3개 이상의 이전 이미지 보관

2. **롤백 절차**
   - 롤백 스크립트 사전 작성
   - 롤백 테스트 수행
   - 롤백 소요 시간 측정

---

## 📚 참고 자료

### Gitea Actions

- [Gitea Actions 공식 문서](https://docs.gitea.io/en-us/actions/)
- [GitHub Actions 호환성](https://docs.gitea.io/en-us/actions/comparison/)

### Jib

- [Jib Gradle Plugin 문서](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin)
- [Jib FAQ](https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md)

### Docker

- [Docker 공식 문서](https://docs.docker.com/)
- [Synology Docker 가이드](https://kb.synology.com/en-global/DSM/help/Docker/docker_desc)

### Spring Boot

- [Spring Boot Docker 가이드](https://spring.io/guides/gs/spring-boot-docker/)

---

## 📝 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2025-10-10 | 1.0 | 초안 작성 | - |

---

## 🚀 시작하기

본 계획서의 작업 체크리스트를 순서대로 진행하면 CI/CD 파이프라인을 구축할 수 있습니다.

**첫 단계부터 시작:**
1. Phase 1의 작업 1번부터 순차적으로 진행
2. 각 작업 완료 시 체크박스 체크
3. 문제 발생 시 주의사항 섹션 참고
4. 모든 Phase 완료 후 최종 검증 수행

**문의 사항:**
- 작업 중 문제가 발생하면 관련 문서 참고
- 필요시 팀원과 상의
