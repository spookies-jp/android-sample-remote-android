package jp.co.spookies.android.a3.websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import jp.co.spookies.android.a3.websocket.server.ConnectionThread;
import jp.co.spookies.android.a3.websocket.server.HttpHandler;
import jp.co.spookies.android.a3.websocket.server.SimpleWebServer;
import jp.co.spookies.android.a3.websocket.server.WebSocketHandler;
import jp.co.spookies.android.a3.websocket.util.WifiInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

public class AndroidWebSocketServer extends Service implements WebSocketHandler {
    private NotificationManager mNM;
    private SimpleWebServer server = null;
    private List<ConnectionThread> connections = null;
    private static final String tagName = "server";
    private AndroidServerInfo serverInfo;

    @Override
    public void onCreate() {
        connections = new ArrayList<ConnectionThread>();
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        serverInfo = new AndroidServerInfo();
        HttpHandler handler = new AndroidHttpHandler(this.getAssets(), this.serverInfo);
        int port = Controller.getPortNumber(this);
        server = new SimpleWebServer(handler, this, port);
        try {
            server.start();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(serverInfo, filter);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO エラー通知
            this.stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mNM.cancel(R.string.app_name);
        setForeground(false);
        try {
            server.stop();
            unregisterReceiver(serverInfo);
            for (ConnectionThread connection : connections) {
                try {
                    connection.disconnect();
                    connection.join();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CharSequence text = WifiInfo.getIpAndPort(this);
        Notification notification = new Notification(R.drawable.notification, getText(R.string.start_service), System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Controller.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        setForeground(true);
        mNM.notify(R.string.app_name, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final RemoteCallbackList<ICameraCallback> callbackList = new RemoteCallbackList<ICameraCallback>();
    private final IRemoteService.Stub binder = new IRemoteService.Stub() {
        @Override
        public void broadcast(byte[] data) throws RemoteException {
            Log.i(tagName, "sendImage");
            for (ConnectionThread connection : connections) {
                try {
                    connection.write(data, "screen");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void registerCallback(ICameraCallback callback) throws RemoteException {
            callbackList.register(callback);
        }

        @Override
        public void unregisterCallback(ICameraCallback callback) throws RemoteException {
            callbackList.unregister(callback);
        }
    };

    @Override
    public byte[] onMessage(byte[] data, OutputStream outstream) {
        if (data != null) {
            String str = new String(data);
            Log.i(tagName, str);
            switch (Command.getByCommand(str)) {
            case CAMERA_START:
                onCameraStart();
                break;
            case CAMERA_STOP:
                onCameraStop();
                break;
            case CAMERA_TAKE:
                return onCameraTake();
            }
        }
        return null;
    }

    @Override
    public void onConnect(ConnectionThread connection) {
        connections.add(connection);
    }

    private void onCameraStart() {
        Intent intent = new Intent(AndroidWebSocketServer.this, RemoteCamera.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void onCameraStop() {
        int n = callbackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                callbackList.getBroadcastItem(i).stopCamera();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        callbackList.finishBroadcast();
    }
    
    private byte[] onCameraTake(){
        int n = callbackList.beginBroadcast();
        byte[] data = null;
        for (int i = 0; i < n; i++) {
            try {
                data = callbackList.getBroadcastItem(i).takeCamera();
                if(data != null){
                    break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        callbackList.finishBroadcast();
        return data;
    }
}

enum Command {
    CAMERA_START("CAMERA_START"), CAMERA_STOP("CAMERA_STOP"), CAMERA_TAKE("CAMERA_TAKE"), NONE("");
    private String command;

    private Command(String command) {
        this.command = command;
    }

    public static Command getByCommand(String command) {
        for (Command c : values()) {
            if (c.command.equals(command)) {
                return c;
            }
        }
        return NONE;
    }
}