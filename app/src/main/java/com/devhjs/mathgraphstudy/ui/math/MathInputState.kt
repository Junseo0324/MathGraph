package com.devhjs.mathgraphstudy.ui.math

import com.devhjs.mathgraphstudy.domain.model.math.*
 
 data class MathInputState(
     val rootNode: VisualMathNode = PlaceholderNode,
     val focusPath: List<Int> = emptyList() // Indices from root to focused node
 )
