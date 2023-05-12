package digital.paynetics.phos.screens;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.BindingObject;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.DataBound;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import digital.paynetics.phos.PhosApplication;
import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.Printer;
import digital.paynetics.phos.classes.enums.BrandingColors;
import digital.paynetics.phos.classes.enums.RowType;
import digital.paynetics.phos.classes.helpers.BluetoothDeviceWrapper;
import digital.paynetics.phos.classes.helpers.BluetoothItem;
import digital.paynetics.phos.classes.helpers.BluetoothManager;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.classes.prefs.PrinterPreference;
import digital.paynetics.phos.common.BaseListAdapter;
import digital.paynetics.phos.databinding.ActivityPrintersBinding;
import digital.paynetics.phos.dialogs.manager.DialogState;
import digital.paynetics.phos.printer.PrinterManager;
import digital.paynetics.phos.printer.ReceiptGenerator;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
@DataBound
@EActivity(R.layout.activity_printers)
public class PrintersActivity extends BaseActivity
        implements BluetoothManager.BluetoothUpdateListener {

    @ViewById
    TextView title;

    @BindingObject
    ActivityPrintersBinding binding;

    @Inject
    BluetoothManager bluetoothManager;

    @Inject
    PrinterManager printerManager;

    @Inject
    PrinterPreference printerPreference;

    @AfterInject
    void onInjectDependencies() {
        PhosApplication.getAppComponent().inject(this);
    }

    @AfterViews
    public void afterViews() {

        title.setText(stringManager.getString(PhosString.printers));

        setResult(RESULT_CANCELED);
        binding.list.setAdapter(new PrinterListAdapter(this));
    }

    @Click(R.id.btnBack)
    public void close() {
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (bluetoothManager.isRequestEnableBluetoothSuccessful(requestCode, resultCode)) {
            startDeviceScanWithPermissionCheck();
        } else {
            close();
        }
    }

    private BluetoothItem createMenuRow(BluetoothDeviceWrapper deviceWrapper) {
        final Printer printer = Printer.fromBluetoothDevice(deviceWrapper.getDevice());
        return new BluetoothItem(RowType.REGULAR,
                getDrawable(printer.getIconRes()),
                printer.getNonNullName().equals("Unknown name") ? stringManager.getString(PhosString.unknown_name).toString() : printer.getNonNullName(),
                printer.getAddress(),
                () -> selectPrinter(printer),
                showAction(deviceWrapper),
                getActionIcon(deviceWrapper),
                clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR),
                getHint(deviceWrapper),
                deviceWrapper);
    }

    private void toMenuRowList(List<BluetoothDeviceWrapper> deviceList,
                               List<BluetoothItem> itemList,
                               Set<Integer> skipItemSeparatorSet) {
        boolean hasSelected = false;
        boolean hasBonded = false;
        boolean hasAvailable = false;
        BluetoothDeviceWrapper device;
        for (int i = 0; i < deviceList.size(); i++) {
            device = deviceList.get(i);
            // Add title menu row if needed
            if (!hasSelected && isSelectedDevice(device)) {
                hasSelected = true;
                itemList.add(createTitleMenuRow(stringManager.getString(PhosString.active).toString()));
                addSkipItemSeparatorIndex(itemList, skipItemSeparatorSet);
            } else if (!hasBonded && isBondedDevice(device)) {
                hasBonded = true;
                itemList.add(createTitleMenuRow(stringManager.getString(PhosString.paired).toString()));
                addSkipItemSeparatorIndex(itemList, skipItemSeparatorSet);
            } else if (!hasAvailable && !isSelectedDevice(device) && !isBondedDevice(device)) {
                hasAvailable = true;
                itemList.add(createTitleMenuRow(stringManager.getString(PhosString.available).toString()));
                addSkipItemSeparatorIndex(itemList, skipItemSeparatorSet);
            }
            // Add device
            itemList.add(createMenuRow(device));
        }
    }

    private void addSkipItemSeparatorIndex(List<BluetoothItem> itemList,
                                           Set<Integer> skipItemSeparatorSet) {
        int index = itemList.size() - 1;
        skipItemSeparatorSet.add(index);
        skipItemSeparatorSet.add(index - 1);
    }

    private BluetoothItem createTitleMenuRow(String title) {
        return new BluetoothItem(RowType.TITLE, title, false);
    }

    private int getActionIcon(BluetoothDeviceWrapper deviceWrapper) {
        if (isBondedDevice(deviceWrapper)) {
            return R.drawable.ic_check_black_24dp;
        } else if (!isSelectedDevice(deviceWrapper)) {
            return R.drawable.ic_add_black_24dp;
        }
        throw new IllegalStateException("Unexpected action");
    }

    private boolean showAction(BluetoothDeviceWrapper deviceWrapper) {
        // TODO: 5/17/2019 Is secondary action needed?
        return false;
//        if (isSelectedDevice(deviceWrapper)) {
//            return false;
//        } else if (isBondedDevice(deviceWrapper)) {
//            return true;
//        } else {
//            return true;
//        }
    }

    private String getHint(BluetoothDeviceWrapper deviceWrapper) {
        if (isSelectedDevice(deviceWrapper)) {
            return stringManager.getString(PhosString.test_print).toString();
        } else if (deviceWrapper.isBonded()) {
            return stringManager.getString(PhosString.select).toString();
        } else if (deviceWrapper.isBonding()) {
            return stringManager.getString(PhosString.pairing).toString();
        } else {
            return stringManager.getString(PhosString.pair).toString();
        }
    }

    private boolean isSelectedDevice(BluetoothDeviceWrapper deviceWrapper) {
        Printer printer = Printer.fromBluetoothDevice(deviceWrapper.getDevice());
        Printer selectedPrinter = Printer.fromString(printerPreference.get());
        return isBondedDevice(deviceWrapper)
                && selectedPrinter != null
                && selectedPrinter.getAddress().equals(printer.getAddress());
    }

    private boolean isBondedDevice(BluetoothDeviceWrapper deviceWrapper) {
        return deviceWrapper.isBonded();
    }

    private void selectPrinter(Printer printer) {
        setResult(RESULT_OK);
        printerPreference.set(printer.toString());
        setPrinterData(bluetoothManager.getAllDevices());
    }

    @Background(serial = "print")
    public void printTestReceipt() {
        Printer printer = Printer.fromString(printerPreference.get());
        if (printer == null) {
            return;
        }

        ReceiptGenerator receiptGenerator = new ReceiptGenerator(this);

        receiptGenerator.lineBreak();
        receiptGenerator.setTextAlignCenter();
        // TODO: 6/11/2021 Why bitmap from clientConfig?
//        receiptGenerator.printImage(clientConfig.getSkinBitmap(BrandingImages.BRANDING_LOGO_IMAGE));
        receiptGenerator.printImage(R.drawable.receipt_test_logo);

        receiptGenerator.setTextNormal();
        receiptGenerator.setTextAlignCenter();
        receiptGenerator.print("Test receipt");

        receiptGenerator.lineBreak(2);
        receiptGenerator.setTextNormal();
        receiptGenerator.setTextFontB();
        receiptGenerator.print("The best way to accept contactless payments with your mobile device!!!");
        receiptGenerator.lineBreak(5);

        printerManager.print(receiptGenerator.generate(), printer);
    }

    public void startDeviceScanWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            startDeviceScanWithPermissionCheckApi31();
        } else {
            startDeviceScanWithPermissionCheckApi30();
        }
    }

    public void stopDeviceScanWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            stopDeviceScanWithPermissionCheckApi31();
        } else {
            stopDeviceScanWithPermissionCheckApi30();
        }
    }

    @NeedsPermission({Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public void startDeviceScanWithPermissionCheckApi31() {
        startDeviceScan();
    }

    public void stopDeviceScanWithPermissionCheckApi31() {
        int scanPermissionState =
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        int connectPermissionState =
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT);
        if (scanPermissionState == PackageManager.PERMISSION_GRANTED
                && connectPermissionState == PackageManager.PERMISSION_GRANTED) {
            stopDeviceScan();
        }
    }

    @OnPermissionDenied({Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    void showDeniedApi31() {
        showDenied();
    }

    @OnNeverAskAgain({Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    void showNeverAskApi31() {
        showNeverAsk(PhosString.permission_printer_rationale_api31);
    }

    @OnShowRationale({Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    void showRationaleApi31(final PermissionRequest request) {
        showRationale(request, PhosString.permission_printer_rationale_api31);
    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    public void startDeviceScanWithPermissionCheckApi30() {
        startDeviceScan();
    }

    public void stopDeviceScanWithPermissionCheckApi30() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            stopDeviceScan();
        }
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_COARSE_LOCATION)
    void showDeniedApi30() {
        showDenied();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_COARSE_LOCATION)
    void showNeverAskApi30() {
        showNeverAsk(PhosString.permission_printer_rationale_api30);
    }

    @OnShowRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
    void showRationaleApi30(final PermissionRequest request) {
        showRationale(request, PhosString.permission_printer_rationale_api30);
    }

    private void startDeviceScan() {
        if (!bluetoothManager.isBluetoothAvailable()) {
            dialogManager.showDialog(new DialogState.UnableToSelectPrinterState(this, stringManager, () -> {
                close();
                return null;
            }), -1);
            return;
        }

        if (bluetoothManager.requestEnableBluetooth(this)) {
            // User's response will be available in onActivityResult()
            return;
        }

        bluetoothManager.startDeviceScan();
    }

    private void stopDeviceScan() {
        bluetoothManager.stopDeviceScan();
    }

    private void showDenied() {
        close();
    }

    private void showNeverAsk(PhosString phosString) {
        dialogManager.showDialog(new DialogState.PrinterPermissionState(this,
                stringManager,
                false, phosString, () -> {
            startActivityForResult(createOpenAppSettingsIntent(), 123);
            return null;
        }, () -> {
            close();
            return null;
        }), -1);
    }

    private void showRationale(final PermissionRequest request, PhosString phosString) {
        dialogManager.showDialog(new DialogState.PrinterPermissionState(this,
                stringManager,
                true, phosString, () -> {
            request.proceed();
            return null;
        }, () -> {
            request.cancel();
            return null;
        }), -1);
    }

    @OnActivityResult(123)
    void onResult(int resultCode) {
        startDeviceScanWithPermissionCheck();
    }

    // TODO: 5/17/2019 Code duplication with splash activity
    private Intent createOpenAppSettingsIntent() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        return intent;
    }



    void setLoadingVisible(boolean loadingVisible) {
        binding.loadingIndicator.setVisibility(loadingVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothManager.setBluetoothUpdateListener(this);
        startDeviceScanWithPermissionCheck();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopDeviceScanWithPermissionCheck();
        bluetoothManager.setBluetoothUpdateListener(null);
    }

    @Override
    public void onBluetoothUpdate(List<BluetoothDeviceWrapper> deviceList, boolean discovering) {
        setLoadingVisible(discovering);
        setPrinterData(deviceList);
    }

    void setPrinterData(List<BluetoothDeviceWrapper> deviceList) {
        fixDeviceListOrder(deviceList);

        List<BluetoothItem> itemList = new ArrayList<>();
        Set<Integer> itemSeparatorSkipSet = new HashSet<>();
        toMenuRowList(deviceList, itemList, itemSeparatorSkipSet);

        ((PrinterListAdapter) binding.list.getAdapter()).update(itemList, itemSeparatorSkipSet);
    }

    void fixDeviceListOrder(List<BluetoothDeviceWrapper> deviceList) {
        // Bonded and selected device should be first
        int selectedDeviceIndex = -1;
        for (int i = 0; i < deviceList.size(); i++) {
            if (isSelectedDevice(deviceList.get(i))) {
                selectedDeviceIndex = i;
                break;
            }
        }

        if (selectedDeviceIndex > 0) {
            BluetoothDeviceWrapper device = deviceList.get(selectedDeviceIndex);
            deviceList.remove(selectedDeviceIndex);
            deviceList.add(0, device);
        }
    }

    void onItemClick(BluetoothItem bluetoothItem) {
        BluetoothDeviceWrapper deviceWrapper = bluetoothItem.getDeviceWrapper();
        if (isSelectedDevice(deviceWrapper)) {
            alertTestPrint();
        } else if (isBondedDevice(deviceWrapper)) {
            final Printer printer = Printer.fromBluetoothDevice(deviceWrapper.getDevice());
            selectPrinter(printer);
        } else {
            bluetoothManager.bindDevice(deviceWrapper.getDevice().getAddress());
        }
    }

    void alertTestPrint() {
        dialogManager.showDialog(new DialogState.TestPrintState(this,
                stringManager,
                () -> {
                    printTestReceipt();
                    return null;
                }, () -> {
            return null;
        }), -1);
    }

    static class PrinterListAdapter extends BaseListAdapter<BluetoothItem> {

        private final PrintersActivity activity;

        PrinterListAdapter(PrintersActivity activity) {
            super();
            this.activity = activity;
        }

        PrinterListAdapter(PrintersActivity activity, List<BluetoothItem> items) {
            super(items);
            this.activity = activity;
        }

        @Override
        protected int getLayoutResourceId(BluetoothItem item) {
            return item.getType() == RowType.REGULAR ? R.layout.row_printer_item
                    : R.layout.row_printer_title;
        }

        @Override
        public void onItemClick(BluetoothItem item) {
            activity.onItemClick(item);
        }
    }
}
