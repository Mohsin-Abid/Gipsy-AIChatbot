package com.askylix.app.data.prefs

import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.StudentProfile

/**
 * The part of the phone's stored settings that the signed-in account reads and writes.
 *
 * It exists so the account and billing code can be tested without Android's DataStore: they only need
 * these few things, and [UserPreferencesRepository] is the one real implementation.
 */
interface AccountPreferences {
    /** Whether this phone has been through setup (or an account's own setup has been brought onto it). */
    suspend fun isFirstRunComplete(): Boolean

    /** The plan as the server last said it. Never written from anything but a server reply. */
    suspend fun setIsPro(pro: Boolean)

    /**
     * Puts the signed-in account's profile on this phone. Profile, language and plan follow the account;
     * reminders belong to the device and are only copied the first time ([seedReminders]).
     */
    suspend fun applyAccount(
        profile: StudentProfile,
        language: AppLanguage?,
        isPro: Boolean,
        seedReminders: Boolean,
    )

    /** Log-out or a deleted account: the profile and plan leave the phone, the device's own settings stay. */
    suspend fun clearAccountData()
}
