package com.devhjs.mathgraphstudy.presentation.math

import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NegateNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.ParenNode
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.PowerNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator

/**
 * 사용자 입력(숫자, 연산자, 함수 등)을 처리하고 [MathInputState]를 업데이트하는 매니저 클래스입니다.
 *
 * 주요 기능:
 * 1. 입력된 키에 따라 현재 포커스 된 노드를 찾고, 적절한 [VisualMathNode] 구조로 변경합니다.
 * 2. 숫자, 연산자, 함수, 변수, 괄호 입력을 처리합니다.
 * 3. 커서 이동(왼쪽/오른쪽) 및 삭제(Backspace) 로직을 수행합니다.
 * 4. 연산자 우선순위(Precedence Climbing)를 고려하여 트리를 재구성합니다.
 */
object MathInputManager {

    const val INPUT_PAREN = "()"
    const val INPUT_LEFT = "←"
    const val INPUT_RIGHT = "→"
    const val INPUT_DELETE = "DEL"

    // 단항 마이너스는 곱셈과 같은 우선순위로 취급 (-2x + 1 에서 + 가 부호 밖으로 나가도록)
    private const val NEGATE_PRECEDENCE = 2

    /**
      * 포커스 경로([focusPath])를 변경하여 커서 위치를 업데이트합니다.
      * 사용자가 특정 노드를 클릭했을 때 호출됩니다.
      */
     fun onFocusChange(state: MathInputState, newPath: List<Int>): MathInputState {
         return state.copy(focusPath = newPath)
     }

    /**
      * 사용자의 키 입력을 받아 상태를 갱신합니다.
      * 입력 종류(숫자, 연산자, 함수, 변수, 괄호, 이동, 삭제)를 감지하여 적절한 처리 함수로 분기합니다.
      */
     fun processInput(state: MathInputState, input: String): MathInputState {
         // 1. Digits
         if (input.all { it.isDigit() || it == '.' }) {
             return handleDigitInput(state, input)
         }

         // 2. Operators
         val op = MathOperator.values().find { it.symbol == input }
         if (op != null) {
             return handleOperatorInput(state, op)
         }

         // 3. Functions
         val func = MathFunction.values().find { it.symbol == input }
         if (func != null) {
              return handleFunctionInput(state, func)
         }

        // 4. Variables
        if (input == "x" || input == "e" || input == "pi" || input in Parameter.NAMES) {
            return handleVariableInput(state, input)
        }

        // 5. Parenthesis
        if (input == INPUT_PAREN) {
            return insertValueNode(state, ParenNode(PlaceholderNode), focusChild = true)
        }

        // 6. Navigation
        if (input == INPUT_RIGHT || input == "RIGHT") {
             return moveFocusRight(state)
        }
        if (input == INPUT_LEFT || input == "LEFT") {
             return moveFocusLeft(state)
        }

        // 7. Delete
        if (input == INPUT_DELETE || input == "⌫") {
             return handleDelete(state)
        }

        return state
    }

    /**
     * 삭제(Backspace) 키 입력을 처리합니다.
     *
     * 1. 숫자가 여러 자리인 경우 마지막 숫자를 지웁니다.
     * 2. 숫자나 변수가 하나만 남은 경우 Placeholder로 변경합니다.
     * 3. 함수나 연산자처럼 구조를 가진 노드가 선택된 경우, 해당 구조를 제거하고 Placeholder로 되돌립니다.
     * 4. 빈 칸(Placeholder)에서 지우면 방금 입력한 구조를 되돌립니다. (예: "2 + ?" -> "2")
     */
    private fun handleDelete(state: MathInputState): MathInputState {
        val (root, path) = state
        val targetNode = findNode(root, path) ?: return state

        return when (targetNode) {
            is NumberNode -> {
                if (targetNode.value.length > 1) {
                    val newValue = targetNode.value.dropLast(1)
                    val newRoot = replaceNode(root, path, NumberNode(newValue))
                    state.copy(rootNode = newRoot)
                } else {
                    // Became empty -> Placeholder
                    val newRoot = replaceNode(root, path, PlaceholderNode)
                    state.copy(rootNode = newRoot)
                }
            }
            is VariableNode -> {
                 // Var -> Placeholder
                 val newRoot = replaceNode(root, path, PlaceholderNode)
                 state.copy(rootNode = newRoot)
            }
            is FunctionNode, is BinaryOpNode, is PowerNode, is ParenNode, is NegateNode -> {
                // If the entire function/op structure is focused, delete it
                val newRoot = replaceNode(root, path, PlaceholderNode)
                state.copy(rootNode = newRoot)
            }
            PlaceholderNode -> deleteEnclosingStructure(state)
        }
    }

