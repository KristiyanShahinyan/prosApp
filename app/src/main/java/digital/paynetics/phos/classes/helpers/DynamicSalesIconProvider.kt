package digital.paynetics.phos.classes.helpers

import android.content.Context
import digital.paynetics.phos.R
import digital.paynetics.phos.sdk.entities.ClientConfig
import java.util.*
import javax.inject.Inject

class DynamicSalesIconProvider
@Inject constructor(
    private val clientConfig: ClientConfig,
    private val context: Context
){

    private fun isDynamicSalesIconEnabled(): Boolean {
        return  context.resources.getBoolean(R.bool.dynamic_sales_icon_enabled)
    }

    private fun getDynamicSalesIcon(): Int {
        var currency = ""
        if (clientConfig.terminal != null && clientConfig.terminal.currency != null) {
            currency = clientConfig.terminal.currency.uppercase(Locale.getDefault())
        }
        return when (currency) {
            "EUR" -> R.drawable.ic_euro
            "GBP" -> R.drawable.ic_pound
            "USD" -> R.drawable.ic_dollar
            else -> R.drawable.ic_payment
        }
    }

    fun getSalesIcon(): Int {
        return if (isDynamicSalesIconEnabled()) {
            getDynamicSalesIcon()
        } else R.drawable.default_sales_icon
    }
}