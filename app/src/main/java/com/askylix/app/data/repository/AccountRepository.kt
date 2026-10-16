package com.askylix.app.data.repository

import com.askylix.app.data.prefs.AccountPreferences
import com.askylix.app.data.remote.ApiBodies
import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.AskylixApi
import com.askylix.app.data.remote.ConfigDto
import com.askylix.app.data.remote.DeleteAccountDto
import com.askylix.app.data.remote.MeDto
import com.askylix.app.data.remote.toAppLanguage
import com.askylix.app.data.remote.toProfile
import com.askylix.app.data.remote.toSignup
import com.askylix.app.data.remote.wire
import com.askylix.app.data.session.SessionStore
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.StudentProfile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Where a person goes once they have signed in. */
enum class SignInDestination {
    /** The account is set up. Its profile is on the phone now. */
    Home,

    /** A new account with no answers to send: the setup wizard comes next. */
    Setup,
}

/**
 * The signed-in account: who it is, what plan it has, and the profile and language that follow it
 * from device to device.
 *
 * It is also where a session ends. Signing out, deleting the account and a refused session all go
 * through [signOut], so the phone is cleared the same way whichever of them it was.
 */
class AccountRepository(
    private val api: AskylixApi,
    private val caller: ApiCaller,
    private val preferences: AccountPreferences,
    private val session: SessionStore,
    private val signOutOfFirebase: suspend () -> Unit,
    private val clearFiles: suspend () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val meLock = Mutex()

    val me: StateFlow<MeDto?> get() = session.me

    /**
     * The app's remote settings. It needs no sign-in, so it runs on every launch; it carries the token
     * when there is one, and a refusal here never ends the session.
     */
    suspend fun loadConfig(): Result<ConfigDto> = runCatching {
        caller.call(endsSessionOn401 = false) { api.config() }.also(session::setConfig)
    }

    /**
     * Reads the account from the server.
     *
     * A reading under [FRESH_MS] old is reused unless [force] is set, so opening three screens in a
     * row makes one call. Concurrent callers wait for the one in progress and share its result.
     */
    suspend fun refreshMe(force: Boolean = false): Result<MeDto> = meLock.withLock {
        val cached = session.me.value
        if (!force && cached != null && now() - session.meLoadedAt < FRESH_MS) {
            return@withLock Result.success(cached)
        }
        runCatching {
            val me = caller.call { api.me() }
            session.setMe(me, now())
            applyToPhone(me)
            me
        }
    }

    /**
     * What to do once Google has signed someone in.
     *
     * - An account that has been set up before comes straight in. Its own profile replaces anything
     *   on the phone: the account is the older and more complete record.
     * - A new account with [localAnswers] — from the wizard, done while skipped — is set up from them.
     * - A new account with none gets the wizard.
     */
    suspend fun resolveAfterSignIn(
        localAnswers: StudentProfile?,
        deviceLanguage: AppLanguage,
    ): Result<SignInDestination> = runCatching {
        val me = refreshMe(force = true).getOrThrow()
        when {
            me.setupCompleted -> SignInDestination.Home
            localAnswers != null -> {
                completeSetup(localAnswers, deviceLanguage).getOrThrow()
                SignInDestination.Home
            }
            else -> SignInDestination.Setup
        }
    }

    /**
     * Finishes the wizard for the signed-in account, unless the account is already set up.
     *
     * Setup can be interrupted after sign-in and picked up later, by which point the account may have
     * been set up from somewhere else. Sending the wizard's answers then would overwrite a profile that
     * is the account's own, so the account is asked first, and an account that is set up keeps what it has.
     */
    suspend fun finishSetup(profile: StudentProfile, deviceLanguage: AppLanguage): Result<Unit> = runCatching {
        val me = refreshMe(force = true).getOrThrow()
        if (!me.setupCompleted) completeSetup(profile, deviceLanguage).getOrThrow()
    }

    /**
     * Sends the setup answers and marks the account as set up.
     *
     * `POST /signup` expects at least one subject and one goal, which the wizard does not insist on:
     * every step can be skipped. When the server refuses an incomplete profile, the same answers go
     * through `PATCH /me` instead, with only what was filled in, so skipping the wizard never keeps
     * anyone out.
     *
     * A new account starts in English whatever the phone is set to, so the language chosen on the
     * first screen is sent up with it: the account takes the phone's language, not the other way round.
     */
    suspend fun completeSetup(profile: StudentProfile, deviceLanguage: AppLanguage): Result<Unit> = meLock.withLock {
        runCatching {
            val me = try {
                caller.call { api.signup(profile.toSignup()) }
            } catch (refused: ApiError) {
                if (refused.code != ApiErrorCode.ValidationFailed) throw refused
                caller.call {
                    api.updateMe(
                        ApiBodies.me(
                            profile = filledProfileFields(profile),
                            settings = buildJsonObject { put("remindersEnabled", profile.reminderEnabled) },
                            setupCompleted = true,
                        )
                    )
                }
            }
            session.setMe(me, now())
            // Best effort: if this one call fails the account is simply in English until the next
            // language change, which is no reason to stop someone getting into the app.
            runCatching {
                caller.call { api.updateMe(ApiBodies.me(settings = buildJsonObject { put("language", deviceLanguage.tag) })) }
            }.onSuccess { session.setMe(it, now()) }

            preferences.applyAccount(
                profile = profile,
                language = deviceLanguage,
                isPro = me.plan.isPro,
                seedReminders = true,
            )
        }
    }

    /**
     * Saves a change to the profile on the account. [fields] are the keys that changed, built by
     * [profilePatch]. A phone with no account has nothing to tell, so the caller does not call this.
     */
    suspend fun updateProfile(fields: JsonObject): Result<Unit> = meLock.withLock {
        runCatching {
            val me = caller.call { api.updateMe(ApiBodies.me(profile = fields)) }
            session.setMe(me, now())
        }
    }

    /** The language is the one setting that follows the account; the rest stay on each device. */
    suspend fun updateLanguage(language: AppLanguage): Result<Unit> = meLock.withLock {
        runCatching {
            val me = caller.call {
                api.updateMe(ApiBodies.me(settings = buildJsonObject { put("language", language.tag) }))
            }
            session.setMe(me, now())
        }
    }

    /** Deletes the account on the server, then clears this phone. */
    suspend fun deleteAccount(): Result<DeleteAccountDto> = runCatching {
        caller.call { api.deleteAccount() }.also { signOut() }
    }

    /** Ends the session on this phone: Firebase, the profile and plan, everything in memory, and downloaded files. */
    suspend fun signOut() {
        runCatching { signOutOfFirebase() }
        preferences.clearAccountData()
        session.clearAccount()
        runCatching { clearFiles() }
    }

    private suspend fun applyToPhone(me: MeDto) {
        val firstTimeOnThisPhone = !preferences.isFirstRunComplete()
        preferences.setIsPro(me.plan.isPro)
        // A fresh account has an empty profile. Writing it would erase answers that are still only on
        // this phone, so only an account that has been set up is copied down.
        if (me.setupCompleted) {
            preferences.applyAccount(
                profile = me.toProfile(),
                language = me.settings.language.toAppLanguage(),
                isPro = me.plan.isPro,
                seedReminders = firstTimeOnThisPhone,
            )
        }
    }

    companion object {
        /** How long a reading of the account is reused before a screen asks again. */
        const val FRESH_MS = 30_000L
    }
}

