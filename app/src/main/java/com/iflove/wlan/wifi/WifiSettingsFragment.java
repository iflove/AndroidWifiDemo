package com.iflove.wlan.wifi;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
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
    private ContentLoadingProgressBar mProgressBar;
    private Switch mWifiSwitch;
    private TextView mSummaryTextView;
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
                        mSummaryTextView.setText("");
                        mSummaryTextView.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.GONE);
                        break;

                    case WifiManager.WIFI_STATE_ENABLING:
                        mProgressBar.setVisibility(View.VISIBLE);
                        mSummaryTextView.setText("正在开启...");
                        mSummaryTextView.setVisibility(View.VISIBLE);
                        break;

                    case WifiManager.WIFI_STATE_DISABLING:
                        mSummaryTextView.setText("正在关闭...");
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
                Log.d(TAG, "onChangedAccessPoints: " + accessPoints.size());
                mListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWifiListAdapter.setDataSources(accessPoints);
                        mWifiListAdapter.notifyDataSetChanged();
                    }
                }, 50);
            }

            @Override
            public void onWifiScanFailure() {
                Toast.makeText(getActivity(), "WiFi扫描失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNeedInputWifiPassword(AccessPoint accessPoint) {
                showInputPasswordDialog(accessPoint);
            }

        });
    }

    private void showInputPasswordDialog(final AccessPoint accessPoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle("Title-" + accessPoint.ssid);

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
                mWifiApHelper.connect(accessPoint, "12345678");
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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
                if (accessPoint.isConnectedState()) {
                    Toast.makeText(getActivity(), "WiFi已连接", Toast.LENGTH_SHORT).show();
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
            TextView summaryTextView = convertView.findViewById(R.id.summaryTextView);
            wifiNameTextView.setText(accessPoint.ssid);
            if (accessPoint.isConnectedState()) {
                summaryTextView.setText("已连接");
            } else {
                AccessPoint.PskType pskType = accessPoint.getPskType();
                String saveText = "已保存";
                summaryTextView.setVisibility(View.VISIBLE);
                if (pskType == AccessPoint.PskType.UNKNOWN) {
                    if (accessPoint.isSaveConfig) {
                        summaryTextView.setText(saveText);
                    } else {
                        summaryTextView.setVisibility(View.GONE);
                    }
                } else {
                    summaryTextView.setText((accessPoint.isSaveConfig ? saveText + ", " : "") + "通过 " + pskType.name() + " 进行保护");
                }
            }
            return convertView;
        }
    }
}
