package digital.paynetics.phos.classes.prefs;

import android.content.SharedPreferences;

import javax.inject.Inject;

import digital.paynetics.phos.sdk.security.PhosCryptoManager;
import digital.paynetics.phos.security.crypto.AsymmetricCrypto;

public final class SecretPreference extends StringPreference {
    @Inject
    SecretPreference(SharedPreferences prefs, @AsymmetricCrypto PhosCryptoManager cryptoManager) {
        // Secret is received encrypted by the backend so we cache it like this
        // and decrypt it when needed.
        super(prefs, "secret", cryptoManager, false, true);
    }
}
