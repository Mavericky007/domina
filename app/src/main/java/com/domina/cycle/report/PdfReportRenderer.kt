package com.domina.cycle.report

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfReportRenderer {
    private const val PAGE_W = 595   // A4 @ 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    fun render(report: Report, out: OutputStream) {
        val doc = PdfDocument()
        val title = Paint().apply { textSize = 22f; isFakeBoldText = true }
        val heading = Paint().apply { textSize = 15f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 12f }
        val muted = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }

        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var c = page.canvas
        var y = MARGIN

        fun newPage() {
            doc.finishPage(page); pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page.canvas; y = MARGIN
        }
        fun line(text: String, paint: Paint, gap: Float) {
            if (y + gap > PAGE_H - MARGIN) newPage()
            c.drawText(text, MARGIN, y, paint); y += gap
        }

        line(report.title, title, 30f)
        line("Generated ${report.generatedOn} · stays on your device", muted, 24f)
        report.sections.forEach { section ->
            line(section.title, heading, 22f)
            section.lines.forEach { line("•  $it", body, 18f) }
            y += 8f
        }
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
