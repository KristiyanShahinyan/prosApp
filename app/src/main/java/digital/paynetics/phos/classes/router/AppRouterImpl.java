package digital.paynetics.phos.classes.router;

import android.app.Activity;
import android.content.Intent;

import digital.paynetics.phos.BuildConfig;
import digital.paynetics.phos.screens.ActivityUtils;
import digital.paynetics.phos.screens.LoginActivity_;
import digital.paynetics.phos.sdk.entities.Keys;
import digital.paynetics.phos.sdk.enums.TransactionType;

import static digital.paynetics.phos.screens.ActivityUtils.extractTransactionExtras;

public class AppRouterImpl implements AppRouter {
    @Override
    public void navigateToLogin(Activity activity) {
        LoginActivity_.intent(activity)
                .extra(Keys.TRANSACTION_TYPE, getTransactionTypeFromExternalApp())
                .extra(Keys.TRANSACTION_EXTRAS, extractTransactionExtras(activity))
                .extra(Keys.AMOUNT_KEY, getAmountFromExternalApp(activity, Keys.AMOUNT))
                .extra(Keys.TIP_AMOUNT_KEY, getAmountFromExternalApp(activity, Keys.TIP_AMOUNT))
                .extra(Keys.APP_LAUNCH, isLaunchFromExternalApp(activity))
                .flags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                .start();
        activity.finishAffinity();
    }

    private TransactionType getTransactionTypeFromExternalApp() {
        // External apps are allowed to start only sale transactions at this stage.
        return TransactionType.SALE;
    }

    private double getAmountFromExternalApp(Activity activity, String amountKey) {
        try {
            String amount = isLaunchFromExternalApp(activity)
                    ? activity.getIntent().getStringExtra(amountKey)
                    : null;
            // Expected format: 0.15 1.00 2.99 etc.
            return amount != null
                    && amount.length() >= 4
                    && amount.length() <= 7
                    && amount.charAt(amount.length() - 3) == '.'
                    ? Double.parseDouble(amount)
                    : extractAmountFromStartIntent(activity, amountKey);
        } catch (Exception e) {
            return 0.0;
        }
    }

    boolean isLaunchFromExternalApp(Activity activity) {
        String action = activity.getIntent().getAction();
        boolean appLaunch = (BuildConfig.APPLICATION_ID + ".TRANSACTION").equalsIgnoreCase(action);
        return activity.getIntent().getBooleanExtra(Keys.APP_LAUNCH, appLaunch);
    }

    private double extractAmountFromStartIntent(Activity activity, String amountKey) {
        return activity.getIntent().getDoubleExtra((amountKey.equalsIgnoreCase(Keys.AMOUNT) ? Keys.AMOUNT_KEY : Keys.TIP_AMOUNT_KEY), 0);
    }

}
