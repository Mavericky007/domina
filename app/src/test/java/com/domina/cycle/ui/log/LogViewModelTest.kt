package com.domina.cycle.ui.log

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.Mood
import com.domina.cycle.data.repository.DayLogRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FakeDayLogRepository : DayLogRepository {
    val saved = mutableMapOf<LocalDate, DayLog>()
    override suspend fun save(log: DayLog) { saved[log.date] = log }
    override suspend fun getByDate(date: LocalDate) = saved[date]
    override fun observeRange(start: LocalDate, end: LocalDate) =
        flowOf(saved.values.filter { it.date in start..end })
}

@Suppress("OPT_IN_USAGE")
class LogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Suppress("OPT_IN_USAGE")
    @Test fun savingPersistsTheEditedLog() = runTest(dispatcher) {
        val repo = FakeDayLogRepository()
        val vm = LogViewModel(repo)
        val date = LocalDate.of(2026, 5, 24)
        vm.load(date)
        advanceUntilIdle()
        vm.setMood(Mood.HAPPY)
        vm.save()
        advanceUntilIdle()
        assertThat(repo.saved[date]?.mood).isEqualTo(Mood.HAPPY)
    }
}
