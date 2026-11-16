package com.dts.splitthebill.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.dts.splitthebill.R

internal fun UiStrings.languageLabel(tag: String): String = get(when (tag) {
    "en" -> R.string.english
    "fr" -> R.string.language_french
    "es" -> R.string.language_spanish
    else -> R.string.system_default
})

/** Android persists this choice and keeps the system's app-language setting in sync. */
@Composable
internal fun LanguagePicker(onDismiss: () -> Unit) {
    val strings = uiStrings()
    val selected = AppCompatDelegate.getApplicationLocales()[0]?.language.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.get(R.string.app_language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.get(R.string.choose_the_language_used_throughout_the_app), style = MaterialTheme.typography.bodyMedium)
                listOf("", "en", "fr", "es").forEach { tag ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .testTag("language_${tag.ifEmpty { "system" }}")
                            .selectable(selected = selected == tag, role = Role.RadioButton, onClick = {
                                onDismiss()
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                            }).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = selected == tag, onClick = null)
                        Text(strings.languageLabel(tag))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.get(R.string.cancel)) } },
    )
}
