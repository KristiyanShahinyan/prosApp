package digital.paynetics.phos.classes.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import digital.paynetics.phos.sdk.entities.Terminal;

// Save/restore some terminal props to prevent crashes when app is restored by Android.
// Sensitive props are excluded on purpose.
public class TerminalParcelable implements Parcelable {

    public static final String KEY = "terminal";

    private final Terminal terminal;

    public TerminalParcelable(Terminal terminal) {
        if (terminal == null) {
            throw new IllegalArgumentException("Provide non null terminal");
        }
        this.terminal = terminal;
    }

    private TerminalParcelable(Parcel in) {
        Terminal terminal = new Terminal();
        terminal.setTip(in.readInt());
        terminal.setCurrency(in.readString());
        this.terminal = terminal;
    }

    @NonNull
    public Terminal getTerminal() {
        return terminal;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Write only properties that are needed and aren't sensitive data.
        // Kernel config and certificates are considered sensitive data.
        dest.writeInt(terminal.getTip());
        dest.writeString(terminal.getCurrency());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TerminalParcelable> CREATOR = new Creator<TerminalParcelable>() {
        @Override
        public TerminalParcelable createFromParcel(Parcel in) {
            return new TerminalParcelable(in);
        }

        @Override
        public TerminalParcelable[] newArray(int size) {
            return new TerminalParcelable[size];
        }
    };
}
