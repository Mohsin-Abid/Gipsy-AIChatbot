package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.icons.AppIcons

/**
 * What Pro unlocks. Four claims, each one a feature this app either has or is going to have — none
 * of them is filler, because a paywall that promises something absent is the fastest way to earn a
 * refund and a one-star review.
 */
enum class ProBenefit(
    @param:StringRes val titleRes: Int,
    @param:StringRes val detailRes: Int,
    val icon: ImageVector,
) {
    UnlimitedQuestions(R.string.pro_benefit_unlimited, R.string.pro_benefit_unlimited_detail, AppIcons.Infinity),
    StepByStep(R.string.pro_benefit_steps, R.string.pro_benefit_steps_detail, AppIcons.Math),
    DocumentSummaries(R.string.pro_benefit_documents, R.string.pro_benefit_documents_detail, AppIcons.Document),
    AllVoices(R.string.pro_benefit_voices, R.string.pro_benefit_voices_detail, AppIcons.SpeakerWave),
}

/**
 * The two subscription terms.
 *
 * [productId] is what Play Billing is asked for. The ids are the conventional ones and must match
 * the products created in the Play Console — nothing here can verify that, so they are the one thing
 * on this screen worth double-checking before release.
 */
enum class ProPlan(
    val productId: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val periodRes: Int,
    @param:StringRes val renewalRes: Int,
    /** The line under the price when no per-month equivalent is available to show instead. */
    @param:StringRes val footnoteRes: Int,
    /** The term the design badges, and the one selected when the screen opens. */
    val bestValue: Boolean = false,
) {
    Yearly(
        productId = "premium_yearly",
        labelRes = R.string.pro_plan_yearly,
        periodRes = R.string.pro_plan_per_year,
        renewalRes = R.string.pro_renews_yearly,
        footnoteRes = R.string.pro_plan_billed_yearly,
        bestValue = true,
    ),
    Monthly(
        productId = "premium_monthly",
        labelRes = R.string.pro_plan_monthly,
        periodRes = R.string.pro_plan_per_month,
        renewalRes = R.string.pro_renews_monthly,
        footnoteRes = R.string.pro_plan_billed_monthly,
    ),
}

/**
 * A plan with a price attached.
 *
 * [formattedPrice] is deliberately a pre-formatted string rather than a number and a currency code:
 * Play Billing returns exactly that, already localized for the buyer's country, and reformatting it
 * — or worse, hardcoding it — is how an app ends up advertising the wrong price in half its markets.
 *
 * A null price means Play has not answered yet, which is why the screen can't let a purchase start.
 */
data class PlanOffer(
    val plan: ProPlan,
    val formattedPrice: String? = null,
    /** A yearly plan's monthly equivalent, which is the comparison that makes it look worth it. */
    val formattedPerMonth: String? = null,
) {
    val isAvailable: Boolean get() = formattedPrice != null
}
