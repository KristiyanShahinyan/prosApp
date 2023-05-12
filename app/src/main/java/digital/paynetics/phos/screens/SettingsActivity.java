package digital.paynetics.phos.screens;

import android.os.Bundle;

import androidx.annotation.Nullable;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;


public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        PhosApplication.getAppComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
}
