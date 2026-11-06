package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.model.Money

/**
 * The sentence under the numpad's amount (PLANNING.md §4.6): what the spend typed would leave.
 *
 * - [Kind.FITS]: `after ≥ 0`, [amount] is what is left after it ("€15 left");
 * - [Kind.GOES_OVER]: `left ≥ 0 > after`, [amount] is how far over it goes ("€30 over");
 * - [Kind.STILL_OVER]: the cap was over already, [amount] is how far over it stays ("still €45 over").
 */
data class SpendPreview(val kind: Kind, val amount: Money) {
    enum class Kind {
        FITS,
        GOES_OVER,
        STILL_OVER,
    }

    companion object {
        /**
         * What a refund of [typed] would leave (owner, 6 Nov 2026): what is left grows by it; a cap
         * over may come back under ([Kind.FITS]) or stay over by less ([Kind.STILL_OVER]).
         */
        fun ofRefund(left: Money, typed: Money): SpendPreview {
            val after = left + typed
            return if (after.isNegative) SpendPreview(Kind.STILL_OVER, -after) else SpendPreview(Kind.FITS, after)
        }

        fun of(left: Money, typed: Money): SpendPreview {
            val after = left - typed
            return when {
                left.isNegative -> SpendPreview(Kind.STILL_OVER, -after)
                after.isNegative -> SpendPreview(Kind.GOES_OVER, -after)
                else -> SpendPreview(Kind.FITS, after)
            }
        }
    }
}
