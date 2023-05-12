package digital.paynetics.phos.app2app

import com.google.gson.Gson
import javax.inject.Inject

interface OperationRequestParser {
    fun parse(payload: String): OperationRequest
}

class OperationRequestParserImpl
@Inject
constructor(val gson: Gson) : OperationRequestParser {
    override fun parse(payload: String): OperationRequest {
        return gson.fromJson(payload, OperationRequest::class.java)
    }
}