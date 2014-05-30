package jp.co.spookies.android.a3.websocket.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import jp.co.spookies.android.a3.websocket.Controller;

public class WifiInfo {
    public static String getIpAndPort(Context context){
        int port = Controller.getPortNumber(context);
        int ip = ((WifiManager)context.getSystemService(context.WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF) + ":" + port;
    }
}
