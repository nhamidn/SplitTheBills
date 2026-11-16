package com.dts.splitthebill.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourcesTest {
    @Test fun translationsCoverEveryStringAndPluralAndKeepFormatArguments() {
        val resources = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val defaults = read(File(resources, "values/strings.xml"))
        for (language in listOf("fr", "es")) {
            val translated = read(File(resources, "values-$language/strings.xml"))
            assertEquals("Missing or extra translations for $language", defaults.keys.map { it.substringBefore('/') }.toSet(),
                translated.keys.map { it.substringBefore('/') }.toSet())
            assertTrue("Missing required quantities for $language", translated.keys.containsAll(defaults.keys))
            defaults.forEach { (key, text) ->
                assertTrue("Empty translation: $language/$key", translated.getValue(key).isNotBlank())
                assertEquals("Format arguments changed: $language/$key", placeholders(text), placeholders(translated.getValue(key)))
            }
            translated.filterKeys { it !in defaults }.forEach { (key, text) ->
                assertTrue("Unexpected quantity: $key", key.endsWith("/many"))
                assertEquals(placeholders(defaults.getValue(key.substringBefore('/') + "/other")), placeholders(text))
            }
        }
    }

    private fun read(file: File): Map<String, String> {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        return buildMap {
            for (i in 0 until root.childNodes.length) {
                val item = root.childNodes.item(i) as? Element ?: continue
                if (item.getAttribute("translatable") == "false") continue
                val name = item.getAttribute("name")
                if (item.tagName == "plurals") {
                    val quantities = item.getElementsByTagName("item")
                    for (j in 0 until quantities.length) {
                        val quantity = quantities.item(j) as Element
                        put("$name/${quantity.getAttribute("quantity")}", quantity.textContent)
                    }
                } else put(name, item.textContent)
            }
        }
    }

    private fun placeholders(value: String): List<String> = Regex("""%\d+\$[sd]""").findAll(value).map { it.value }.sorted().toList()
}
