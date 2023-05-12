package digital.paynetics.phos.screens;

import static digital.paynetics.phos.screens.ActivityUtils.extractTransactionExtras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.helpers.Convert;
import digital.paynetics.phos.classes.helpers.DynamicSalesIconProvider;
import digital.paynetics.phos.classes.helpers.IdProvider;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.presentation.view.TransactionFlowActivity;
import digital.paynetics.phos.sdk.BuildConfig;
import digital.paynetics.phos.sdk.CompletionHandler;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.entities.ClientConfig;
import digital.paynetics.phos.sdk.entities.DashboardStats;
import digital.paynetics.phos.sdk.entities.Keys;


@EActivity(R.layout.activity_main)
public class MainActivity extends BaseActivity {

    @ViewById
    ImageView salesIcon;

    @ViewById
    ImageView transactionsIcon;

    @ViewById
    ImageView analyticsIcon;

    @ViewById
    ImageView loyaltyIcon;

    @ViewById
    ImageView imgLogo;

    @ViewById
    TextView txtCompanyName;

    @ViewById
    LinearLayout bottomBar;

    @ViewById
    TextView labelDaily;

    @ViewById
    TextView labelMonthly;

    @ViewById
    TextView txtSubtitle;

    @ViewById
    TextView salesLabel;

    @ViewById
    TextView transactionsLabel;

    @ViewById
    TextView analyticsLabel;

    @ViewById
    TextView loyaltyLabel;

    @ViewById
    TextView labelDailySales;

    @ViewById
    TextView labelMonthlySales;

    @ViewById
    ImageView openBankingIcon;

    @ViewById
    TextView openBankingLabel;

    @ViewById
    View btnOpenBanking;

    @Inject
    IdProvider idProvider;

    @Inject
    DynamicSalesIconProvider iconProvider;

