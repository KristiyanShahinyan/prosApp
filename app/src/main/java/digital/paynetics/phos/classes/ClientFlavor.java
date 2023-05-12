package digital.paynetics.phos.classes;

import android.content.Context;

import javax.inject.Inject;

import digital.paynetics.phos.R;

public class ClientFlavor {

    private final Context context;

    @Inject
    ClientFlavor(Context context) {
        this.context = context;
    }

    public String getSignUpUrl() {
        return context.getString(R.string.signup_url);
    }

    public boolean hasSignUpUrl() {
        return getSignUpUrl().length() > 0;
    }
}
