package com.devhjs.mathgraphstudy.ui.theme

import androidx.compose.ui.graphics.Color

val BackgroundCheck = Color(0xFF121212)
val SurfaceCard = Color(0xFF1E1E1E)
val PrimaryGold = Color(0xFFFFD700)
val PrimaryGoldVariant = Color(0xFFFFD54F)
val BlueAccent = Color(0xFF42A5F5)
val GreyAccent = Color(0xFF757575)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
val BorderColor = Color(0xFF333333)
val GridColor = Color(0xFF252525)

// Keep existing names if used elsewhere, or map them to new ones if appropriate.
// For safety, I'll keep the old ones if they are used by other files not yet checked,
// but for this task I will primarily define the new ones requested.
// Actually, looking at Theme.kt, it uses Blue500, Cyan400 etc. I should ideally replace those usages.
// Let's keep the file clean and precise. I will replace the content with the new palette
// and aliases for the old names if necessary to prevent compilation errors, 
// OR I will just update Theme.kt to use the new names. 
// Given the user wants a design change, I will stick to the new names and update usages.

val BlackCharcoal = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val Gold = Color(0xFFFFD700)
val YellowAction = Color(0xFFFFD54F)
val GraphBlue = Color(0xFF42A5F5)
val GraphGrey = Color(0xFF757575)
val WhiteText = Color(0xFFFFFFFF)
val GreyText = Color(0xFFB0B0B0)
val DarkBorder = Color(0xFF333333)
val GraphGrid = Color(0xFF252525)

// Aliases for compatibility if needed (but I will update Theme.kt)
val Blue500 = GraphBlue
val Cyan400 = GraphBlue 
val Red500 = Color(0xFFEF4444) // Keep Red for error
val Slate950 = BlackCharcoal
val Slate900 = DarkSurface
val Slate800 = DarkSurface
val Gray200 = WhiteText