# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 의사소통 지침

- **모든 대답과 설명은 반드시 한글로 작성해야 합니다.**
- **작업을 시작하기 전에 추가적인 정보나 명확한 설명이 필요한 경우, 반드시 사용자에게 질문하여 확인해야 합니다.**
- 불확실한 상태로 작업을 진행하지 말고, 항상 사용자의 의도를 정확히 파악한 후 작업을 수행하세요.

## 프로젝트 개요

WedSnap은 Java 17, Gradle, Thymeleaf 템플릿 엔진, Lombok을 사용하는 Spring Boot 3.5.6 웹 애플리케이션입니다.

**기본 패키지**: `me.agfe.wedsnap`

### 프로젝트 목적

WedSnap은 결혼식이나 행사에서 참석자들이 모바일 기기로 촬영한 사진을 손쉽게 수집하기 위한 웹 기반 이미지 업로드 서비스입니다. QR 코드 또는 링크를 통해 누구나 간편하게 접속하여 자신이 찍은 사진을 업로드할 수 있으며, 업로드된 사진은 Synology NAS의 지정된 폴더에 사용자 이름별로 구조화되어 저장됩니다.

### 핵심 기능

1. **QR 코드/링크 접속**
    - 행사 참석자들이 QR 코드를 스캔하거나 링크를 클릭하여 모바일 기기에서 바로 접속
    - 별도의 앱 설치 없이 웹 브라우저를 통한 접속

2. **사용자 정보 입력**
    - 업로드하는 사람의 이름을 입력할 수 있는 인풋 박스 제공
    - 이름은 NAS 폴더 생성 시 사용됨

3. **이미지 선택 및 업로드**
    - 모바일 기기의 갤러리에서 이미지 선택 (단일/다중 선택 지원 예정)
    - 선택한 이미지를 Synology NAS로 업로드

4. **자동 폴더 관리**
    - 입력한 이름으로 NAS의 지정된 경로에 폴더 자동 생성
    - 동일한 이름의 폴더가 이미 존재할 경우 순차적으로 숫자 부여 (예: `홍길동`, `홍길동_2`, `홍길동_3`)
    - 각 사용자 폴더에 업로드한 이미지 저장

5. **업로드 상태 피드백**
    - 업로드 진행 상황 표시
    - 성공/실패 메시지 제공

## 빌드 및 개발 명령어

### 프로젝트 빌드

```bash
# Windows
gradlew build

# Unix/Linux/Mac
./gradlew build
```

### 애플리케이션 실행

```bash
# Windows
gradlew bootRun

# Unix/Linux/Mac
./gradlew bootRun
```

### 테스트 실행

```bash
# 모든 테스트 실행
gradlew test

# 특정 테스트 클래스 실행
gradlew test --tests me.agfe.wedsnap.WedSnapApplicationTests

# 패턴으로 테스트 실행
gradlew test --tests "*ControllerTests"
```

### 기타 유용한 명령어

```bash
# 빌드 산출물 정리
gradlew clean

# 의존성 업데이트 확인
gradlew dependencyUpdates

# 프로젝트 리포트 생성
gradlew build --scan
```

## 프로젝트 구조

```
src/
├── main/
│   ├── java/me/agfe/wedsnap/     # 애플리케이션 소스 코드
│   │   └── WedSnapApplication.java # Spring Boot 메인 클래스
│   └── resources/
│       ├── static/                # 정적 자원 (CSS, JS, 이미지)
│       ├── templates/             # Thymeleaf 템플릿
│       └── application.properties # 애플리케이션 설정 파일
└── test/
    └── java/me/agfe/wedsnap/      # 테스트 클래스
```

## 주요 기술 스택

- **Spring Boot 3.5.6**: 메인 프레임워크 (Java 17+ 필요)
- **Thymeleaf**: 웹 뷰를 위한 서버 사이드 템플릿 엔진
- **Lombok**: 보일러플레이트 코드 생성을 위한 애노테이션 프로세서 (@Data, @Getter, @Setter 등)
- **JUnit Platform**: 테스트 프레임워크

## 아키텍처 가이드

이 프로젝트는 표준 Spring Boot MVC 애플리케이션입니다. 새로운 기능을 추가할 때:

- **컨트롤러(Controllers)**: 기본 패키지 또는 `controller` 서브패키지에 배치
- **서비스(Services)**: 비즈니스 로직을 위한 `service` 서브패키지에 배치
- **모델/엔티티(Models/Entities)**: `model` 또는 `entity` 서브패키지에 배치
- **리포지토리(Repositories)**: 데이터베이스 지원 추가 시 `repository` 서브패키지에 배치
- **Thymeleaf 템플릿**: `src/main/resources/templates/`에 배치
- **정적 리소스** (CSS/JS/이미지): `src/main/resources/static/`에 배치

### 주요 컴포넌트 구조

#### 1. 컨트롤러 레이어

- `UploadController`: 업로드 페이지 표시 및 파일 업로드 처리
- `QRCodeController` (선택): QR 코드 생성 엔드포인트

