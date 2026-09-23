package com.aitutor.chatbot.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.domain.model.GradeLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow(StudentProfile())
    val profile: StateFlow<StudentProfile> = _profile.asStateFlow()

    fun onGradeSelected(grade: GradeLevel) {
        _profile.value = _profile.value.copy(grade = grade)
    }

    fun onSubjectToggled(subject: Subject) {
        val current = _profile.value.subjects
        _profile.value = _profile.value.copy(
            subjects = if (subject in current) current - subject else current + subject
        )
    }

    fun onGoalSelected(goal: StudyGoal) {
        _profile.value = _profile.value.copy(goal = goal)
    }

    /** Saves whatever was answered — every step is skippable, so a partial profile is valid. */
    fun onOnboardingFinished(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setStudentProfile(_profile.value)
            preferencesRepository.setOnboardingComplete(true)
            onDone()
        }
    }
}
