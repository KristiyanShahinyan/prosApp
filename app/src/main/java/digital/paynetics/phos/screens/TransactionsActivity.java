package digital.paynetics.phos.screens;

import static digital.paynetics.phos.domain.ScaType.SCA_TYPE_3D;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.ListPopupWindow;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.adapters.CustomListAdapter;
import digital.paynetics.phos.adapters.TransactionsAdapter;
import digital.paynetics.phos.classes.EndlessRecyclerViewScrollListener;
import digital.paynetics.phos.classes.MenuRow;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.enums.LoadingState;
import digital.paynetics.phos.classes.enums.RowType;
import digital.paynetics.phos.classes.helpers.Convert;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.dialogs.manager.DialogManager;
import digital.paynetics.phos.sdk.CompletionHandler;
import digital.paynetics.phos.sdk.ResponseCode;
import digital.paynetics.phos.sdk.entities.ClientConfig;
import digital.paynetics.phos.sdk.entities.Transaction;
import digital.paynetics.phos.sdk.entities.Transactions;
import digital.paynetics.phos.sdk.enums.TransactionState;
import digital.paynetics.phos.sdk.enums.TransactionType;
import digital.paynetics.phos.sdk.enums.TransactionsListType;
import digital.paynetics.phos.sdk.BuildConfig;


@EActivity(R.layout.activity_transactions)
public class TransactionsActivity extends BaseActivity {


    private static final int RECORDS_ON_PAGE = 20;
    private static final String TAG = "TransactionsActivity";

    private TransactionsAdapter adapter;

    private ArrayList<Transaction> records = new ArrayList<>();

    private Date date = null;
    private TransactionType type;
    private TransactionsListType transactionsListType;
    private TransactionState status;
    private boolean displayScaTransactions = false;

    private LoadingState state = LoadingState.NONE;

    private int totalPages = 0;

    private int currentPage = 0;

    private int currentButtonPosition = 0;
    private int[] btnContactlessLoc = new int[2], btnEcommerceLoc = new int[2], btnOpenBankingLoc = new int[2];

    @ViewById(R.id.transactionsList)
    RecyclerView recyclerView;

    @ViewById
    ImageButton btnDateSelect;

    @ViewById
    ImageButton btnTypeSelect;

    @ViewById
    ImageButton btnStatusSelect;

    @ViewById
    TextView txtDate;

    @ViewById
    TextView txtType;

    @ViewById
    TextView txtStatus;

    @ViewById(R.id.loadingIndicator)
    SpinKitView indicator;

    @ViewById
    TextView labelCentered;

    @ViewById
    TextView txtTitle;

    @ViewById
    Button buttonContactless;

    @ViewById
    Button buttonEcommerce;

    @ViewById
    View eCommerceContainer;

    @ViewById
    Button buttonOpenBanking;

    @ViewById
    View openBankingContainer;

    @ViewById
    View selector;

    @ViewById
    LinearLayout secureTransactionsSelectorLayout;

    @Inject
    ClientConfig clientConfig;

    @Inject
    DialogManager dialogManager;

    private Drawable arrowGray;
    private Drawable arrowBlue;

    private int textColorGray;
    private int textColorBlue;

    private String textPayment;
    private String textRefund;
    private String textVoid;
    private String textAll;

    private String textType;
    private String textStatus;
    private String textDate;

    private String textSuccessful;
    private String textPending;
    private String textFailed;

    private String textNoRecords;
    private String textNoRecordsFilter;
    private String textError;

    private Calendar minDateCal = Calendar.getInstance();

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    @AfterViews
    public void afterViews() {

        txtTitle.setText(stringManager.getString(PhosString.transactions));

        arrowBlue = getResources().getDrawable(R.drawable.down_arrow_blue);
        arrowGray = getResources().getDrawable(R.drawable.down_arrow_gray);

        textColorGray = getResources().getColor(R.color.gray);
        textColorBlue = clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR);

