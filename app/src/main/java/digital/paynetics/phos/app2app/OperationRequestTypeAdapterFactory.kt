package digital.paynetics.phos.app2app

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

// https://stackoverflow.com/a/36784255
class OperationRequestTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!OperationRequest::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val jsonElementAdapter = gson.getAdapter(JsonElement::class.java)
        val loginAdapter: TypeAdapter<OperationData.Login> = gson.getDelegateAdapter(this, TypeToken.get(OperationData.Login::class.java))
        val saleAdapter: TypeAdapter<OperationData.Sale> = gson.getDelegateAdapter(this, TypeToken.get(OperationData.Sale::class.java))
        val refundAdapter: TypeAdapter<OperationData.Refund> = gson.getDelegateAdapter(this, TypeToken.get(OperationData.Refund::class.java))
        val voidAdapter: TypeAdapter<OperationData.Void> = gson.getDelegateAdapter(this, TypeToken.get(OperationData.Void::class.java))
        val historyAdapter: TypeAdapter<OperationData.History> = gson.getDelegateAdapter(this, TypeToken.get(OperationData.History::class.java))
        val languageAdapter: TypeAdapter<OperationData.Language> = gson.getDelegateAdapter(this, TypeToken.get(OperationData.Language::class.java))
        val receiptAdapter: TypeAdapter<OperationData.Receipt> = gson.getDelegateAdapter(this, TypeToken.get(OperationData.Receipt::class.java))

        return OperationDataTypeAdapter(jsonElementAdapter, loginAdapter,
                saleAdapter, refundAdapter, voidAdapter, historyAdapter, languageAdapter, receiptAdapter
        ) as TypeAdapter<T>
    }


    private class OperationDataTypeAdapter(
            private val jsonElementAdapter: TypeAdapter<JsonElement>,
            private val loginAdapter: TypeAdapter<OperationData.Login>,
            private val saleAdapter: TypeAdapter<OperationData.Sale>,
            private val refundAdapter: TypeAdapter<OperationData.Refund>,
            private val voidAdapter: TypeAdapter<OperationData.Void>,
            private val historyAdapter: TypeAdapter<OperationData.History>,
            private val languageAdapter: TypeAdapter<OperationData.Language>,
            private val receiptAdapter: TypeAdapter<OperationData.Receipt>
    ) : TypeAdapter<OperationRequest>() {

        override fun write(out: JsonWriter?, value: OperationRequest) {
            if (value.javaClass.isAssignableFrom(OperationData.Login::class.java)) {
                loginAdapter.write(out, value as OperationData.Login)
            } else if (value.javaClass.isAssignableFrom(OperationData.Sale::class.java)) {
                saleAdapter.write(out, value as OperationData.Sale)
            } else if (value.javaClass.isAssignableFrom(OperationData.Refund::class.java)) {
                refundAdapter.write(out, value as OperationData.Refund)
            } else if (value.javaClass.isAssignableFrom(OperationData.Void::class.java)) {
                voidAdapter.write(out, value as OperationData.Void)
            } else if (value.javaClass.isAssignableFrom(OperationData.History::class.java)) {
                historyAdapter.write(out, value as OperationData.History)
            } else if (value.javaClass.isAssignableFrom(OperationData.Language::class.java)) {
                languageAdapter.write(out, value as OperationData.Language)
            } else if (value.javaClass.isAssignableFrom(OperationData.Receipt::class.java)) {
                receiptAdapter.write(out, value as OperationData.Receipt)
            }
        }

        override fun read(reader: JsonReader): OperationRequest? {
            val objectJson = jsonElementAdapter.read(reader).asJsonObject
            val operation = objectJson.get("operation").asString
            val data = objectJson.getAsJsonObject("data")

            return when (operation) {
                "login" -> OperationRequest(Operation.LOGIN, loginAdapter.fromJsonTree(data))
                "sale" -> OperationRequest(Operation.SALE, saleAdapter.fromJsonTree(data))
                "refund" -> OperationRequest(Operation.REFUND, refundAdapter.fromJsonTree(data))
                "void" -> OperationRequest(Operation.VOID, voidAdapter.fromJsonTree(data))
                "transaction_history" -> OperationRequest(Operation.HISTORY, historyAdapter.fromJsonTree(data))
                "set_language" -> OperationRequest(Operation.LANGUAGE, languageAdapter.fromJsonTree(data))
                "send_receipt" -> OperationRequest(Operation.RECEIPT, receiptAdapter.fromJsonTree(data))
                else -> null
            }

        }
    }

}