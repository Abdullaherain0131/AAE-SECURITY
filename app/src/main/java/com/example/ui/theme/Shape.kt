package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Köşe yarıçapı ölçeği.
 *
 * Mat bir arayüzde köşe, malzemenin kalınlığını anlatan tek ipucudur; gölge
 * olmadığı için tutarsız yarıçap doğrudan "özensiz" olarak okunur. Ekranlarda
 * 8 / 12 / 16 / 20 dp rastgele dağılmış durumdaydı; ölçek bunu sabitliyor.
 */
val AaeShapes = Shapes(
    /** Rozet, çip, küçük gösterge. */
    extraSmall = RoundedCornerShape(6.dp),
    /** Giriş alanı, liste satırı. */
    small = RoundedCornerShape(10.dp),
    /** Standart kart. */
    medium = RoundedCornerShape(14.dp),
    /** Panel, öne çıkan kart. */
    large = RoundedCornerShape(20.dp),
    /** Alt sayfa, tam ekran diyalog. */
    extraLarge = RoundedCornerShape(28.dp)
)
