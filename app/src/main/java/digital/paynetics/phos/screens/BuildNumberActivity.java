package digital.paynetics.phos.screens;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.inject.Inject;

import digital.paynetics.phos.classes.helpers.IdProvider;
import digital.paynetics.phos.sdk.BuildConfig;
import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.helpers.Sha256Calculator;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.sdk.CompletionHandler;
import digital.paynetics.phos.sdk.PhosLogger;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.security.SecureTimer;

@EActivity(R.layout.activity_build_number)
public class BuildNumberActivity extends BaseActivity {

    @ViewById
    TextView txtInstructions;

    @ViewById
    TextView buildNumberTitle;

    @ViewById
    TextView buildNumber;

    @ViewById
    FrameLayout copyBuildNumber;

    @ViewById
    FrameLayout header;

    @ViewById
    ConstraintLayout version;

    @Inject
    PhosLogger logger;

    @Inject
    IdProvider idProvider;

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
        copyBuildNumber.setVisibility(View.GONE);

        ((TextView)header.findViewById(R.id.title))
                .setText(stringManager.getString(PhosString.app_verify));

        ((TextView)version.findViewById(R.id.title))
                .setText(SettingsHelpersKt.getVersionName(this, idProvider));
        ((TextView)version.findViewById(R.id.hint))
                .setText(SettingsHelpersKt.getVersionCode(stringManager, idProvider));

        String email = getString(R.string.support_email);
        String instructions = stringManager.getString(PhosString.build_number_explanation)
                .toString()
                .replace("%email", email);
        txtInstructions.setText(instructions);
        SettingsHelpersKt.makeTextClickable(txtInstructions, email, () -> {
            Intent intent = intentHelper.getSendEmailIntent(email);
            SettingsHelpersKt.tryStartActivity(intent, BuildNumberActivity.this);
            return null;
        });

        buildNumberTitle.setText(stringManager.getString(PhosString.build_number));

        phosConnect.retrieveInstanceToken(BuildConfig.APP_INSTANCE, new InstanceHandlerToken());
    }

    @Click(R.id.btnBack)
    public void close() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Click(R.id.copyBuildNumber)
    public void copy() {
        String confirmation = stringManager.getString(PhosString.copy_confirmation).toString();
        SettingsHelpersKt
                .copyTextToClipboard(buildNumber.getText().toString(), this, confirmation);
    }

    private String getDate() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormatter.format(Date.from(SecureTimer.getInstant()));
    }

    class InstanceHandlerToken implements CompletionHandler.General {

        @Override
        public void onError(ResponseCode responseCode) {
            displayErrorMessage();
        }

        @Override
        public void onSuccess(JsonObject jsonObject) {
            JsonObject resultData = jsonObject != null && jsonObject.getAsJsonObject("data") != null
                    ? jsonObject.getAsJsonObject("data")
                    : null;

            if (resultData == null) {
                displayErrorMessage();
                return;
            }

            JsonElement resultToken = resultData.get("instance_token");

            if (resultToken == null) {
                displayErrorMessage();
                return;
            }

            String checkSum = Sha256Calculator.sha256(resultToken.getAsString() + getDate(), logger);

            if (checkSum != null) {
                copyBuildNumber.setVisibility(View.VISIBLE);
                buildNumber.setText(checkSum);
            } else {
                displayErrorMessage();
            }
        }
    }

    private void displayErrorMessage() {
        buildNumber.setText(stringManager.getString(PhosString.error_retrieving_instance_token));
    }
}
