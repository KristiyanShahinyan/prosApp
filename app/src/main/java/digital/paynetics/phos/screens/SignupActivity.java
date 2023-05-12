package digital.paynetics.phos.screens;

import android.content.Intent;
import android.widget.TextView;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.helpers.IdProvider;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.sdk.PhosLogger;

@EActivity(R.layout.activity_signup)
public class SignupActivity extends BaseActivity {

    @ViewById
    TextView txtTitle;

    @ViewById
    TextView deviceId;

    @Inject
    IdProvider idProvider;

    @Inject
    PhosLogger logger;

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    protected boolean shouldSaveCoreInstanceState() {
        // Core state like merchant and terminal data is available after login.
        // Return false for login and all other screen where user isn't logged.
        return false;
    }

    @AfterViews
    public void afterViews() {
        txtTitle.setText(stringManager.getString(PhosString.title_device_id));
        deviceId.setText(idProvider.getDeviceId());
        logger.d("DeviceId", idProvider.getDeviceId());
    }

    @Click(R.id.btnBack)
    public void close() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Click(R.id.deviceId)
    public void share() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, idProvider.getDeviceId());
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
