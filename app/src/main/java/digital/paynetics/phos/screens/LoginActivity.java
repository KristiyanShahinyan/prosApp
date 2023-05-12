package digital.paynetics.phos.screens;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.textfield.TextInputLayout;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import javax.inject.Inject;

import digital.paynetics.phos.dialogs.manager.DialogState;
import digital.paynetics.phos.sdk.BuildConfig;
import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.enums.BrandingImages;
import digital.paynetics.phos.classes.external.KeyboardUtils;
import digital.paynetics.phos.classes.helpers.Convert;
import digital.paynetics.phos.classes.helpers.IdProvider;
import digital.paynetics.phos.classes.helpers.KeyboardHelper;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.classes.prefs.ResetTokenPreference;
import digital.paynetics.phos.classes.prefs.TransactionHashPreference;
import digital.paynetics.phos.sdk.CompletionHandler;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.entities.AccessToken;
import digital.paynetics.phos.sdk.security.AuditLogger;
import digital.paynetics.phos.sdk.security.PhosConnectEndpoint;
import digital.paynetics.phos.sdk.security.PhosCryptoManager;
import digital.paynetics.phos.sdk.security.PhosCryptoManagerFactory;
import digital.paynetics.phos.sdk.services.PhosConnect;
import digital.paynetics.phos.security.custom.EditableWrapper;

import static digital.paynetics.phos.sdk.security.AuditErrorCodes.USER_LOGIN_FAILED;


@EActivity(R.layout.activity_login)
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LA";

    @ViewById(R.id.btnSignup)
    Button btnSignup;

    @ViewById(R.id.btnForgot)
    Button btnForgot;

    @ViewById(R.id.btnLogin)
    Button btnLogin;

    @ViewById(R.id.email)
    EditText email;

    @ViewById(R.id.password)
    EditText password;

    @ViewById(R.id.layoutEmail)
    TextInputLayout layoutEmail;


    @ViewById(R.id.layoutPassword)
    TextInputLayout layoutPassword;

    @ViewById(R.id.logo)
    ImageView logo;

    @ViewById(R.id.mainScreen)
    ConstraintLayout mainScreen;

    @ViewById
    LinearLayout loadingView;

    @ViewById
    TextView errorMessage;

    @ViewById
    TextView version;

    @Inject
    IdProvider idProvider;

    @Inject
    TransactionHashPreference transactionHashPreference;

    @Inject
    ResetTokenPreference resetToken;

    @Inject
    PhosCryptoManagerFactory cryptoManagerFactory;

    EditableWrapper passwordWrapper;
    EditableWrapper emailWrapper;

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    protected boolean shouldSaveCoreInstanceState() {
        // Core state like merchant and terminal data is available after login.
        // Return false for login and all other screen where user isn't logged.
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    @AfterViews
    public void afterViews() {

        layoutEmail.setHint(stringManager.getString(PhosString.email));
        layoutPassword.setHint(stringManager.getString(PhosString.password));
        btnForgot.setText(stringManager.getString(PhosString.forgot_password));
        btnLogin.setText(stringManager.getString(PhosString.login));
        btnLogin.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_ENABLED_COLOR), PorterDuff.Mode.SRC_ATOP);

        ((SpinKitView) loadingView.findViewById(R.id.loadingIndicator)).setColor(clientConfig.getDynamicColor(BrandingColors.LOADING_INDICATOR_COLOR));

        btnSignup.setText(stringManager.getString(clientFlavor.hasSignUpUrl()
                ? PhosString.signup
                : PhosString.see_device_id));
        if (getResources().getBoolean(R.bool.align_buttons_horizontally)) {
            setButtonsHorizontally(mainScreen);
        }

        email.addTextChangedListener(new LoginTextWatcher(email));
        password.addTextChangedListener(new LoginTextWatcher(password));

        clientConfig.getSkinRequestCreator(BrandingImages.BRANDING_LOGO_IMAGE, true).into(logo);

        version.setText(String.format(getString(R.string.version_string), stringManager.getString(PhosString.version).toString(), idProvider.getAppName()));

        KeyboardUtils.addKeyboardToggleListener(this, this::showHideLogo);

        if (resetToken.isAvailable()) {
            openResetPassword();
        }

        passwordWrapper = EditableWrapper.create(password);
        emailWrapper = EditableWrapper.create(email);

