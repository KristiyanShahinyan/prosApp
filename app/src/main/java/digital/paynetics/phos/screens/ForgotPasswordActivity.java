package digital.paynetics.phos.screens;

import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.textfield.TextInputLayout;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EditorAction;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.ViewById;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.helpers.ViewFocusManager;
import digital.paynetics.phos.classes.helpers.Validate;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.dialogs.manager.DialogManager;
import digital.paynetics.phos.dialogs.manager.DialogState;
import digital.paynetics.phos.sdk.CompletionHandler;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.services.PhosConnect;

@EActivity(R.layout.activity_forgot_password)
public class ForgotPasswordActivity extends BaseActivity {

    @ViewById
    TextInputLayout emailLayoutView;

    @ViewById
    TextView emailView;

    @ViewById
    LinearLayout loadingView;

    @ViewById
    TextView errorView;

    @ViewById
    TextView hintView;

    @ViewById
    TextView titleView;

    @ViewById
    Button confirmButton;

    @Inject
    PhosConnect phosConnect;

    @Inject
    ViewFocusManager viewFocusManager;

    @Inject
    DialogManager dialogManager;

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

        hintView.setText(stringManager.getString(PhosString.reset_password_hint));
        titleView.setText(stringManager.getString(PhosString.reset_password));
        emailLayoutView.setHint(stringManager.getString(PhosString.email));

        confirmButton.setText(stringManager.getString(PhosString.send));
        confirmButton.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_ENABLED_COLOR), PorterDuff.Mode.SRC_ATOP);

        emailView.addTextChangedListener(createTextWatcher(emailLayoutView));
        viewFocusManager.setFocusChangeListenerToView(emailView);
        enableAllLayoutChangeAnimations(R.id.root, viewFocusManager.getLayoutTransitionListener());

        ((SpinKitView) loadingView.findViewById(R.id.loadingIndicator)).setColor(clientConfig.getDynamicColor(BrandingColors.LOADING_INDICATOR_COLOR));
    }

    @EditorAction(R.id.emailView)
    boolean onEditorAction(int actionId, KeyEvent key) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEND || (key.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

            requestPasswordReset();

            handled = true;
        }
        return handled;
    }

    @Click(R.id.btnBack)
    public void close() {
        finish();
    }

    @Click(R.id.confirmButton)
    public void requestPasswordReset() {
        String email = extractEmail();

        if (isInvalidEmail(email)) {
            update(emailLayoutView, true);
            requestFocus(emailLayoutView);
            return;
        }

        errorView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);

        phosConnect.forgotPassword(email, createCompletionHandler());
    }

    @Touch(R.id.btnLogin)
    public boolean requestPasswordResetTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            confirmButton.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_PRESSED_COLOR), PorterDuff.Mode.SRC_ATOP);
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            confirmButton.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_ENABLED_COLOR), PorterDuff.Mode.SRC_ATOP);
        }

        return false;
    }

    private String extractEmail() {
        return emailView.getText().toString().trim();
    }

    private boolean isInvalidEmail(String email) {
        return email.isEmpty() || !Validate.isValidEmail(email.trim());
    }

    private void update(TextInputLayout layout, boolean showError, @StringRes int errorMessage) {
        if (showError) {
            layout.setError(getString(errorMessage));
        } else {
            layout.setErrorEnabled(false);
        }
    }

    private void update(TextInputLayout layout, boolean showError) {
        if (showError) {
            layout.setError(stringManager.getString(PhosString.err_msg_email));
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
                boolean showError = isInvalidEmail(s.toString());
                update(target, showError);
            }
        };
    }

    private CompletionHandler.Result createCompletionHandler() {
        return new CompletionHandler.Result() {
            @Override
            public void onSuccess() {
                showConfirmationDialog();
            }

            @Override
            public void onError(ResponseCode responseCode) {
                errorView.setText(responseCodeToText(responseCode));
                errorView.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
            }
        };
    }

    private void showConfirmationDialog() {
        dialogManager.showDialog(new DialogState.PasswordResetConfirmationStateState(this, stringManager, () -> {
            setResult(RESULT_OK);
            finish();
            return null;
        }), -1);
    }

    protected boolean shouldSaveCoreInstanceState() {
        // Core state like merchant and terminal data is available after login.
        // Return false for login and all other screen where user isn't logged.
        return false;
    }
}
