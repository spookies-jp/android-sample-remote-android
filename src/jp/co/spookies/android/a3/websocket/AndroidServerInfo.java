package jp.co.spookies.android.a3.websocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;

public class AndroidServerInfo extends BroadcastReceiver{
    private int status;
    private int health;
    private boolean present;
    private int level;
    private int scale;
    private int icon_small;
    private int plugged;
    private int voltage;
    private int temperature;
    private String technology;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_BATTERY_CHANGED)){
            status = intent.getIntExtra("status", 0);
            health = intent.getIntExtra("health", 0);
            present = intent.getBooleanExtra("present", false);
            level = intent.getIntExtra("level", 0);
            scale = intent.getIntExtra("scale", 0);
            icon_small = intent.getIntExtra("icon-small", 0);
            plugged = intent.getIntExtra("plugged", 0);
            voltage = intent.getIntExtra("voltage", 0);
            temperature = intent.getIntExtra("temperature", 0);
            technology = intent.getStringExtra("technology");
        }
    }
    
    public String getBatteryInfo(){
        String statusString = "";
        
        switch(status){
        case BatteryManager.BATTERY_STATUS_UNKNOWN:
            statusString = "unknown";
            break;
        case BatteryManager.BATTERY_STATUS_CHARGING:
            statusString = "charging";
            break;
        case BatteryManager.BATTERY_STATUS_DISCHARGING:
            statusString = "discharging";
            break;
        case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
            statusString = "not charging";
            break;
        case BatteryManager.BATTERY_STATUS_FULL:
            statusString = "full";
            break;
        }
        
        String healthString = "";
        switch (health) {
        case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            healthString = "unknown";
            break;
        case BatteryManager.BATTERY_HEALTH_GOOD:
            healthString = "good";
            break;
        case BatteryManager.BATTERY_HEALTH_OVERHEAT:
            healthString = "overheat";
            break;
        case BatteryManager.BATTERY_HEALTH_DEAD:
            healthString = "dead";
            break;
        case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
            healthString = "voltage";
            break;
        case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
            healthString = "unspecified failure";
            break;
        }
        
        String pluggedString = "";
        switch(plugged){
        case BatteryManager.BATTERY_PLUGGED_AC:
            pluggedString = "plugged ac";
            break;
        case BatteryManager.BATTERY_PLUGGED_USB:
            pluggedString = "plugged usb";
            break;
        }
        StringBuilder json = new StringBuilder();
        json.append("\"status\":" + "\"" + statusString + "\",");
        json.append("\"health\":" + "\"" + healthString + "\",");
        json.append("\"present\":" + getPresent() + ",");
        json.append("\"level\":" + getLevel() + ",");
        json.append("\"scale\":" + getScale() + ",");
        json.append("\"icon_small\":" + getIconSmall() + ",");
        json.append("\"plugged\":" + "\"" + pluggedString + "\",");
        json.append("\"voltage\":" + getVoltage() + ",");
        json.append("\"temperature\":" + getTemperature() + ",");
        json.append("\"technology\":" + "\"" + getTechnology() + "\"");
        
        return "{" + json.toString() + "}";
    }
    
    public int getStatus(){
        return status;
    }
    public int getHealth(){
        return health;
    }
    public boolean getPresent(){
        return present;
    }
    public int getLevel(){
        return level;
    }
    public int getScale(){
        return scale;
    }
    public int getIconSmall(){
        return icon_small;
    }
    public int getPlugged(){
        return plugged;
    }
    public int getVoltage(){
        return voltage;
    }
    public int getTemperature(){
        return temperature;
    }
    public String getTechnology(){
        return technology;
    }
    
}
