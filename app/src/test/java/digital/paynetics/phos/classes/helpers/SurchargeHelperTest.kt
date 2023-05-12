package digital.paynetics.phos.classes.helpers


import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import digital.paynetics.phos.sdk.entities.Terminal
import org.junit.Test
import java.math.BigDecimal

class SurchargeHelperTest {



    @Test
    fun `hasSurcharge SHOULD return false WHEN surcharge is null`() {
        val sut = SurchargeHelper(null, 2)
        assertThat(sut.hasSurcharge()).isFalse()
    }

    @Test
    fun `hasSurcharge SHOULD return false WHEN surcharge type is not multiplier`() {
        val sut = SurchargeHelper(Terminal.Surcharge(Terminal.SurchargeType.FLAT, BigDecimal.ONE), 2)
        assertThat(sut.hasSurcharge()).isFalse()
    }

    @Test
    fun `hasSurcharge SHOULD return false WHEN surcharge type is multiplier`() {
        val sut = SurchargeHelper(Terminal.Surcharge(Terminal.SurchargeType.MULTIPLIER, BigDecimal.ONE), 2)
        assertThat(sut.hasSurcharge()).isTrue()
    }

    @Test
    fun `calculateSurcharge SHOULD return 0 WHEN hasSurcharge returns 0`() {
        val sut = SurchargeHelper(null, 2)
        val surcharge = sut.calculateSurcharge(BigDecimal("100.00"), false)
        assertThat(surcharge).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `calculateSurcharge SHOULD return the correct amount WHEN alreadyScaled is false`() {
        val sut = SurchargeHelper(Terminal.Surcharge(Terminal.SurchargeType.MULTIPLIER, BigDecimal("0.01")), 2)
        val surcharge = sut.calculateSurcharge(BigDecimal("550"), false)
        assertThat(surcharge).isEqualTo(BigDecimal("5.50"))
    }

    @Test
    fun `calculateSurcharge SHOULD return the correct amount WHEN alreadyScaled is true`() {
        val sut = SurchargeHelper(Terminal.Surcharge(Terminal.SurchargeType.MULTIPLIER, BigDecimal("0.01")), 2)
        val surcharge = sut.calculateSurcharge(BigDecimal("550"), true)
        assertThat(surcharge).isEqualTo(BigDecimal("6"))
    }
}