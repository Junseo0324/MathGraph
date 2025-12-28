# Math Graph Study - 제품 요구사항 문서 (PRD)

## 1. 프로젝트 개요
**Math Graph Study**는 사용자가 입력한 수학 함수(일차, 이차, 삼각함수 등)를 시각적인 그래프로 보여주는 안드로이드 애플리케이션입니다. 수학 학습이나 간단한 그래프 확인 용도로 사용될 수 있습니다.

## 2. 목표 (Goals)
- 사용자가 직관적으로 함수를 입력하고 그래프를 확인할 수 있어야 한다.
- 그래프는 부드럽게 렌더링되어야 하며, 확대/축소 및 이동(Zoom & Pan)이 가능해야 한다.
- 안드로이드 최신 기술 스택(Compose, Kotlin)을 활용하여 개발한다.

## 3. 타겟 사용자
- 수학 함수 그래프를 쉽게 확인하고 싶은 학생 및 사용자
- 안드로이드 그래픽스 및 차트 라이브러리 학습 목적의 개발자

## 4. 핵심 기능 (Features)

### 4.1 함수 입력 및 관리 (Function Management)
- **수식 직접 입력 (Expression Parsing)**:
    - 텍스트 필드에 수학 공식을 문자열로 직접 입력 (예: `sin(x) + x^2`, `2x + 1`).
    - **지원 연산자 및 함수**: `+`, `-`, `*`, `/`, `^`(제곱), `sin`, `cos`, `tan`, `log`, `ln`, `exp`, `sqrt`, `abs`.
    - **지원 상수**: `e`, `pi` (파이).
    - 암시적 곱셈 지원 (예: `2x` -> `2*x`).
- **다중 함수 지원**: 여러 개의 함수를 리스트로 추가하여 동시에 그래프에 표시.
- **함수 리스트 관리**:
    - 각 함수별 **색상 지정** (자동 또는 수동).
    - **Visible Toggle**: 눈 아이콘으로 특정 함수 숨기기/보이기 토글.
    - **삭제**: 리스트에서 함수 제거.

### 4.2 그래프 뷰어 (Graph Viewer)
- **Custom Canvas Rendering**:
    - 라이브러리(MPAndroidChart 등)를 사용하지 않고, **Compose Canvas**를 사용하여 직접 픽셀 단위로 드로잉 로직 구현.
    - **Grid & Axes**:
        - 동적인 격자(Grid) 그리기.
        - X축, Y축 강조 표시.
        - 줌 레벨에 따른 축 라벨(숫자) 동적 표시.
- **실시간 상호작용 (Interaction)**:
    - **Zoom**: Pinch 제스처로 스케일(Scale) 조절 (확대/축소).
    - **Pan**: Drag 제스처로 원점(Offset) 이동 (화면 이동).
    - 부드러운 렌더링을 위한 최적화 고려.

## 5. 기술 스택 (Tech Stack)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Math Engine**:
    - **Parser**: 문자열 수식을 파싱하여 계산할 수 있는 로직 구현 또는 라이브러리 사용 (예: `exp4j` 또는 직접 구현).
    - $y = f(x)$ 계산 로직.
- **Graphing Engine**:
    - **Jetpack Compose Canvas**: `DrawScope`를 활용해 선(Line), 텍스트(Text) 직접 그리기.
    - *Reference*: `figmaex/components/GraphCanvas.tsx`의 렌더링 로직(픽셀 루프 및 좌표 변환)을 Kotlin/Compose로 포팅.
- **Architecture**: MVVM (Model-View-ViewModel) + State Hoisting


## 6. UI/UX 디자인 가이드
- **Material Design 3** 적용.
- 심플하고 직관적인 레이아웃.
- **화면 상단**: 그래프 영역 (전체 화면의 60~70%).
- **화면 하단**: 컨트롤 패널 (함수 선택 및 입력).

## 7. 마일스톤 (Milestones)
1. **Phase 1 (MVP)**: 기본 일차/이차 함수 그리기, 기본적인 좌표 평면 구현.
2. **Phase 2**: 삼각함수 추가, Zoom/Pan 제스처 인터랙션 구현.
3. **Phase 3**: 다항함수 파싱 엔진 추가 (문자열 수식 입력 처리), UI 고도화.