        indicator.setColor(clientConfig.getDynamicColor(BrandingColors.LOADING_INDICATOR_COLOR));

        // types
        textPayment = stringManager.getString(PhosString.type_payment).toString();
        textRefund = stringManager.getString(PhosString.type_refund).toString();
        textVoid = stringManager.getString(PhosString.type_void).toString();

        textNoRecords = stringManager.getString(PhosString.no_records_found).toString();
        textNoRecordsFilter = stringManager.getString(PhosString.no_records_found_filter).toString();
        textError = stringManager.getString(PhosString.retry_msg).toString();

        // types - 3DS or not
        if (clientConfig.getTerminal().getScaType() < SCA_TYPE_3D) { // Only display the buttons if 3DS transactions are available to the client
            eCommerceContainer.setVisibility(View.GONE);
            if(shouldShowOpenBanking()) {
                buttonContactless.setText(stringManager.getString(PhosString.contactless_transactions));
            } else {
                secureTransactionsSelectorLayout.setVisibility(View.GONE);
            }
        } else {
            eCommerceContainer.setVisibility(View.VISIBLE);
            buttonContactless.setText(stringManager.getString(PhosString.contactless_transactions));
            buttonEcommerce.setText(stringManager.getString(PhosString.ecommerce_transactions));
            shouldShowOpenBanking();
        }

        // statuses
        textSuccessful = stringManager.getString(PhosString.status_successful).toString();
        textPending = stringManager.getString(PhosString.status_pending).toString();
        textFailed = stringManager.getString(PhosString.status_failed).toString();

        // generic
        textDate = stringManager.getString(PhosString.date).toString();
        textType = stringManager.getString(PhosString.type).toString();
        textStatus = stringManager.getString(PhosString.status).toString();
        textAll = stringManager.getString(PhosString.type_all).toString();

        adapter = new TransactionsAdapter(this, records, false, clientConfig.getFractionDigit(), dialogManager);

        GridLayoutManager layout = new GridLayoutManager(this, 1);

