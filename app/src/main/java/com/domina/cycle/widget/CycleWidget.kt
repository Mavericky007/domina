package com.domina.cycle.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.domina.cycle.MainActivity
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun dayLogRepository(): DayLogRepository
    fun settingsRepository(): SettingsRepository
}

class CycleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val today = LocalDate.now()
        val logs = ep.dayLogRepository().observeRange(today.minusDays(400), today).first()
        val mode = ep.settingsRepository().appMode.first()
        val due = ep.settingsRepository().dueDate.first()
        val info: WidgetInfo = WidgetData.build(logs, mode, due, today)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        provideContent { WidgetContent(info, launchIntent) }
    }

    @Composable
    private fun WidgetContent(data: WidgetInfo, launchIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .background(ColorProvider(Color(0xFFFDF4FA)))
                .clickable(actionStartActivity(launchIntent)),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Text(
                data.headline,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF6A4C93)),
                )
            )
            Text(
                data.subline,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8A5A9E)),
                )
            )
        }
    }
}
