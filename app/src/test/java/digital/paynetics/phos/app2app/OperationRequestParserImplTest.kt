package digital.paynetics.phos.app2app

import com.google.common.truth.Truth
import com.google.gson.GsonBuilder
import digital.paynetics.phos.sdk.enums.ReceiptType
import digital.paynetics.phos.sdk.enums.TransactionState
import digital.paynetics.phos.sdk.enums.TransactionType
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class OperationRequestParserImplTest {

    private val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, TransactionDateTypeAdapter())
            .registerTypeAdapterFactory(OperationRequestTypeAdapterFactory())
            .create()
    private val sut = OperationRequestParserImpl(gson)

    @Test
    fun parseLogin() {
        val payload = """
            {
                "operation": "login",
                "data": {
                    "issuer": "issuer",
                    "token": "token",
                    "license": "license"
                }
            }
        """.trimIndent()
        val expected = OperationRequest(Operation.LOGIN, OperationData.Login("issuer", "token", "license"))
        val actual = sut.parse(payload)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseSale() {
        val payload = """
            {
                "operation": "sale",
                "data": {
                    "amount": "123.45",
                    "tip": "12.34",
                    "order_reference": "ORDER_REF",
                    "extras" : {
                        "key1": "value1",
                        "key2": "value2"
                    }
                }
            }
        """.trimIndent()
        val extras: MutableMap<String, String> = mutableMapOf()
        extras["key1"] = "value1"
        extras["key2"] = "value2"
        val expected = OperationRequest(Operation.SALE, OperationData.Sale(BigDecimal("123.45"), BigDecimal("12.34"), "ORDER_REF", extras))
        val actual = sut.parse(payload)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseRefund() {
        val payload = """
            {
                "operation": "refund",
                "data": {
                    "transaction_id": "TRANSACTION_ID",
                    "amount": "123.45",
                    "extras" : {
                        "key1": "value1",
                        "key2": "value2"
                    }
                }
            }
        """.trimIndent()
        val extras: MutableMap<String, String> = mutableMapOf()
        extras["key1"] = "value1"
        extras["key2"] = "value2"
        val expected = OperationRequest(Operation.REFUND, OperationData.Refund("TRANSACTION_ID", BigDecimal("123.45"), extras))
        val actual = sut.parse(payload)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseVoid() {
        val payload = """
            {
                "operation": "void",
                "data": {
                    "transaction_id": "TRANSACTION_ID",
                    "extras" : {
                        "key1": "value1",
                        "key2": "value2"
                    }
                }
            }
        """.trimIndent()
        val extras: MutableMap<String, String> = mutableMapOf()
        extras["key1"] = "value1"
        extras["key2"] = "value2"
        val expected = OperationRequest(Operation.VOID, OperationData.Void("TRANSACTION_ID", extras))
        val actual = sut.parse(payload)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseHistory() {
        val payload = """
            {
                "operation": "transaction_history",
                "data": {
                    "page": 1,
                    "limit": 50,
                    "date": "2022-10-14T20:00:00UTC",
                    "transaction_type": "sale",
                    "transaction_state": "failed"
                }
            }
        """.trimIndent()
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        calendar.set(2022, Calendar.OCTOBER, 14, 20, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val date = calendar.time
        println(gson.toJson(date))
        val expectedData = OperationData.History(1, 50, date, TransactionType.SALE, TransactionState.FAILED)
        val expected = OperationRequest(Operation.HISTORY, expectedData)
        val actual = sut.parse(payload)

        Truth.assertThat(actual.operation).isEqualTo(expected.operation)
        val actualData = actual.data as OperationData.History
        Truth.assertThat(actualData.page).isEqualTo(expectedData.page)
        Truth.assertThat(actualData.limit).isEqualTo(expectedData.limit)
        Truth.assertThat(actualData.date.time).isEqualTo(expectedData.date.time)
        Truth.assertThat(actualData.transactionType).isEqualTo(expectedData.transactionType)
        Truth.assertThat(actualData.transactionState).isEqualTo(expectedData.transactionState)
    }

    @Test
    fun parseLanguage() {
        val payload = """
            {
                "operation": "set_language",
                "data": {
                    "language": "en"
                    }
            }
        """.trimIndent()
        val expected = OperationRequest(Operation.LANGUAGE, OperationData.Language("en"))
        val actual = sut.parse(payload)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseReceipt() {
        val payload = """
            {
                "operation": "send_receipt",
                "data": {
                    "transaction_id": "TRANSACTION_ID",
                    "type": "email",
                    "recipient": "example@email.com"
                    }
            }
        """.trimIndent()
        val expected = OperationRequest(Operation.RECEIPT, OperationData.Receipt("TRANSACTION_ID", ReceiptType.EMAIL, "example@email.com"))
        val actual = sut.parse(payload)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}