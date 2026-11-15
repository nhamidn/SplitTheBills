package com.dts.splitthebill

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.dts.splitthebill.domain.model.SignedInUser
import com.dts.splitthebill.ui.AppScreen
import com.dts.splitthebill.ui.SplitTheBillsApp
import com.dts.splitthebill.ui.SplitTheBillsUiState
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Render real components so fixed light colors cannot silently bypass the dark palette. */
@OptIn(ExperimentalTestApi::class)
class ThemeRenderingTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun darkHomeKeepsBalanceCardDarkAndAmountReadable() = verifyBalanceCard(dark = true)

    @Test
    fun lightHomeKeepsBalanceCardDarkAndAmountReadable() = verifyBalanceCard(dark = false)

    @Test
    fun darkSignInUsesDarkBackgroundBeforeAuthentication() {
        render(dark = true, signedIn = false)
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        assertTrue("Sign-in background must follow the dark theme", pixels[0, pixels.height / 2].luminance() < 0.05f)
    }

    private fun verifyBalanceCard(dark: Boolean) {
        render(dark = dark, signedIn = true)
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val card = compose.onNodeWithTag("balance_card").fetchSemanticsNode().boundsInRoot
        val background = pixels[card.center.x.toInt(), card.top.toInt() + 8].luminance()
        assertTrue("The balance card must retain a dark background", background < 0.1f)

        val amount = compose.onNodeWithText("+$0.00").fetchSemanticsNode().boundsInRoot
        var brightest = 0f
        for (y in amount.top.toInt() until amount.bottom.toInt()) {
            for (x in amount.left.toInt() until amount.right.toInt()) {
                brightest = maxOf(brightest, pixels[x, y].luminance())
            }
        }
        val contrast = (brightest + 0.05f) / (background + 0.05f)
        assertTrue("Balance amount must have at least 4.5:1 contrast (was $contrast)", contrast >= 4.5f)
    }

    private fun render(dark: Boolean, signedIn: Boolean) {
        compose.setContent {
            SplitTheBillsTheme(darkTheme = dark) {
                SplitTheBillsApp(
                    state = SplitTheBillsUiState(
                        authResolved = true,
                        user = if (signedIn) SignedInUser("theme-preview", "alex@example.test", "Alex") else null,
                        screen = if (signedIn) AppScreen.GROUPS else AppScreen.AUTH,
                    ),
                    onAction = {},
                    onGoogleSignIn = {},
                )
            }
        }
    }
}
