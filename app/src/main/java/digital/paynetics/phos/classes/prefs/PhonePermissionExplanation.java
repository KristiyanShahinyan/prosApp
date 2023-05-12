package digital.paynetics.phos.classes.prefs;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import javax.inject.Inject;

public final class PhonePermissionExplanation {

    private static final String SHOW_PHONE_PERMISSION_EXPLANATION = "SHOW_PHONE_PERMISSION_EXPLANATION";

    private final SharedPreferences prefs;

    @Inject
    PhonePermissionExplanation(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean shouldShow() {
        return prefs.getBoolean(SHOW_PHONE_PERMISSION_EXPLANATION, true);
    }

    public void enable() {
        prefs.edit().putBoolean(SHOW_PHONE_PERMISSION_EXPLANATION, true).apply();
    }

    public void disable() {
        prefs.edit().putBoolean(SHOW_PHONE_PERMISSION_EXPLANATION, false).apply();
    }
}
