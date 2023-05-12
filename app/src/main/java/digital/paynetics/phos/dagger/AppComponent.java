package digital.paynetics.phos.dagger;

import dagger.Component;
import digital.paynetics.phos.di.SdkComponent;
import digital.paynetics.phos.screens.AnalyticsActivity;
import digital.paynetics.phos.screens.App2AppActivity;
import digital.paynetics.phos.screens.BuildNumberActivity;
import digital.paynetics.phos.screens.ChangePasswordActivity;
import digital.paynetics.phos.screens.ForgotPasswordActivity;
import digital.paynetics.phos.screens.LoginActivity;
import digital.paynetics.phos.screens.LoyaltyActivity;
import digital.paynetics.phos.screens.MainActivity;
import digital.paynetics.phos.screens.PinEncryptionKeyProviderActivity;
import digital.paynetics.phos.screens.PrintersActivity;
import digital.paynetics.phos.screens.SettingsActivity;
import digital.paynetics.phos.screens.SettingsAppVersionFragment;
import digital.paynetics.phos.screens.SettingsFragment;
import digital.paynetics.phos.screens.SettingsHelpFragment;
import digital.paynetics.phos.screens.SettingsLanguageFragment;
import digital.paynetics.phos.screens.SettingsLogoutDialogFragment;
import digital.paynetics.phos.screens.SettingsScreensFragment;
import digital.paynetics.phos.screens.SettingsSupportFragment;
import digital.paynetics.phos.screens.SignupActivity;
import digital.paynetics.phos.screens.SplashActivity;
import digital.paynetics.phos.screens.TransactionsActivity;

@Component(
        dependencies = {SdkComponent.class},
        modules = {AppModule.class}
        )
@PhosScope
public interface AppComponent {
    void inject(AnalyticsActivity target);
    void inject(ChangePasswordActivity target);
    void inject(ForgotPasswordActivity target);
    void inject(LoginActivity target);
    void inject(LoyaltyActivity target);
    void inject(MainActivity target);
    void inject(PrintersActivity target);
    void inject(SettingsActivity target);
    void inject(SettingsFragment target);
    void inject(SettingsHelpFragment target);
    void inject(SettingsScreensFragment target);
    void inject(SettingsSupportFragment target);
    void inject(SettingsAppVersionFragment target);
    void inject(SettingsLanguageFragment target);
    void inject(SettingsLogoutDialogFragment target);
    void inject(SignupActivity target);
    void inject(SplashActivity target);
    void inject(TransactionsActivity target);
    void inject(PinEncryptionKeyProviderActivity target);
    void inject(BuildNumberActivity target);
    void inject(App2AppActivity target);
}
