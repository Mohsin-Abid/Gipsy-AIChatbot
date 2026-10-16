package com.askylix.app

import com.askylix.app.data.prefs.AccountPreferences
import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.ApiClient
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.MeDto
import com.askylix.app.data.remote.PurchaseDto
import com.askylix.app.data.remote.QuotaDto
import com.askylix.app.data.remote.TokenProvider
import com.askylix.app.data.repository.BillingRepository
import com.askylix.app.data.session.SessionStore
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.StudentProfile
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pro is only ever what the server says. */
class BillingRepositoryTest {

    private lateinit var server: MockWebServer
    private val session = SessionStore()
    private var storedPlan: Boolean? = null
    private lateinit var repo: BillingRepository

    private val prefs = object : AccountPreferences {
        override suspend fun isFirstRunComplete() = true
        override suspend fun setIsPro(pro: Boolean) { storedPlan = pro }
        override suspend fun applyAccount(profile: StudentProfile, language: AppLanguage?, isPro: Boolean, seedReminders: Boolean) = Unit
        override suspend fun clearAccountData() = Unit
    }

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val client = ApiClient(server.url("/v1/").toString(), TokenProvider { "T" })
        repo = BillingRepository(client.api, ApiCaller(), prefs, session)
        session.setMe(MeDto(userId = "u1", quota = QuotaDto(limit = 15, remaining = 3)), at = 1L)
    }

    @After
    fun stop() = runCatching { server.shutdown() }.let { }

    private val purchase = listOf(PurchaseDto("premium_yearly", "token-from-play"))

    @Test
    fun `a verified purchase makes the account Pro, with no daily limit`() = runBlocking {
        server.enqueue(jsonResponse("""{"plan":"PRO","productId":"premium_yearly","expiresAt":"2027-10-15T09:50:26Z","autoRenewing":true}"""))

        val plan = repo.verify(purchase, attemptKey = "attempt-1").getOrThrow()

        assertTrue(plan.isPro)
        assertEquals(true, storedPlan)
        assertTrue(session.me.value!!.plan.isPro)
        assertTrue(session.quota.value!!.unlimited)
        val request = server.takeRequest()
        assertEquals("attempt-1", request.getHeader("Idempotency-Key"))
        assertEquals("""{"purchases":[{"productId":"premium_yearly","purchaseToken":"token-from-play"}]}""", request.body.readUtf8())
    }

    @Test
    fun `a purchase the server cannot verify changes nothing`() = runBlocking {
        server.enqueue(errorResponse(402, "PAYMENT_VERIFICATION_FAILED"))

        val result = repo.verify(purchase)

        assertEquals(ApiErrorCode.PaymentVerificationFailed, (result.exceptionOrNull() as ApiError).code)
        assertNull("the plan was never touched", storedPlan)
        assertEquals(3, session.quota.value?.remaining)
    }

    @Test
    fun `restoring sends every active purchase at once`() = runBlocking {
        server.enqueue(jsonResponse("""{"plan":"PRO","productId":"premium_monthly"}"""))

        repo.verify(listOf(PurchaseDto("premium_monthly", "a"), PurchaseDto("premium_yearly", "b")))

        assertEquals(2, Regex("purchaseToken").findAll(server.takeRequest().body.readUtf8()).count())
    }
}
