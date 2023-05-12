package digital.paynetics.phos.screens;

import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import digital.paynetics.phos.BuildConfig;
import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.ClientFlavor;
import digital.paynetics.phos.classes.CompletionHandler;
import digital.paynetics.phos.classes.helpers.DynamicMerchantLogoBinding;
import digital.paynetics.phos.classes.helpers.IntentHelper;
import digital.paynetics.phos.classes.helpers.DynamicImage;
import digital.paynetics.phos.classes.helpers.NotifierWrapper;
import digital.paynetics.phos.classes.parcelables.MerchantParcelable;
import digital.paynetics.phos.classes.parcelables.TerminalParcelable;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.classes.lang.PhosStringProvider;
import digital.paynetics.phos.classes.prefs.PrefState;
import digital.paynetics.phos.classes.prefs.RefInfoPreference;
import digital.paynetics.phos.classes.prefs.TipPreference;
import digital.paynetics.phos.common.ImageViewBindings;
import digital.paynetics.phos.dialogs.manager.DialogManager;
import digital.paynetics.phos.dialogs.manager.DialogState;
import digital.paynetics.phos.exceptions.AttestationException;
import digital.paynetics.phos.sdk.PhosResponseHandler;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.entities.ClientConfig;
import digital.paynetics.phos.sdk.entities.Keys;
import digital.paynetics.phos.sdk.entities.Merchant;
import digital.paynetics.phos.sdk.entities.Terminal;
import digital.paynetics.phos.sdk.enums.TransactionType;
import digital.paynetics.phos.sdk.security.AuditErrorCodes;
import digital.paynetics.phos.sdk.security.AuditLogger;
import digital.paynetics.phos.sdk.security.SecureTimer;
import digital.paynetics.phos.sdk.services.PhosConnect;
import digital.paynetics.phos.sdk.services.PhosTokenManager;
import digital.paynetics.phos.security.checks.BackendAttestationManager;
import digital.paynetics.phos.security.checks.CallVerifier;
import digital.paynetics.phos.security.checks.ScheduledSecurityManager;
import digital.paynetics.phos.security.checks.SystemSettingsChecker;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

import static digital.paynetics.phos.screens.ActivityUtils.extractTransactionExtras;

public class BaseActivity extends AppCompatActivity {
    public static final int RESULT_SELECT_PRINTER = 147;
    public static final int RESULT_CODE_PASS_CHANGED = 148;
    public static final int RESULT_CODE_PASS_RESET = 149;
    public static final int RESULT_CODE_RELOAD = 150;

    protected Gson gson = new Gson();

    protected static final int DEFAULT_ANIM_DURATION = 200;
    public static final String NOTIFICATION_LOGOUT = "logout";

    // used in SalesActivity and AnalyticsActivity
    protected static final int SELECTOR_ANIMATION_DURATION = 150;
    protected static final int SELECTOR_ANIMATION_DURATION_SLOWER = 240; // used for end-to-end selector control animations

    protected PhosApplication getApp() {
        return (PhosApplication) getApplication();
    }

    // those two variables are used only in splash activity for the moment, to control
    // for how long (at least) the splash activity should be displayed, if the server request
    // performs faster. They are not used anywhere else for now, but the same openMainScreen() and
    // openLoginScreen() methods can be called from any activity
    protected boolean shouldSkip = false; // used if the user presses back button during init, in splash activity
    protected final long DELAY_MIN_MS = 1500; // for how long the splash should be displayed
    protected long startTime = 0; // used for measuring the display time, in splash activity

    // used by TransactionsActivity and AnalyticsActivity
    protected DateFormat dateFormat =  new SimpleDateFormat("d MMM yyyy", Locale.US);

    protected NfcAdapter nfcAdapter;

    private boolean hasSavedInstanceState;

    private Integer offlineDialogId = 0x5678;

    @Inject
    NotifierWrapper notifier;

    @Inject
    ScheduledSecurityManager scheduledSecurityManager;

    @Inject
    PhosConnect phosConnect;

    @Inject
    @Named("no-encryption")
    PhosConnect phosConnectNoEncryption;

    @Inject
    PhosTokenManager tokenManager;

    @Inject
    PhosStringProvider stringManager;

    @Inject
    SystemSettingsChecker systemSettingsChecker;

