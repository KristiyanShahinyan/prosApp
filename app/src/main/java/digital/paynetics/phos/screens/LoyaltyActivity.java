package digital.paynetics.phos.screens;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.BindingObject;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.DataBound;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.helpers.IntentHelper;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.common.BaseListAdapter;
import digital.paynetics.phos.databinding.ActivityLoyaltyBinding;
import digital.paynetics.phos.sdk.entities.ApiResponse;
import digital.paynetics.phos.sdk.entities.LoyaltyItem;
import digital.paynetics.phos.sdk.services.PhosConnect;


@DataBound
@EActivity(R.layout.activity_loyalty)
public class LoyaltyActivity extends BaseActivity {

    @ViewById
    TextView titleView;

    @BindingObject
    ActivityLoyaltyBinding binding;

    @Inject
    IntentHelper intentHelper;

    @Inject
    PhosConnect phosConnect;

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    @AfterViews
    public void afterViews() {

        titleView.setText(stringManager.getString(PhosString.loyalty));

        binding.list.setAdapter(new LoyaltyListAdapter(this));
        getLoyaltyData();

        binding.loadingIndicator.setColor(clientConfig.getDynamicColor(BrandingColors.LOADING_INDICATOR_COLOR));
    }


    @Click(R.id.btnBack)
    public void close() {
        finish();
    }

    void open(LoyaltyItem item) {
        if (item.isAppType()) {
            openApp(item.target);
        } else if (item.isUrlType()) {
            openUrl(item.target);
        } else {
            throw new IllegalStateException("Invalid type: " + item.type);
        }
    }

    void openApp(String packageName) {
        Intent intent = intentHelper.getOpenOrDownloadAppIntent(packageName);
        startActivity(intent);
    }

    void openUrl(String url) {
        Intent intent = intentHelper.getOpenUrlIntent(url);
        startActivity(intent);
    }

    @Override
    public void startActivity(Intent intent) {
        try {
            if (intent != null) {
                super.startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    @Background
    void getLoyaltyData() {
        setLoadingVisible(true);

        ApiResponse<List<LoyaltyItem>> response = phosConnect.getLoyaltyData();
        if (response.isSuccessful()) {
            setLoyaltyData(response.getData());
        }

        setLoadingVisible(false);
    }

    @UiThread
    void setLoyaltyData(List<LoyaltyItem> itemList) {
        itemList = setInstalledFlag(itemList);
        ((LoyaltyListAdapter) binding.list.getAdapter()).update(itemList);
    }

    @UiThread
    void setLoadingVisible(boolean loadingVisible) {
        binding.loadingView.setVisibility(loadingVisible ? View.VISIBLE : View.GONE);
    }

    List<LoyaltyItem> setInstalledFlag(List<LoyaltyItem> itemList) {
        List<LoyaltyItem> resultItemList = new ArrayList<>();
        for (LoyaltyItem item : itemList) {
            boolean installed = intentHelper.isAppInstalled(item.target);
            resultItemList.add(item.withInstalledFlag(installed));
        }
        return resultItemList;
    }

    static class LoyaltyListAdapter extends BaseListAdapter<LoyaltyItem> {

        private final LoyaltyActivity activity;

        LoyaltyListAdapter(LoyaltyActivity activity) {
            super();
            this.activity = activity;
        }

        LoyaltyListAdapter(LoyaltyActivity activity, List<LoyaltyItem> items) {
            super(items);
            this.activity = activity;
        }

        @Override
        protected int getLayoutResourceId(LoyaltyItem item) {
            return R.layout.row_loyalty_item;
        }

        @Override
        public void onItemClick(LoyaltyItem item) {
            activity.open(item);
        }
    }
}