    @Inject
    ClientConfig clientConfig;

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    @AfterViews
    public void afterViews() {
        initIcons();

        boolean transitionToSale = !hasSavedInstanceState()
                && getIntent().getBooleanExtra(Keys.TRANSITION_TO_SALE, false);
        if (transitionToSale) {
            Intent intent = new Intent(this, TransactionFlowActivity.class);
            intent.putExtra(Keys.TRANSACTION_TYPE, extractTransactionTypeFromStartIntent())
                    .putExtra(Keys.TRANSACTION_EXTRAS, extractTransactionExtras(this))
                    .putExtra(Keys.AMOUNT_KEY, extractAmountFromStartIntent(Keys.AMOUNT))
                    .putExtra(Keys.TIP_AMOUNT_KEY, extractAmountFromStartIntent(Keys.TIP_AMOUNT))
                    .putExtra(Keys.TRANSITION_TO_READER, getIntent().getBooleanExtra(Keys.TRANSITION_TO_READER, false))
                    .putExtra(Keys.TRANSITION_TO_SALE, getIntent().getBooleanExtra(Keys.TRANSITION_TO_SALE, false))
                    .putExtra(Keys.APP_LAUNCH, isLaunchFromExternalApp())
                    .putExtra(Keys.ENABLE_ORDER_REFERENCE, true)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
        }

        if (isLaunchFromExternalApp()) {
            finishAffinity();
        }

        if (getResources().getBoolean(R.bool.show_merchant_name)) {
            txtCompanyName.setText(clientConfig.getMerchant().getName());
        } else {
            txtCompanyName.setVisibility(View.GONE);
        }

        dynamicMerchantLogo.showInto(imgLogo);

        // settings screen can send the LOGOUT event, and we catch that to properly logout the user.

        notifier.registerForEvents(BaseActivity.NOTIFICATION_LOGOUT, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);

                tokenManager.clear();

                // remove merchant info
                clientConfig.setMerchant(null);

                openLoginScreen();

                // finish this activity in the background, so it's smooth
                new Handler().postDelayed(MainActivity.this::finish, 2000);
            }
        });

        // load sales dashboard values

        labelDaily.setText("...");
        labelMonthly.setText("...");
        if (BuildConfig.SDK_OPEN_BANKING) {
            initAdditionalIcons();
            if(clientConfig.getMerchant().isOpenBankingEnabled()) {
                btnOpenBanking.setVisibility(View.VISIBLE);
                checkIfRearrangeNeeded();
            }
        }
    }

    private void initAdditionalIcons() {
        openBankingIcon.setImageDrawable(getTintedDrawableFromRuntimeColor(R.drawable.ic_open_banking_icon_24dp, clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR)));
        openBankingLabel.setText(stringManager.getString(PhosString.open_banking));
        final View btnOpenBanking = findViewById(R.id.btnOpenBanking);
        if(btnOpenBanking != null) {
            btnOpenBanking.setOnClickListener(view -> startActivity(getTransactionIntent()
                    .putExtra(Keys.IS_OPEN_BANKING, true)));
        }
    }

    private void checkIfRearrangeNeeded() {
        final boolean isLoyaltyButtonVisible = getResources().getInteger(R.integer.loyalty_button_visibility) == 0;
        if(!isLoyaltyButtonVisible) {
            ConstraintLayout constraintLayout = findViewById(R.id.root);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.btnOpenBanking,ConstraintSet.START,R.id.btnLoyalty,ConstraintSet.START,0);
            constraintSet.connect(R.id.btnOpenBanking,ConstraintSet.TOP,R.id.btnLoyalty,ConstraintSet.TOP,0);
            constraintSet.connect(R.id.btnOpenBanking,ConstraintSet.END,R.id.btnLoyalty,ConstraintSet.END,0);
            constraintSet.applyTo(constraintLayout);
        }
    }

    private void initIcons() {
        salesIcon.setImageDrawable(getTintedDrawableFromRuntimeColor(iconProvider.getSalesIcon(), clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR)));
        transactionsIcon.setImageDrawable(getTintedDrawableFromRuntimeColor(R.drawable.ic_transactions_24dp, clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR)));
        analyticsIcon.setImageDrawable(getTintedDrawableFromRuntimeColor(R.drawable.ic_analytics_24dp, clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR)));
        loyaltyIcon.setImageDrawable(getTintedDrawableFromRuntimeColor(R.drawable.ic_loyalty_24dp, clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR)));
    }

    @Override
    protected void setCorrectViewsLanguage() {
        txtSubtitle.setText(stringManager.getString(PhosString.dashboard));
        if (!getResources().getBoolean(R.bool.show_dashboard_title)) {
            txtSubtitle.setVisibility(View.GONE);
        }

        PhosString label = getResources().getBoolean(R.bool.predefined_amounts)
                ? PhosString.donate
                : PhosString.btn_sale;
        salesLabel.setText(stringManager.getString(label));

        transactionsLabel.setText(stringManager.getString(PhosString.transactions));

        analyticsLabel.setText(stringManager.getString(PhosString.analytics));

        loyaltyLabel.setText(stringManager.getString(PhosString.loyalty));

        labelDailySales.setText(stringManager.getString(PhosString.daily_sales));

        labelMonthlySales.setText(stringManager.getString(PhosString.monthly_sales));
    }

    @Override
    protected void onResume() {
        super.onResume();

        retries = 0;
        reloadDashboardInfo();
    }

    int retries = 0;

    private void reloadDashboardInfo() {
        String timezone = idProvider.getDefaultTimeZone();
        phosConnect.loadDashboardInfo(timezone, new CompletionHandler.DashboardInfo() {
            @Override
            public void onSuccess(DashboardStats stats) {

                retries = 0;

                labelDaily.setText(Convert.formatCurrencyWithFraction(stats.getDaily(),
                        stats.getCurrency(),
                        clientConfig.getFractionDigit()));
                labelMonthly.setText(Convert.formatCurrencyWithFraction(stats.getMonthly(),
                        stats.getCurrency(),
                        clientConfig.getFractionDigit()));
            }


            @Override
            public void onError(ResponseCode responseCode) {

                //do nothing if could not load info, will retry next time

                if (labelDaily.getText().equals("...")) {
                    retries++;

                    if (retries < 5) {
                        // retry in 2 seconds again, for max 5 times
                        new Handler().postDelayed(MainActivity.this::reloadDashboardInfo, 2000);
                    } else {
                        labelDaily.setText("x");
                        labelMonthly.setText("x");
                    }
                }
            }
        });
    }

    //bottomBar

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    private Intent getTransactionIntent() {
        Intent intent = new Intent(this, TransactionFlowActivity.class);
        return intent.putExtra(Keys.TRANSACTION_TYPE, extractTransactionTypeFromStartIntent())
                .putExtra(Keys.AMOUNT_KEY, extractAmountFromStartIntent(Keys.AMOUNT_KEY))
                .putExtra(Keys.TIP_AMOUNT_KEY, extractAmountFromStartIntent(Keys.TIP_AMOUNT_KEY))
                .putExtra(Keys.APP_LAUNCH, isLaunchFromExternalApp())
                .putExtra(Keys.ENABLE_ORDER_REFERENCE, true)
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
    }

    @Click(R.id.btnSales)
    public void sales() {
        startActivity(getTransactionIntent());
    }

    @Click(R.id.btnSettings)
    public void settings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Click(R.id.btnAnalytics)
    public void analytics() {
        AnalyticsActivity_.intent(MainActivity.this).start();
    }

    @Click(R.id.btnTransactions)
    public void transactions() {
        TransactionsActivity_.intent(MainActivity.this).start();
    }

    @Click(R.id.btnLoyalty)
    public void loyalty() {
        LoyaltyActivity_.intent(MainActivity.this).start();
    }


}
