package jp.co.spookies.android.a3.websocket.server;

import java.io.OutputStream;

public interface WebSocketHandler {
    public void onConnect(ConnectionThread connection);

    public byte[] onMessage(byte[] data, OutputStream stream);
}
