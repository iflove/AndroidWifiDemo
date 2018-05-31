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

    public static final int INVALID_NETWORK_ID = -1;
    public static final int DISABLED_UNKNOWN_REASON                         = 0;
    public static final int DISABLED_DNS_FAILURE                            = 1;
    public static final int DISABLED_DHCP_FAILURE                           = 2;
    public static final int DISABLED_AUTH_FAILURE                           = 3;
    public static final int DISABLED_ASSOCIATION_REJECT                     = 4;
    private static final Class<?> WIFI_MANAGER_ACTION_LISTENER = createWifiManagerActionListener();

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
    public static void connect(@NonNull final WifiManager wifiManager, int networkId,
                               @NonNull final ProxyActionListener listener) {
        Class<? extends WifiManager> wifiClazz = wifiManager.getClass();

        try {
            Method forget = wifiClazz.getDeclaredMethod("connect", int.class, WIFI_MANAGER_ACTION_LISTENER);
            Object proxyInstance = Proxy.newProxyInstance(wifiClazz.getClassLoader(), new Class[]{WIFI_MANAGER_ACTION_LISTENER}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (args == null || args.length == 0) {
                        listener.onSuccess();
                    } else {
                        listener.onFailure((Integer) args[0]);
                    }
                    return listener;
                }
            });
            forget.invoke(wifiManager, networkId, proxyInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("PrivateApi")
    public static void forget(@NonNull final WifiManager wifiManager, int networkId,
                              @NonNull final ProxyActionListener listener) {
        Class<? extends WifiManager> wifiClazz = wifiManager.getClass();

        try {
            Method forget = wifiClazz.getDeclaredMethod("forget", int.class, WIFI_MANAGER_ACTION_LISTENER);
            Object proxyInstance = Proxy.newProxyInstance(wifiClazz.getClassLoader(), new Class[]{WIFI_MANAGER_ACTION_LISTENER}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (args == null || args.length == 0) {
                        listener.onSuccess();
                    } else {
                        listener.onFailure((Integer) args[0]);
                    }
                    return listener;
                }
            });
            forget.invoke(wifiManager, networkId, proxyInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("PrivateApi")
    public static void connect(@NonNull final WifiManager wifiManager, WifiConfiguration wifiConfiguration,
                               @NonNull final ProxyActionListener listener) {
        Class<? extends WifiManager> wifiClazz = wifiManager.getClass();
        try {
            Method forget = wifiClazz.getDeclaredMethod("connect", WifiConfiguration.class, WIFI_MANAGER_ACTION_LISTENER);
            Object proxyInstance = Proxy.newProxyInstance(wifiClazz.getClassLoader(), new Class[]{WIFI_MANAGER_ACTION_LISTENER}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (args == null || args.length == 0) {
                        listener.onSuccess();
                    } else {
                        listener.onFailure((Integer) args[0]);
                    }
                    return listener;
                }
            });
            forget.invoke(wifiManager, wifiConfiguration, proxyInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public interface ProxyActionListener {
        /**
         * The operation succeeded
         */
        public void onSuccess();

        /**
         * The operation failed
         *
         * @param reason The reason for failure could be one of
         */
        public void onFailure(int reason);
    }
}
