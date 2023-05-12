package digital.paynetics.phos.classes.prefs;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import javax.inject.Inject;

public final class DoNotDisturbPreference extends StringPreference {
    @Inject
    DoNotDisturbPreference(@NonNull SharedPreferences prefs) {
        super(prefs, "dnd");
    }
}
