package com.aitutor.chatbot.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.data.chat.ChatRepository
import com.aitutor.chatbot.app.data.firebase.UserProfileRepository
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.modes
import com.aitutor.chatbot.app.domain.model.recommendationsFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

private const val WEEK_DAYS = 7

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val userProfileRepository: UserProfileRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val weekStartMillis = System.currentTimeMillis() - (WEEK_DAYS - 1L) * 24 * 60 * 60 * 1000

    init {
        chatRepository.startSync()
        userProfileRepository.startSync()
    }

    val uiState: StateFlow<UiState<DashboardData>> = combine(
        chatRepository.allChats(),
        chatRepository.questionCount(),
        chatRepository.questionTimestampsSince(weekStartMillis),
        userProfileRepository.plan,
        preferencesRepository.studentProfile,
    ) { chats, questionCount, weekTimestamps, plan, profile ->
        val chatTimestamps = chats.map { it.timestampMillis }
        DashboardData(
            greeting = buildGreeting(),
            profile = profile,
            plan = plan,
            modes = modes,
            recommendations = recommendationsFor(profile),
            recentChats = chats.take(6),
            weeklyActivity = buildWeeklyActivity(weekTimestamps),
            streakDays = computeStreakDays(chatTimestamps),
            questionsAsked = questionCount,
            chatsThisWeek = chatTimestamps.count { it >= weekStartMillis },
        )
    }
        .map<DashboardData, UiState<DashboardData>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Couldn't load your dashboard.", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onProfileUpdated(profile: StudentProfile) {
        viewModelScope.launch { preferencesRepository.setStudentProfile(profile) }
    }

    private fun buildGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }
        return "Good $timeOfDay"
    }

    /** Oldest-to-newest buckets so the chart reads left-to-right, ending on today. */
    private fun buildWeeklyActivity(timestamps: List<Long>): List<DayActivity> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val countsByDay = timestamps
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .groupingBy { it }
            .eachCount()

        return (WEEK_DAYS - 1 downTo 0).map { daysAgo ->
            val day = today.minusDays(daysAgo.toLong())
            DayActivity(
                label = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                count = countsByDay[day] ?: 0,
                isToday = day == today,
            )
        }
    }

    /**
     * Consecutive days ending today (or yesterday, so the streak survives until the day is over)
     * on which this student studied at least once.
     */
    private fun computeStreakDays(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val activeDays = timestamps
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay() }
            .distinct()
            .sortedDescending()

        val today = LocalDate.now(zone).toEpochDay()
        if (activeDays.first() < today - 1) return 0

        var streak = 1
        for (index in 1 until activeDays.size) {
            if (activeDays[index] == activeDays[index - 1] - 1) streak++ else break
        }
        return streak
    }
}
