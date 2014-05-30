package jp.co.spookies.android.a3.websocket;

import java.io.IOException;
import java.io.InputStream;

import jp.co.spookies.android.a3.websocket.server.HttpHandler;
import jp.co.spookies.android.a3.websocket.server.HttpRequest;
import jp.co.spookies.android.a3.websocket.server.HttpResponse;

import org.apache.commons.lang.StringUtils;

import android.content.res.AssetManager;

public class AndroidHttpHandler extends HttpHandler {
    AssetManager assetManager = null;
    private static final String DIRECTORY_INDEX = "index.html";
    private AndroidServerInfo serverInfo = null;

    public AndroidHttpHandler(AssetManager assetManager, AndroidServerInfo serverInfo) {
        this.assetManager = assetManager;
        this.serverInfo = serverInfo;
    }

    @Override
    public HttpResponse getResponse(HttpRequest request) {
        String action = request.getAction();
        String path = ("/".equals(action)) ? DIRECTORY_INDEX : action.substring(1);
        String mimeType = getMimeType(path);
        HttpResponse response = null;
        try {
            InputStream stream;
            byte[] content = null;
            stream = assetManager.open(path);
            content = new byte[stream.available()];
            stream.read(content);
            stream.close();
            response = new HttpResponse(content, mimeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    private String getMimeType(String path) {
        String[] splited = StringUtils.split(path, '.');
        String ext = splited[splited.length - 1];
        return MimeType.getByExt(ext).getMimeType();
    }
}

enum MimeType {
    HTML("html", "text/html"), GIF("gif", "image/gif"), JS("js", "application/javascript"),
    CSS("css", "text/css"), JSON("json", "text/plain"), NONE("", "text/html");
    private String ext;
    private String mimeType;

    private MimeType(String ext, String mimeType) {
        this.ext = ext;
        this.mimeType = mimeType;
    }

    public String getExt() {
        return ext;
    }
    public String getMimeType() {
        return mimeType;
    }

    public static MimeType getByExt(String ext) {
        for (MimeType mimeType : values()) {
            if (mimeType.ext.equals(ext)) {
                return mimeType;
            }
        }
        return NONE;
    }
}