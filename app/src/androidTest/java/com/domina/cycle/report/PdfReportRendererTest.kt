package com.domina.cycle.report

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PdfReportRendererTest {
    @Test fun rendersAValidPdf() {
        val report = Report(
            "Domina — Cycle Report", LocalDate.parse("2026-04-01"),
            listOf(ReportSection("Cycle summary", listOf("Average cycle length: 28 days", "Range: 27–30 days"))),
        )
        val out = ByteArrayOutputStream()
        PdfReportRenderer.render(report, out)
        val bytes = out.toByteArray()
        assertThat(bytes.size).isGreaterThan(100)
        assertThat(String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)).isEqualTo("%PDF-")
    }
}
