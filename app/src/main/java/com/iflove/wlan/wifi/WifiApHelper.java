package com.iflove.wlan.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wi-Fi连接工具
 * Created by rentianlong on 2018/5/29
 */
public class WifiApHelper {
    private static final String TAG = "WifiApHelper";
    // Combo scans can take 5-6s to complete - set to 10s.
    private static final int WIFI_RESCAN_INTERVAL_MS = 10 * 1000;

    private final Context mContext;
    private final Switch mSwitch;

    private final WifiManager mWifiManager;
    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;

    private NetworkInfo.DetailedState mLastState;
    /**
     * 返回有关当前Wi-Fi连接的动态信息（如果有的话）
     */
    private WifiInfo mLastInfo;

    private final List<AccessPoint> mAccessPoints = new ArrayList<>();

    private final AtomicBoolean mConnected = new AtomicBoolean(false);
    private final WifiStateListener mWifiStateListener;
    private boolean mP2pSupported;

    private final CompoundButton.OnCheckedChangeListener mSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked) {
                openWifi();
            } else {
                closeWifi();
            }
        }
    };


    public WifiApHelper(@NonNull final Context context, @NonNull final Switch wifiSwith,
                        @NonNull final WifiStateListener wifiStateListener) {
        this.mContext = context.getApplicationContext();
        this.mSwitch = wifiSwith;
        this.mWifiStateListener = wifiStateListener;
        mP2pSupported = this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);

        //注册WiFi-网络相关广播
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiSystemApi.CONFIGURED_NETWORKS_CHANGED_ACTION);
        mFilter.addAction(WifiSystemApi.LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };

        mScanner = new Scanner();
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mFilter);
        enableWifiSwitch();
        updateAccessPoints();
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mScanner.pause();
    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "action：" + action);
        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            enableWifiSwitch();
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) ||
                WifiSystemApi.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action) ||
                WifiSystemApi.LINK_CONFIGURATION_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            //Ignore supplicant state changes when network is connected
            //TODO: we should deprecate SUPPLICANT_STATE_CHANGED_ACTION and
            //introduce a broadcast that combines the supplicant and network
            //network state change events so the apps dont have to worry about
            //ignoring supplicant state change when network is connected
            //to get more fine grained information.
            SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (!mConnected.get() && WifiSystemApi.isHandshakeState(state)) {
                updateConnectionState(WifiInfo.getDetailedStateOf(state));
            } else {
                // During a connect, we may have the supplicant
                // state change affect the detailed network state.
                // Make sure a lost connection is updated as well.
                updateConnectionState(null);
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            mConnected.set(info.isConnected());
            updateAccessPoints();
            updateConnectionState(info.getDetailedState());
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);
        }
    }

    /**
     * Shows the latest access points available with supplimental information like
     * the strength of network and the security for it.
     */
    private void updateAccessPoints() {
        // Safeguard from some delayed event handling

        final int wifiState = mWifiManager.getWifiState();
        if (mWifiStateListener != null) {
            mWifiStateListener.onChangedWifiState(wifiState);
        }
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            // AccessPoints are automatically sorted with TreeSet.
            List<AccessPoint> constructAccessPoints = constructAccessPoints();
            if (!constructAccessPoints.isEmpty() && mAccessPoints.equals(constructAccessPoints)) {
                return;
            }
            mAccessPoints.clear();
            mAccessPoints.addAll(constructAccessPoints);
            if (mWifiStateListener != null) {
                mWifiStateListener.onChangedAccessPoints(mAccessPoints);
            }
        }
    }

    private void updateWifiState(int state) {
        //        Activity activity = getActivity();
        //        if (activity != null) {
        //            activity.invalidateOptionsMenu();
        //        }

        switch (state) {
            case WifiManager.WIFI_STATE_DISABLED:
                enableWifiSwitch();
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                enableWifiSwitch();
                mScanner.resume();
                return; // not break, to avoid the call to pause() below

            default:
                break;
        }

        mLastInfo = null;
        mLastState = null;
        mScanner.pause();

        if (mWifiStateListener != null) {
            mWifiStateListener.onChangedWifiState(mWifiManager.getWifiState());
        }
    }

    private void updateConnectionState(NetworkInfo.DetailedState state) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }

        if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }

        mLastInfo = mWifiManager.getConnectionInfo();
        if (state != null) {
            mLastState = state;
        }

        for (AccessPoint accessPoint : mAccessPoints) {
            accessPoint.update(mLastInfo, mLastState);
        }
        if (mWifiStateListener != null) {
            mWifiStateListener.onChangedAccessPoints(mAccessPoints);
        }
    }

    /**
     * Returns sorted list of access points
     */
    private List<AccessPoint> constructAccessPoints() {
        ArrayList<AccessPoint> scanAccessPoints = new ArrayList<>();
        /** Lookup table to more quickly update AccessPoints by only considering objects with the
         * correct SSID.  Maps SSID -> List of AccessPoints with the given SSID.  */
        Multimap<String, AccessPoint> apMap = new Multimap<>();
        //返回为当前前台用户配置的所有网络的列表。
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                AccessPoint accessPoint = new AccessPoint(mContext, config);
                accessPoint.isScan = false;
                accessPoint.update(mLastInfo, mLastState);
                scanAccessPoints.add(accessPoint);
                apMap.put(accessPoint.ssid, accessPoint);
            }
        }
        //results 最新的接入点扫描结果
        final List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }
                boolean found = false;
                for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
                    if (accessPoint.update(result)) {
                        accessPoint.isScan = true;
                        found = true;
                    }
                }
                if (!found) {
                    AccessPoint accessPoint = new AccessPoint(mContext, result);
                    scanAccessPoints.add(accessPoint);
                    apMap.put(accessPoint.ssid, accessPoint);
                }
            }
        }
        //todo 优化
        Iterator<AccessPoint> accessPointIterator = scanAccessPoints.iterator();
        while (accessPointIterator.hasNext()) {
            AccessPoint next = accessPointIterator.next();
            if (!next.isScan) {
                accessPointIterator.remove();
            }
        }
        // Pre-sort scanAccessPoints to speed preference insertion
        Collections.sort(scanAccessPoints);
        return scanAccessPoints;
    }

    /**
     * A restricted multimap for use in constructAccessPoints
     */
    private class Multimap<K, V> {
        private final HashMap<K, List<V>> store = new HashMap<K, List<V>>();

        /**
         * retrieve a non-null list of values with key K
         */
        List<V> getAll(K key) {
            List<V> values = store.get(key);
            return values != null ? values : Collections.<V>emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = store.get(key);
            if (curVals == null) {
                curVals = new ArrayList<V>(3);
                store.put(key, curVals);
            }
            curVals.add(val);
        }
    }


    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                //                Activity activity = getActivity();
                //                if (activity != null) {
                //                    Toast.makeText(activity, R.string.wifi_fail_to_scan,
                //                            Toast.LENGTH_LONG).show();
                //                }
                return;
            }
            sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
        }
    }

    public void enableWifiSwitch() {
        mSwitch.setChecked(mWifiManager.isWifiEnabled());
        mSwitch.setOnCheckedChangeListener(mSwitchListener);
    }

    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            return;
        }
        if (mWifiStateListener != null) {
            mWifiStateListener.onChangedWifiState(mWifiManager.getWifiState());
        }
    }

    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            return;
        }
        if (mWifiStateListener != null) {
            mWifiStateListener.onChangedWifiState(mWifiManager.getWifiState());
        }
    }

    public void connect(@NonNull final AccessPoint accessPoint, @NonNull final ProxyActionListener connectListener) {
        if (accessPoint.networkId != WifiSystemApi.INVALID_NETWORK_ID) {
            //mWifiManager.enableNetwork(accessPoint.networkId,true);
            WifiSystemApi.createWifiConnectListener(mWifiManager, accessPoint.networkId, connectListener);
        } else if (accessPoint.security == AccessPoint.SECURITY_NONE) {
            /** Bypass dialog for unsecured networks */
            accessPoint.generateOpenNetworkConfig();
            WifiSystemApi.createWifiConnectListener(mWifiManager, accessPoint.getConfig(), connectListener);
        } else {
            showDialog(accessPoint, true);
        }
    }

    private void showDialog(AccessPoint accessPoint, boolean b) {

    }


    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    public interface ProxyActionListener {
        /**
         * The operation succeeded
         */
        void onSuccess();

        /**
         * The operation failure
         */
        void onFailure(int reason);
    }

    public interface WifiStateListener {
        /**
         * WifiState changed
         */
        void onChangedWifiState(final int state);

        /**
         * AccessPoints changed
         */
        void onChangedAccessPoints(final List<AccessPoint> accessPoints);
    }
}
