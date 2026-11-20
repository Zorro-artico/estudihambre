package com.example.data.excel

import android.content.Context
import android.net.Uri
import com.example.domain.model.MasterTableRow
import com.example.util.DateUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element

data class ImportedExcelRow(
    val dayNumber: Int,
    val logicalDate: String,
    val intakeKcal: Int,
    val expenditureKcal: Int,
    val deficitKcal: Int,
    val weightKg: Double?,
    val proteinG: Float,
    val biologicalNote: String
)

class ExcelManager {

    companion object {
        val HEADERS = listOf(
            "Día",
            "Fecha",
            "Ingesta (kcal)",
            "Gasto (kcal)",
            "Déficit (kcal)",
            "Peso (kg)",
            "Proteína (g)",
            "Nota Biológica"
        )
    }

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        rows: List<MasterTableRow>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val ordered = rows.sortedBy { it.logicalDate }
            val outBytes = buildWorkbookBytes(ordered)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(outBytes)
                os.flush()
            } ?: throw IllegalStateException("No se pudo abrir el archivo destino.")
            ordered.size
        }
    }

    suspend fun importFromUri(
        context: Context,
        uri: Uri
    ): Result<List<ImportedExcelRow>> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("No se pudo leer el archivo seleccionado.")
            parseWorkbookBytes(bytes)
        }
    }

    fun buildWorkbookBytes(rows: List<MasterTableRow>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zos, "_rels/.rels", rootRelsXml())
            writeZipEntry(zos, "xl/workbook.xml", workbookXml())
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zos, "xl/styles.xml", stylesXml())
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml(rows))
        }
        return baos.toByteArray()
    }

    fun parseWorkbookBytes(bytes: ByteArray): List<ImportedExcelRow> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) }.orEmpty()
        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: entries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }?.value
            ?: throw IllegalArgumentException("El archivo .xlsx no contiene una hoja de cálculo válida.")

        val grid = parseSheetRows(sheetBytes, sharedStrings)
        if (grid.isEmpty()) return emptyList()

        // Skip header row if first row contains text headers like "Día" or "Fecha"
        val startIndex = if (grid.first().any { it.contains("Fecha", ignoreCase = true) || it.contains("Ingesta", ignoreCase = true) }) {
            1
        } else {
            0
        }

        val parsedRows = mutableListOf<ImportedExcelRow>()
        for (i in startIndex until grid.size) {
            val cols = grid[i]
            if (cols.all { it.isBlank() }) continue
            val dayRaw = cols.getOrNull(0)?.trim().orEmpty()
            val dateRaw = cols.getOrNull(1)?.trim().orEmpty()
            val intakeRaw = cols.getOrNull(2)?.trim().orEmpty()
            val expenditureRaw = cols.getOrNull(3)?.trim().orEmpty()
            val deficitRaw = cols.getOrNull(4)?.trim().orEmpty()
            val weightRaw = cols.getOrNull(5)?.trim().orEmpty()
            val proteinRaw = cols.getOrNull(6)?.trim().orEmpty()
            val noteRaw = cols.getOrNull(7)?.trim().orEmpty()

            val logicalDate = normalizeAnyDate(dateRaw) ?: continue
            val dayNumber = dayRaw.toDoubleOrNull()?.toInt() ?: (parsedRows.size + 1)
            val intake = intakeRaw.toDoubleOrNull()?.toInt() ?: 0
            val expenditure = expenditureRaw.toDoubleOrNull()?.toInt() ?: (intake + (deficitRaw.toDoubleOrNull()?.toInt() ?: 0))
            val deficit = deficitRaw.toDoubleOrNull()?.toInt() ?: (expenditure - intake)
            val weight = weightRaw.replace(',', '.').toDoubleOrNull()
            val protein = proteinRaw.replace(',', '.').toFloatOrNull() ?: 0f

            parsedRows.add(
                ImportedExcelRow(
                    dayNumber = dayNumber.coerceAtLeast(1),
                    logicalDate = logicalDate,
                    intakeKcal = intake.coerceAtLeast(0),
                    expenditureKcal = expenditure.coerceAtLeast(0),
                    deficitKcal = deficit,
                    weightKg = weight,
                    proteinG = protein.coerceAtLeast(0f),
                    biologicalNote = noteRaw
                )
            )
        }
        return parsedRows
    }

    private fun normalizeAnyDate(raw: String): String? {
        DateUtils.normalizeExcelDate(raw)?.let { return it }
        // Handle Excel serial date numbers (days since 1899-12-30)
        val serial = raw.toDoubleOrNull() ?: return null
        if (serial < 30000.0 || serial > 90000.0) return null
        val cal = Calendar.getInstance(Locale.US).apply {
            set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
            add(Calendar.DAY_OF_YEAR, serial.toInt())
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        val siNodes = doc.getElementsByTagName("si")
        val list = ArrayList<String>(siNodes.length)
        for (i in 0 until siNodes.length) {
            val el = siNodes.item(i) as? Element ?: continue
            val tNodes = el.getElementsByTagName("t")
            val sb = StringBuilder()
            for (j in 0 until tNodes.length) {
                sb.append(tNodes.item(j).textContent.orEmpty())
            }
            list.add(sb.toString())
        }
        return list
    }

    private fun parseSheetRows(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        val rowNodes = doc.getElementsByTagName("row")
        val rows = ArrayList<List<String>>(rowNodes.length)

        for (i in 0 until rowNodes.length) {
            val rowEl = rowNodes.item(i) as? Element ?: continue
            val rowValues = mutableListOf<String>()
            val cellNodes = rowEl.getElementsByTagName("c")
            for (j in 0 until cellNodes.length) {
                val cellEl = cellNodes.item(j) as? Element ?: continue
                val cellType = cellEl.getAttribute("t")
                val ref = cellEl.getAttribute("r")
                val colIdx = if (!ref.isNullOrBlank()) columnIndexFromRef(ref) else rowValues.size

                val rawValue = when (cellType) {
                    "inlineStr" -> {
                        val tNodes = cellEl.getElementsByTagName("t")
                        val sb = StringBuilder()
                        for (k in 0 until tNodes.length) sb.append(tNodes.item(k).textContent.orEmpty())
                        sb.toString()
                    }
                    else -> {
                        val vNodes = cellEl.getElementsByTagName("v")
                        if (vNodes.length > 0) vNodes.item(0).textContent.orEmpty() else ""
                    }
                }

                val resolved = when (cellType) {
                    "s" -> rawValue.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                    else -> rawValue
                }
                while (rowValues.size <= colIdx) rowValues.add("")
                rowValues[colIdx] = resolved
            }
            rows.add(rowValues)
        }
        return rows
    }

    private fun columnIndexFromRef(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun contentTypesXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelsXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
                <sheet name="Tabla Maestra" sheetId="1" r:id="rId1"/>
            </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun stylesXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <fonts count="2">
                <font><sz val="11"/><name val="Calibri"/></font>
                <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
            </fonts>
            <fills count="3">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FF1E1A16"/></patternFill></fill>
            </fills>
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="2">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
            </cellXfs>
        </styleSheet>
    """.trimIndent()

    private fun sheetXml(rows: List<MasterTableRow>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<cols>""")
        sb.append("""<col min="1" max="1" width="8" customWidth="1"/>""")
        sb.append("""<col min="2" max="2" width="14" customWidth="1"/>""")
        sb.append("""<col min="3" max="7" width="15" customWidth="1"/>""")
        sb.append("""<col min="8" max="8" width="54" customWidth="1"/>""")
        sb.append("""</cols>""")
        sb.append("""<sheetData>""")

        // Row 1: Header
        sb.append("""<row r="1">""")
        HEADERS.forEachIndexed { colIdx, title ->
            val ref = "${colLetter(colIdx)}1"
            sb.append("""<c r="$ref" s="1" t="inlineStr"><is><t>${escapeXml(title)}</t></is></c>""")
        }
        sb.append("""</row>""")

        // Data Rows
        rows.forEachIndexed { idx, row ->
            val excelRow = idx + 2
            sb.append("""<row r="$excelRow">""")
            sb.append(numCell("${colLetter(0)}$excelRow", row.dayNumber.toString()))
            sb.append(strCell("${colLetter(1)}$excelRow", row.logicalDate))
            sb.append(numCell("${colLetter(2)}$excelRow", row.intakeKcal.toString()))
            sb.append(numCell("${colLetter(3)}$excelRow", row.expenditureKcal.toString()))
            sb.append(numCell("${colLetter(4)}$excelRow", row.deficitKcal.toString()))
            if (row.weightKg != null) {
                sb.append(numCell("${colLetter(5)}$excelRow", String.format(Locale.US, "%.2f", row.weightKg)))
            } else {
                sb.append(strCell("${colLetter(5)}$excelRow", ""))
            }
            sb.append(numCell("${colLetter(6)}$excelRow", String.format(Locale.US, "%.1f", row.proteinG)))
            sb.append(strCell("${colLetter(7)}$excelRow", row.biologicalNote))
            sb.append("""</row>""")
        }

        sb.append("""</sheetData>""")
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    private fun colLetter(index: Int): String {
        return ('A' + index).toString()
    }

    private fun numCell(ref: String, value: String): String {
        return """<c r="$ref"><v>$value</v></c>"""
    }

    private fun strCell(ref: String, value: String): String {
        return """<c r="$ref" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>"""
    }

    private fun escapeXml(input: String): String {
        return buildString(input.length) {
            for (ch in input) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') append(ch)
                }
            }
        }
    }
}
