package com.devhjs.mathgraphstudy.presentation.math

import com.devhjs.mathgraphstudy.domain.model.math.*
 
/**
 * 수식 입력기(Math Input)의 상태를 보유하는 데이터 클래스입니다.
 *
 * @property rootNode 현재 작성된 수식의 전체 시각적 트리 구조 (Visual AST)
 * @property focusPath 현재 커서(포커스)가 위치한 노드를 가리키는 인덱스 경로 (예: [0, 1] -> root의 왼쪽 자식의 오른쪽 자식)
 */
 data class MathInputState(
     val rootNode: VisualMathNode = PlaceholderNode,
     val focusPath: List<Int> = emptyList() // Indices from root to focused node
 )
