package com.iflove.wlan.wifi;

import android.annotation.SuppressLint;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * WiFi 系统Api
 * Created by rentianlong on 2018/5/30
 */
final class WifiSystemApi {

    private static final String TAG = "WifiSystemApi";


    public static final String CONFIGURED_NETWORKS_CHANGED_ACTION =
            "android.net.wifi.CONFIGURED_NETWORKS_CHANGE";
    public static final String LINK_CONFIGURATION_CHANGED_ACTION =
            "android.net.wifi.LINK_CONFIGURATION_CHANGED";
    public static final String EXTRA_AVAILABLE_TETHER = "availableArray";
    public static final String EXTRA_ACTIVE_TETHER = "tetherArray";
    public static final String EXTRA_ERRORED_TETHER = "erroredArray";
    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    public static final int WIFI_AP_STATE_FAILED = 14;
    public static final int INVALID_NETWORK_ID = -1;
    private static final Class<?> sWifiManagerActionListener = createWifiManagerActionListener();

    public static boolean isHandshakeState(SupplicantState state) {
        switch (state) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
                return true;
            case COMPLETED:
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
                return false;
            default:
                throw new IllegalArgumentException("Unknown supplicant state");
        }
    }

    @SuppressLint("PrivateApi")
    public static Class<?> createWifiManagerActionListener() {
        try {
            return Class.forName("android.net.wifi.WifiManager$ActionListener");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "fail to get ActionListener...");
        }
        return null;
    }

    @SuppressLint("PrivateApi")
    public static void createWifiConnectListener(@NonNull final WifiManager wifiManager, int networkId,
                                                 @NonNull final WifiApHelper.ProxyActionListener listener) {
        Class<? extends WifiManager> wifiClazz = wifiManager.getClass();

        try {
            Method forget = wifiClazz.getDeclaredMethod("connect", int.class, sWifiManagerActionListener);
            Object proxyInstance = createProxyActionListener(listener, wifiClazz);
            forget.invoke(wifiManager, networkId, proxyInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("PrivateApi")
    public static void createWifiConnectListener(@NonNull final WifiManager wifiManager, WifiConfiguration wifiConfiguration,
                                                 @NonNull final WifiApHelper.ProxyActionListener listener) {
        Class<? extends WifiManager> wifiClazz = wifiManager.getClass();
        try {
            Method forget = wifiClazz.getDeclaredMethod("connect", WifiConfiguration.class, sWifiManagerActionListener);
            Object proxyInstance = createProxyActionListener(listener, wifiClazz);
            forget.invoke(wifiManager, wifiConfiguration, proxyInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @NonNull
    private static Object createProxyActionListener(@NonNull final WifiApHelper.ProxyActionListener listener, Class<? extends WifiManager> wifiClazz) {
        return Proxy.newProxyInstance(wifiClazz.getClassLoader(), new Class[]{sWifiManagerActionListener}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (args == null) {
                    listener.onSuccess();
                } else {
                    listener.onFailure((Integer) args[0]);
                }
                return listener;
            }
        });
    }
}
