package com.devhjs.mathgraphstudy.ui.math

import com.devhjs.mathgraphstudy.domain.model.math.*
 
 import androidx.compose.foundation.border
 import androidx.compose.foundation.background
 import androidx.compose.foundation.clickable
 import androidx.compose.foundation.layout.Box
 import androidx.compose.foundation.layout.Row
 import androidx.compose.foundation.layout.Column
 import androidx.compose.foundation.layout.fillMaxWidth
 import androidx.compose.foundation.layout.height
 import androidx.compose.foundation.layout.width
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
 import androidx.compose.ui.draw.drawBehind
 import androidx.compose.ui.graphics.Path
 import androidx.compose.ui.graphics.drawscope.Stroke
 import androidx.compose.ui.graphics.StrokeCap
 import androidx.compose.ui.graphics.StrokeJoin
 import androidx.compose.foundation.layout.IntrinsicSize
 
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
                 if (node.op == MathOperator.DIVIDE) {
                     // Vertical Fraction Layout
                     androidx.compose.foundation.layout.Column(
                         horizontalAlignment = Alignment.CenterHorizontally,
                         modifier = Modifier
                             .padding(horizontal = 2.dp)
                             .width(IntrinsicSize.Max)
                     ) {
                         MathNodeView(node.left, currentPath + 0, focusPath, onFocusRequest)
                         // Fraction Bar
                         androidx.compose.foundation.layout.Box(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(1.dp)
                                 .background(MaterialTheme.colorScheme.onSurface)
                         )
                         MathNodeView(node.right, currentPath + 1, focusPath, onFocusRequest)
                     }
                 } else {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         MathNodeView(node.left, currentPath + 0, focusPath, onFocusRequest)
                        if (node.op == MathOperator.MULTIPLY) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        } else {
                            Text(text = " ${node.op.symbol} ", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                         MathNodeView(node.right, currentPath + 1, focusPath, onFocusRequest)
                     }
                 }
             }
             is FunctionNode -> {
                 if (node.func == MathFunction.SQRT) {
                     val color = MaterialTheme.colorScheme.onSurface
                     Row(
                         modifier = Modifier.drawBehind {
                             val strokeWidth = 1.5.dp.toPx()
                             val path = Path().apply {
                                 // Coordinates for the root symbol
                                 // 1. Start (small tick left) - approx (2dp, 65% height)
                                 moveTo(2.dp.toPx(), size.height * 0.65f)
                                 // 2. Valley (bottom point) - approx (6dp, height - 2dp)
                                 lineTo(6.dp.toPx(), size.height - 2.dp.toPx())
                                 // 3. Beak (top point near text start) - (12dp, line_top)
                                 // The horizontal line is drawn at y = strokeWidth/2 derived from top padding
                                 val lineY = 4.dp.toPx() / 2 // Centered in the 4dp top spacing
                                 lineTo(12.dp.toPx(), lineY)
                                 // 4. Horizontal Line (Vinculum)
                                 lineTo(size.width, lineY)
                             }
                             drawPath(
                                 path = path,
                                 color = color,
                                 style = Stroke(
                                     width = strokeWidth,
                                     cap = StrokeCap.Round,
                                     join = StrokeJoin.Round
                                 )
                             )
                         }
                     ) {
                         // Reserve space for the root symbol on the left
                         androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 14.dp))
                         
                         // Content with top padding to make room for the horizontal line
                         androidx.compose.foundation.layout.Box(
                             modifier = Modifier.padding(top = 4.dp)
                         ) {
                             androidx.compose.material3.ProvideTextStyle(
                                 value = MaterialTheme.typography.labelMedium
                             ) {
                                 MathNodeView(node.arg, currentPath + 0, focusPath, onFocusRequest)
                             }
                         }
                     }
                 } else {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(text = node.func.symbol)
                         androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                         MathNodeView(node.arg, currentPath + 0, focusPath, onFocusRequest)
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
