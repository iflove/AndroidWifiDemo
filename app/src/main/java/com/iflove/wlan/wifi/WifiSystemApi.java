package com.iflove.wlan.wifi;

import android.net.wifi.SupplicantState;

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
}
