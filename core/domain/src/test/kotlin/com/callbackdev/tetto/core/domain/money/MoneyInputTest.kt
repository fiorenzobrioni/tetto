package com.callbackdev.tetto.core.domain.money

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.JPY
import com.callbackdev.tetto.core.domain.KWD
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.money.MoneyInput.Key
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The numpad's buffer (PLANNING.md §4.1; §4.7, Money). */
class MoneyInputTest {
    private fun MoneyInput.type(keys: String, separator: Char = ','): MoneyInput = keys.fold(this) { input, char ->
        input.press(checkNotNull(MoneyInput.keyFor(char, separator)) { "No key for $char" })
    }

    private fun typed(keys: String, currency: AppCurrency = EUR) = MoneyInput.empty(currency).type(keys)

    @Test
    fun `0,10 plus 0,20 is exactly 30 cents`() {
        val sum = typed("0,10").amount + typed("0,20").amount
        assertThat(sum).isEqualTo(Money(30))
        // The sum a binary floating point gets wrong, a cent at a time.
        val tenTimes = (1..10).map { typed("0,10").amount }.fold(Money.ZERO, Money::plus)
        assertThat(tenTimes).isEqualTo(euros(1))
    }

    @Test
    fun `the separator of each locale is the separator key`() {
        assertThat(MoneyInput.keyFor(',', decimalSeparator = ',')).isEqualTo(Key.Separator)
        assertThat(MoneyInput.keyFor('.', decimalSeparator = '.')).isEqualTo(Key.Separator)
        // Arabic's own decimal separator, and its digits.
        assertThat(MoneyInput.keyFor('٫', decimalSeparator = '٫')).isEqualTo(Key.Separator)
        assertThat(MoneyInput.keyFor('٣', decimalSeparator = '٫')).isEqualTo(Key.Digit(3))
        // A dot typed on an Italian phone's keyboard still means the decimals: no grouping is typed.
        assertThat(MoneyInput.keyFor('.', decimalSeparator = ',')).isEqualTo(Key.Separator)
        assertThat(MoneyInput.keyFor('a', decimalSeparator = ',')).isNull()
        assertThat(MoneyInput.empty(EUR).type("12.5", separator = '.').amount).isEqualTo(euros(12, 50))
        assertThat(MoneyInput.empty(EUR).type("12,5", separator = ',').amount).isEqualTo(euros(12, 50))
    }

    @Test
    fun `a currency with no decimals has no separator key`() {
        val yen = MoneyInput.empty(JPY)
        assertThat(yen.hasSeparator).isFalse()
        assertThat(yen.accepts(Key.Separator)).isFalse()
        assertThat(typed("1250", JPY).amount).isEqualTo(Money(1250))
        assertThat(typed("12", JPY).press(Key.Separator)).isEqualTo(typed("12", JPY))
    }

    @Test
    fun `a currency with three decimals takes three, and refuses the fourth`() {
        val dinars = typed("1,234", KWD)
        assertThat(dinars.amount).isEqualTo(Money(1_234))
        assertThat(dinars.accepts(Key.Digit(5))).isFalse()
        assertThat(typed("2,5", KWD).amount).isEqualTo(Money(2_500))
    }

    @Test
    fun `cents beyond the currency's decimals are refused`() {
        val input = typed("3,99")
        assertThat(input.accepts(Key.Digit(9))).isFalse()
        assertThat(input.press(Key.Digit(9))).isSameInstanceAs(input)
        assertThat(input.amount).isEqualTo(euros(3, 99))
    }

    @Test
    fun `nine digits before the separator, and the tenth refused`() {
        val input = typed("999999999")
        assertThat(input.amount).isEqualTo(euros(999_999_999))
        assertThat(input.accepts(Key.Digit(9))).isFalse()
        // The decimals are still open after nine digits.
        assertThat(input.type(",99").amount).isEqualTo(euros(999_999_999, 99))
    }

    @Test
    fun `a zero amount cannot be added`() {
        assertThat(MoneyInput.empty(EUR).canAdd).isFalse()
        assertThat(typed("0").canAdd).isFalse()
        assertThat(typed("0,00").canAdd).isFalse()
        assertThat(typed(",").canAdd).isFalse()
        assertThat(typed("0,01").canAdd).isTrue()
    }

    @Test
    fun `a leading zero is replaced, and a second zero refused`() {
        assertThat(typed("05").whole).isEqualTo("5")
        assertThat(typed("0").accepts(Key.Digit(0))).isFalse()
        assertThat(typed(",5").whole).isEqualTo("0")
        assertThat(typed(",5").amount).isEqualTo(euros(0, 50))
    }

    @Test
    fun `one separator only`() {
        val input = typed("1,")
        assertThat(input.accepts(Key.Separator)).isFalse()
        assertThat(input.fraction).isEmpty()
    }

    @Test
    fun `delete takes the last key back, and clear takes them all`() {
        val input = typed("12,34")
        assertThat(input.press(Key.Delete).amount).isEqualTo(euros(12, 30))
        assertThat(input.press(Key.Delete).press(Key.Delete).fraction).isEmpty()
        assertThat(input.press(Key.Delete).press(Key.Delete).press(Key.Delete).fraction).isNull()
        assertThat(input.press(Key.Clear).isEmpty).isTrue()
        assertThat(MoneyInput.empty(EUR).accepts(Key.Delete)).isFalse()
        assertThat(MoneyInput.empty(EUR).accepts(Key.Clear)).isFalse()
    }

    @Test
    fun `a spend's amount is typed back for a correction`() {
        assertThat(MoneyInput.of(euros(5), EUR)).isEqualTo(typed("5"))
        assertThat(MoneyInput.of(euros(5, 50), EUR)).isEqualTo(typed("5,5"))
        assertThat(MoneyInput.of(euros(5, 5), EUR)).isEqualTo(typed("5,05"))
        assertThat(MoneyInput.of(Money(1250), JPY)).isEqualTo(typed("1250", JPY))
        assertThat(MoneyInput.of(euros(5, 5), EUR).amount).isEqualTo(euros(5, 5))
    }
}
