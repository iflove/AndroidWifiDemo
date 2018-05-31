package com.iflove.wlan.wifi;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.iflove.wlan.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.iflove.wlan.wifi.AccessPoint.SECURITY_NONE;

/**
 * TODO 功能描述
 * Created by rentianlong on 2018/5/29
 */
public class WifiSettingsFragment extends Fragment {
    private static final String TAG = "WifiSettingsFragment";
    private ListView mListView;
    private ContentLoadingProgressBar mProgressBar;
    private Switch mWifiSwitch;
    private TextView mSummaryTextView;
    private WifiApHelper mWifiApHelper;
    private WifiListAdapter mWifiListAdapter;
    private AlertDialog mInputPasswordDialog;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;
    private AccessPoint mSelectedAccessPoint;
    private static final int[] STATE_SECURED = {
            R.attr.state_encrypted
    };
    private static final int[] STATE_NONE = {};

    private final WifiSystemApi.ProxyActionListener mForgetActionListener = new WifiSystemApi.ProxyActionListener() {
        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(int reason) {

        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_settings, container, false);
        mListView = rootView.findViewById(R.id.listView);
        mProgressBar = rootView.findViewById(R.id.progressBar);
        View headerView = inflater.inflate(R.layout.fragment_wifi_settings_switch_header, mListView, false);
        mListView.addHeaderView(headerView);
        mWifiSwitch = headerView.findViewById(R.id.wifiSwitch);
        mSummaryTextView = headerView.findViewById(R.id.summaryTextView);

        mWifiListAdapter = new WifiListAdapter();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                List<AccessPoint> dataSources = mWifiListAdapter.getDataSources();
                AccessPoint accessPoint = dataSources.get(position - 1);
                //                dataSources.remove(accessPoint);
                //                dataSources.add(0, accessPoint);
                //                mWifiListAdapter.notifyDataSetChanged();
                if (accessPoint.isConnectedState()) {
                    Toast.makeText(getActivity(), R.string.wifi_wifi_connected, Toast.LENGTH_SHORT).show();
                } else {
                    mWifiApHelper.connect(accessPoint);
                }
            }
        });
        mListView.setAdapter(mWifiListAdapter);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(mListView);
        mWifiApHelper = new WifiApHelper(getActivity(), mWifiSwitch, new WifiApHelper.WifiStateListener() {
            @Override
            public void onChangedWifiState(final int state) {
                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        // AccessPoints are automatically sorted with TreeSet.
                        mSummaryTextView.setText("");
                        mSummaryTextView.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.GONE);
                        break;

                    case WifiManager.WIFI_STATE_ENABLING:
                        mProgressBar.setVisibility(View.VISIBLE);
                        mSummaryTextView.setText(R.string.wifi_starting);
                        mSummaryTextView.setVisibility(View.VISIBLE);
                        break;

                    case WifiManager.WIFI_STATE_DISABLING:
                        mSummaryTextView.setText(R.string.wifi_closing);
                        mSummaryTextView.setVisibility(View.VISIBLE);
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        mSummaryTextView.setText("");
                        mSummaryTextView.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onChangedAccessPoints(final List<AccessPoint> accessPoints) {
                mListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWifiListAdapter.setDataSources(accessPoints);
                        mWifiListAdapter.notifyDataSetChanged();
                    }
                }, 30);
            }

            @Override
            public void onWifiScanFailure() {
                Toast.makeText(getActivity(), R.string.wifi_scan_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNeedInputWifiPassword(AccessPoint accessPoint) {
                showInputPasswordDialog(accessPoint);
            }

        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mWifiApHelper.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWifiApHelper.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterForContextMenu(mListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position - 1;
            mSelectedAccessPoint = mWifiListAdapter.getDataSources().get(position);
            menu.setHeaderTitle(mSelectedAccessPoint.ssid);
            if (mSelectedAccessPoint.getLevel() != -1
                    && mSelectedAccessPoint.getState() == null) {
                menu.add(Menu.NONE, MENU_ID_CONNECT, 0, R.string.wifi_menu_connect);
            }
            if (mSelectedAccessPoint.networkId != WifiSystemApi.INVALID_NETWORK_ID) {
                menu.add(Menu.NONE, MENU_ID_FORGET, 0, R.string.wifi_menu_forget);
                menu.add(Menu.NONE, MENU_ID_MODIFY, 0, R.string.wifi_menu_modify);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mSelectedAccessPoint == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case MENU_ID_CONNECT: {
                mWifiApHelper.connect(mSelectedAccessPoint);
                return true;
            }
            case MENU_ID_FORGET: {
                mWifiApHelper.forget(mSelectedAccessPoint, mForgetActionListener);
                return true;
            }
            case MENU_ID_MODIFY: {
                showInputPasswordDialog(mSelectedAccessPoint);
                return true;
            }
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void showInputPasswordDialog(final AccessPoint accessPoint) {
        if (mWifiListAdapter.getDataSources().isEmpty()) {
            return;
        }

        if (mInputPasswordDialog != null && mInputPasswordDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle("连接-" + accessPoint.ssid);

        // Set up the input
        final EditText input = new EditText(this.getActivity());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();
                mWifiApHelper.connect(accessPoint, text);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        mInputPasswordDialog = builder.show();
    }

    private class WifiListAdapter extends BaseAdapter {
        private List<AccessPoint> mDataSources = new ArrayList<>();

        @Override
        public int getCount() {
            return mDataSources.size();
        }

        @Override
        public Object getItem(int position) {
            return mDataSources.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setDataSources(List<AccessPoint> dataSources) {
            this.mDataSources.clear();
            this.mDataSources.addAll(dataSources);
        }

        public List<AccessPoint> getDataSources() {
            return mDataSources;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Context context = parent.getContext();
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_wifi, parent, false);
            }
            final AccessPoint accessPoint = mDataSources.get(position);
            TextView wifiNameTextView = convertView.findViewById(R.id.wifiNameTextView);
            final TextView summaryTextView = convertView.findViewById(R.id.summaryTextView);
            ImageView signal = convertView.findViewById(R.id.signal);
            int level = accessPoint.getLevel();
            int security = accessPoint.getSecurity();
            if (level == -1) {
                signal.setImageDrawable(null);
            } else {
                //根据信号强弱选择对应的图标
                signal.setImageLevel(level);
                //这种设置和在XML中android:src="?attr/wifi_signal"设置是等价的
                signal.setImageDrawable(context.getTheme().obtainStyledAttributes(new int[]{R.attr.wifi_signal}).getDrawable(0));

                signal.setImageState((security != SECURITY_NONE) ? STATE_SECURED : STATE_NONE, true);
            }
            wifiNameTextView.setText(accessPoint.ssid);
            if (accessPoint.isConnectedState()) {
                summaryTextView.setText(R.string.wifi_connected);
            } else {
                final WifiConfiguration config = accessPoint.getConfig();
                final NetworkInfo.DetailedState state = accessPoint.getState();
                if (state != null) {
                    // This is the active connection
                    summaryTextView.setText(Summary.get(context, state));
                } else if (level == -1) {
                    // Wifi out of range
                    summaryTextView.setText(R.string.wifi_not_in_range);
                } else if (config != null && config.status == WifiConfiguration.Status.DISABLED) {
                    try {
                        //TODO 如何兼容高版本
                        Field disableReason = config.getClass().getDeclaredField("disableReason");
                        int disableReasonInt = disableReason.getInt(config);
                        switch (disableReasonInt) {
                            case WifiSystemApi.DISABLED_AUTH_FAILURE:
                                summaryTextView.setText(R.string.wifi_disabled_password_failure);
                                //                                showInputPasswordDialog(accessPoint);
                                break;
                            case WifiSystemApi.DISABLED_DHCP_FAILURE:
                            case WifiSystemApi.DISABLED_DNS_FAILURE:
                                summaryTextView.setText(R.string.wifi_disabled_network_failure);
                                break;
                            case WifiSystemApi.DISABLED_UNKNOWN_REASON:
                                summaryTextView.setText(R.string.wifi_remembered);
                                break;
                            case WifiSystemApi.DISABLED_ASSOCIATION_REJECT:
                                summaryTextView.setText(R.string.wifi_disabled_association_reject);
                                break;
                            default:
                                break;
                        }
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else { // In range, not disabled.
                    StringBuilder summary = new StringBuilder();
                    if (config != null || accessPoint.isSaveConfig) {
                        // Is saved network
                        summary.append(context.getResources().getString(R.string.wifi_remembered));
                    }
                    if (security != SECURITY_NONE) {
                        String securityStrFormat;
                        if (summary.length() == 0) {
                            securityStrFormat = context.getString(R.string.wifi_secured_first_item);
                        } else {
                            securityStrFormat = context.getString(R.string.wifi_secured_second_item);
                        }
                        summary.append(String.format(securityStrFormat,
                                Summary.getSecurityString(context, security, accessPoint.getPskType(), true)));
                    }
                    summaryTextView.setText(summary.toString());
                }
            }
            if (TextUtils.isEmpty(summaryTextView.getText().toString().trim())) {
                summaryTextView.setVisibility(View.GONE);
            } else {
                summaryTextView.setVisibility(View.VISIBLE);
            }
            return convertView;
        }
    }
}