    /**
     * 빈 칸에서 삭제했을 때, 그 빈 칸을 만든 부모 구조를 제거합니다.
     * 이항 연산/거듭제곱은 남은 쪽 자식으로, 함수/괄호/부호는 빈 칸으로 되돌립니다.
     */
    private fun deleteEnclosingStructure(state: MathInputState): MathInputState {
        val path = state.focusPath
        if (path.isEmpty()) return state

        val lastIndex = path.last()
        val parentPath = path.dropLast(1)
        val parentNode = findNode(state.rootNode, parentPath) ?: return state

        val replacement = when (parentNode) {
            is BinaryOpNode -> if (lastIndex == 1) parentNode.left else parentNode.right
            is PowerNode -> if (lastIndex == 1) parentNode.base else parentNode.exponent
            else -> PlaceholderNode
        }
        return state.copy(
            rootNode = replaceNode(state.rootNode, parentPath, replacement),
            focusPath = parentPath
        )
    }

    /**
     * 오른쪽 화살표(→) 입력을 처리하여 포커스를 이동합니다.
     *
     * 현재 노드의 구조(BinaryOp, Function, Power)를 파악하여,
     * 자식 노드 간의 이동(예: 왼쪽 -> 오른쪽)이나 부모 노드로의 탈출을 수행합니다.
     */
    private fun moveFocusRight(state: MathInputState): MathInputState {
        val path = state.focusPath
        if (path.isEmpty()) return state // Already at root

        val lastIndex = path.last()
        val parentPath = path.dropLast(1)

        // Find parent node to know structure
        val parentNode = findNode(state.rootNode, parentPath) ?: return state

        return when (parentNode) {
            is BinaryOpNode, is PowerNode -> {
                if (lastIndex == 0) {
                    // Left/Base -> Right/Exponent
                    state.copy(focusPath = parentPath + 1)
                } else {
                    // Right/Exponent -> Parent/Exit
                    state.copy(focusPath = parentPath)
                }
            }
            is FunctionNode, is ParenNode, is NegateNode -> {
                // Arg(0) -> Parent/Exit
                state.copy(focusPath = parentPath)
            }
            else -> state // Should not happen if path is valid
        }
    }

    /**
     * 왼쪽 화살표(←) 입력을 처리하여 포커스를 이동합니다.
     *
     * 오른쪽 자식에서는 왼쪽 자식으로, 왼쪽(또는 유일한) 자식에서는 부모 노드로 이동합니다.
     */
    private fun moveFocusLeft(state: MathInputState): MathInputState {
        val path = state.focusPath
        if (path.isEmpty()) return state // Already at root

        val parentPath = path.dropLast(1)
        val parentNode = findNode(state.rootNode, parentPath) ?: return state

        return if ((parentNode is BinaryOpNode || parentNode is PowerNode) && path.last() == 1) {
            // Right/Exponent -> Left/Base
            state.copy(focusPath = parentPath + 0)
        } else {
            state.copy(focusPath = parentPath)
        }
    }

    /**
      * 숫자(0-9, .) 입력을 처리합니다.
      * 현재 포커스 된 노드가 Placeholder이면 숫자로 교체하고,
      * 이미 숫자 노드라면 뒤에 숫자를 이어 붙입니다.
      */
     private fun handleDigitInput(state: MathInputState, digit: String): MathInputState {
         val (root, path) = state
         val targetNode = findNode(root, path)

         val newNode = when (targetNode) {
             is PlaceholderNode -> NumberNode(digit)
             is NumberNode -> {
                 // 소수점은 한 번만 허용
                 if (digit.contains('.') && targetNode.value.contains('.')) return state
                 NumberNode(targetNode.value + digit)
             }
             else -> return state // Cannot append digit to Op or Func directly without explicit focus logic
         }

         val newRoot = replaceNode(root, path, newNode)
         return state.copy(rootNode = newRoot)
     }

