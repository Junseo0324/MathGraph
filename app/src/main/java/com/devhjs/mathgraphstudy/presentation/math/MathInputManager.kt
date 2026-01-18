package com.devhjs.mathgraphstudy.presentation.math
 
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
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
 * 2. 숫자, 연산자, 함수, 변수 입력을 처리합니다.
 * 3. 커서 이동(오른쪽) 및 삭제(Backspace) 로직을 수행합니다.
 * 4. 연산자 우선순위(Precedence Climbing)를 고려하여 트리를 재구성합니다.
 */
object MathInputManager {
 
    /**
      * 포커스 경로([focusPath])를 변경하여 커서 위치를 업데이트합니다.
      * 사용자가 특정 노드를 클릭했을 때 호출됩니다.
      */
     fun onFocusChange(state: MathInputState, newPath: List<Int>): MathInputState {
         return state.copy(focusPath = newPath)
     }
 
    /**
      * 사용자의 키 입력을 받아 상태를 갱신합니다.
      * 입력 종류(숫자, 연산자, 함수, 변수, 이동, 삭제)를 감지하여 적절한 처리 함수로 분기합니다.
      */
     fun processInput(state: MathInputState, input: String): MathInputState {
         val currentPath = state.focusPath
         val root = state.rootNode
         
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
        if (input == "x" || input == "e" || input == "pi") {
            return handleVariableInput(state, input)
        }

        // 5. Navigation
        if (input == "→" || input == "RIGHT") {
             return moveFocusRight(state)
        }
        
        // 6. Delete
        if (input == "DEL" || input == "⌫") {
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
            is FunctionNode, is BinaryOpNode, is PowerNode -> {
                // If the entire function/op structure is focused, delete it
                val newRoot = replaceNode(root, path, PlaceholderNode)
                state.copy(rootNode = newRoot)
            }
            else -> state
        }
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
            is BinaryOpNode -> {
                if (lastIndex == 0) {
                    // Left -> Right
                    state.copy(focusPath = parentPath + 1)
                } else {
                    // Right -> Parent/Exit
                    state.copy(focusPath = parentPath)
                }
            }
            is FunctionNode -> {
                // Arg(0) -> Parent/Exit
                state.copy(focusPath = parentPath)
            }
            is PowerNode -> {
                if (lastIndex == 0) {
                    // Base -> Exponent
                    state.copy(focusPath = parentPath + 1)
                } else {
                    // Exponent -> Parent/Exit
                    state.copy(focusPath = parentPath)
                }
            }
            else -> state // Should not happen if path is valid
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
             is NumberNode -> NumberNode(targetNode.value + digit)
             else -> return state // Cannot append digit to Op or Func directly without explicit focus logic
         }
 
         val newRoot = replaceNode(root, path, newNode)
         return state.copy(rootNode = newRoot)
     }
 
    /**
     * 연산자(+, -, *, /, ^) 입력을 처리합니다.
     *
     * **Precedence Climbing (우선순위 상승)**:
     * 현재 위치에서 상위 노드로 거슬러 올라가며 연산자 우선순위를 비교합니다.
     * 더 낮은 우선순위의 연산자가 나올 때까지 올라간 뒤, 새로운 연산자 노드로 감싸서 트리를 재구성합니다.
     * 이를 통해 `2 * x + 1`과 같은 식이 올바른 연산 순서를 가지게 됩니다.
     */
    private fun handleOperatorInput(state: MathInputState, op: MathOperator): MathInputState {
        val (root, path) = state
        
        // Precedence Climbing Logic
        var currentPath = path
        var targetNode = findNode(root, currentPath) ?: return state

        // Climb up while parent has higher/equal precedence
        // We only climb if target is NOT a Placeholder (if it is, we are filling a slot, so don't climb)
        // Actually, if we just typed "2", "x", target is "x" (VariableNode).
        // Parent is "*". * precedence 2. + precedence 1. 2 >= 1 -> Climb.
        
        // We also stop climbing if we hit a Group/Parenthesis (not implemented yet) or Function argument boundary?
        // For FunctionNode argument, it acts like a Group. "sin(x) + 1".
        // If inside sin(x), Parent is FunctionNode. Should we climb out of function?
        // Usually, yes, if we typed "sin(x)+", we mean "sin(x) + ...". 
        // But if we are "sin(x...)", we need explicit exit "→" to type "+".
        // Current design: FunctionNode has explicit boundary. Automatic climb out might be confusing vs "sin(x+1)".
        // So let's ONLY climb over BinaryOpNode for now. Users must use "→" to exit functions.

        while (currentPath.isNotEmpty()) {
            val parentPath = currentPath.dropLast(1)
            val parentNode = findNode(root, parentPath)
            
            if (parentNode is BinaryOpNode) {
                 val parentPrec = parentNode.op.precedence
                 val newPrec = op.precedence
                 
                 // Left-associative for +, -, *, /
                 if (parentPrec >= newPrec) {
                     currentPath = parentPath
                     targetNode = parentNode
                     continue
                 }
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
      * 현재 노드를 함수 노드(`FunctionNode`)로 교체하고, 함수의 인자(Placeholder)로 포커스를 이동합니다.
      */
     private fun handleFunctionInput(state: MathInputState, func: MathFunction): MathInputState {
         val (root, path) = state
         val targetNode = findNode(root, path)
         
         // FunctionNode(func, arg)
         // If placeholder, replace with Func(Placeholder). Focus arg.
         val newNode = FunctionNode(func, PlaceholderNode)
         
         val newRoot = replaceNode(root, path, newNode)
         val newPath = path + 0 // Arg is index 0
         
         return state.copy(rootNode = newRoot, focusPath = newPath)
     }
     

    /**
     * 변수(x, e, pi) 입력을 처리합니다.
     *
     * 1. 현재 노드가 숫자인 경우, 암시적 곱셈을 적용하여 `3x` -> `3 * x` 형태로 변환합니다.
     * 2. 그 외의 경우(Placeholder), 해당 위치를 변수 노드로 교체합니다.
     */
    private fun handleVariableInput(state: MathInputState, name: String): MathInputState {
        val (root, path) = state
        val targetNode = findNode(root, path)

        if (targetNode is NumberNode) {
            // Implicit Multiplication: 3 -> 3*x
            val newBinary = BinaryOpNode(
                left = targetNode,
                op = MathOperator.MULTIPLY,
                right = VariableNode(name)
            )
            val newRoot = replaceNode(root, path, newBinary)
            val newPath = path + 1 // Focus the variable
            return state.copy(rootNode = newRoot, focusPath = newPath)
        }
        
        // Replace placeholder with var
        if (targetNode !is PlaceholderNode) return state

        val newNode = VariableNode(name)
        val newRoot = replaceNode(root, path, newNode)
        return state.copy(rootNode = newRoot)
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
             else -> root
         }
     }
 }