    @Inject
    AuditLogger auditLogger;

    @Inject
    CallVerifier callVerifier;

    @Inject
    ClientConfig clientConfig;

    @Inject
    DialogManager dialogManager;

    @Inject
    @DynamicMerchantLogoBinding
    DynamicImage dynamicMerchantLogo;

    @Inject
    BackendAttestationManager backendAttestationManager;

    @Inject
    IntentHelper intentHelper;

    @Inject
    ClientFlavor clientFlavor;

    @Inject
    TipPreference tipPreference;

    @Inject
    RefInfoPreference refInfoPreference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hasSavedInstanceState = savedInstanceState != null;
        restoreMerchantInstanceState(savedInstanceState);
        restoreTerminalInstanceState(savedInstanceState);

        dateFormat =  new SimpleDateFormat("d MMM yyyy", stringManager.getCurrentLocale());

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath(getString(R.string.font_path_regular))
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());
    }

    private void restoreMerchantInstanceState(@Nullable Bundle savedInstanceState) {
        if (clientConfig.getMerchant() == null && savedInstanceState != null) {
            MerchantParcelable parcelable = savedInstanceState.getParcelable(MerchantParcelable.KEY);
            if (parcelable != null) {
                clientConfig.setMerchant(parcelable.getMerchant());
            }
        }
    }

    private void restoreTerminalInstanceState(@Nullable Bundle savedInstanceState) {
        if (clientConfig.getTerminal() == null && savedInstanceState != null) {
            TerminalParcelable parcelable = savedInstanceState.getParcelable(TerminalParcelable.KEY);
            if (parcelable != null) {
                Terminal terminal = parcelable.getTerminal();
                clientConfig.setTerminal(terminal);
                tipPreference.setBackendPref(terminal.getTipPrefState());
                refInfoPreference.setBackendPref(terminal.getRefInfoPrefState());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (shouldSaveCoreInstanceState() && !outState.containsKey(MerchantParcelable.KEY)) {
            outState.putParcelable(MerchantParcelable.KEY, new MerchantParcelable(clientConfig.getMerchant()));
        }
        if (shouldSaveCoreInstanceState() && !outState.containsKey(TerminalParcelable.KEY)) {
            outState.putParcelable(TerminalParcelable.KEY, new TerminalParcelable(clientConfig.getTerminal()));
        }
    }

    protected boolean shouldSaveCoreInstanceState() {
        // Core state like merchant and terminal data is available after login.
        // Return false for login and all other screen where user isn't logged.
        return true;
    }

    protected boolean hasSavedInstanceState() {
        return hasSavedInstanceState;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    void openOffline() {
        if (!dialogManager.isDialogCurrentlyShowing(offlineDialogId)) {
            dialogManager.showDialog(new DialogState.OfflineState(this, stringManager, () -> {
                finish();
                return null;
            }), offlineDialogId);
        }
    }

    protected void setCorrectViewsLanguage(){

    }


    protected void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    protected void getInitialConfig(CompletionHandler handler) {

        getTerminalConfig(handler);

    }

    protected void getTerminalConfig(CompletionHandler handler) {

        if (shouldSkip)
            return;

        phosConnect.terminalConfig(new PhosResponseHandler<Terminal>() {

            @Override
            public void onSuccess(Terminal terminal) {
                clientConfig.setTerminal(terminal);
                tipPreference.setBackendPref(terminal.getTipPrefState());
                refInfoPreference.setBackendPref(terminal.getRefInfoPrefState());
                getMerchantInfo(handler);
            }

            @Override
            public void onError(ResponseCode responseCode) {
                // TODO: 31.07.18 Retry properly
                tipPreference.setBackendPref(PrefState.OFF);
                refInfoPreference.setBackendPref(PrefState.OFF);
                Toast.makeText(BaseActivity.this, "Could not load response from server", Toast.LENGTH_SHORT).show();
                handler.onFailure();

            }
        });
    }


    protected void getMerchantInfo(CompletionHandler handler) {

        if (shouldSkip)
            return;

        phosConnect.merchantDetails(new digital.paynetics.phos.sdk.CompletionHandler.MerchantDetails() {

            @Override
            public void onSuccess(Merchant merchant) {
                clientConfig.setMerchant(merchant); // maybe somewhere else later, but for now is okay.
                handler.onSuccess();
            }

            @Override
            public void onError(ResponseCode responseCode) {
                // TODO: 31.07.18 Retry properly
                Toast.makeText(BaseActivity.this, "Could not load response from server", Toast.LENGTH_SHORT).show();
                handler.onFailure();
            }
        });

    }


    @MainThread
    protected void openMainScreen() {
        long diff = startTime == 0 ? 0 : Math.max(0, DELAY_MIN_MS - (System.currentTimeMillis() - startTime));

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {

            if (!shouldSkip) {
                MainActivity_.intent(BaseActivity.this)
                        .extra(Keys.TRANSACTION_TYPE, getTransactionTypeFromExternalApp())
                        .extra(Keys.TRANSACTION_EXTRAS, extractTransactionExtras(this))
                        .extra(Keys.AMOUNT_KEY, getAmountFromExternalApp(Keys.AMOUNT))
                        .extra(Keys.TIP_AMOUNT_KEY, getAmountFromExternalApp(Keys.TIP_AMOUNT))
                        .extra(Keys.APP_LAUNCH, isLaunchFromExternalApp())
                        .extra(Keys.TRANSITION_TO_SALE, true)
                        .flags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .start();
                finish();
                if(isLaunchFromExternalApp()) {
                    getIntent().putExtras(new Bundle());
                }
            }
        }, diff);

    }

    boolean isLaunchFromExternalApp() {
        String action = getIntent().getAction();
        boolean appLaunch = (BuildConfig.APPLICATION_ID + ".TRANSACTION").equalsIgnoreCase(action);
        return getIntent().getBooleanExtra(Keys.APP_LAUNCH, appLaunch);
    }

    TransactionType getTransactionTypeFromExternalApp() {
        // External apps are allowed to start only sale transactions at this stage.
        return TransactionType.SALE;
    }

    double getAmountFromExternalApp(String amountKey) {
        try {
            String amount = isLaunchFromExternalApp()
                    ? getIntent().getStringExtra(amountKey)
                    : null;
            // Expected format: 0.15 1.00 2.99 etc.
            return amount != null
                    && amount.length() >= 4
                    && amount.length() <= 7
                    && amount.charAt(amount.length() - 3) == '.'
                    ? Double.parseDouble(amount)
                    : extractAmountFromStartIntent(amountKey);
        } catch (Exception e) {
            return 0.0;
        }
    }

    @MainThread
    public void openLoginScreen() {

        long diff = startTime == 0 ? 0 : Math.max(0, DELAY_MIN_MS - (System.currentTimeMillis() - startTime));

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (!shouldSkip) {
                LoginActivity_.intent(BaseActivity.this)
                        .extra(Keys.TRANSACTION_TYPE, (getIntent().getAction() != null) ? getTransactionTypeFromExternalApp() : null)
                        .extra(Keys.TRANSACTION_EXTRAS, (getIntent().getAction() != null) ? extractTransactionExtras(BaseActivity.this) : null)
                        .extra(Keys.AMOUNT_KEY, (getIntent().getAction() != null) ? getAmountFromExternalApp(Keys.AMOUNT) : 0.0)
                        .extra(Keys.TIP_AMOUNT_KEY, (getIntent().getAction() != null) ? getAmountFromExternalApp(Keys.TIP_AMOUNT) : 0.0)
                        .extra(Keys.APP_LAUNCH, (getIntent().getAction() != null && isLaunchFromExternalApp()))
                        .flags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .start();
                finishAffinity();
            }
        }, diff);

    }


    /**
     * Animated change text color from to for a button
     *
     * Used in SalesActivity and AnalyticsActivity
     *
     * @param button which button to update
     * @param fromColor starting color
     * @param toColor end color
     * @param duration duration in ms
     */
    protected void animateButtonTextColor(Button button, int fromColor, int toColor, int duration) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimation.setDuration(duration);
        colorAnimation.addUpdateListener(animator -> button.setTextColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    protected boolean shouldUpdateNfcReaderMode() {
        return true;
    }

    protected boolean isNfcEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isNfcEnabled() && shouldUpdateNfcReaderMode()) {
            nfcAdapter.enableReaderMode(this,
                    tag -> {
                    },
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS |
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null
            );
        }

        if (!BuildConfig.DEBUG) {
            if (this instanceof LoginActivity) {
                callVerifier.addTags(CallVerifier.TAG_DEV_OPTIONS);
                systemSettingsChecker.showBlockerAlertOnDevOptionsEnabled(this);

                try {
                    callVerifier.verifyTags(CallVerifier.TAG_DEV_OPTIONS);
                } catch (AttestationException ex) {
                } // Should not be possible
            }
        }

        doLoginCheck();

        doAuditCacheCheck();

        new Thread(this::checkForServerTime).start();

        setCorrectViewsLanguage();
    }

    void checkLoginRequiredOnBackend() {
        // Override this on important screens - for example before initiating a sale.
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isNfcEnabled() && shouldUpdateNfcReaderMode()) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    private void doLoginCheck() {
        if (isLoginRequired()) {
            createLoginRequiredAlert(this);
        } else {
            checkLoginRequiredOnBackend();
        }
    }

    private void doAuditCacheCheck() {
        if (auditLogger.isCacheTooStale()) {
            displaySecurityAlert();
        }
    }

    private boolean isLoginRequired() {
        return isLoginProtectedScreen() && !tokenManager.hasTokens();
    }

    private boolean isLoginProtectedScreen() {
        boolean unprotectedScreen = this instanceof SplashActivity
                || this instanceof LoginActivity
                || this instanceof SignupActivity
                || this instanceof ForgotPasswordActivity
                || this instanceof ChangePasswordActivity
                || this instanceof BuildNumberActivity;
        return !unprotectedScreen;
    }

    protected void createLoginRequiredAlert(Context context) {
        dialogManager.showDialog(new DialogState.LoginRequiredState(this, stringManager, () -> {
            openLoginScreen();
            return null;
        }), -1);
    }

    protected void enableAllLayoutChangeAnimations(int rootLayoutId,
                                                   LayoutTransition.TransitionListener listener) {
        // Set android:animateLayoutChanges="true" in the rootLayout
        LayoutTransition transition = ((ViewGroup) findViewById(rootLayoutId)).getLayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        if (listener != null) {
            transition.addTransitionListener(listener);
        }
    }

    protected void enableAllLayoutChangeAnimations(int rootLayoutId) {
        enableAllLayoutChangeAnimations(rootLayoutId, null);
    }

    // TODO: 11/14/2019 Handle zero-decimal currencies like JPY
    protected int amountInCents(double amount) {
        return (int) Math.round((amount * 100));
    }

    protected double amountInBackendFormat(int amountInCents) {
        return (double) amountInCents / 100;
    }

    protected TransactionType extractTransactionTypeFromStartIntent() {
        Serializable type = getIntent().getSerializableExtra(Keys.TRANSACTION_TYPE);
        if (type instanceof TransactionType) {
            return (TransactionType) type;
        }
        return TransactionType.SALE;
    }

    protected double extractAmountFromStartIntent(String amountKey) {
        return getIntent().getDoubleExtra((amountKey.equalsIgnoreCase(Keys.AMOUNT) ? Keys.AMOUNT_KEY : Keys.TIP_AMOUNT_KEY), 0);
    }

    protected Drawable getTintedDrawableFromRuntimeColor(int drawableRes, int color) {
        return ImageViewBindings.tintDrawable(ImageViewBindings.getDrawable(this, drawableRes), color);
    }

    public PhosStringProvider getStringManager() {
        return stringManager;
    }

    // Doesn't require permission. Test and use it instead
    // of KeyboardHelper.vibrateKeyPress(Context)
    protected void performHapticFeedback(View view) {
        view.setHapticFeedbackEnabled(true);
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    void checkForServerTime() {
        if (!SecureTimer.isTimeAvailable()) {
            auditLogger.logAlert(
                    "ServerTime",
                    AuditErrorCodes.FAILED_TO_GET_SERVER_TIME,
                    new AuditLogger.Pair("reasonFailed", "Server time is unavailable!"));

            new Handler(getMainLooper()).post(this::displaySecurityAlert);
        }
    }

    private void displaySecurityAlert() {
        dialogManager.showDialog(new DialogState.SecurityCheckFailedState(this, stringManager, () -> {
            finish();
            return null;
        }), -1);
    }

    @Override
    protected void onStop() {
        super.onStop();
        dialogManager.cleanUp();
    }
}