/** The fields of a profile that were actually filled in, for a `PATCH /me`. */
internal fun filledProfileFields(profile: StudentProfile): JsonObject = buildJsonObject {
    if (profile.name.isNotBlank()) put("name", profile.name.trim())
    profile.level?.let { put("level", it.wire()) }
    profile.grade?.let { put("grade", it.wire()) }
    if (profile.subjects.isNotEmpty()) {
        put("subjects", buildJsonArray { profile.subjects.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.wire())) } })
    }
    if (profile.goals.isNotEmpty()) {
        put("goals", buildJsonArray { profile.goals.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.wire())) } })
    }
    put("dailyTime", profile.dailyTime.wire())
}

/**
 * Only the profile fields that differ between [old] and [new], or null if none do.
 *
 * Reminders are left out on purpose: they belong to the device, and are not part of the account's
 * profile after setup. A field cleared to nothing is sent as an explicit null, because leaving it out
 * would mean "unchanged".
 */
fun profilePatch(old: StudentProfile, new: StudentProfile): JsonObject? {
    val changed = buildJsonObject {
        if (new.name != old.name) put("name", new.name.trim())
        if (new.level != old.level) put("level", new.level?.wire())
        if (new.grade != old.grade) put("grade", new.grade?.wire())
        if (new.subjects != old.subjects) {
            put("subjects", buildJsonArray { new.subjects.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.wire())) } })
        }
        if (new.goals != old.goals) {
            put("goals", buildJsonArray { new.goals.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.wire())) } })
        }
        if (new.dailyTime != old.dailyTime) put("dailyTime", new.dailyTime.wire())
    }
    return changed.takeIf { it.isNotEmpty() }
}
