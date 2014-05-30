package jp.co.spookies.android.a3.websocket.server;

public class HttpResponse {
    private byte[] content;
    private String contentType;

    public HttpResponse(byte[] content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public int getContentLength() {
        return getContent().length;
    }
}
