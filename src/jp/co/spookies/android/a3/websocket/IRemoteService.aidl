package jp.co.spookies.android.a3.websocket;
import jp.co.spookies.android.a3.websocket.ICameraCallback;
interface IRemoteService{
    void broadcast(in byte[] data);
    void registerCallback(ICameraCallback callback);
    void unregisterCallback(ICameraCallback callback);
}