        recyclerView.setLayoutManager(layout);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);

        recyclerView.setAdapter(adapter);

        EndlessRecyclerViewScrollListener scrollListener = new EndlessRecyclerViewScrollListener(layout) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                loadRecords(false);
            }
        };
        recyclerView.addOnScrollListener(scrollListener);

        loadRecords(true);

        minDateCal.set(2018, Calendar.JANUARY, 1, 0, 0, 0);
    }

    private Boolean shouldShowOpenBanking() {
        if(BuildConfig.SDK_OPEN_BANKING) {
            if (clientConfig.getMerchant().isOpenBankingEnabled()) {
                openBankingContainer.setVisibility(View.VISIBLE);
                buttonOpenBanking.setText(stringManager.getString(PhosString.open_banking_transactions));
                return true;
            }
        }
        return false;
    }

    @Click(R.id.txtTitle)
    public void titleClick() {
        recyclerView.scrollToPosition(0);
    }

    @OnActivityResult(BaseActivity.RESULT_CODE_RELOAD)
    void onResultCodeReload(int resultCode) {
        if (resultCode == RESULT_OK) {
            loadRecords(true);
        }
    }

    private void loadRecords(final boolean clear) {

        if (clear) {

            currentPage = 0;
            totalPages = 0;

            if (type != null) {
                txtType.setText(getTypeFromValue(type.getType()));
                txtType.setTextColor(textColorBlue);
                btnTypeSelect.setImageDrawable(arrowBlue);
            } else {
                txtType.setText(textType);
                txtType.setTextColor(textColorGray);
                btnTypeSelect.setImageDrawable(arrowGray);
            }

            if (status != null) {

                txtStatus.setText(getStatusFromCode(status.getCode()));
                txtStatus.setTextColor(textColorBlue);
                btnStatusSelect.setImageDrawable(arrowBlue);

            } else {
                txtStatus.setText(textStatus);
                txtStatus.setTextColor(textColorGray);
                btnStatusSelect.setImageDrawable(arrowGray);
            }

            if (date == null) {

                txtDate.setText(textDate);
                txtDate.setTextColor(textColorGray);
                btnDateSelect.setImageDrawable(arrowGray);

            } else {

                txtDate.setText(dateFormat.format(date));
                txtDate.setTextColor(textColorBlue);
                btnDateSelect.setImageDrawable(arrowBlue);

            }

            records.clear();
            adapter.setUseShortFormat(date != null);
        }

        currentPage++;

        if (currentPage > totalPages && totalPages != 0) {

            // do nothing, no more pages
            Log.i(TAG, "End of scroll, total pages: " + totalPages + ", current page" + currentPage);
            state = LoadingState.LOADED;
            return;
        }

        state = LoadingState.LOADING;

        if (clear) {
            updateInterface();
        }
        phosConnect.loadTransactions(currentPage,
                RECORDS_ON_PAGE,
                date, getTypes(),//(type == null) ? null : type.getType(),
                (status == null) ? null : status.getCode(),
                displayScaTransactions,
                new CompletionHandler.TransactionsList() {

                    @Override
                    public void onSuccess(Transactions transactionDetails) {

                        if (transactionDetails != null && transactionDetails.getItems() != null) {
                            totalPages = transactionDetails.getPagesCount();

                            if (clear) {
                                records.clear();
                            }
                            records.addAll(transactionDetails.getItems());
                        }

                        Log.i(TAG, "Loaded records, total pages: " + totalPages + ", current page" + currentPage);

                        state = LoadingState.LOADED;
                        updateInterface();
                    }

                    @Override
                    public void onError(ResponseCode responseCode) {
                        records.clear();
                        state = LoadingState.FAILED;
                        updateInterface();
                    }
                });

    }

    private String[] getTypes() {
        if(transactionsListType == null) {
            transactionsListType = TransactionsListType.CONTACTLESS;
        }
        switch(transactionsListType) {
            case ECOMMERCE:
            case CONTACTLESS:
                if(type == null) {
                    return new String[]{TransactionType.SALE.getType(), TransactionType.REFUND.getType(), TransactionType.VOID.getType()};
                } else {
                    return new String[]{type.getType()};
                }
            default:
                return handleDefaultCase();
        }
    }

    private String[] handleDefaultCase() {
        if(BuildConfig.SDK_OPEN_BANKING) {
            if(type == null) {
                return new String[]{TransactionType.NUAPAY_SALE.getType(), TransactionType.NUAPAY_REFUND.getType()};
            } else {
                switch (type) {
                    case SALE:
                        return new String[]{TransactionType.NUAPAY_SALE.getType()};
                    case REFUND:
                        return new String[]{TransactionType.NUAPAY_REFUND.getType()};
                }
            }
        }
        return new String[]{};
    }

    // can be done better
    private String getStatusFromCode(Integer code) {

        if (code == null)
            return textAll;

        if (code == TransactionState.SUCCESSFUL.getCode()) {
            return textSuccessful;
        } else if (code == TransactionState.PENDING.getCode()) {
            return textPending;
        } else if (code == TransactionState.FAILED.getCode()) {
            return textFailed;
        }

        return "";
    }


    // can be done better
    private String getTypeFromValue(String text) {
        if (text == null)
            return textAll;

        if (text.equals(TransactionType.SALE.getType())) {
            return textPayment;
        } else if (text.equals(TransactionType.VOID.getType())) {
            return textVoid;
        }
        if (text.equals(TransactionType.REFUND.getType())) {
            return textRefund;
        }

        return "";
    }


    private void updateInterface() {

        switch (state) {
            case LOADED:

                if (records.size() > 0) {
                    labelCentered.setVisibility(View.GONE);
                } else {
                    labelCentered.setVisibility(View.VISIBLE);
                    labelCentered.setText((date == null && status == null && type == null) ? textNoRecords : textNoRecordsFilter);
                }

                indicator.setVisibility(View.GONE);

                break;
            case FAILED:
                labelCentered.setVisibility(View.VISIBLE);
                labelCentered.setText(textError);

                indicator.setVisibility(View.GONE);

                break;
            case LOADING:
                labelCentered.setVisibility(View.GONE);
                indicator.setVisibility(View.VISIBLE);

                break;
        }


        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Click(R.id.btnBack)
    public void close() {
        finish();
    }

    @Click({R.id.btnTypeSelect, R.id.btnStatusSelect})
    public void selectTypeOrStatus(ImageButton button) {
        showListMenu(button);
    }

    private ArrayList<MenuRow> getDataForType(final ListPopupWindow popupWindow) {

        ArrayList<MenuRow> data = new ArrayList<>();

        MenuRow pmr = new MenuRow(RowType.TITLE, null, textType, null);
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.popup_type_all), textAll, () -> {
            type = null;
            popupWindow.dismiss();
            loadRecords(true);
        });
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.popup_type_sale), textPayment, () -> {
            type = TransactionType.SALE;
            popupWindow.dismiss();
            loadRecords(true);
        });
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.popup_type_refund), textRefund, () -> {
            type = TransactionType.REFUND;
            popupWindow.dismiss();
            loadRecords(true);
        });
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.popup_type_void), textVoid, () -> {
            type = TransactionType.VOID;
            popupWindow.dismiss();
            loadRecords(true);

        });
        data.add(pmr);

        return data;
    }


    private ArrayList<MenuRow> getDataForStatus(final ListPopupWindow popupWindow) {

        ArrayList<MenuRow> data = new ArrayList<>();

        MenuRow pmr = new MenuRow(RowType.TITLE, null, textStatus, null);
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.popup_type_all), textAll, () -> {
            status = null;
            popupWindow.dismiss();
            loadRecords(true);
        });
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.icon_transaction_ok), stringManager.getString(PhosString.status_successful).toString(), () -> {
            status = TransactionState.SUCCESSFUL;
            popupWindow.dismiss();
            loadRecords(true);
        });
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.icon_transaction_pending), stringManager.getString(PhosString.status_pending).toString(), () -> {
            status = TransactionState.PENDING;
            popupWindow.dismiss();
            loadRecords(true);
        });
        data.add(pmr);

        pmr = new MenuRow(RowType.REGULAR, getResources().getDrawable(R.drawable.icon_transaction_failed), stringManager.getString(PhosString.status_failed).toString(), () -> {
            status = TransactionState.FAILED;
            popupWindow.dismiss();
            loadRecords(true);
        });
        data.add(pmr);

        return data;
    }


    private void showListMenu(View targetView) {
        ListPopupWindow popupWindow = new ListPopupWindow(this);

        ArrayList<MenuRow> data;

        // default is "All"
        int selectedRow = 1;

        // which dropdown was pressed?
        if (targetView.getId() == R.id.btnTypeSelect) {
            data = getDataForType(popupWindow);


            if (type != null) {
                switch (type) {
                    case SALE:
                        selectedRow = 2;
                        break;
                    case REFUND:
                        selectedRow = 3;
                        break;
                    case VOID:
                        selectedRow = 4;
                        break;
                }
            }

        } else {
            data = getDataForStatus(popupWindow);

            if (status != null) {
                switch (status) {
                    case SUCCESSFUL:
                        selectedRow = 2;
                        break;
                    case PENDING:
                        selectedRow = 3;
                        break;
                    case FAILED:
                        selectedRow = 4;
                        break;
                }
            }

        }


        CustomListAdapter adapter = new CustomListAdapter(this, 0, data, selectedRow);

        popupWindow.setAnchorView(targetView);
        popupWindow.setAdapter(adapter);
        popupWindow.setWidth(Convert.dpToPixels(160, this));

        popupWindow.show();
    }

    @Click(R.id.btnDateSelect)
    public void selectDate() {
        Calendar cal = Calendar.getInstance();

        if (date != null) {
            cal.setTime(date);
        }

        final CalendarConstraints.DateValidator dateValidator = new CalendarConstraints.DateValidator() {
            @Override
            public boolean isValid(long date) {
                return date >= minDateCal.getTimeInMillis() && date <= Calendar.getInstance().getTimeInMillis();
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel parcel, int i) {

            }
        };

        final CalendarConstraints.Builder calendarConstraintsBuilder = new CalendarConstraints.Builder()
                .setStart(minDateCal.getTimeInMillis())
                .setEnd(Calendar.getInstance().getTimeInMillis())
                .setValidator(dateValidator);

        if (date != null) {
            calendarConstraintsBuilder.setOpenAt(date.getTime());
        }

        MaterialDatePicker.Builder<Long> materialDateBuilder = MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(calendarConstraintsBuilder.build());

        if (date != null) {
            materialDateBuilder.setSelection(date.getTime());
        }
        MaterialDatePicker<Long> picker = materialDateBuilder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            date = new Date(selection);
            loadRecords(true);
        });
        picker.show(getSupportFragmentManager(), "date-picker");

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        buttonContactless.getLocationInWindow(btnContactlessLoc);
        buttonEcommerce.getLocationInWindow(btnEcommerceLoc);
        buttonOpenBanking.getLocationInWindow(btnOpenBankingLoc);

        super.onWindowFocusChanged(hasFocus);
    }

    private void resetFilters() {
        date = null;
        type = null;
        status = null;
    }

    @Click(R.id.buttonContactless)
    public void buttonContactless() {
        resetFilters();

        init3dsSelectorButtons(0);
    }

    @Click(R.id.buttonEcommerce)
    public void buttonEcommerce() {
        resetFilters();

        init3dsSelectorButtons(1);
    }

    @Click(R.id.buttonOpenBanking)
    public void buttonOpenBanking() {
        resetFilters();

        init3dsSelectorButtons(2);
    }

    private void init3dsSelectorButtons(int newPosition) {
        if (newPosition != currentButtonPosition) {
            int animationDuration = SELECTOR_ANIMATION_DURATION;

            switch (newPosition) {
                case 0:
                    displayScaTransactions = false;
                    transactionsListType = TransactionsListType.CONTACTLESS;
                    selector.animate().setDuration(animationDuration).translationX(0);
                    break;
                case 1:
                    displayScaTransactions = true;
                    transactionsListType = TransactionsListType.ECOMMERCE;
                    selector.animate().setDuration(animationDuration).translationX(btnEcommerceLoc[0] - btnContactlessLoc[0]);
                    break;
                case 2:
                    displayScaTransactions = false;
                    transactionsListType = TransactionsListType.OPEN_BANKING;
                    selector.animate().setDuration(animationDuration).translationX(btnOpenBankingLoc[0] - btnContactlessLoc[0]);
                    break;
            }

            updateButtonTextColor(newPosition);

            currentButtonPosition = newPosition;
        }

        loadRecords(true);
    }

    private void updateButtonTextColor(int newPosition) {
        if (newPosition == currentButtonPosition)
            return;

        Button buttonFrom = null;
        Button buttonTo = null;

        int fromColor = clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR);
        int toColor = getResources().getColor(R.color.gray);

        switch (newPosition) {
            case 0:
                buttonTo = buttonContactless;
                break;
            case 1:
                buttonTo = buttonEcommerce;
                break;
            case 2:
                buttonTo = buttonOpenBanking;
                break;
        }

        switch (currentButtonPosition) {
            case 0:
                buttonFrom = buttonContactless;
                break;
            case 1:
                buttonFrom = buttonEcommerce;
                break;
            case 2:
                buttonFrom = buttonOpenBanking;
                break;
        }

        animateButtonTextColor(buttonFrom, fromColor, toColor, SELECTOR_ANIMATION_DURATION);
        animateButtonTextColor(buttonTo, toColor, fromColor, SELECTOR_ANIMATION_DURATION);
    }
}