//        requestChangePassword();
    }

    private void setButtonsHorizontally(ConstraintLayout cl) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(cl);

        constraintSet.constrainWidth(btnLogin.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet.connect(btnLogin.getId(), ConstraintSet.BOTTOM, cl.getId(), ConstraintSet.BOTTOM, 64);
        constraintSet.connect(btnLogin.getId(), ConstraintSet.TOP, btnSignup.getId(), ConstraintSet.TOP, 0);
        constraintSet.connect(btnLogin.getId(), ConstraintSet.START, layoutPassword.getId(), ConstraintSet.START, 0);
        constraintSet.connect(btnLogin.getId(), ConstraintSet.END, btnSignup.getId(), ConstraintSet.START, 1);

        constraintSet.connect(btnSignup.getId(), ConstraintSet.BOTTOM, cl.getId(), ConstraintSet.BOTTOM, 64);
        constraintSet.connect(btnSignup.getId(), ConstraintSet.END, layoutPassword.getId(), ConstraintSet.END, 0);
        constraintSet.connect(btnSignup.getId(), ConstraintSet.START, btnLogin.getId(), ConstraintSet.END, 0);

        constraintSet.applyTo(cl);
    }

    @OnActivityResult(RESULT_CODE_PASS_CHANGED)
    void onPassChangeResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            openMainScreen();
            return;
        }

        logout();
    }

    @OnActivityResult(RESULT_CODE_PASS_RESET)
    void onPassResetResult(int resultCode) {
        if (resetToken.isAvailable()) {
            resetToken.delete();
        }

        if (resultCode == RESULT_OK) {
            if (phosConnect.isLoggedIn()) {
                openMainScreen();
            }
            return;
        }

        logout();
    }

    private void showHideLogo(boolean shouldHide) {

        if (shouldHide) {
            logo.animate().setDuration(DEFAULT_ANIM_DURATION * 2).alpha(0);
            mainScreen.animate().setDuration(DEFAULT_ANIM_DURATION).translationY(Convert.dpToPixels(-90, this));
            btnLogin.animate().setDuration(DEFAULT_ANIM_DURATION).translationY(Convert.dpToPixels(-120, this));
            btnSignup.animate().setDuration(DEFAULT_ANIM_DURATION).alpha(0);
            btnForgot.animate().setDuration(DEFAULT_ANIM_DURATION).alpha(0);

            btnSignup.setEnabled(false);
            btnForgot.setEnabled(false);

            errorMessage.setVisibility(View.GONE);

        } else {
            logo.animate().setDuration(DEFAULT_ANIM_DURATION * 2).alpha(1);
            mainScreen.animate().setDuration(DEFAULT_ANIM_DURATION).translationY(0);
            btnLogin.animate().setDuration(DEFAULT_ANIM_DURATION).translationY(0);
            btnSignup.animate().setDuration(DEFAULT_ANIM_DURATION).alpha(1);
            btnForgot.animate().setDuration(DEFAULT_ANIM_DURATION).alpha(1);

            btnSignup.setEnabled(true);
            btnForgot.setEnabled(true);
        }

    }

    private class LoginTextWatcher implements TextWatcher {

        private View view;

        private boolean textChanged;

        private LoginTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // Called even when password visibility toggle is tapped,
            // there is no text change in this case.
            textChanged = i1 != i2;
        }

        public void afterTextChanged(Editable editable) {
            if (isLoggingIn()) {
                return;
            }
            if (!textChanged) {
                return;
            }
            switch (view.getId()) {
                case R.id.email:
                    validateEmail();
                    break;
                case R.id.password:
                    validatePassword();
                    break;
            }
        }
    }


    @Click(R.id.btnLogin)
    public void login() {

        if (!validateEmail() || !validatePassword()) {
            return;
        }

        clearFocus();

        loginUser();
    }

    @Touch(R.id.btnLogin)
    public boolean loginTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            btnLogin.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_PRESSED_COLOR), PorterDuff.Mode.SRC_ATOP);
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            btnLogin.getBackground().setColorFilter(clientConfig.getDynamicColor(BrandingColors.BUTTON_ENABLED_COLOR), PorterDuff.Mode.SRC_ATOP);
        }

        return false;
    }

    @Click(R.id.btnForgot)
    public void forgot() {
        ForgotPasswordActivity_.intent(this).start();
    }

    @Click(R.id.btnSignup)
    public void signup() {
        if (clientFlavor.hasSignUpUrl()) {
            startActivity(intentHelper.getOpenUrlIntent(clientFlavor.getSignUpUrl()));
        } else {
            startActivity(getSignUpIntent());
        }
    }

    @Click(R.id.mainScreen)
    public void clickMainScreen() {
        clearFocus();
        KeyboardHelper.hideKeyboard(this);
    }

    @Click(R.id.version)
    public void clickVersion() {
        BuildNumberActivity_.intent(this).start();
    }

    @Background
    void loginUser() {
        showLoading();

        PhosCryptoManager cryptoManager = cryptoManagerFactory.create(PhosConnectEndpoint.API,
                PhosConnect.API_VERSION);
        String encryptedEmail = cryptoManager.encryptToEncodedString(emailWrapper.getValue());
        String encryptedPass = cryptoManager.encryptToEncodedString(passwordWrapper.getValue());

        clear();

        callLogin(encryptedEmail, encryptedPass);
    }

    @UiThread
    void showLoading() {
        startTime = System.currentTimeMillis();
        clickMainScreen();
        loadingView.setVisibility(View.VISIBLE);
        errorMessage.setVisibility(View.GONE);
    }

    @UiThread
    void callLogin(String email, String pass) {
        phosConnect.login(email, pass, BuildConfig.APP_INSTANCE, new CompletionHandler.UserLogin() {

            @Override
            public void onSuccess(AccessToken token) {
                setupPhosConnect(token);
                getInitialConfigAndTryOpenMainScreen(token.isChangePasswordRequired());

                auditLogger.logAudit(
                        TAG,
                        new AuditLogger.Pair("message", "Successful login."));
            }

            @Override
            public void onError(ResponseCode responseCode) {

                // get the response text
                String responseText = stringManager.getStringByResponseCode(responseCode.getCode());


                // check if we should wait a bit more, min is DELAY_MIN_MS milliseconds
                long diff = startTime == 0 ? 0 : Math.max(0, DELAY_MIN_MS - (System.currentTimeMillis() - startTime));

                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {

                    errorMessage.setText(responseText);
                    errorMessage.setVisibility(View.VISIBLE);

                    loadingView.setVisibility(View.GONE);

                }, diff);

                auditLogger.logAlert(
                        TAG,
                        USER_LOGIN_FAILED,
                        new AuditLogger.Pair("reasonFailed", "Unsuccessful login!"));

            }

        });
    }

    @UiThread
    void clear() {
        emailWrapper.clear();
        passwordWrapper.clear();
    }

    private void getInitialConfigAndTryOpenMainScreen(boolean changePasswordRequired) {
        // get terminal config and merchant info and on success, open main screen
        getInitialConfig(new digital.paynetics.phos.classes.CompletionHandler() {
            @Override
            public void onSuccess() {
                if (changePasswordRequired) {
                    loadingView.setVisibility(View.GONE);
                    requestChangePassword();
                } else {
                    openMainScreen();
                }
            }

            @Override
            public void onFailure() {
                loadingView.setVisibility(View.GONE);
                errorMessage.setText(stringManager.getString(PhosString.init_failed));
                errorMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupPhosConnect(AccessToken token) {
        String lastTransactionHash = token.getLastTransactionHash() != null
                ? token.getLastTransactionHash()
                : "unknown";
        transactionHashPreference.set(lastTransactionHash);// save in case app is closed
        phosConnect.setLastTransactionHash(lastTransactionHash); // set internally for usage in transaction call
    }


    private boolean validateEmail() {
        if (!isValidEmail(emailWrapper)) {
            layoutEmail.setError(stringManager.getString(PhosString.err_msg_email));
            requestFocus(email);
            return false;
        } else {
            layoutEmail.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validatePassword() {
        byte[] passwordValue = passwordWrapper.getValue();
        if (passwordValue.length == 0) {
            layoutPassword.setError(stringManager.getString(PhosString.err_msg_password));
            requestFocus(password);
            return false;
        } else {
            layoutPassword.setErrorEnabled(false);
        }

        return true;
    }

    private void requestChangePassword() {
        dialogManager.showDialog(new DialogState.ChangePasswordRequestState(this,
                stringManager,
                () -> {
                    openChangePassword();
                    return null;
                }, () -> {
            logout();
            return null;
        }), -1);
    }

    private void openChangePassword() {
        Intent intent = new Intent(this, ChangePasswordActivity_.class);
        startActivityForResult(intent, RESULT_CODE_PASS_CHANGED);
    }

    private void openResetPassword() {
        Intent intent = new Intent(this, ChangePasswordActivity_.class);
        startActivityForResult(intent, RESULT_CODE_PASS_RESET);
    }

    private void logout() {
        tokenManager.clear();
        clientConfig.setMerchant(null);
    }

    private void clearFocus() {
        logo.requestFocus();
    }

    private boolean isEmpty(byte[] value) {
        return value == null || value.length == 0;
    }

    private boolean isValidEmail(EditableWrapper editableWrapper) {
        byte[] emailValue = editableWrapper.getValue();
        // TODO: 4/12/2019 FIX, matcher converts to String, find an alternative...
        return !isEmpty(emailValue)
                && android.util.Patterns.EMAIL_ADDRESS.matcher(editableWrapper.getEditable()).matches();
    }

    private boolean isLoggingIn() {
        return loadingView.getVisibility() == View.VISIBLE;
    }

    // Remove this helper once android annotations is removed
    private Intent getSignUpIntent() {
        return SignupActivity_.intent(this).get();
    }
}