#### 2. 서비스 레이어

- `FileUploadService`: 파일 업로드 비즈니스 로직 처리
    - 파일 검증 (타입, 크기)
    - NAS 업로드 처리
    - 폴더 생성 및 중복 처리
- `NasStorageService`: Synology NAS 연동
    - NAS 연결 관리
    - 파일 전송
    - 폴더 존재 여부 확인 및 생성
- `QRCodeService` (선택): QR 코드 생성

#### 3. DTO/모델

- `UploadRequest`: 업로드 요청 정보 (이름, 파일)
- `UploadResponse`: 업로드 결과 정보

#### 4. 설정 클래스

- `NasConfig`: NAS 연결 설정 관리
- `FileUploadConfig`: 파일 업로드 설정 (크기 제한, 허용 타입 등)

## 의존성

현재 포함된 의존성:

- `spring-boot-starter-web` - REST 및 MVC 지원
- `spring-boot-starter-thymeleaf` - 템플릿 엔진
- `lombok` - 코드 생성 (애노테이션 처리 필요)
- `spring-boot-starter-test` - JUnit 5를 포함한 테스트 유틸리티

새로운 의존성을 추가할 때는 `build.gradle`의 `dependencies` 블록을 수정하세요.

### 추가 예정 의존성

구현 진행 시 다음 의존성 추가가 필요할 수 있습니다:

- **Synology NAS 연동**: WebDAV 클라이언트 라이브러리 (예: Apache HttpClient, Sardine) 또는 JCIFS-ng (SMB)
- **QR 코드 생성**: ZXing (Zebra Crossing) 라이브러리
- **파일 업로드**: Spring Boot의 기본 `MultipartFile` 지원 (추가 설정 필요)
- **이미지 처리**: Apache Commons FileUpload, ImageIO (기본 제공)

## API 엔드포인트

### 주요 엔드포인트

#### `GET /`

- **설명**: 메인 업로드 페이지 표시
- **응답**: Thymeleaf 템플릿 렌더링 (이름 입력, 파일 선택 UI)
- **템플릿**: `templates/upload.html`

#### `POST /upload`

- **설명**: 이미지 파일 업로드 처리
- **요청 파라미터**:
    - `userName` (String): 업로드하는 사용자의 이름
    - `files` (MultipartFile[]): 업로드할 이미지 파일(들)
- **응답**:
    - 성공 시: 업로드 성공 메시지 및 저장된 파일 정보
    - 실패 시: 에러 메시지 및 상태 코드
- **처리 흐름**:
    1. 입력 검증 (이름 존재 여부, 파일 존재 여부)
    2. 파일 타입 및 크기 검증
    3. NAS 폴더 존재 확인 및 생성 (중복 시 번호 부여)
    4. 파일 NAS 업로드
    5. 결과 응답

#### `GET /qr` (선택 사항)

- **설명**: 업로드 페이지 QR 코드 생성
- **요청 파라미터**:
    - `url` (선택): QR 코드로 인코딩할 URL (기본값: 서버의 루트 URL)
- **응답**: QR 코드 이미지 (PNG)

## 환경 설정

### application.properties 필수 설정

```properties
# 애플리케이션 설정
spring.application.name=WedSnap
server.port=8080
# 파일 업로드 설정
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
# Synology NAS 연결 설정 (TODO: 구체적인 연결 방식 결정 필요)
nas.host=192.168.x.x
nas.port=5000
nas.protocol=webdav
nas.username=${NAS_USERNAME}
nas.password=${NAS_PASSWORD}
nas.base-path=/photos/wedsnap
# 파일 업로드 설정
upload.allowed-extensions=jpg,jpeg,png,heic,gif
upload.max-file-size=10485760
upload.folder-name-pattern={userName}
```

### 환경 변수

민감한 정보는 환경 변수로 관리하는 것을 권장합니다:

```bash
export NAS_USERNAME="your_nas_username"
export NAS_PASSWORD="your_nas_password"
```

또는 `.env` 파일 사용 (프로덕션 환경에서 권장):

```
NAS_USERNAME=your_nas_username
NAS_PASSWORD=your_nas_password
```

**중요**: `.env` 파일은 `.gitignore`에 추가하여 버전 관리에서 제외해야 합니다.

## 보안 고려사항

### 1. 파일 검증

- **파일 타입 검증**: MIME 타입 확인 및 허용된 확장자만 수락
- **파일 크기 제한**: 개별 파일 및 전체 요청 크기 제한
- **악성 파일 방지**: 파일 내용 검증 (매직 넘버 확인)

### 2. 입력 검증

