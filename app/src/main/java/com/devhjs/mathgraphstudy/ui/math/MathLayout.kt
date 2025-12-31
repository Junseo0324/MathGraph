package com.devhjs.mathgraphstudy.ui.math

import com.devhjs.mathgraphstudy.domain.model.math.*
 
 import androidx.compose.foundation.border
 import androidx.compose.foundation.clickable
 import androidx.compose.foundation.layout.Box
 import androidx.compose.foundation.layout.Row
 import androidx.compose.foundation.layout.padding
 import androidx.compose.material3.MaterialTheme
 import androidx.compose.material3.Text
 import androidx.compose.runtime.Composable
 import androidx.compose.ui.Alignment
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.graphics.Color
 import androidx.compose.ui.unit.dp
 import androidx.compose.ui.unit.sp
 import androidx.compose.foundation.layout.offset
 
 @Composable
 fun MathNodeView(
     node: VisualMathNode,
     currentPath: List<Int>,
     focusPath: List<Int>,
     onFocusRequest: (List<Int>) -> Unit
 ) {
     val isFocused = currentPath == focusPath
     val modifier = if (isFocused) {
         Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
     } else {
         Modifier
     }
 
     Box(modifier = modifier.padding(2.dp)) {
         when (node) {
             is NumberNode -> Text(text = node.value)
             is VariableNode -> Text(text = node.name)
             is BinaryOpNode -> {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     MathNodeView(node.left, currentPath + 0, focusPath, onFocusRequest)
                    if (node.op == MathOperator.MULTIPLY && node.left is NumberNode && node.right is VariableNode) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                    } else {
                        Text(text = " ${node.op.symbol} ", modifier = Modifier.padding(horizontal = 4.dp))
                    }
                     MathNodeView(node.right, currentPath + 1, focusPath, onFocusRequest)
                 }
             }
             is FunctionNode -> {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Text(text = node.func.symbol)
                     if (node.func == MathFunction.SQRT) {
                         // SQRT still uses brackets for now to distinguish scope
                         Text("(")
                     } else {
                         // Space for "sin x" style
                         androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                     }
                     
                     MathNodeView(node.arg, currentPath + 0, focusPath, onFocusRequest)
                     
                     if (node.func == MathFunction.SQRT) {
                         Text(")")
                     }
                 }
             }
             is PowerNode -> {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     MathNodeView(node.base, currentPath + 0, focusPath, onFocusRequest)
                     androidx.compose.foundation.layout.Box(
                         modifier = Modifier
                             .padding(start = 2.dp)
                             .offset(y = (-8).dp)
                     ) {
                        androidx.compose.material3.ProvideTextStyle(
                            value = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        ) {
                            MathNodeView(node.exponent, currentPath + 1, focusPath, onFocusRequest)
                        }
                     }
                 }
             }
             PlaceholderNode -> {
                 Box(
                     modifier = Modifier
                         .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.extraSmall)
                         .clickable { onFocusRequest(currentPath) }
                         .padding(horizontal = 8.dp, vertical = 4.dp)
                 ) {
                     Text(" ") // Empty space to give size
                 }
             }
         }
     }
 }
