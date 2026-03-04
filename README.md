# GPS Attendance (GPS 기반 자동 출퇴근 관리 시스템)

본 프로젝트는 사용자의 GPS 위치를 기반으로 **지정된 근무지 반경 진입/이탈 시 자동으로 출퇴근을 기록**하고, 팀 내에서 **위치 정보를 공유**할 수 있는 서비스입니다.

## 🚀 핵심 기능

### 1. 위치 기반 자동 출퇴근 (Geofencing)
- **자동 출근**: 사용자가 설정된 근무지 반경(예: 200m) 내로 진입하면 별도의 조작 없이 자동으로 출근 처리됩니다.
- **자동 퇴근**: 사용자가 근무지 반경을 벗어나면 자동으로 퇴근 처리됩니다.
- **퇴근 유예 시간 (Grace Period)**: GPS 신호 불안정으로 인해 일시적으로 반경을 벗어나는 경우를 대비하여, 설정된 시간(예: 5분) 이상 반경 밖에 머물 때만 퇴근으로 확정합니다.

### 2. 팀 및 조직 관리
- **팀 계층 구조**: 회사-부서-팀으로 이어지는 계층 구조를 지원합니다.
- **근무 정책 설정**: 팀별로 근무지 좌표, 출근 허용 반경, 퇴근 유예 시간을 다르게 설정할 수 있습니다.

### 3. 위치 정보 공유 (Social & Management)
- **팀 리더 뷰**: 팀 리더는 관리 중인 팀원들의 실시간 위치 및 출근 여부를 지도에서 확인할 수 있습니다.
- **친한 동료 공유**: 서로 위치 공유를 허용한 동료끼리는 실시간 위치를 공유하여 협업 및 친목 도모에 활용할 수 있습니다.

## 📚 핵심 기술 명세 요약 (Documentation Summary)

이 프로젝트의 주요 설계 및 기술 전략입니다. 상세 내용은 링크된 문서를 참조하세요.

1. **[DB 설계 초안 (DATABASE.md)](docs/DATABASE.md)**
   - **핵심**: 자동 퇴근 유예(`outside_since`) 및 위치 공유 권한(`permission_type`) 처리.
   - **구조**: 사용자 최신 위치 캐싱을 통한 실시간 팀원 지도 조회 최적화.

2. **[시스템 설계 리뷰 (SYSTEM_DESIGN_REVIEW.md)](docs/SYSTEM_DESIGN_REVIEW.md)**
   - **핵심**: **'삼중 체크(Triple Check)'** 인증 (GPS + 사내 Wi-Fi + 비콘/PC).
   - **보안**: GPS 위변조 방지 및 프라이버시 보호(업무 시간 외 추적 차단) 가이드.

3. **[백그라운드 위치 전략 (BACKGROUND_LOCATION_STRATEGY.md)](docs/BACKGROUND_LOCATION_STRATEGY.md)**
   - **핵심**: 앱 종료 상태에서도 작동하는 **지오펜싱(Geofencing)** 기술.
   - **해결**: 배터리 소모 최소화 및 OS 레벨의 위치 권한 획득 전략.

---

## 🛠 기술 스택

- **Backend**: Java 17, Spring Boot 3.x, Spring Data JPA, QueryDSL
- **Frontend**: Next.js 14 (App Router), Tailwind CSS
- **Mobile**: Android (Kotlin/Compose), iOS (Swift/SwiftUI)
- **Database**: PostgreSQL (PostGIS 확장 고려), Redis (위치 캐싱)
- **Infra**: Docker, Docker Compose

---

## 2) 빠른 실행 순서

1. DB/Redis 실행
2. Backend 실행 (Gradle)
3. Frontend 실행 (선택)
4. Android 또는 iOS 앱 실행

---

## 3) Backend 실행 (Gradle)
... (기존 내용과 동일) ...

`backend`는 Maven이 아니라 **Gradle Wrapper**를 사용합니다.

### 3-1. 인프라(PostgreSQL, Redis) 실행

```bash
cd backend
docker compose up -d
```

기본 포트:
- PostgreSQL: `5432`
- Redis: `6379`

### 3-2. 서버 실행

Windows:
```bash
cd backend
.\gradlew.bat bootRun
```

macOS/Linux:
```bash
cd backend
./gradlew bootRun
```

서버 주소:
- `http://localhost:8080`

### 3-3. 테스트 실행

Windows:
```bash
cd backend
.\gradlew.bat test
```

macOS/Linux:
```bash
cd backend
./gradlew test
```

### 3-4. 주요 환경변수 (선택)

`backend/src/main/resources/application.yml` 기준 기본값:
- `DB_HOST=localhost`
- `DB_PORT=5432`
- `DB_NAME=gps_attendance`
- `DB_SCHEMA=attendance`
- `DB_USERNAME=gps_user`
- `DB_PASSWORD=gps_pass`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`

---

## 4) Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

웹 주소:
- `http://localhost:3000`

---

## 5) Android 실행

1. Android Studio에서 `android-app` 열기
2. Gradle Sync
3. 백엔드 실행 상태 확인
4. 에뮬레이터 또는 실기기에서 앱 실행

기본 API 주소:
- `android-app/app/build.gradle.kts`의 `BASE_URL`
- 기본값: `http://10.0.2.2:8080/` (에뮬레이터 기준)

카카오 지도 키:
- `KAKAO_NATIVE_APP_KEY`를 `gradle.properties`에 설정 필요

---

## 6) iOS 실행

`ios-app`은 SwiftUI 기반 스캐폴드이며, macOS에서만 실행 가능합니다.

```bash
cd ios-app
brew install xcodegen
xcodegen generate
```

생성된 `GPSAttendanceiOS.xcodeproj`를 Xcode로 열어 실행하세요.

참고:
- 지도 UI는 시뮬레이터에서도 확인 가능
- 실제 GPS 정확도/백그라운드 위치 검증은 실기기(iPhone) 권장

---

## 7) 현재 API 경로

백엔드 컨트롤러 기준:
- `POST /api/v1/users/register`
- `POST /api/v1/users/login`
- `POST /api/v1/users/refresh`
- `POST /api/v1/attendance/me/location`
- `GET /api/v1/attendance/me/sessions`
- `GET /api/v1/attendance/visible-sessions`
- `GET /api/v1/teams`
- `POST /api/v1/teams`
- `POST /api/v1/teams/work-policies`
