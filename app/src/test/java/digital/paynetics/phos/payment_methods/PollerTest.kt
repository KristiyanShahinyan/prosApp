package digital.paynetics.phos.payment_methods

import digital.paynetics.phos.domain.ScaType
import digital.paynetics.phos.presentation.model.TransactionEvent
import digital.paynetics.phos.sdk.ResponseCode
import digital.paynetics.phos.sdk.entities.ApiResponse
import digital.paynetics.phos.sdk.entities.Transaction
import digital.paynetics.phos.sdk.services.PhosConnect
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import java.util.*

class PollerTest {

    private val testDispatcher = TestCoroutineDispatcher()

    private val mockTransaction = Transaction(
        "123",
        "stan",
        "authCode",
        "appid",
        1,
        "SALE",
        "1234-5678-9123-4567",
        "Visa",
        Calendar.getInstance().time,
        100.0,
        10.0,
        "GBP",
        false,
        100.0,
        ScaType.SCA_TYPE_3D
    )
    private val mockPhosConnect = mock<PhosConnect>()
    private val transactionResultPoller = TransactionResultPoller(mockPhosConnect)

    @Test
    fun `poll every second get single success result`() {
        Mockito.`when`(mockPhosConnect.getTransactionByTrKey("12345")).thenReturn(ApiResponse.createSuccessResponse(mockTransaction))
        val flow = transactionResultPoller.poll(1000, Pair(TRANSACTION_KEY, "12345"))
        runBlocking {
            launch {
                val item = flow.first()
                Assert.assertTrue(item is TransactionEvent.TransactionSuccess)
            }
            testDispatcher.advanceTimeBy(2000)
        }
    }

    @Test
    fun `poll every second get single fail result`() {
        Mockito.`when`(mockPhosConnect.getTransactionByTrKey("12345")).thenReturn(ApiResponse.createErrorResponse(ResponseCode.BAD_REQUEST))
        val flow = transactionResultPoller.poll(1000, Pair(TRANSACTION_KEY, "12345"))
        runBlocking {
            launch {
                val item = flow.first()
                Assert.assertTrue(item is TransactionEvent.TransactionError)
                Assert.assertEquals((item as TransactionEvent.TransactionError).errorCode, ResponseCode.BAD_REQUEST.code)
            }
            testDispatcher.advanceTimeBy(2000)
        }
    }
}