- **이름 검증**: 특수 문자 제한, 길이 제한
- **경로 인젝션 방지**: 이름에 경로 구분자(`/`, `\`, `..`) 포함 방지
- **XSS 방지**: 사용자 입력 이스케이프 처리

### 3. NAS 연결 보안

- **인증 정보 암호화**: 설정 파일에 평문 저장 금지
- **환경 변수 사용**: 민감한 정보는 환경 변수로 관리
- **연결 타임아웃**: 장시간 대기 방지
- **재시도 로직**: 일시적 네트워크 오류 처리

### 4. 접근 제어

- **CORS 설정**: 필요 시 특정 도메인만 허용
- **Rate Limiting**: 무분별한 업로드 방지 (선택 사항)
- **세션 관리**: 필요 시 세션 기반 업로드 제한

## 업로드 흐름

```
1. 사용자가 QR 코드 스캔 또는 링크 클릭
   ↓
2. 업로드 페이지 접속 (GET /)
   - 이름 입력 폼 표시
   - 이미지 선택 버튼 표시
   ↓
3. 사용자가 이름 입력 및 이미지 선택
   ↓
4. "업로드" 버튼 클릭
   ↓
5. 서버로 POST /upload 요청
   ↓
6. 서버 측 처리:
   a. 입력 검증 (이름, 파일)
   b. 파일 검증 (타입, 크기)
   c. NAS 연결
   d. 폴더 존재 확인
      - 존재하지 않으면: 새 폴더 생성
      - 존재하면: 순차 번호 부여 (예: 홍길동_2)
   e. 파일 업로드
   ↓
7. 결과 응답
   - 성공: 성공 메시지 및 업로드된 파일 정보
   - 실패: 에러 메시지
```

## NAS 폴더 구조

```
/photos/wedsnap/              # NAS 기본 경로 (설정 가능)
├── 홍길동/                    # 사용자 이름으로 생성된 폴더
│   ├── IMG_001.jpg
│   ├── IMG_002.jpg
│   └── IMG_003.jpg
├── 홍길동_2/                  # 중복 이름 처리 (순차 번호)
│   ├── IMG_004.jpg
│   └── IMG_005.jpg
├── 김철수/
│   └── IMG_006.jpg
└── ...
```

### 폴더 명명 규칙

- 기본: `{사용자_이름}`
- 중복 시: `{사용자_이름}_{순차번호}`
- 순차 번호는 2부터 시작
- 공백 및 특수문자 처리 규칙 (TODO: 정책 결정 필요)

## 구현 예정 및 TODO 항목

### 우선순위 높음

- [ ] **NAS 연동 방식 결정**: WebDAV, SMB/CIFS, FTP 중 선택
- [ ] **NAS 연결 라이브러리 선택 및 통합**
- [ ] **업로드 컨트롤러 구현**
- [ ] **파일 업로드 서비스 로직 구현**
- [ ] **폴더 중복 처리 로직 구현**
- [ ] **Thymeleaf 업로드 페이지 작성**
- [ ] **모바일 반응형 UI 구현**

### 우선순위 중간

- [ ] **QR 코드 생성 기능**: 라이브러리 선택 (ZXing 등)
- [ ] **파일 검증 강화**: MIME 타입, 매직 넘버 확인
- [ ] **업로드 진행률 표시**: JavaScript 프로그레스 바
- [ ] **에러 처리 및 사용자 피드백 개선**
- [ ] **다중 파일 업로드 지원**

### 우선순위 낮음 (향후 고려)

- [ ] **업로드 내역 저장**: 데이터베이스 통합
- [ ] **관리자 페이지**: 업로드 통계, 사용자 관리
- [ ] **이미지 썸네일 생성**: 미리보기 기능
- [ ] **이미지 메타데이터 추출**: EXIF 정보 저장
- [ ] **업로드 제한**: IP별, 세션별 제한
- [ ] **이미지 자동 회전**: EXIF Orientation 처리

### 기술적 결정 필요

- [ ] **NAS 연결 방식**: WebDAV vs SMB vs FTP?
- [ ] **이미지 파일 형식**: HEIC 지원 여부?
- [ ] **파일명 처리**: 원본 파일명 유지 vs UUID 생성?
- [ ] **동시 업로드 처리**: 동시성 제어 필요 여부?
- [ ] **사용자 이름 중복 정책**: 숫자 부여 vs 타임스탬프 추가?

## 개발 참고사항

- 이 프로젝트는 Lombok을 사용하므로 IDE에 Lombok 플러그인이 설치되어 있고 애노테이션 처리가 활성화되어 있는지 확인하세요
- 애플리케이션은 기본적으로 포트 8080에서 실행됩니다 (`application.properties`에서 설정 가능)
- Java 툴체인은 버전 17로 설정되어 있습니다 - JDK 17 이상이 설치되어 있어야 합니다

## 협업 및 배포 가이드

WedSnap 프로젝트의 Git 워크플로우 및 CI/CD 파이프라인에 대한 상세 내용은 아래 문서를 참고하세요:

### Git 워크플로우

- **문서**: `GIT_FLOW.md`
- **내용**: Git 브랜치 전략, 커밋 컨벤션, Pull Request 프로세스 등

### CI/CD 파이프라인

- **문서**: `CICD.md`
- **내용**: GitHub Actions 기반 자동화, Docker 배포, 환경별 설정 등
