package digital.paynetics.phos;

import com.bolyartech.forge.android.app_unit.UnitApplication;

import org.androidannotations.annotations.EApplication;

import digital.paynetics.phos.classes.router.AppRouterImpl;
import digital.paynetics.phos.dagger.AppComponent;
import digital.paynetics.phos.dagger.AppModule;
import digital.paynetics.phos.dagger.DaggerAppComponent;
import digital.paynetics.phos.di.SdkComponent;
import digital.paynetics.phos.sdk.BuildConfig;


@EApplication
public class PhosApplication extends UnitApplication {
    public static AppComponent getAppComponent() {
        return appComponent;
    }

    private static AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = buildComponent();
    }

    protected AppComponent buildComponent() {
        AppModule appModule = AppModule.create();
        SdkComponent sdkComponent = PhosSdkProduction.getInstance().init(this, new AppRouterImpl(), BuildConfig.APP_INSTANCE);
        return DaggerAppComponent.builder()
                .appModule(appModule)
                .sdkComponent(sdkComponent)
                .build();
    }
}
