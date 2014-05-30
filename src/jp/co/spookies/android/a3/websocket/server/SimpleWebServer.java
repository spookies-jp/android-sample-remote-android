package jp.co.spookies.android.a3.websocket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleWebServer implements Runnable {
    private ServerSocket server = null;
    private HttpHandler httpHandler = null;
    private WebSocketHandler webSocketHandler = null;
    private int port;

    public SimpleWebServer(HttpHandler http, WebSocketHandler webSocket, int port) {
        httpHandler = http;
        webSocketHandler = webSocket;
        this.port = port;
    }

    public void start() throws IOException {
        server = new ServerSocket(port);
        new Thread(this).start();
    }

    public void stop() throws IOException {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = server.accept();
                ConnectionThread thread = new ConnectionThread(socket, httpHandler, webSocketHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
