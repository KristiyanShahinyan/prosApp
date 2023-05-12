package digital.paynetics.phos.app2app
import javax.inject.Inject

interface PayloadSigner {
    fun sign(payload: String): SignedPayload
    fun decrypt(signedPayload: SignedPayload) : String
}

class PayloadSignerNoSigning
@Inject
constructor() : PayloadSigner {
    override fun sign(payload: String): SignedPayload {
        return SignedPayload(
                payload = payload,
                signature = ""
        )
    }

    override fun decrypt(signedPayload: SignedPayload): String {
        return signedPayload.payload
    }

}