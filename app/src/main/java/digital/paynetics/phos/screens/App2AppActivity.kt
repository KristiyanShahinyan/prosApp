package digital.paynetics.phos.screens

import android.content.Intent
import android.os.Bundle
import digital.paynetics.phos.PhosApplication
import digital.paynetics.phos.PhosSdk
import digital.paynetics.phos.R
import digital.paynetics.phos.app2app.App2App
import digital.paynetics.phos.app2app.App2AppKeys
import digital.paynetics.phos.app2app.Operation
import digital.paynetics.phos.app2app.OperationData
import digital.paynetics.phos.classes.lang.PhosLanguage
import digital.paynetics.phos.exceptions.PhosException
import digital.paynetics.phos.internal.App2AppApi2Marker
import digital.paynetics.phos.sdk.PhosLogger
import digital.paynetics.phos.sdk.ResponseCode
import digital.paynetics.phos.sdk.callback.SendReceiptCallback
import digital.paynetics.phos.sdk.callback.TransactionCallback
import digital.paynetics.phos.sdk.callback.TransactionListCallback
import digital.paynetics.phos.sdk.entities.Entity
import digital.paynetics.phos.sdk.entities.Transaction
import digital.paynetics.phos.sdk.entities.Transactions
import digital.paynetics.phos.sdk.enums.ReceiptType
import digital.paynetics.phos.sdk.transaction.TransactionConfig
import javax.inject.Inject

class App2AppActivity : BaseActivity(), App2AppApi2Marker {

    companion object {
        private const val TAG = "App2AppActivity"
    }

    @Inject
    lateinit var app2app: App2App

    @Inject
    lateinit var logger: PhosLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        PhosApplication.getAppComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app2app)

        app2app.init(this)
        handleOperation()
    }

    private val transactionCallback = object : TransactionCallback {
        override fun onSuccess(transaction: Transaction?, extras: MutableMap<String, String>?) {
            setTransactionResult(ResponseCode.SUCCESS.code, transaction, extras)
        }

        override fun onFailure(transaction: Transaction?, e: PhosException, extras: MutableMap<String, String>?) {
            setTransactionResult(e.code, transaction, extras)
        }
    }

    private val sendReceiptCallback = object : SendReceiptCallback {

        override fun onSuccess(entity: Entity) {
            setReceiptResult(ResponseCode.SUCCESS.code,  "Success")
        }

        override fun onError(phosException: PhosException, extras: Map<String, String>?) {
            setReceiptResult(phosException.code,  "Failure")
        }

    }

    private fun setTransactionResult(code: Int, transaction: Transaction?, extras: Map<String, String>?) {
        val resultData = Intent().apply {
            putExtra(packageName + "." + App2AppKeys.OPERATION_RESULT,
                    app2app.createTransactionOperationResult(code, transaction, extras))
        }
        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun setHistoryResult(code: Int, transactions: Transactions?) {
        val resultData = Intent().apply {
            putExtra(packageName + "." + App2AppKeys.OPERATION_RESULT,
                    app2app.createHistoryOperationResult(code, transactions))
        }
        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun setLanguageResult(code: Int, message: String) {
        val resultData = Intent().apply {
            putExtra(packageName + "." + App2AppKeys.OPERATION_RESULT,
                    app2app.createLanguageOperationResult(code, message))
        }
        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun setReceiptResult(code: Int, message: String) {
        val resultData = Intent().apply {
            putExtra(packageName + "." + App2AppKeys.OPERATION_RESULT,
                    app2app.createReceiptOperationResult(code, message))
        }
        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun handleOperation() {
        app2app.operationRequest?.let {
            logger.d(TAG, it.data.toString())
            when (it.operation) {
                Operation.LOGIN -> {
                    logger.e(TAG, "LOGIN operation should not be handled here")
                    finish()
                }
                Operation.SALE, Operation.REFUND, Operation.VOID -> {
                    logger.d(TAG, "${it.operation.name} operation")
                    val config: TransactionConfig = when (it.data) {
                        is OperationData.Sale -> {
                            TransactionConfig.Sale(it.data.amount, it.data.tip, it.data.orderReference, true, it.data.extras)
                        }
                        is OperationData.Refund -> {
                            TransactionConfig.Refund(it.data.transactionId, null, it.data.amount, promptUserConfirmation = false, showResult = true, it.data.extras)
                        }
                        is OperationData.Void -> {
                            TransactionConfig.Void(it.data.transactionId, null, promptUserConfirmation = false, showResult = true, it.data.extras)
                        }
                        else -> throw IllegalStateException("Not a transaction operation")
                    }

                    PhosSdk.getInstance().launchTransaction(this, config, transactionCallback)
                }

                Operation.HISTORY -> {
                    logger.d(TAG, "HISTORY operation")
                    val data = it.data as OperationData.History
                    PhosSdk.getInstance().getTransactionHistory(data.page, data.limit.coerceAtMost(100), data.date, data.transactionType, data.transactionState,
                            object : TransactionListCallback {
                                override fun onSuccess(data: Transactions, extras: MutableMap<String, String>?) {
                                    setHistoryResult(ResponseCode.SUCCESS.code, data)
                                }

                                override fun onFailure(error: PhosException, extras: MutableMap<String, String>?) {
                                    setHistoryResult(error.code, null)
                                }

                            })
                }

                Operation.RECEIPT -> {
                    logger.d(TAG, "RECEIPT operation")

                    val data = it.data as OperationData.Receipt

                    when (data.type) {

                        ReceiptType.EMAIL -> {
                            PhosSdk.getInstance().sendReceiptByEmail(data.recipient, data.transactionId, sendReceiptCallback)
                        }

                        ReceiptType.SMS -> {
                            PhosSdk.getInstance().sendReceiptBySms(data.recipient, data.transactionId, sendReceiptCallback)
                        }

                        else -> {
                            logger.e(TAG, "Unsupported Receipt type: ${data.type}")
                        }
                    }

                }

                Operation.LANGUAGE -> {
                    logger.d(TAG, "LANGUAGE operation")
                    val data = it.data as OperationData.Language
                    try {
                        val lang = PhosLanguage.valueOf(data.language)
                        stringManager.activateLanguage(lang)
                        setLanguageResult(ResponseCode.SUCCESS.code, "Success")
                    } catch (e: Exception) {
                        setLanguageResult(ResponseCode.BAD_REQUEST.code, "Unsupported language")
                    }
                }
            }
        } ?: run {
            logger.e(TAG, "operationRequest is null")
            finish()
        }
    }


}