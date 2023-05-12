package digital.paynetics.phos.app2app

import com.google.gson.annotations.SerializedName
import digital.paynetics.phos.sdk.entities.Transaction
import digital.paynetics.phos.sdk.enums.ReceiptType
import digital.paynetics.phos.sdk.enums.TransactionState
import digital.paynetics.phos.sdk.enums.TransactionType
import java.math.BigDecimal
import java.util.*

enum class Operation {
    @SerializedName("login")
    LOGIN,

    @SerializedName("sale")
    SALE,

    @SerializedName("refund")
    REFUND,

    @SerializedName("void")
    VOID,

    @SerializedName("transaction_history")
    HISTORY,

    @SerializedName("send_receipt")
    RECEIPT,

    @SerializedName("set_language")
    LANGUAGE;
}

sealed class OperationData {
    data class Login(
            @SerializedName("issuer")
            val issuer: String,
            @SerializedName("token")
            val token: String,
            @SerializedName("license")
            val license: String,
    ) : OperationData()

    data class Sale(
            @SerializedName("amount")
            val amount: BigDecimal,
            @SerializedName("tip")
            val tip: BigDecimal,
            @SerializedName("order_reference")
            val orderReference: String,
            @SerializedName("extras")
            val extras: Map<String, String>,
    ) : OperationData()

    data class Refund(
            @SerializedName("transaction_id")
            val transactionId: String,
            @SerializedName("amount")
            val amount: BigDecimal,
            @SerializedName("extras")
            val extras: Map<String, String>,
    ) : OperationData()

    data class Void(
            @SerializedName("transaction_id")
            val transactionId: String,
            @SerializedName("extras")
            val extras: Map<String, String>,
    ) : OperationData()

    data class History(
            @SerializedName("page")
            val page: Int,
            @SerializedName("limit")
            val limit: Int,
            @SerializedName("date")
            val date: Date,
            @SerializedName("transaction_type")
            val transactionType: TransactionType,
            @SerializedName("transaction_state")
            val transactionState: TransactionState
    ) : OperationData()

    data class Language(
            @SerializedName("language")
            val language: String
    ) : OperationData()

    data class Receipt(
            @SerializedName("transaction_id")
            val transactionId: String,
            @SerializedName("type")
            val type: ReceiptType,
            @SerializedName("recipient")
            val recipient: String
    ) : OperationData()
}

data class OperationRequest(
        @SerializedName("operation")
        val operation: Operation,
        @SerializedName("data")
        val data: OperationData?
)

data class OperationResult<T>(
        @SerializedName("resultCode")
        val resultCode: Int,
        @SerializedName("responseBody")
        val responseBody: T
)

data class TransactionBody(
        @SerializedName("transaction")
        val transaction: Transaction?,
        @SerializedName("extras")
        val extras: Map<String, String>?
)

data class SignedPayload(
        @SerializedName("payload")
        val payload: String,
        @SerializedName("signature")
        val signature: String
)

