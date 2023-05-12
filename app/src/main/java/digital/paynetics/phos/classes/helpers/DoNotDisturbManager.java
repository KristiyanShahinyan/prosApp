package digital.paynetics.phos.classes.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;

import javax.inject.Inject;

import digital.paynetics.phos.dagger.PhosScope;
import digital.paynetics.phos.classes.prefs.DoNotDisturbPreference;
import digital.paynetics.phos.security.PhosLifecycleManager;

@PhosScope
public class DoNotDisturbManager {

    private static final String TAG = "DNDManager";

    private static final String ENABLED = "enabled";

    private final PhosLifecycleManager lifecycleManager;
    private final NotificationManager notificationManager;
    private final DoNotDisturbPreference dndPreference;

    @Inject
    DoNotDisturbManager(NotifierWrapper notifier,
                        PhosLifecycleManager lifecycleManager,
                        NotificationManager notificationManager,
                        DoNotDisturbPreference dndPreference) {
        this.lifecycleManager = lifecycleManager;
        this.notificationManager = notificationManager;
        this.dndPreference = dndPreference;
        notifier.registerForEvents(lifecycleManager.getEvents(), createEventReceiver());
    }

    private BroadcastReceiver createEventReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (lifecycleManager.isInForegroundEvent(intent)) {
                    turnOn();
                }
                if (lifecycleManager.isInBackgroundEvent(intent)) {
                    turnOff();
                }
            }
        };
    }

    private void turnOn() {
        if (isSupported() && isAccessGrantedAndEnabled()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        }
    }

    private void turnOff() {
        if (isSupported() && isAccessGrantedAndEnabled()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private void tryEnable(Activity context) {
        if (isAccessGranted()) {
            enable();
        } else {
            createAndRegisterAccessGrantedReceiver(context);
            openSettings(context);
        }
    }

    private void createAndRegisterAccessGrantedReceiver(Activity activity) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                if (isAccessGranted()) {
                    enable();
                }
            }
        };
        IntentFilter filter = new IntentFilter(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED);
        activity.registerReceiver(receiver, filter);
    }

    private void openSettings(Activity context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    private void enable() {
        dndPreference.set(ENABLED);
        turnOn();
    }

    private void disable() {
        turnOff();
        dndPreference.set("disabled");
    }

    public void toggle(Activity context) {
        if (isEnabled()) {
            disable();
        } else {
            tryEnable(context);
        }
    }

    public boolean isAccessGrantedAndEnabled() {
        return isAccessGranted() && isEnabled();
    }

    private boolean isEnabled() {
        String value = dndPreference.get();
        return value != null && value.equals(ENABLED);
    }

    private boolean isAccessGranted() {
        return notificationManager.isNotificationPolicyAccessGranted();
    }

    public void resetOnNoAccessGranted() {
        if (isAccessGranted()) {
            return;
        }
        disable();
    }
}
