package com.dts.splitthebill.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Currency
import org.json.JSONObject

@Composable
internal fun CurrencyPickerField(
    currencyCode: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val currencies = remember(context, locale) {
        val json = context.assets.open("currencies.json").bufferedReader().use { it.readText() }
        val codes = JSONObject(json).getJSONArray("codes")
        List(codes.length()) { index ->
            val code = codes.getString(index)
            val name = runCatching { Currency.getInstance(code).getDisplayName(locale) }.getOrDefault(code)
            code to name
        }
    }
    var open by rememberSaveable { mutableStateOf(false) }
    val selectedName = currencies.firstOrNull { it.first == currencyCode }?.second ?: currencyCode
    SelectionField(
        label = "Currency",
        value = "$currencyCode · $selectedName",
        icon = Icons.Outlined.Payments,
        onClick = { open = true },
        modifier = modifier.testTag("group_currency"),
    )
    if (open) {
        var query by rememberSaveable { mutableStateOf("") }
        val filtered = remember(currencies, query) {
            val search = query.trim()
            currencies.filter { (code, name) -> code.contains(search, true) || name.contains(search, true) }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Choose currency") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search by code or name") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("currency_search"),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        Text("No matching currencies", modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyColumn(Modifier.weight(1f, fill = false).testTag("currency_options")) {
                            items(filtered, key = { it.first }) { (code, name) ->
                                ListItem(
                                    headlineContent = { Text(code) },
                                    supportingContent = { Text(name) },
                                    trailingContent = {
                                        if (code == currencyCode) Icon(Icons.Outlined.Check, contentDescription = "Selected")
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .testTag("currency_$code")
                                        .selectable(selected = code == currencyCode, role = Role.RadioButton) {
                                            onCurrencySelected(code)
                                            open = false
                                        },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OperationDateField(
    date: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    var open by rememberSaveable { mutableStateOf(false) }
    SelectionField(
        label = "Date",
        value = OperationDate.display(date, locale),
        icon = Icons.Outlined.CalendarMonth,
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        val initialMillis = OperationDate.toPickerMillis(date)
        val initialYear = remember(initialMillis) {
            Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                if (initialMillis != null) timeInMillis = initialMillis
            }.get(Calendar.YEAR)
        }
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = minOf(1900, initialYear)..maxOf(2100, initialYear),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let { onDateSelected(OperationDate.fromPickerMillis(it)) }
                        open = false
                    },
                    enabled = picker.selectedDateMillis != null,
                    modifier = Modifier.testTag("date_confirm"),
                ) { Text("Use date") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = picker, showModeToggle = false)
        }
    }
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).semantics { role = Role.Button },
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
    }
}
