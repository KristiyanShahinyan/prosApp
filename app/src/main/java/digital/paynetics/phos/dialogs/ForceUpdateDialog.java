package digital.paynetics.phos.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.classes.lang.PhosStringProvider;
import digital.paynetics.phos.screens.BaseActivity;

public class ForceUpdateDialog extends DialogFragment {

    private SimpleDateFormat FORCE_UPDATE_DATE_FORMATTER = new SimpleDateFormat("dd.MM.yyyy");
    private TextView titleTv;
    private TextView descriptionTv;
    private int mode;
    private long untilDate;

    private TextView btnUpdate, btnSkip, btnUpdateSkip, btnUpdateNow, btnContinue;
    private String descriptionValueFromBackend;

    /**
     * @param mode 1 = mandatory mode, 2 optional no date shown, 3 optional date shown
     * @return
     */
    public static ForceUpdateDialog newInstance(int mode) {

        return newInstance(mode, 0);
    }

    /**
     * @param mode      1 = mandatory mode, 2 optional no date shown, 3 optional date shown
     * @param untilDate until date to be shown
     * @return
     */
    public static ForceUpdateDialog newInstance(int mode, long untilDate) {

        return newInstance(mode, untilDate, null);
    }

    /**
     * @param mode      1 = mandatory mode, 2 optional no date shown, 3 optional date shown
     * @param untilDate until date to be shown
     * @param desc description to be shown from backend
     * @return
     */
    public static ForceUpdateDialog newInstance(int mode, long untilDate, String desc) {

        ForceUpdateDialog forceUpdateDialog = new ForceUpdateDialog();

        Bundle args = new Bundle();
        args.putInt("mode", mode);
        args.putLong("date", untilDate);
        args.putString("backend_desc", desc);
        forceUpdateDialog.setArguments(args);
        return forceUpdateDialog;
    }

    PhosStringProvider stringManager;
    UpdateCallback updateCallback;


    public ForceUpdateDialog() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        return inflater.inflate(R.layout.dialog_force_update, container);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setCancelable(false);

        stringManager = ((BaseActivity) getActivity()).getStringManager();
        updateCallback = ((UpdateCallback)getActivity());

        mode = getArguments().getInt("mode", 0);
        untilDate = getArguments().getLong("date", 0);

        descriptionValueFromBackend = getArguments().getString("backend_desc");

        validateDialog();

        titleTv = getView().findViewById(R.id.dialog_force_update_title);
        descriptionTv = getView().findViewById(R.id.dialog_force_update_description);

        btnUpdate = getView().findViewById(R.id.btn_update_mandatory);
        btnSkip = getView().findViewById(R.id.btn_skip);
        btnUpdateSkip = getView().findViewById(R.id.btn_update_skip);
        btnUpdateNow = getView().findViewById(R.id.btn_update_now);
        btnContinue = getView().findViewById(R.id.btn_continue);


        setClickListeners();

        setLocalizations();

        showBtnContainer();
    }

    private void showBtnContainer() {
        View containerMandatory = getView().findViewById(R.id.dialog_force_update_btn_container_mandatory);
        View containerSkip = getView().findViewById(R.id.dialog_force_update_btn_container_skip);
        View containerUntilDate = getView().findViewById(R.id.dialog_force_update_btn_container_until_date);

        containerMandatory.setVisibility(View.GONE);
        containerSkip.setVisibility(View.GONE);
        containerUntilDate.setVisibility(View.GONE);

        if (mode == 1) {
            containerMandatory.setVisibility(View.VISIBLE);
        } else if (mode == 2) {
            containerSkip.setVisibility(View.VISIBLE);
        } else if (mode == 3) {
            containerUntilDate.setVisibility(View.VISIBLE);
        }
    }

    private void setClickListeners() {
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGooglePlay();
                updateCallback.onUpdateResult(false);
            }
        });

        btnUpdateSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGooglePlay();
                updateCallback.onUpdateResult(false);
            }
        });

        btnUpdateNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGooglePlay();
                updateCallback.onUpdateResult(false);
            }
        });

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                continueWithoutUpdate();
                updateCallback.onUpdateResult(true);
            }
        });

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                continueWithoutUpdate();
                updateCallback.onUpdateResult(true);
            }
        });

    }

    private void validateDialog() {
        if (mode == 0) {
            throw new RuntimeException("Please init the Dialog with correct mode. Possible values:1,2,3");
        }

        if (mode == 3 && untilDate == 0) {
            throw new RuntimeException("Please init the Dialog with correct until date");
        }

    }

    private void setLocalizations() {

        titleTv.setText(stringManager.getString(PhosString.force_update_dialog_title));

        if (mode == 3) {

            Date untilDateAsDate = new Date(untilDate);

            String descriptionMessage = stringManager.getString(PhosString.force_update_dialog_desc_with_date).toString();

            descriptionTv.setText(String.format(descriptionMessage, FORCE_UPDATE_DATE_FORMATTER.format(untilDateAsDate)));
        } else {
            String descriptionMessage = stringManager.getString(PhosString.force_update_dialog_desc_no_date).toString();

            descriptionTv.setText(descriptionMessage);
        }

        if (!TextUtils.isEmpty(descriptionValueFromBackend)) {
            descriptionTv.setText(descriptionValueFromBackend);
        }

        btnUpdate.setText(stringManager.getString(PhosString.force_update_dialog_update));
        btnUpdateSkip.setText(stringManager.getString(PhosString.force_update_dialog_update));
        btnUpdateNow.setText(stringManager.getString(PhosString.force_update_dialog_update_now));
        btnSkip.setText(stringManager.getString(PhosString.skip));
        btnContinue.setText(stringManager.getString(PhosString.continue_label));
    }

    private void navigateToGooglePlay() {
        final String appPackageName = getActivity().getPackageName(); // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    private void continueWithoutUpdate() {
        dismiss();
    }

    public interface UpdateCallback {
        void onUpdateResult(boolean updatedSkipped);
    }
}
