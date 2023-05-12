package digital.paynetics.phos.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.RowClickListener;
import digital.paynetics.phos.classes.helpers.Convert;
import digital.paynetics.phos.classes.lang.PhosString;
import digital.paynetics.phos.classes.lang.PhosStringProvider;
import digital.paynetics.phos.dialogs.manager.DialogManager;
import digital.paynetics.phos.dialogs.manager.DialogState;
import digital.paynetics.phos.presentation.view.TransactionFlowActivity;
import digital.paynetics.phos.screens.BaseActivity;
import digital.paynetics.phos.presentation.view.TransactionResultContainerActivity;
import digital.paynetics.phos.sdk.entities.Keys;
import digital.paynetics.phos.sdk.entities.Transaction;
import digital.paynetics.phos.sdk.enums.TransactionState;
import digital.paynetics.phos.sdk.enums.TransactionType;
import digital.paynetics.phos.sdk.BuildConfig;

import static digital.paynetics.phos.screens.BaseActivity.RESULT_CODE_RELOAD;


public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ItemViewHolder> {

    private static ArrayList<Transaction> dataList;
    private Context context;

    private Drawable placeholder;
    private Drawable transactionOk;
    private Drawable transactionFailed;
    private Drawable transactionPending;

    private RowClickListener clickListener;

    private int colorPayment;
    private int colorRefund;
    private int colorVoid;

    private boolean useShortFormat;

    private PhosStringProvider stringProvider;

    private DateFormat dateFormatTime;
    private DateFormat dateFormatLong;
    private int fractionDigit;
    private DialogManager dialogManager;


    public TransactionsAdapter(Context ctx, ArrayList<Transaction> data, boolean useShortFormat, int fractionDigit, DialogManager dialogManager) {
        context = ctx;
        dataList = data;
        this.fractionDigit = fractionDigit;
        this.dialogManager = dialogManager;

        this.useShortFormat = useShortFormat;
        stringProvider = ((BaseActivity) context).getStringManager();
        dateFormatTime = new SimpleDateFormat("HH:mm", stringProvider.getCurrentLocale());
        dateFormatLong = new SimpleDateFormat("dd MMM yyyy HH:mm", stringProvider.getCurrentLocale());
        // Display the transaction times as they are received from the backend
        dateFormatTime.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatLong.setTimeZone(TimeZone.getTimeZone("UTC"));

        placeholder = ContextCompat.getDrawable(context, R.drawable.ic_transactions_24dp);
        transactionOk = ContextCompat.getDrawable(context, R.drawable.icon_transaction_ok);
        transactionFailed = ContextCompat.getDrawable(context, R.drawable.icon_transaction_failed);
        transactionPending = ContextCompat.getDrawable(context, R.drawable.icon_transaction_pending);

        colorPayment = context.getResources().getColor(R.color.row_payment);
        colorRefund = context.getResources().getColor(R.color.row_refund);
        colorVoid = context.getResources().getColor(R.color.row_void);
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            DialogInterface.OnClickListener {
        private ImageView icon;
        private TextView title;
        private TextView date;
        private TextView amount;
        private View parent;

        private Transaction transaction;

        public ItemViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.txtTitle);
            date = itemView.findViewById(R.id.txtSubtitle);
            amount = itemView.findViewById(R.id.txtAmount);
            parent = itemView;
        }

        @Override
        public void onClick(View v) {
            if (transaction.getState() == TransactionState.SUCCESSFUL && areOptionsNeeded()) {
                showOptions();
            } else {
                showDetails();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            PhosString[] options = createExpectedOptions();
            if (options[which] == PhosString.details) {
                showDetails();
            } else if (options[which] == PhosString.type_refund) {
                doVoidRefund(TransactionType.REFUND, transaction.getRefundableAmount());
            } else if (options[which] == PhosString.type_void) {
                doVoidRefund(TransactionType.VOID, transaction.getAmount());
            }
        }

        boolean areOptionsNeeded() {
            return transaction.isRefundable() || transaction.isVoidable();
        }

        boolean isVoidOptionExpected() {
            return parent.getResources().getBoolean(R.bool.void_enabled)
                    && transaction.isVoidable();
        }

        boolean isRefundOptionExpected() {
            return parent.getResources().getBoolean(R.bool.refund_enabled)
                    && transaction.isRefundable();
        }

        PhosString[] createExpectedOptions() {
            if (isRefundOptionExpected() && isVoidOptionExpected()) {
                return new PhosString[]{PhosString.details, PhosString.type_refund, PhosString.type_void};
            } else if (isRefundOptionExpected()) {
                return new PhosString[]{PhosString.details, PhosString.type_refund};
            } else if (isVoidOptionExpected()) {
                return new PhosString[]{PhosString.details, PhosString.type_void};
            } else {
                return new PhosString[]{PhosString.details};
            }
        }

        CharSequence[] createOptions() {
            PhosString[] options = createExpectedOptions();
            CharSequence[] labels = new CharSequence[options.length];
            for (int i=0; i< options.length; i++) {
                labels[i] = stringProvider.getString(options[i]);
            }
            return labels;
        }

        void showOptions() {
            CharSequence[] options = createOptions();
            dialogManager.showDialog(new DialogState.OperationsState(context, stringProvider, options, this), -1);
        }

        void showDetails() {
            TransactionResultContainerActivity.Companion.launchForTransaction((Activity) itemView.getContext(), transaction);
        }

        void doVoidRefund(TransactionType type, double amount) {
            Intent intent = new Intent(itemView.getContext(), TransactionFlowActivity.class);
            intent.  putExtra(Keys.TRANSACTION_TYPE, type)
                    .putExtra(Keys.TRANSACTION_KEY, transaction.getTransactionKey())
                    .putExtra(Keys.TRANSACTION_DATE, getTransactionDate(transaction, false))
                    .putExtra(Keys.TRANSACTION_CARD, (TextUtils.isEmpty(transaction.getCard()) ? transaction.getAccountNumber() : transaction.getCardLabel()))
                    .putExtra(Keys.IS_ORIGINAL_TRANSACTION_3DS, transaction.isTransaction3ds())
                    .putExtra(Keys.AMOUNT_KEY, amount);

            if(BuildConfig.SDK_OPEN_BANKING) {
                if(transaction.getType() == TransactionType.NUAPAY_SALE || transaction.getType() == TransactionType.NUAPAY_REFUND) {
                    intent.putExtra(Keys.IS_OPEN_BANKING, true);
                }
            }

            if (context instanceof Activity){
                ((Activity) context).startActivityForResult(intent, RESULT_CODE_RELOAD);
            } else {
                context.startActivity(intent);
            }
        }
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_transaction, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

        Transaction transaction = dataList.get(position);

        switch (transaction.getState()) {
            case SUCCESSFUL:
                holder.icon.setImageDrawable(transactionOk);
                break;
            case FAILED:
                holder.icon.setImageDrawable(transactionFailed);
                break;
            case PENDING:
                holder.icon.setImageDrawable(transactionPending);
                break;
            default:
                holder.icon.setImageDrawable(null);
                break;

        }

        switch ((transaction.getType())) {
            case VOID:
                holder.parent.setBackgroundColor(colorVoid);
                break;
            case REFUND:
                holder.parent.setBackgroundColor(colorRefund);
                break;
            default:
                holder.parent.setBackgroundColor(getColor(transaction));

        }

        holder.title.setText((TextUtils.isEmpty(transaction.getCard()) ? transaction.getAccountNumber() : transaction.getCard()));
        holder.date.setText(getTransactionDate(transaction, useShortFormat));
        holder.amount.setText(Convert.formatCurrencyWithFraction(transaction.getAmount(), transaction.getCurrency(), fractionDigit));
        holder.transaction = transaction;
    }

    private int getColor(Transaction transaction) {
        if(BuildConfig.SDK_OPEN_BANKING) {
            if(transaction.getType() == TransactionType.NUAPAY_REFUND) {
                return colorRefund;
            }
        }
        return colorPayment;
    }

    // Helper method for changing the color of a vector drawable
    private void tint(ImageView view, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(color));
    }

    private String getTransactionDate(Transaction transaction, boolean useShortFormat) {
        if (transaction.getDate() != null) {
            return useShortFormat ? dateFormatTime.format(transaction.getDate())
                    : dateFormatLong.format(transaction.getDate());
        }
        return null;
    }

    public void setUseShortFormat(boolean useShortFormat) {
        this.useShortFormat = useShortFormat;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

}
