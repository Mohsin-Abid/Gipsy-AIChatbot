package com.askylix.app.data.repository

import com.askylix.app.data.prefs.AccountPreferences
import com.askylix.app.data.remote.ApiBodies
import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.AskylixApi
import com.askylix.app.data.remote.PlanDto
import com.askylix.app.data.remote.PurchaseDto
import com.askylix.app.data.remote.QuotaDto
import com.askylix.app.data.session.SessionStore
import java.util.UUID

/**
 * Pro. The plan is only ever what the server says it is: a purchase is sent here to be checked, and
 * the plan that comes back is the only thing that is stored. Nothing on the phone grants it.
 */
class BillingRepository(
    private val api: AskylixApi,
    private val caller: ApiCaller,
    private val preferences: AccountPreferences,
    private val session: SessionStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Sends purchases from Google Play to be verified. After a purchase, send that one; to restore,
     * send every active one. Each attempt has its own key, and a retry of the same attempt reuses
     * [attemptKey] so the server grants once.
     */
    suspend fun verify(
        purchases: List<PurchaseDto>,
        attemptKey: String = UUID.randomUUID().toString(),
    ): Result<PlanDto> = runCatching {
        val plan = caller.call { api.verify(attemptKey, ApiBodies.verify(purchases)) }
        preferences.setIsPro(plan.isPro)
        session.me.value?.let { me ->
            session.setMe(
                me.copy(plan = plan, quota = if (plan.isPro) QuotaDto(unlimited = true) else me.quota),
                now(),
            )
        }
        plan
    }
}
