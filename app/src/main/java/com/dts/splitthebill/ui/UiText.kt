package com.dts.splitthebill.ui

import android.content.Context
import androidx.annotation.StringRes

/** Resolve messages when displayed, so retained ViewModel state follows language changes. */
data class UiText(
    @param:StringRes val resourceId: Int,
    val arguments: List<Any> = emptyList(),
    val literal: String? = null,
) {
    fun resolve(context: Context): String = literal ?: if (arguments.isEmpty()) context.getString(resourceId)
        else context.getString(resourceId, *arguments.toTypedArray())

    companion object {
        /** Text already localized by an Android system prompt. */
        fun system(value: String) = UiText(0, literal = value)
    }
}

internal fun uiText(@StringRes resourceId: Int, vararg arguments: Any) = UiText(resourceId, arguments.toList())

internal class UiTextException(val text: UiText) : IllegalArgumentException()

internal inline fun requireUi(condition: Boolean, text: () -> UiText) {
    if (!condition) throw UiTextException(text())
}

/** The UI accepts decimal commas; the shared financial contract always receives decimal points. */
internal fun canonicalDecimal(value: String): String = value.trim().replace(',', '.')