    /**
     * 연산자(+, -, *, /, ^) 입력을 처리합니다.
     *
     * 빈 칸에서 "-"를 누르면 뺄셈이 아니라 음수 부호([NegateNode])로 처리합니다.
     *
     * **Precedence Climbing (우선순위 상승)**:
     * 현재 위치에서 상위 노드로 거슬러 올라가며 연산자 우선순위를 비교합니다.
     * 더 낮은 우선순위의 연산자가 나올 때까지 올라간 뒤, 새로운 연산자 노드로 감싸서 트리를 재구성합니다.
     * 이를 통해 `2 * x + 1`과 같은 식이 올바른 연산 순서를 가지게 됩니다.
     */
    private fun handleOperatorInput(state: MathInputState, op: MathOperator): MathInputState {
        val (root, path) = state

        var currentPath = path
        var targetNode = findNode(root, currentPath) ?: return state

        if (targetNode is PlaceholderNode) {
            // 빈 칸에는 "-"(부호)만 입력 가능. 그 외 연산자는 왼쪽 피연산자가 없으므로 무시
            if (op != MathOperator.MINUS) return state
            val newRoot = replaceNode(root, path, NegateNode(PlaceholderNode))
            return state.copy(rootNode = newRoot, focusPath = path + 0)
        }

        // Climb up while parent has higher/equal precedence.
        // Function/Paren 노드는 경계 역할을 하므로 넘어가지 않음 (탈출은 "→" 로 명시적으로)
        while (currentPath.isNotEmpty()) {
            val parentPath = currentPath.dropLast(1)
            val parentPrec = when (val parentNode = findNode(root, parentPath)) {
                is BinaryOpNode -> parentNode.op.precedence
                is NegateNode -> NEGATE_PRECEDENCE
                else -> break
            }

            // Left-associative for +, -, *, /
            if (parentPrec >= op.precedence) {
                currentPath = parentPath
                targetNode = findNode(root, parentPath) ?: return state
                continue
            }
            break
        }

        // Wrap current node
        val newNode = if (op == MathOperator.POWER) {
             PowerNode(base = targetNode, exponent = PlaceholderNode)
        } else {
             BinaryOpNode(
                 left = targetNode,
                 op = op,
                 right = PlaceholderNode
             )
        }

        val newRoot = replaceNode(root, currentPath, newNode)
        val newPath = currentPath + 1 // Focus moves to Right child

        return state.copy(rootNode = newRoot, focusPath = newPath)
    }

    /**
      * 함수(sin, cos 등) 입력을 처리합니다.
      * 함수 노드(`FunctionNode`)를 삽입하고, 함수의 인자(Placeholder)로 포커스를 이동합니다.
      */
     private fun handleFunctionInput(state: MathInputState, func: MathFunction): MathInputState {
         return insertValueNode(state, FunctionNode(func, PlaceholderNode), focusChild = true)
     }

    /**
     * 변수(x, e, pi) 입력을 처리합니다.
     */
    private fun handleVariableInput(state: MathInputState, name: String): MathInputState {
        return insertValueNode(state, VariableNode(name), focusChild = false)
    }

    /**
     * 값 노드(변수, 함수, 괄호)를 현재 위치에 삽입합니다.
     *
     * 1. 현재 노드가 빈 칸이면 그 자리를 새 노드로 교체합니다.
     * 2. 현재 노드가 이미 값(숫자, 변수, 함수 등)이면 암시적 곱셈을 적용합니다. (예: `3` + `x` -> `3 * x`)
     * 3. 그 외(이항 연산 전체가 선택된 경우 등)에는 입력을 무시합니다.
     *
     * @param focusChild true면 새 노드의 첫 번째 자식(함수 인자, 괄호 내부)으로 포커스를 이동합니다.
     */
    private fun insertValueNode(state: MathInputState, newNode: VisualMathNode, focusChild: Boolean): MathInputState {
        val (root, path) = state
        val targetNode = findNode(root, path) ?: return state

        val (replacement, newNodePath) = when {
            targetNode is PlaceholderNode -> newNode to path
            canMultiplyImplicitly(targetNode) -> {
                // Implicit Multiplication: 3 -> 3*x
                BinaryOpNode(left = targetNode, op = MathOperator.MULTIPLY, right = newNode) to path + 1
            }
            else -> return state
        }

        val newRoot = replaceNode(root, path, replacement)
        val newPath = if (focusChild) newNodePath + 0 else newNodePath
        return state.copy(rootNode = newRoot, focusPath = newPath)
    }

