package com.jjmluz.nationslingo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Reads the string resources straight from `res`, since unit tests have no Android resources. */
class TranslationsTest {
    private val res = File("src/main/res")
    private val languages = listOf("values", "values-pt", "values-es", "values-fr", "values-de")

    private fun strings(dir: String, file: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/$file"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
    }

    private fun names(dir: String, tag: String, file: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/$file"))
        val nodes = doc.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .map { it.attributes.getNamedItem("name").nodeValue }.toSet()
    }

    @Test
    fun `every language names every country, each once`() {
        val expected = COUNTRIES.map { "country_${it.code}" }.toSet()
        for (dir in languages) {
            val countries = strings(dir, "countries.xml")
            assertEquals(dir, expected, countries.keys)
            assertEquals(dir, countries.size, countries.values.toSet().size)
        }
    }

    @Test
    fun `every language translates every text`() {
        for (tag in listOf("string", "plurals")) {
            val expected = names("values", tag, "strings.xml")
            for (dir in languages) assertEquals("$dir $tag", expected, names(dir, tag, "strings.xml"))
        }
    }
}
