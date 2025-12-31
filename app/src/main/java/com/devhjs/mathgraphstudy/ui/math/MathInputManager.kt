package com.devhjs.mathgraphstudy.ui.math
 
 import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode.*
import com.devhjs.mathgraphstudy.domain.model.math.*
 
 object MathInputManager {
 
     fun onFocusChange(state: MathInputState, newPath: List<Int>): MathInputState {
         return state.copy(focusPath = newPath)
     }
 
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
