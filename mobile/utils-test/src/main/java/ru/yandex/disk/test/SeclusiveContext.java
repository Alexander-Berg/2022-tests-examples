package ru.yandex.disk.test;

import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.textservice.TextServicesManager;
import ru.yandex.disk.util.Exceptions;
import ru.yandex.disk.utils.IntentMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SeclusiveContext extends ContextWrapper {

    private final LinkedList<ContextActionRequest> startedServices;
    private final LinkedList<StartActivityRequest> startedActivities;
    private final LinkedList<ContextActionRequest> sendBroadcastRequests;
    private final Map<String, Object> mockSystemServices;
    private final LinkedList<RegisterBroadcastReceiverRequest> registeredReceivers;
    private Resources resources;
    private PackageManager packageManager;

    public SeclusiveContext(Context baseContext) {
        super(baseContext);
        mockSystemServices = new HashMap<String, Object>();
        startedServices = new LinkedList<ContextActionRequest>();
        startedActivities = new LinkedList<StartActivityRequest>();
        registeredReceivers = new LinkedList<RegisterBroadcastReceiverRequest>();
        sendBroadcastRequests = new LinkedList<>();
    }

    private Application newApplication(Class<? extends Application> applicationClass) {
        try {
            return newInstance(applicationClass);
        } catch (Exception e) {
             return Exceptions.crashValue(e);
        }
    }

    private Application newInstance(Class<? extends Application> applicationClass)
            throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        return applicationClass.getConstructor(Context.class).newInstance(this);
    }

    private void injectLoadedApk(Application application)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {
        Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
        Constructor<?> loadedApkConstructor = loadedApkClass.getConstructors()[1];
        Object loadedApk = loadedApkConstructor.newInstance(null, "test", this, null, null);

        Field field = Application.class.getField("mLoadedApk");
        field.set(application, loadedApk);
    }

    @Override
    public ComponentName startService(Intent service) {
        startedServices.add(new ContextActionRequest(service));
        return new ComponentName("dummy", "dummy");
    }

    @Override
    public void startActivity(Intent intent) {
        startedActivities.add(new StartActivityRequest(intent));
    }

    public List<Intent> getStartedServices() {
        List<Intent> list = new ArrayList<>();
        for (ContextActionRequest request : startedServices) {
            list.add(request.getIntent());
        }
        return list;
    }

    public Intent getLastStartedService() {
        ContextActionRequest request = getLastStartedServiceRequest();
        return request != null ? request.getIntent() : null;
    }

    @Override
    public Object getSystemService(String name) {
        Object service = mockSystemServices.get(name);
        if (service != null) {
            return service;
        } else {
            return null;
        }
    }

    public void setPowerManager(PowerManager powerManager) {
        mockSystemServices.put(POWER_SERVICE, powerManager);
    }

    public void setLayoutInflater(LayoutInflater inflater) {
        mockSystemServices.put(LAYOUT_INFLATER_SERVICE, inflater);
    }

    public void setAccountManager(AccountManager accountManager) {
        mockSystemServices.put(ACCOUNT_SERVICE, accountManager);
    }

    public void setNotificationManager(NotificationManager notificationManager) {
        mockSystemServices.put(NOTIFICATION_SERVICE, notificationManager);
    }

    public void setActivityManager(ActivityManager activityManager) {
        mockSystemServices.put(ACTIVITY_SERVICE, activityManager);
    }

    public void setWindowManager(WindowManager windowManager) {
        mockSystemServices.put(WINDOW_SERVICE, windowManager);
    }

    public void setWifiManager(WifiManager wifiManager) {
        mockSystemServices.put(WIFI_SERVICE, wifiManager);
    }

    public void setAudioManager(AudioManager audioManager) {
        mockSystemServices.put(AUDIO_SERVICE, audioManager);
    }

    public void setConnectivityManager(ConnectivityManager connectivityManager) {
        mockSystemServices.put(Context.CONNECTIVITY_SERVICE, connectivityManager);
    }

    public void setTextServicesManager(TextServicesManager textServicesManager) {
        mockSystemServices.put(Context.TEXT_SERVICES_MANAGER_SERVICE, textServicesManager);
    }

    public void setKeyguardManager(KeyguardManager keyguardManager) {
        mockSystemServices.put(Context.KEYGUARD_SERVICE, keyguardManager);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        registeredReceivers.add(new RegisterBroadcastReceiverRequest(receiver, filter));
        return null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        registeredReceivers.remove(new RegisterBroadcastReceiverRequest(receiver, null));
    }

    public LinkedList<RegisterBroadcastReceiverRequest> getRegisteredReceivers() {
        return registeredReceivers;
    }

    @Override
    public void sendBroadcast(Intent intent) {
        ArrayList<RegisterBroadcastReceiverRequest> receivers = new ArrayList<>(registeredReceivers);
        for (RegisterBroadcastReceiverRequest request : receivers) {
            if (IntentMatcher.match(request.filter, intent)) {
                request.receiver.onReceive(this, intent);
            }
        }

        sendBroadcastRequests.add(new ContextActionRequest(intent));

        super.sendBroadcast(intent);
    }

    public LinkedList<ContextActionRequest> getSendBroadcastRequests() {
        return sendBroadcastRequests;
    }

    public ContextActionRequest getLastStartedServiceRequest() {
        return startedServices.size() > 0 ? startedServices.getLast() : null;
    }

    public List<Intent> getAndClearBroadcastIntents() {
        List<Intent> intents = new ArrayList<>();
        for (ContextActionRequest request : sendBroadcastRequests) {
            intents.add(request.getIntent());
        }
        sendBroadcastRequests.clear();
        return intents;
    }

    public static class RegisterBroadcastReceiverRequest {
        public final BroadcastReceiver receiver;
        public final IntentFilter filter;

        public RegisterBroadcastReceiverRequest(BroadcastReceiver receiver, IntentFilter filter) {
            this.receiver = receiver;
            this.filter = filter;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(receiver).append(" :\n\t");
            for (int i = 0, count = filter.countActions(); i < count; i++) {
                s.append(filter.getAction(0));
                if (i < count - 1) {
                    s.append("\n\t");
                }
            }
            return s.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RegisterBroadcastReceiverRequest)) {
                return false;
            }
            RegisterBroadcastReceiverRequest another = (RegisterBroadcastReceiverRequest) o;
            return receiver.equals(another.receiver);
        }

    }

    public void enableLayoutInflater() {
        LayoutInflater inflater = LayoutInflater.from(getBaseContext());
        setLayoutInflater(inflater);
    }

    public Intent findStartedService(String action) {
        for (ContextActionRequest request : startedServices) {
            if (action.equals(request.getIntent().getAction())) {
                return request.getIntent();
            }
        }
        return null;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    @Override
    public Resources getResources() {
        if (resources != null) {
            return resources;
        } else {
            return super.getResources();
        }
    }

    public void setPackageManager(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    @Override
    public PackageManager getPackageManager() {
        if (packageManager != null) {
            return packageManager;
        } else {
            return super.getPackageManager();
        }
    }

    public void useRealPackageManager() {
        packageManager = null;
    }

    public void startActivity(Intent intent, int requestCode) {
        startedActivities.add(new StartActivityRequest(intent, requestCode));
    }

    public Intent getStartedActivity() {
        StartActivityRequest startActivityRequest = getStartActivityRequest();

        if (!startActivityRequest.isResultRequest()) {
            return startActivityRequest.getIntent();
        } else {
            throw new IllegalStateException("activity started for result, "
                    + "use getStartActivityRequest()");
        }

    }

    public StartActivityRequest getStartActivityRequest() throws AssertionError {
        if (startedActivities.size() == 1) {
            return startedActivities.get(0);
        } else if (startedActivities.size() == 0) {
            throw new AssertionError("no activity started");
        } else {
            throw new AssertionError("more then one activity started:" + startedActivities);
        }
    }

    public boolean isNoStartedActivity() {
        return startedActivities.isEmpty();
    }

    public void clearStartActivityRequests() {
        startedActivities.clear();
    }

    public void clearStartServiceRequests() {
        startedServices.clear();
    }

    public static class StartActivityRequest {

        private final Intent intent;
        private final int requestCode;

        public StartActivityRequest(Intent intent, int requestCode) {
            this.intent = intent;
            this.requestCode = requestCode;
        }

        public boolean isResultRequest() {
            return requestCode != -1;
        }

        public StartActivityRequest(Intent intent) {
            this(intent, -1);
        }

        public Intent getIntent() {
            return intent;
        }

        public int getRequestCode() {
            return requestCode;
        }

    }

    public void shutdown() {
        //getContentResolver().shutdown();
    }

    @Override
    public int checkSelfPermission(final String permission) {
        return PackageManager.PERMISSION_GRANTED;
    }

    // No @Override annotation here since this method marked as @hide
    // But we can define method with similar signature and this
    // implementation will be called during test case
    public int checkPermission(final String permission, final int pid, final int uid) {
        return PackageManager.PERMISSION_GRANTED;
    }
}
