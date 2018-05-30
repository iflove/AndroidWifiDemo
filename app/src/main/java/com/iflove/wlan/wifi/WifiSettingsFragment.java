package com.iflove.wlan.wifi;

import android.app.Fragment;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.iflove.wlan.R;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO 功能描述
 * Created by rentianlong on 2018/5/29
 */
public class WifiSettingsFragment extends Fragment {
    private static final String TAG = "WifiSettingsFragment";
    private ListView mListView;
    private Switch mWifiSwitch;

    private WifiApHelper mWifiApHelper;
    private WifiListAdapter mWifiListAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiApHelper = new WifiApHelper(getActivity(), mWifiSwitch, new WifiApHelper.WifiStateListener() {
            @Override
            public void onChangedWifiState(final int state) {
                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        // AccessPoints are automatically sorted with TreeSet.

                        break;

                    case WifiManager.WIFI_STATE_ENABLING:
                        //                getPreferenceScreen().removeAll();
                        break;

                    case WifiManager.WIFI_STATE_DISABLING:
                        //                addMessagePreference(R.string.wifi_stopping);
                        break;

                    case WifiManager.WIFI_STATE_DISABLED:
                        //                setOffMessage();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onChangedAccessPoints(List<AccessPoint> accessPoints) {
                mWifiListAdapter.setDataSources(accessPoints);
                mWifiListAdapter.notifyDataSetChanged();
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_settings, container, false);
        mListView = rootView.findViewById(R.id.listView);
        View headerView = inflater.inflate(R.layout.fragment_wifi_settings_switch_header, mListView, false);
        mListView.addHeaderView(headerView);
        mWifiSwitch = headerView.findViewById(R.id.wifiSwith);
        mWifiListAdapter = new WifiListAdapter();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                List<AccessPoint> dataSources = mWifiListAdapter.getDataSources();
                AccessPoint accessPoint = dataSources.get(position - 1);
                Log.d(TAG, "onItemClick: " + accessPoint);
                if (accessPoint.isConnectedState()) {
                    Toast.makeText(getActivity(), "WiFi已连接", Toast.LENGTH_SHORT).show();
                } else {
                    mWifiApHelper.connect(accessPoint, new WifiApHelper.ProxyActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getActivity(), "WiFi连接成功", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(getActivity(), "WiFi连接失败", Toast.LENGTH_SHORT).show();
                        }
                    });
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

    private class WifiListAdapter extends BaseAdapter {
        private List<AccessPoint> mDataSources = new ArrayList<>();

        @Override
        public int getCount() {
            return mDataSources.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
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
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifi, parent, false);
            }
            AccessPoint accessPoint = mDataSources.get(position);
            TextView wifiNameTextView = convertView.findViewById(R.id.wifiNameTextView);
            TextView connectStateTextView = convertView.findViewById(R.id.connectStateTextView);
            wifiNameTextView.setText(accessPoint.ssid);
            if (accessPoint.isConnectedState()) {
                connectStateTextView.setVisibility(View.VISIBLE);
                connectStateTextView.setText("已连接");
            } else {
                connectStateTextView.setVisibility(View.GONE);
            }
            return convertView;
        }
    }
}
