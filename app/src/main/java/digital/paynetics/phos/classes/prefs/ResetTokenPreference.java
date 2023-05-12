package digital.paynetics.phos.classes.prefs;

import android.content.SharedPreferences;

import javax.inject.Inject;

import digital.paynetics.phos.security.crypto.SharedPrefsCryptoManager;

public final class ResetTokenPreference extends StringPreference {
    @Inject
    ResetTokenPreference(SharedPreferences prefs,
                         SharedPrefsCryptoManager cryptoManager) {
        super(prefs, "reset-token", cryptoManager);
    }
}
