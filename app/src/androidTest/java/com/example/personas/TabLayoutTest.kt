package com.example.personas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personas.ui.PestanasFormulario
import com.example.personas.ui.theme.PersonasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun largeTextDoesNotSplitContactTabAcrossLines() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                PersonasTheme { Box(Modifier.width(300.dp)) { PestanasFormulario(0, true) {} } }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Contacto", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        val layout = layouts.single()
        assertFalse("size=${layout.size}, width=${layout.getLineRight(0)}, overflowWidth=${layout.didOverflowWidth}, overflowHeight=${layout.didOverflowHeight}, end=${layout.getLineEnd(0)}, text=${layout.layoutInput.text}, paragraphHeight=${layout.multiParagraph.height}, bottom=${layout.getLineBottom(0)}, constraints=${layout.layoutInput.constraints}, style=${layout.layoutInput.style}", layout.hasVisualOverflow)
    }
}
