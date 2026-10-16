package com.askylix.app

import com.askylix.app.data.prefs.AccountPreferences
import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.ApiClient
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.TokenProvider
import com.askylix.app.data.repository.AccountRepository
import com.askylix.app.data.repository.SignInDestination
import com.askylix.app.data.session.SessionStore
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.Grade
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyGoal
import com.askylix.app.domain.model.StudyLevel
import com.askylix.app.domain.model.Subject
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** What the phone keeps of an account, written down as calls the test can read back. */
private class FakeAccountPreferences : AccountPreferences {
    var firstRunComplete = false
    var isPro: Boolean? = null
    var applied: Applied? = null
    var cleared = false

    data class Applied(val profile: StudentProfile, val language: AppLanguage?, val isPro: Boolean, val seedReminders: Boolean)

    override suspend fun isFirstRunComplete() = firstRunComplete
    override suspend fun setIsPro(pro: Boolean) { isPro = pro }
    override suspend fun applyAccount(profile: StudentProfile, language: AppLanguage?, isPro: Boolean, seedReminders: Boolean) {
        applied = Applied(profile, language, isPro, seedReminders)
        firstRunComplete = true
    }
    override suspend fun clearAccountData() { cleared = true }
}

/** The signed-in account: reading it, setting it up, changing it, and ending the session. */
class AccountRepositoryTest {

    private lateinit var server: MockWebServer
    private val prefs = FakeAccountPreferences()
    private val session = SessionStore()
    private var clock = 1_000_000L
    private var signedOutOfFirebase = false
    private var filesCleared = false
    private lateinit var repo: AccountRepository

