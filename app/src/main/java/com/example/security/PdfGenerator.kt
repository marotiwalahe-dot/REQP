package com.example.security

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import java.io.ByteArrayOutputStream

data class GeneratedPdfResult(
    val pdfBase64: String,
    val pageCount: Int,
    val fileSizeLabel: String,
    val pageTexts: List<String>
)

object PdfGenerator {

    fun createStudyPdf(
        title: String,
        subject: String,
        sections: List<Pair<String, String>>
    ): GeneratedPdfResult {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageTexts = mutableListOf<String>()

        try {
            val pages = if (sections.isEmpty()) {
                listOf(title to "Protected study material for $subject.")
            } else {
                sections
            }

            pages.forEachIndexed { index, (heading, body) ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Dark premium paper header band
                val headerPaint = Paint().apply {
                    color = Color.rgb(16, 16, 16)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerPaint)

                // Liquid yellow accent stripe
                val accentPaint = Paint().apply {
                    color = Color.rgb(255, 214, 0)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 106f, pageWidth.toFloat(), 112f, accentPaint)

                val badgePaint = Paint().apply {
                    color = Color.rgb(255, 214, 0)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("SECURE STUDY  //  ${subject.uppercase()}", 42f, 40f, badgePaint)

                val docTitlePaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 20f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText(title.take(46), 42f, 76f, docTitlePaint)

                val sectionHeadingPaint = Paint().apply {
                    color = Color.rgb(24, 24, 27)
                    textSize = 18f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("${index + 1}. $heading", 42f, 158f, sectionHeadingPaint)

                val bodyPaint = Paint().apply {
                    color = Color.rgb(45, 45, 52)
                    textSize = 12.5f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    isAntiAlias = true
                }

                var y = 196f
                val maxWidth = pageWidth - 84f
                val paragraphs = body.split("\n")
                for (paragraph in paragraphs) {
                    if (paragraph.isBlank()) {
                        y += 14f
                        continue
                    }
                    val words = paragraph.split(" ")
                    var currentLine = StringBuilder()
                    for (word in words) {
                        val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (bodyPaint.measureText(candidate) > maxWidth) {
                            if (y < pageHeight - 70f) {
                                canvas.drawText(currentLine.toString(), 42f, y, bodyPaint)
                                y += 20f
                            }
                            currentLine = StringBuilder(word)
                        } else {
                            currentLine = StringBuilder(candidate)
                        }
                    }
                    if (currentLine.isNotEmpty() && y < pageHeight - 70f) {
                        canvas.drawText(currentLine.toString(), 42f, y, bodyPaint)
                        y += 24f
                    }
                }

                // Footer
                val footerPaint = Paint().apply {
                    color = Color.rgb(120, 120, 128)
                    textSize = 10f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                    isAntiAlias = true
                }
                canvas.drawText(
                    "Protected In-App Copy • Page ${index + 1} of ${pages.size}",
                    42f,
                    pageHeight - 32f,
                    footerPaint
                )

                document.finishPage(page)
                pageTexts.add("$heading\n$body")
            }

            val output = ByteArrayOutputStream()
            document.writeTo(output)
            val bytes = output.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val kb = (bytes.size / 1024.0).coerceAtLeast(1.0)
            val sizeLabel = if (kb >= 1024.0) {
                String.format("%.1f MB", kb / 1024.0)
            } else {
                "${kb.toInt()} KB"
            }
            return GeneratedPdfResult(
                pdfBase64 = base64,
                pageCount = pages.size,
                fileSizeLabel = sizeLabel,
                pageTexts = pageTexts
            )
        } finally {
            document.close()
        }
    }
}
