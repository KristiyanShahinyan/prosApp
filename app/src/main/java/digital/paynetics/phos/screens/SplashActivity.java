package digital.paynetics.phos.screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import digital.paynetics.phos.BuildConfig;
import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.app2app.App2App;
import digital.paynetics.phos.app2app.App2AppKeys;
import digital.paynetics.phos.app2app.OperationRequestParser;
import digital.paynetics.phos.classes.CompletionHandler;
import digital.paynetics.phos.classes.enums.BrandingImages;
import digital.paynetics.phos.classes.helpers.DoNotDisturbManager;
import digital.paynetics.phos.classes.helpers.IdProvider;
import digital.paynetics.phos.classes.helpers.NetworkManager;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.classes.prefs.PhonePermissionExplanation;
import digital.paynetics.phos.classes.prefs.ResetTokenPreference;
import digital.paynetics.phos.classes.prefs.TransactionHashPreference;
import digital.paynetics.phos.dialogs.ForceUpdateDialog;
import digital.paynetics.phos.dialogs.manager.DialogState;
import digital.paynetics.phos.exceptions.AttestationException;
import digital.paynetics.phos.exceptions.PhosException;
import digital.paynetics.phos.sca.ScaProviderClient;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.callback.AuthCallback;
import digital.paynetics.phos.sdk.entities.ApiResponse;
import digital.paynetics.phos.sdk.entities.ApplicationSkin;
import digital.paynetics.phos.sdk.entities.DynamicWhitelabel;
import digital.paynetics.phos.sdk.entities.DynamicWhitelabelData;
import digital.paynetics.phos.sdk.entities.Keys;
import digital.paynetics.phos.sdk.entities.Update;
import digital.paynetics.phos.sdk.enums.TransactionType;
import digital.paynetics.phos.security.checks.CallVerifier;
import digital.paynetics.phos.security.checks.SecurityManager;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
@EActivity(R.layout.activity_splash)
public class SplashActivity extends BaseActivity implements ForceUpdateDialog.UpdateCallback {

    private static final int CODE_APP_SETTINGS = 1;

    @ViewById
    TextView version;

    @ViewById
    TextView lblSecurityCheck;

    @ViewById
    ImageView logo;

    @Inject
    NetworkManager networkManager;

    @Inject
    IdProvider idProvider;

    @Inject
    PhonePermissionExplanation phonePermissionExplanation;

    @Inject
    TransactionHashPreference transactionHashPreference;

    @Inject
    SecurityManager securityManager;

    @Inject
    DoNotDisturbManager dndManager;

    @Inject
    ResetTokenPreference resetToken;

    @Inject
    CallVerifier callVerifier;

    @Inject
    ScaProviderClient scaProviderClient;

    @Inject
    OperationRequestParser operationRequestParser;

    @Inject
    App2App app2app;

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
    void afterViews() {
        clientConfig.getSkinRequestCreator(BrandingImages.BRANDING_LOADING_IMAGE, true).into(logo);
        version.setText(idProvider.getAppName());

        lblSecurityCheck.setText(stringManager.getString(PhosString.security_check));

        handleStartFromUrl();
        initWithExplanationAndPermissionCheck(this);
    }

    @OnActivityResult(CODE_APP_SETTINGS)
    void onResult(int resultCode) {
        initWithExplanationAndPermissionCheck(this);
    }

    @SuppressLint("MissingPermission")
    private void initWithExplanationAndPermissionCheck(SplashActivity target) {
        if (idProvider.isImeiAccessRestricted()) {
            startTime = System.currentTimeMillis();
            bgInit();
            return;
        }

        if (PermissionUtils.hasSelfPermissions(target, Manifest.permission.READ_PHONE_STATE)) {
            init();
        } else {
            if (phonePermissionExplanation.shouldShow() ||
                    PermissionUtils.shouldShowRequestPermissionRationale(target, Manifest.permission.READ_PHONE_STATE)) {
                dialogManager.showDialog(new DialogState.PhonePermissionRequestState(this, stringManager, false, () -> {
                    phonePermissionExplanation.disable();
                    init();
                    return null;
                }, ()-> {
                    finish();
                    return null;
                }), -1);
            } else {
                dialogManager.showDialog(new DialogState.PhonePermissionRequestState(this, stringManager, true, () -> {
                    startActivityForResult(createOpenAppSettingsIntent(), CODE_APP_SETTINGS);
                    return null;
                }, ()-> {
                    finish();
                    return null;
                }), -1);
            }
        }
    }

    @NeedsPermission(Manifest.permission.READ_PHONE_STATE)
    @SuppressLint("MissingPermission")
    void init() {
        startTime = System.currentTimeMillis();
        bgInit();
    }

    @OnPermissionDenied(Manifest.permission.READ_PHONE_STATE)
    void showDeniedForPhone() {
        Toast.makeText(this, "Permission denied. Closing app...", Toast.LENGTH_SHORT).show();
        finish();
    }