    private val sara = StudentProfile(
        name = "Sara", level = StudyLevel.School, grade = Grade.Grade8,
        subjects = setOf(Subject.Math), goals = setOf(StudyGoal.AceExams), reminderEnabled = true,
    )

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val client = ApiClient(server.url("/v1/").toString(), TokenProvider { "T" })
        repo = AccountRepository(
            api = client.api, caller = ApiCaller(), preferences = prefs, session = session,
            signOutOfFirebase = { signedOutOfFirebase = true }, clearFiles = { filesCleared = true },
            now = { clock },
        )
    }

    @After
    fun stop() = runCatching { server.shutdown() }.let { }

    private fun me(setupCompleted: Boolean, language: String = "en", plan: String = "FREE", name: String? = "Sara") = jsonResponse(
        """{"userId":"u1","isAnonymous":false,"email":"s@x.test","displayName":"Sara Khan","setupCompleted":$setupCompleted,
        "profile":{"name":${name?.let { "\"$it\"" }},"level":"SCHOOL","grade":"GRADE_8","subjects":["MATH"],"goals":["ACE_EXAMS"],"dailyTime":"MINUTES_30"},
        "settings":{"language":"$language","remindersEnabled":true,"themeMode":"SYSTEM","voiceInputEnabled":true},
        "plan":{"plan":"$plan"},"quota":{"unlimited":false,"limit":15,"remaining":15}}"""
    )

    // ---- Reading ----

    @Test
    fun `an account that is set up brings its profile, language and plan onto the phone`() = runBlocking {
        server.enqueue(me(setupCompleted = true, language = "ur", plan = "PRO"))

        repo.refreshMe(force = true).getOrThrow()

        val applied = prefs.applied!!
        assertEquals("Sara", applied.profile.name)
        assertEquals(AppLanguage.Urdu, applied.language)
        assertTrue(applied.isPro)
        assertEquals(true, prefs.isPro)
    }

    @Test
    fun `a fresh account's empty profile never overwrites answers still only on the phone`() = runBlocking {
        server.enqueue(me(setupCompleted = false, name = null))

        repo.refreshMe(force = true).getOrThrow()

        assertNull("nothing applied", prefs.applied)
        assertEquals("but the plan is still recorded", false, prefs.isPro)
    }

    @Test
    fun `reminders are copied from the account only the first time on a phone`() = runBlocking {
        server.enqueue(me(setupCompleted = true))
        repo.refreshMe(force = true)
        assertTrue("first time: seeded", prefs.applied!!.seedReminders)

        server.enqueue(me(setupCompleted = true))
        repo.refreshMe(force = true)
        assertFalse("after that the device keeps its own", prefs.applied!!.seedReminders)
    }

    @Test
    fun `a reading under thirty seconds old is reused`() = runBlocking {
        server.enqueue(me(setupCompleted = true))

        repo.refreshMe(force = false)
        clock += 5_000
        repo.refreshMe(force = false)

        assertEquals(1, server.requestCount)
        assertNotNull(repo.me.value)
    }

    @Test
    fun `the app's settings are read, and carry the token like every other call`() = runBlocking {
        server.enqueue(jsonResponse("""{"minAppVersion":"1.0.0","latestAppVersion":"1.1.0","maintenance":{"active":false},"urls":{},"freeDailyLimit":15,"tools":[{"tool":"CHATBOT","enabled":false}]}"""))

        repo.loadConfig().getOrThrow()

        assertEquals("1.1.0", session.config.value?.latestAppVersion)
        assertFalse(session.toolFlags.value.getValue("CHATBOT").enabled)
        assertEquals("Bearer T", server.takeRequest().getHeader("Authorization"))
    }

    // ---- After signing in ----

    @Test
    fun `an account set up before comes straight in, with no setup call`() = runBlocking {
        server.enqueue(me(setupCompleted = true))

        val destination = repo.resolveAfterSignIn(localAnswers = sara, deviceLanguage = AppLanguage.English).getOrThrow()

        assertEquals(SignInDestination.Home, destination)
        assertEquals("only the read, nothing sent up", 1, server.requestCount)
    }

    @Test
    fun `a new account is set up from the answers saved while skipping`() = runBlocking {
        server.enqueue(me(setupCompleted = false))   // GET /me
        server.enqueue(me(setupCompleted = true))    // POST /signup
        server.enqueue(me(setupCompleted = true))    // PATCH language

        val destination = repo.resolveAfterSignIn(sara, AppLanguage.French).getOrThrow()

        assertEquals(SignInDestination.Home, destination)
        assertEquals("GET", server.takeRequest().method)
        val signup = server.takeRequest()
        assertEquals("/v1/signup", signup.path)
        assertTrue(signup.body.readUtf8().contains(""""subjects":["MATH"]"""))
        assertEquals("""{"settings":{"language":"fr"}}""", server.takeRequest().body.readUtf8())
        assertEquals("the phone's language is kept, not the account's English default", AppLanguage.French, prefs.applied!!.language)
    }

    @Test
    fun `a new account with no saved answers goes to the wizard`() = runBlocking {
        server.enqueue(me(setupCompleted = false))

        val destination = repo.resolveAfterSignIn(localAnswers = null, deviceLanguage = AppLanguage.English).getOrThrow()

        assertEquals(SignInDestination.Setup, destination)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `an account that cannot be reached is a failure, not a guess`() = runBlocking {
        server.shutdown()

        val result = repo.resolveAfterSignIn(sara, AppLanguage.English)

        assertTrue((result.exceptionOrNull() as ApiError).isConnectionProblem)
    }

    // ---- Setting up ----

    @Test
    fun `a wizard with steps skipped is accepted by falling back to a patch`() = runBlocking {
        val skipped = StudentProfile(name = "", level = null, grade = null, subjects = emptySet(), goals = emptySet())
        server.enqueue(errorResponse(400, "VALIDATION_FAILED"))   // /signup wants subjects and goals
        server.enqueue(me(setupCompleted = true))                  // PATCH /me
        server.enqueue(me(setupCompleted = true))                  // PATCH language

        repo.completeSetup(skipped, AppLanguage.English).getOrThrow()

        assertEquals("/v1/signup", server.takeRequest().path)
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        val body = patch.body.readUtf8()
        assertTrue(body.contains(""""setupCompleted":true"""))
        assertFalse("nothing was filled in, so no empty subjects are sent", body.contains("subjects"))
    }

    @Test
    fun `any other refusal of the answers is a failure`() = runBlocking {
        server.enqueue(errorResponse(500, "INTERNAL"))

        val result = repo.completeSetup(sara, AppLanguage.English)

        assertEquals(ApiErrorCode.Internal, (result.exceptionOrNull() as ApiError).code)
        assertNull("nothing was kept on the phone", prefs.applied)
    }

    @Test
    fun `finishing setup leaves an account that is already set up alone`() = runBlocking {
        server.enqueue(me(setupCompleted = true))

        repo.finishSetup(StudentProfile(name = "Someone else"), AppLanguage.English).getOrThrow()

        assertEquals("the account was only read; its profile was not overwritten", 1, server.requestCount)
        assertEquals("Sara", prefs.applied!!.profile.name)
    }

    @Test
    fun `finishing setup on a new account sends the answers`() = runBlocking {
        server.enqueue(me(setupCompleted = false))
        server.enqueue(me(setupCompleted = true))
        server.enqueue(me(setupCompleted = true))

        repo.finishSetup(sara, AppLanguage.English).getOrThrow()

        server.takeRequest()
        assertEquals("/v1/signup", server.takeRequest().path)
    }

    // ---- Changing it ----

    @Test
    fun `a profile change is sent as a patch of just that`() = runBlocking {
        server.enqueue(me(setupCompleted = true))
        val fields = kotlinx.serialization.json.buildJsonObject { put("name", kotlinx.serialization.json.JsonPrimitive("Sam")) }

        repo.updateProfile(fields).getOrThrow()

        assertEquals("""{"profile":{"name":"Sam"}}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `changing the language is a patch of the one setting`() = runBlocking {
        server.enqueue(me(setupCompleted = true, language = "ur"))

        repo.updateLanguage(AppLanguage.Urdu).getOrThrow()

        assertEquals("""{"settings":{"language":"ur"}}""", server.takeRequest().body.readUtf8())
    }

    // ---- Ending the session ----

    @Test
    fun `signing out clears Firebase, the profile and plan, memory, and downloaded files`() = runBlocking {
        session.setMe(com.askylix.app.data.remote.MeDto(userId = "u1"), 1L)

        repo.signOut()

        assertTrue(signedOutOfFirebase)
        assertTrue(prefs.cleared)
        assertNull(session.me.value)
        assertTrue(filesCleared)
    }

    @Test
    fun `signing out finishes even if one step fails`() = runBlocking {
        val failing = AccountRepository(
            api = ApiClient(server.url("/v1/").toString(), TokenProvider { "T" }).api,
            caller = ApiCaller(), preferences = prefs, session = session,
            signOutOfFirebase = { error("Firebase is unhappy") }, clearFiles = { filesCleared = true },
        )

        failing.signOut()

        assertTrue("the rest still happened", prefs.cleared && filesCleared)
    }

    @Test
    fun `deleting the account deletes it on the server, then clears the phone`() = runBlocking {
        server.enqueue(jsonResponse("""{"deleted":true,"activeSubscriptionRemains":true}"""))

        val result = repo.deleteAccount().getOrThrow()

        assertTrue("the person is told to cancel in Play", result.activeSubscriptionRemains)
        assertEquals("DELETE", server.takeRequest().method)
        assertTrue(signedOutOfFirebase && prefs.cleared)
    }

    @Test
    fun `an account that could not be deleted leaves the phone signed in`() = runBlocking {
        server.enqueue(errorResponse(500, "INTERNAL"))

        val result = repo.deleteAccount()

        assertTrue(result.isFailure)
        assertFalse(signedOutOfFirebase)
        assertFalse(prefs.cleared)
    }
}
