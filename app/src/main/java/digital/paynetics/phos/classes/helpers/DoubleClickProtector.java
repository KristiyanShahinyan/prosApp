package digital.paynetics.phos.classes.helpers;

import android.util.SparseLongArray;

import org.androidannotations.api.builder.ActivityIntentBuilder;

import javax.inject.Inject;

public class DoubleClickProtector {

    public interface Callback {
        void onClick();
    }

    private static final long DOUBLE_TAP_DELTA_MILLIS = 1000;
    private static final int DEFAULT_ID = 0;

    private final SparseLongArray clickTimes = new SparseLongArray();

    @Inject
    DoubleClickProtector() {
    }

    private long getLastClickTime(int buttonId) {
        return clickTimes.get(buttonId, 0);
    }

    private void setLastClickTime(int buttonId, long time) {
        clickTimes.put(buttonId, time);
    }

    private boolean isValidTap(int buttonId) {
        long tapTime = System.currentTimeMillis();
        long deltaTime = tapTime - getLastClickTime(buttonId);
        setLastClickTime(buttonId, tapTime);
        return deltaTime > DOUBLE_TAP_DELTA_MILLIS;
    }

    public void click(ActivityIntentBuilder builder) {
        click(DEFAULT_ID, builder);
    }

    public void click(int buttonId, ActivityIntentBuilder builder) {
        if (isValidTap(buttonId)) {
            builder.start();
        }
    }

    public void click(Callback callback) {
        click(DEFAULT_ID, callback);
    }

    public void click(int buttonId, Callback callback) {
        if (isValidTap(buttonId)) {
            callback.onClick();
        }
    }
}
