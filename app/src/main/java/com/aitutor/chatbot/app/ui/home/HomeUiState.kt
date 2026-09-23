package com.aitutor.chatbot.app.ui.home

import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.ModeRecommendation
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.UserPlan

/** One bar in the dashboard's 7-day activity chart. */
data class DayActivity(val label: String, val count: Int, val isToday: Boolean)

data class DashboardData(
    val greeting: String,
    val profile: StudentProfile,
    val plan: UserPlan,
    val modes: List<Mode>,
    val recommendations: List<ModeRecommendation>,
    val recentChats: List<ChatSummary>,
    val weeklyActivity: List<DayActivity>,
    val streakDays: Int,
    val questionsAsked: Int,
    val chatsThisWeek: Int,
)
