# Catch

Catch는 사용자가 기기 저장소의 동영상을 정리하고 재생할 수 있게 해주는 Android용 동영상 관리 애플리케이션입니다.

## 주요 기능

- **폴더 기반 관리:** 기기의 폴더를 앱 라이브러리에 추가하여 동영상을 관리합니다.
- **동영상 재생:** 원활한 시청 경험을 위한 커스텀 비디오 플레이어.
- **썸네일 미리보기:** 동영상 썸네일을 자동으로 생성하고 표시합니다.
- **수직 및 수평 페이징:** 폴더 간에는 수직으로, 폴더 내 동영상 간에는 수평으로 스와이프하여 탐색합니다.

## 사용된 기술

- **UI:** Jetpack Compose
- **비디오 재생:** ExoPlayer
- **데이터베이스:** Room
- **비동기 처리:** Kotlin Coroutines 및 Flow
- **권한 관리:** Accompanist Permissions

## 시작하기

앱을 빌드하고 실행하려면 Android Studio와 Android 기기 또는 에뮬레이터가 필요합니다.

1.  저장소를 복제합니다.
2.  Android Studio에서 프로젝트를 엽니다.
3.  앱을 빌드하고 실행합니다.

## 프로젝트 구조

- **`data`:** Room 데이터베이스 엔티티, DAO, 레포지토리 및 데이터베이스 설정을 포함합니다.
- **`ui`:** 모든 Jetpack Compose UI 컴포넌트, 화면 및 `VideoViewModel`을 포함합니다.
- **`utils`:** `ThumbnailExtractor`와 같은 유틸리티 클래스를 포함합니다.
- **`navigation`:** (해당하는 경우) Jetpack Navigation Compose를 사용한 내비게이션 설정을 포함합니다.
