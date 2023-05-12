package digital.paynetics.phos.app2app

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import digital.paynetics.phos.PhosSdk
import digital.paynetics.phos.dagger.Operations
import digital.paynetics.phos.screens.App2AppActivity
import digital.paynetics.phos.sdk.ResponseCode
import digital.paynetics.phos.sdk.callback.AuthCallback
import digital.paynetics.phos.sdk.entities.Transaction
import digital.paynetics.phos.sdk.entities.Transactions
import javax.inject.Inject

class App2App
@Inject constructor(
        private val payloadSigner: PayloadSigner,
        private val operationRequestParser: OperationRequestParser,
        @Operations private val gson: Gson
) {
    var operationRequest: OperationRequest? = null

    fun init(activity: Activity) {
        val operationConfiguration: String? = activity.intent.getStringExtra(
                "${activity.packageName}.${App2AppKeys.OPERATION_CONFIGURATION}")

        if (operationConfiguration != null) {
            val signedPayload = gson.fromJson(operationConfiguration, SignedPayload::class.java)

            val payload = payloadSigner.decrypt(signedPayload)
            operationRequest = operationRequestParser.parse(payload)
        }
    }

    fun handleLogin(callback: AuthCallback): Boolean {
        operationRequest?.let { request ->
            if (request.operation == Operation.LOGIN) {
                val loginData = request.data as? OperationData.Login
                loginData?.let {
                    PhosSdk.getInstance().authenticate(it.issuer, it.token, it.license, callback)
                    return true
                }
                return false
            }
            return false
        }
        return false
    }

    fun handleStartIntent(activity: Activity): Boolean {
        operationRequest?.let {
            activity.startActivity(Intent(activity, App2AppActivity::class.java).apply {
                putExtras(activity.intent)
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            })
            activity.finish()
            return true
        }
        return false
    }

    fun createLoginSuccessResponse(): String {
        val loginResult = OperationResult(
                resultCode = ResponseCode.SUCCESS.code,
                "Success"
        )

        return signOperationResult(loginResult)
    }

    fun createLoginFailureResponse(code: Int): String {
        val loginResult = OperationResult(
                resultCode = code,
                "Failure"
        )
        return signOperationResult(loginResult)
    }

    private fun <T> signOperationResult(result: OperationResult<T>): String {
        val payload = gson.toJson(result)
        val signedPayload = payloadSigner.sign(payload)
        return gson.toJson(signedPayload)
    }

    fun createTransactionOperationResult(code: Int, transaction: Transaction?, extras: Map<String, String>?): String {
        val result = OperationResult(code, TransactionBody(transaction, extras))
        return signOperationResult(result)
    }

    fun createHistoryOperationResult(code: Int, transactions: Transactions?): String {
        val result = OperationResult(code, transactions)
        return signOperationResult(result)
    }

    fun createLanguageOperationResult(code: Int, message: String): String {
        val result = OperationResult(code, message)
        return signOperationResult(result)
    }

    fun createReceiptOperationResult(code: Int, message: String): String {
        val result = OperationResult(code, message)
        return signOperationResult(result)
    }

}