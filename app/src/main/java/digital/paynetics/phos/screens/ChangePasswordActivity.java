package digital.paynetics.phos.screens;

import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.ViewById;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.helpers.ViewFocusManager;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.classes.prefs.ResetTokenPreference;
import digital.paynetics.phos.sdk.CompletionHandler;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.services.PhosConnect;

@EActivity(R.layout.activity_change_password)
public class ChangePasswordActivity extends BaseActivity {

    @ViewById
    TextInputLayout passwordLayout;

    @ViewById
    TextView passwordLabel;

    @ViewById
    TextView newPasswordLabel;

    @ViewById
    TextView confirmPasswordLabel;

    @ViewById
    TextInputEditText password;

    @ViewById
    TextInputLayout newPasswordLayout;

    @ViewById
    TextInputEditText newPassword;

    @ViewById
    TextInputLayout newPasswordLayoutConfirm;

    @ViewById
    TextInputEditText newPasswordConfirm;

    @ViewById
    LinearLayout loadingView;

    @ViewById
    TextView errorMessage;

    @ViewById
    TextView title;

    @ViewById
    Button btnChange;

    @Inject
    PhosConnect phosConnect;

    @Inject
    ResetTokenPreference resetToken;

    @Inject
    ViewFocusManager viewFocusManager;

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    @AfterViews
    public void afterViews() {
        setResult(RESULT_CANCELED);
        setMode();

        updateBtnChangeEnabledState();

        passwordLabel.setText(stringManager.getString(PhosString.current_password));
        password.setHint(stringManager.getString(PhosString.enter_current_password));

        newPasswordLabel.setText(stringManager.getString(PhosString.new_password));
        newPassword.setHint(stringManager.getString(PhosString.enter_new_password));

        confirmPasswordLabel.setText(stringManager.getString(PhosString.confirm_new_password));
        newPasswordConfirm.setHint(stringManager.getString(PhosString.repeat_new_password));

        viewFocusManager.setFocusChangeListenerToView(password);
        viewFocusManager.setFocusChangeListenerToView(newPassword);
        password.addTextChangedListener(createTextWatcher(passwordLayout));
        newPassword.addTextChangedListener(createTextWatcher(newPasswordLayout));
        newPasswordConfirm.addTextChangedListener(createTextWatcher(newPasswordLayout));
        enableAllLayoutChangeAnimations(R.id.root, viewFocusManager.getLayoutTransitionListener());

        btnChange.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_ENABLED_COLOR), PorterDuff.Mode.SRC_ATOP);
        ((SpinKitView) loadingView.findViewById(R.id.loadingIndicator)).setColor(clientConfig.getDynamicColor(BrandingColors.LOADING_INDICATOR_COLOR));
    }

    @Click(R.id.btnBack)
    public void close() {
        finish();
    }

    @Click(R.id.btnChange)
    public void changePassword() {
        String pass = password.getText().toString();
        String newPass = newPassword.getText().toString();
        String newPassConfirm = newPasswordConfirm.getText().toString();

        if (isChangePasswordMode() && isInvalidPassword(pass)) {
            showInvalidPassword(passwordLayout, true);
            requestFocus(passwordLayout);
            return;
        }

        if (isInvalidPassword(newPass)) {
            showInvalidPassword(newPasswordLayout, true);
            requestFocus(newPasswordLayout);
            return;
        }

        if (isInvalidPassword(newPassConfirm)) {
            showInvalidPassword(newPasswordLayoutConfirm, true);
            requestFocus(newPasswordLayoutConfirm);
            return;
        }

        if (isConfirmPasswordsNotIdentical(newPass, newPassConfirm)) {
            showPasswordsNotIdentical(newPasswordLayoutConfirm, true);
            requestFocus(newPasswordLayoutConfirm);
            return;
        }

        passwordLayout.setErrorEnabled(false);
        newPasswordLayout.setErrorEnabled(false);
        newPasswordLayoutConfirm.setErrorEnabled(false);
        errorMessage.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);

        if (isChangePasswordMode()) {
            phosConnect.changePassword(pass, newPass, createCompletionHandler());
        } else {
            String token = resetToken.get();
            phosConnect.resetPassword(token, newPass, createCompletionHandler());
        }
    }

    @Touch(R.id.btnChange)
    public boolean changePasswordTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            btnChange.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_PRESSED_COLOR), PorterDuff.Mode.SRC_ATOP);
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            btnChange.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_ENABLED_COLOR), PorterDuff.Mode.SRC_ATOP);
        }

        return false;
    }

    private boolean isInvalidPassword(String password) {
        return password.trim().isEmpty();
    }

    private boolean isConfirmPasswordsNotIdentical(String password1, String password2) {
        return !password1.trim().equals(password2.trim());
    }

    private void showInvalidPassword(TextInputLayout layout, boolean show) {
        if (show) {
            layout.setError(stringManager.getString(PhosString.err_msg_password));
        } else {
            layout.setErrorEnabled(false);
        }
    }

    private void showPasswordsNotIdentical(TextInputLayout layout, boolean show) {
        if (show) {
            layout.setError(stringManager.getString(PhosString.passwords_not_identical));
        } else {
            layout.setErrorEnabled(false);
        }
    }


    private String responseCodeToText(ResponseCode responseCode) {
        return stringManager.getStringByResponseCode(responseCode.getCode());
    }

    private TextWatcher createTextWatcher(TextInputLayout target) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateBtnChangeEnabledState();
                showInvalidPassword(target, false);
            }
        };
    }

    private CompletionHandler.Result createCompletionHandler() {
        return new CompletionHandler.Result() {

            @Override
            public void onSuccess() {
                setResult(RESULT_OK);
                Toast.makeText(ChangePasswordActivity.this,
                        stringManager.getString(PhosString.password_updated),
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }

            @Override
            public void onError(ResponseCode responseCode) {
                errorMessage.setText(responseCodeToText(responseCode));
                errorMessage.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
            }

        };
    }

    private boolean isChangePasswordMode() {
        return !isResetPasswordMode();
    }

    private boolean isResetPasswordMode() {
        return resetToken.isAvailable();
    }

    private void setMode() {
        if (isChangePasswordMode()) {
            setChangePasswordMode();
        } else {
            setResetPasswordMode();
        }
    }

    private void setResetPasswordMode() {
        title.setText(stringManager.getString(PhosString.reset_password));
        btnChange.setText(stringManager.getString(PhosString.reset));
        passwordLayout.setVisibility(View.GONE);
        passwordLabel.setVisibility(View.GONE);
    }

    private void setChangePasswordMode() {
        title.setText(stringManager.getString(PhosString.change_password));
        btnChange.setText(stringManager.getString(PhosString.save_changes));
        passwordLayout.setVisibility(View.VISIBLE);
        passwordLabel.setVisibility(View.VISIBLE);
    }

    private void updateBtnChangeEnabledState() {
        boolean hasAllInput = hasInput(newPassword) && hasInput(newPasswordConfirm)
                && (passwordLayout.getVisibility() != View.VISIBLE || hasInput(password));
        btnChange.setEnabled(hasAllInput);
    }

    private boolean hasInput(TextInputEditText input) {
        return !TextUtils.isEmpty(input.getText());
    }
}
