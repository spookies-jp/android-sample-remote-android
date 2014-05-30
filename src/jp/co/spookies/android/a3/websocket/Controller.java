package jp.co.spookies.android.a3.websocket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import jp.co.spookies.android.a3.websocket.util.TextDialogPreference;

public class Controller extends PreferenceActivity {
    WifiManager wifiManager = null;
    WifiInfo wifiInfo = null;
    TextDialogPreference textDialogPreference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        addPreferencesFromResource(R.xml.pref);
        CheckBoxPreference c = (CheckBoxPreference) findPreference(getText(R.string.key_service_switch));
        if (isRunning(this)) {
            startService();
        }
        textDialogPreference = (TextDialogPreference) findPreference(getText(R.string.key_wifi_info));
//        setCurrentWifiInfo();
        c.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (((Boolean) newValue).booleanValue()) {
                    startService();
                } else {
                    stopService();
                }
                return true;
            }
        });
    }
    
    @Override
    protected void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mBroadcastReceiver, filter);
    }
    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }
    
    private static final int MENU_ABOUT = 1;
    private static final int MENU_HELP = 2;
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ABOUT, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, MENU_HELP, 0, R.string.menu_help).setIcon(android.R.drawable.ic_menu_help);
        
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ABOUT:
                showDialog(this,"about", getString(R.string.app_name)+"\n  "+getString(R.string.current_version)+"\n"+getString(R.string.copyright));
                return true;
            case MENU_HELP:
                showDialog(this,"Help",
                    getString(R.string.help_text)
                        );
                return true;
        }
        return true;
    }    

    
    private static void showDialog(final Activity activity, String title,String text) {
        (new AlertDialog.Builder(activity))
        .setTitle(title)
        .setMessage(text)
        .setPositiveButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int whichButton) {
                activity.setResult(Activity.RESULT_OK);
            }
        }).create().show();
    }
    
    public void onClickCamera(View v){
        Log.i("Controller", "onClickCamera");
        Intent intent = new Intent(Controller.this, RemoteCamera.class);
        startActivity(intent);
    }
    
    private void setCurrentWifiInfo(){
        String summary = getWifiState();
        String message = getWifiState();
        if(wifiManager.isWifiEnabled()){
            int i =0;
            do {
                wifiInfo = wifiManager.getConnectionInfo();
                i++;
            } while (wifiInfo.getIpAddress() == 0 && i < 10000);
            message = 
                wifiInfo.getSSID() + "\n"
                + getIpAddress();
            summary = getIpAddress();
        }
        textDialogPreference.setSummary(summary);
        textDialogPreference.setDialogMessage(message);
    }

    private String getIpAddress(){
        int ip = wifiInfo.getIpAddress();
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }
    private String getWifiState(){
        int state = wifiManager.getWifiState();
        String s = "";
        switch(state){
        case WifiManager.WIFI_STATE_DISABLED:
            s = "wifi disabled";
            break;
        case WifiManager.WIFI_STATE_DISABLING:
            s = "wifi disabling";
            break;
        case WifiManager.WIFI_STATE_ENABLED:
            s = "wifi enabled";
            break;
        case WifiManager.WIFI_STATE_ENABLING:
            s = "wifi enabling";
            break;
        case WifiManager.WIFI_STATE_UNKNOWN:
            s= "wifi unknown";
            break;
        }
        return s;
    }

    private void startService() {
        findPreference(getText(R.string.key_camera_button)).setEnabled(true);
        startService(new Intent(Controller.this, AndroidWebSocketServer.class));
    }

    private void stopService() {
        findPreference(getText(R.string.key_camera_button)).setEnabled(false);
        stopService(new Intent(Controller.this, AndroidWebSocketServer.class));
    }

    public static boolean isRunning(Context context) {
        String key = context.getString(R.string.key_service_switch);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    public static int getPortNumber(Context context) {
        return getInt(context, R.string.key_port_number, R.string.default_port_number);
    }

    public static int getCameraInterval(Context context) {
        return getInt(context, R.string.key_camera_interval, R.string.default_camera_interval);
    }

    public static int getCameraQuality(Context context) {
        return getInt(context, R.string.key_camera_quality, R.string.default_camera_quality);
    }

    private static int getInt(Context context, int keyId, int defaultValueId) {
        String key = context.getString(keyId);
        String defaultValue = context.getString(defaultValueId);
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue));
    }
    
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                setCurrentWifiInfo();
            }
        }
    };
}
