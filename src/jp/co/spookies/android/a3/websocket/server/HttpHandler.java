package jp.co.spookies.android.a3.websocket.server;

public abstract class HttpHandler {
    public abstract HttpResponse getResponse(HttpRequest request);
}
