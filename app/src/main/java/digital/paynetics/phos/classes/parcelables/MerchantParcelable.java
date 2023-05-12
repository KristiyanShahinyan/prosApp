package digital.paynetics.phos.classes.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import digital.paynetics.phos.sdk.entities.Merchant;

// Save/restore some merchant props to prevent crashes when app is restored by Android.
public class MerchantParcelable implements Parcelable {

    public static final String KEY = "merchant";

    private final Merchant merchant;

    public MerchantParcelable(Merchant merchant) {
        if (merchant == null) {
            throw new IllegalArgumentException("Provide non null merchant");
        }
        this.merchant = merchant;
    }

    private MerchantParcelable(Parcel in) {
        Merchant merchant = new Merchant();
        merchant.setName(in.readString());
        merchant.setLogoUrl(in.readString());
        this.merchant = merchant;
    }

    @NonNull
    public Merchant getMerchant() {
        return merchant;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Write only properties that are needed
        dest.writeString(merchant.getName());
        dest.writeString(merchant.getLogoUrl());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MerchantParcelable> CREATOR = new Creator<MerchantParcelable>() {
        @Override
        public MerchantParcelable createFromParcel(Parcel in) {
            return new MerchantParcelable(in);
        }

        @Override
        public MerchantParcelable[] newArray(int size) {
            return new MerchantParcelable[size];
        }
    };
}
