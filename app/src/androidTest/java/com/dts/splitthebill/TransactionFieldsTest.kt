package com.dts.splitthebill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dts.splitthebill.ui.CurrencyPickerField
import com.dts.splitthebill.ui.OperationDateField
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TransactionFieldsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun searchesAndSelectsACurrencyBeyondTheOriginalFive() {
        var currency by mutableStateOf("EUR")
        compose.setContent {
            SplitTheBillsTheme {
                CurrencyPickerField(currencyCode = currency, onCurrencySelected = { currency = it })
            }
        }
        compose.onNodeWithTag("group_currency").performClick()
        compose.onNodeWithTag("currency_search").performTextInput("KWD")
        compose.onNodeWithTag("currency_KWD").performClick()
        compose.runOnIdle { assertEquals("KWD", currency) }
    }

    @Test
    fun dateCanOnlyBeChangedThroughCalendarConfirmation() {
        var date by mutableStateOf("2024-02-29")
        compose.setContent {
            SplitTheBillsTheme {
                OperationDateField(date, { date = it }, Modifier.testTag("operation_date"))
            }
        }
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithTag("operation_date").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Use date"), timeoutMillis = 5_000)
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("15", substring = true).performClick()
        compose.onNodeWithTag("date_confirm").performClick()
        compose.runOnIdle { assertEquals("2024-02-15", date) }
    }

    @Test
    fun cancellingTheCalendarKeepsTheOriginalDate() {
        var date by mutableStateOf("2024-02-29")
        compose.setContent {
            SplitTheBillsTheme {
                OperationDateField(date, { date = it }, Modifier.testTag("operation_date"))
            }
        }
        compose.onNodeWithTag("operation_date").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Use date"), timeoutMillis = 5_000)
        compose.onNodeWithText("15", substring = true).performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals("2024-02-29", date) }
    }
}