    /** 뒤에 값을 붙였을 때 곱셈으로 해석할 수 있는 완성된 값 노드인지 확인합니다. */
    private fun canMultiplyImplicitly(node: VisualMathNode): Boolean =
        node is NumberNode || node is VariableNode || node is FunctionNode ||
            node is ParenNode || node is PowerNode

    /**
     * 기존 수식을 불러와 편집할 때 커서를 둘 "수식 끝" 위치의 경로를 반환합니다.
     *
     * 이항 연산은 오른쪽으로 따라 내려가되, 함수/괄호/거듭제곱/부호는 안으로 들어가지 않고 그 노드 전체를 선택합니다.
     * 예: "2x + sin x" 에서는 `sin x` 전체가 선택되어, 바로 "+ 1" 을 입력하면 "2x + sin x + 1" 이 됩니다.
     * (숫자/변수가 끝이면 그 노드가 선택되어 바로 이어서 입력하거나 지울 수 있습니다)
     */
    fun endOfExpressionPath(node: VisualMathNode): List<Int> {
        return when (node) {
            is BinaryOpNode -> listOf(1) + endOfExpressionPath(node.right)
            else -> emptyList()
        }
    }

     // --- AST Helper---

    /**
      * 주어진 경로([path])를 따라 트리를 순회하여 대상 노드를 찾습니다.
      */
     private fun findNode(root: VisualMathNode, path: List<Int>): VisualMathNode? {
         if (path.isEmpty()) return root
         val index = path.first()
         val remainder = path.drop(1)

         return when (root) {
             is BinaryOpNode -> {
                 if (index == 0) findNode(root.left, remainder)
                 else if (index == 1) findNode(root.right, remainder)
                 else null
             }
             is FunctionNode -> {
                 if (index == 0) findNode(root.arg, remainder) else null
             }
             is PowerNode -> {
                 if (index == 0) findNode(root.base, remainder)
                 else if (index == 1) findNode(root.exponent, remainder)
                 else null
             }
             is ParenNode -> {
                 if (index == 0) findNode(root.inner, remainder) else null
             }
             is NegateNode -> {
                 if (index == 0) findNode(root.operand, remainder) else null
             }
             else -> null // Number, Var, Placeholder have no children
         }
     }

     /**
     * 주어진 경로([path])에 위치한 노드를 새로운 노드([newNode])로 교체하고,
     * 변경된 전체 트리의 루트를 반환합니다. (불변성 유지)
     */
    private fun replaceNode(root: VisualMathNode, path: List<Int>, newNode: VisualMathNode): VisualMathNode {
         if (path.isEmpty()) return newNode

         val index = path.first()
         val remainder = path.drop(1)

         return when (root) {
             is BinaryOpNode -> {
                 if (index == 0) root.copy(left = replaceNode(root.left, remainder, newNode))
                 else if (index == 1) root.copy(right = replaceNode(root.right, remainder, newNode))
                 else root
             }
             is FunctionNode -> {
                 if (index == 0) root.copy(arg = replaceNode(root.arg, remainder, newNode))
                 else root
             }
             is PowerNode -> {
                 if (index == 0) root.copy(base = replaceNode(root.base, remainder, newNode))
                 else if (index == 1) root.copy(exponent = replaceNode(root.exponent, remainder, newNode))
                 else root
             }
             is ParenNode -> {
                 if (index == 0) root.copy(inner = replaceNode(root.inner, remainder, newNode))
                 else root
             }
             is NegateNode -> {
                 if (index == 0) root.copy(operand = replaceNode(root.operand, remainder, newNode))
                 else root
             }
             else -> root
         }
     }
 }