    @OnNeverAskAgain(Manifest.permission.READ_PHONE_STATE)
    void showNeverAskForPhone() {
        Toast.makeText(this, "Permission never ask. Closing app...", Toast.LENGTH_SHORT).show();
        finish();
    }

    private Intent createOpenAppSettingsIntent() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        return intent;
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    @Background
    void bgInit() {
        if (networkManager.isOfflineMode()) {
            openOffline();
            return;
        }

        Result result = setWhitelabelFromServer();
        if (result == Result.NO_CONFIG) {
            showMissingConfigAlert();
            return;
        }
        if (result == Result.UPDATE_PENDING) {
            finishOnUiThread();
            return;
        }

        // set last transaction hash, if we have it.
        phosConnect.setLastTransactionHash(transactionHashPreference.get());

        //// in sdk
        callVerifier.addTags(CallVerifier.TAG_SECURITY_MANAGER);
        if (securityManager.hasSecurityIssues()) {
            openSecurityCheckFailed();
            return;
        }
        try {
            callVerifier.verifyTags(CallVerifier.TAG_SECURITY_MANAGER);
        } catch (AttestationException ex) {
            openSecurityCheckFailed();
            return;
        }
        ///// end in sdk

        scaProviderClient.generateKeys();

        app2app.init(this);

        if (app2app.handleLogin(authCallback)) {
            return;
        }

        if (!phosConnect.isLoggedIn()) {
            openLoginScreen();
        } else {

            // try to get terminal config and then if okay, open main screen or login screen on failure
            getInitialConfig(new CompletionHandler() {
                @Override
                public void onSuccess() {
                    if (resetToken.isAvailable()) {
                        openLoginScreen();
                    } else {
                        if(!app2app.handleStartIntent(SplashActivity.this)){
                            openMainScreen();
                        }
                    }
                }

                @Override
                public void onFailure() {
                    openLoginScreen();
                }
            });
        }
    }

    private final AuthCallback authCallback = new AuthCallback() {
        @Override
        public void onSuccess(Void data, @Nullable Map<String, String> extras) {
            Intent resultData = new Intent();
            resultData.putExtra(getPackageName() + "." + App2AppKeys.OPERATION_RESULT,
                    app2app.createLoginSuccessResponse());
            setResult(Activity.RESULT_OK, resultData);
            finish();
        }

        @Override
        public void onFailure(PhosException error, @Nullable Map<String, String> extras) {
            Intent resultData = new Intent();
            resultData.putExtra(getPackageName() + "." + App2AppKeys.OPERATION_RESULT,
                    app2app.createLoginFailureResponse(error.getCode()));
            setResult(Activity.RESULT_OK, resultData);
            finish();
        }
    };

    @UiThread
    void openSecurityCheckFailed() {
        dialogManager.showDialog(new DialogState.SecurityCheckFailedState(this, stringManager, () -> {
            finish();
            return null;
        }), -1);
        killApplicationAfterDelay();
    }

    @Background
    void killApplicationAfterDelay() {
        try {
            Thread.sleep(3000); // 3 seconds
        } catch (InterruptedException ex) {
        }

        // Kill the app with a NullPointerException
        String nullString = null;
        nullString.length();
    }

    @UiThread
    void openOffline() {
        super.openOffline();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        shouldSkip = true;
    }

    void handleStartFromUrl() {
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();

        resetToken.delete();

        if (appLinkData != null) {
            if (appLinkData.getLastPathSegment() != null) {
                if (appLinkData.getLastPathSegment().equalsIgnoreCase("transaction")) {
                    String amount = appLinkData.getQueryParameter("amount");
                    String tipAmount = appLinkData.getQueryParameter("tip");
                    String orderReference = appLinkData.getQueryParameter("order_reference");
                    getIntent().setAction((BuildConfig.APPLICATION_ID + ".TRANSACTION"));
                    Double parsedAmount;
                    Double parsedTip;
                    try {
                        parsedAmount = Double.parseDouble(amount);
                        parsedTip = Double.parseDouble(tipAmount);
                    } catch (NumberFormatException e) {
                        parsedAmount = 0.0;
                        parsedTip = 0.0;
                    }
                    getIntent().putExtra(Keys.AMOUNT_KEY, parsedAmount);
                    getIntent().putExtra(Keys.TIP_AMOUNT_KEY, parsedTip);

                    getIntent().putExtra(Keys.TRANSACTION_TYPE, TransactionType.SALE);
                    getIntent().putExtra(Keys.TRANSITION_TO_READER, true);
                    getIntent().putExtra(Keys.TRANSITION_TO_SALE, true);
                    getIntent().putExtra(Keys.ORDER_REFERENCE_APP2APP, orderReference);
                } else {
                    String token = appLinkData.getLastPathSegment();
                    if (Intent.ACTION_VIEW.equals(appLinkAction)) {
                        resetToken.set(token);
                    }
                }
            }
        }
    }

    private Result setWhitelabelFromServer() {
        ApiResponse<DynamicWhitelabel> response = phosConnectNoEncryption
                .getBrandingSkin(digital.paynetics.phos.sdk.BuildConfig.APP_INSTANCE);
        DynamicWhitelabel responseData = response.isSuccessful() ? response.getData() : null;
        if (responseData != null) {
            if (clientConfig.isDynamicRebrandingEnabled()) {
                setBrandingSkin(responseData);
            }
            return handleUpdateConfig(responseData);
        }
        return Result.NO_CONFIG;
    }

    @UiThread
    void showMissingConfigAlert() {
        dialogManager.showDialog(new DialogState.MissingConfigState(this, stringManager, () -> {
            finish();
            return null;
        }), -1);
    }

    private Result handleUpdateConfig(DynamicWhitelabel config) {
        int forceUpdateMode =
                config.getResponseCode() == ResponseCode.UNSUPPORTED_APP_VERSION.getCode() ? 1 : 0;
        Update update = config.getDynamicWhitelabelData() != null
                ? config.getDynamicWhitelabelData().getUpdate()
                : null;
        if (config.getResponseCode() == ResponseCode.SUCCESS.getCode()) {
            forceUpdateMode = getForceUpdateMode(update);
        }

        if (forceUpdateMode > 0) {
            showForceUpdateDialog(forceUpdateMode, update);
            awaitUserConfirmation();
            return updatedSkipped ? Result.UPDATE_SKIPPED : Result.UPDATE_PENDING;
        }

        return Result.UPDATE_NOT_REQUIRED;
    }

    private int getForceUpdateMode(Update update) {
        if (update != null) {
            if (shouldShowForceUpdateMandatoryDialog(update)) {
                return 1;
            } else if (shouldShowForceUpdateSkipDialog(update)) {
                return 2;
            } else if (shouldShowForceUpdateUntilDateDialog(update)) {
                return 3;
            }
        }
        return 0;
    }

    @UiThread
    void showForceUpdateDialog(int forceUpdateMode, Update update) {
        String desc = update != null ? update.getDescription() : null;
        long untilDate = 0;
        if (forceUpdateMode == 3) {
            try {
                untilDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
                        .parse(update.getLastDateBeforeForce())
                        .getTime();
            } catch (ParseException ignore) {
            }
        }
        ForceUpdateDialog dialog = ForceUpdateDialog.newInstance(forceUpdateMode, untilDate, desc);
        dialog.show(getSupportFragmentManager(), ForceUpdateDialog.class.getSimpleName());
    }

    private boolean shouldShowForceUpdateSkipDialog(Update update) {
        int currentVersionCode = BuildConfig.VERSION_CODE;
        int backendVersionCode = update.getVersion();
        boolean backendForceFlag = update.isForceUpdate();
        String backendForceDateAsString = update.getLastDateBeforeForce();
        return currentVersionCode < backendVersionCode
                && !backendForceFlag
                && TextUtils.isEmpty(backendForceDateAsString);
    }

    private boolean shouldShowForceUpdateUntilDateDialog(Update update) {
        int currentVersionCode = BuildConfig.VERSION_CODE;
        int backendVersionCode = update.getVersion();
        boolean backendForceFlag = update.isForceUpdate();
        String backendForceDateAsString = update.getLastDateBeforeForce();
        return currentVersionCode < backendVersionCode
                && !backendForceFlag
                && !TextUtils.isEmpty(backendForceDateAsString);
    }

    private boolean shouldShowForceUpdateMandatoryDialog(Update update) {
        int currentVersionCode = BuildConfig.VERSION_CODE;
        int backendVersionCode = update.getVersion();
        boolean backendForceFlag = update.isForceUpdate();
        if (currentVersionCode < backendVersionCode && backendForceFlag) {
            return true;
        } else {
            return backendVersionCode - currentVersionCode > 1;
        }
    }

    private void setBrandingSkin(DynamicWhitelabel config) {
        // Try set whitelabel data (colors and images) if provided by the backend,
        // otherwise fallback to default in-app resources.
        DynamicWhitelabelData data = config.getDynamicWhitelabelData();
        ApplicationSkin applicationSkin = data != null ? data.getSkin() : null;
        if (applicationSkin != null) {
            clientConfig.setRebrandingData(config);
        }
    }

    // TODO Move all update related code to a separate class.

    private enum Result {
        UPDATE_NOT_REQUIRED,
        NO_CONFIG,
        UPDATE_SKIPPED,
        UPDATE_PENDING
    }

    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile boolean updatedSkipped;

    @Override
    public void onUpdateResult(boolean updatedSkipped) {
        this.updatedSkipped = updatedSkipped;
        latch.countDown();
    }

    private void awaitUserConfirmation() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    void finishOnUiThread() {
        finish();
    }
}
