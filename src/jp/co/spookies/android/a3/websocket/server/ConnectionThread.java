package jp.co.spookies.android.a3.websocket.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

import android.util.Log;

public class ConnectionThread extends Thread {
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private HttpHandler httpHandler = null;
    private WebSocketHandler webSocketHandler = null;
    private Object lockObject = new Object();
    private boolean continueFlag = true;
    private Socket socket = null;

    public ConnectionThread(Socket socket, HttpHandler httpHandler, WebSocketHandler webSocketHandler) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.httpHandler = httpHandler;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void run() {
        try {
            HttpRequest request = new HttpRequest(inputStream);
            if (request.isWebSocket()) {
                System.setProperty("line.separator", "\r\n");
                PrintStream outstream = new PrintStream(outputStream);
                outstream.println("HTTP/1.1 101 WebSocket Protocol Handshake");
                outstream.println("Upgrade: WebSocket");
                outstream.println("Sec-WebSocket-Origin: " + request.getOrigin());
                outstream.println("Sec-WebSocket-Location: " + request.getLocation());
                outstream.println("Sec-WebSocket-Protocol: " + request.getProtocol());
                outstream.println("");
                outstream.write(request.getChallenge());
                outstream.flush();
                webSocketHandler.onConnect(this);
                while (continueFlag) {
                    byte[] data = readData();
                    data = webSocketHandler.onMessage(data, outstream);
                    if(data != null){
                        write(data, "preview");
                    }
                }
                outstream.close();
                inputStream.close();
            } else {
                HttpResponse response = httpHandler.getResponse(request);
                PrintStream outstream = new PrintStream(outputStream);
                if(response == null){
                    outstream.println("HTTP/1.0 404 Not Found");
                    outstream.println("");
                    // TODO: 404ページ作成 その他のエラーページは？
                }else{
                    outstream.println("HTTP/1.0 200 OK");
                    outstream.println("MIME_version:1.0");
                    outstream.println("Content-Type:" + response.getContentType());
                    outstream.println("Content-Length:" + response.getContentLength());
                    outstream.println("");
                    outstream.write(response.getContent());
                }
                outstream.flush();
                outstream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] data, String name) throws IOException {
        if(data == null){
            return;
        }
        synchronized (lockObject) {
            outputStream.write(0x00);
            outputStream.write((name + "|").getBytes());
            outputStream.write(data);
            outputStream.write(0xff);
            outputStream.flush();
        }
    }

    public void disconnect() throws IOException {
        synchronized (lockObject) {
            outputStream.write(0xff);
            outputStream.write(0x00);
            outputStream.flush();
            socket.shutdownInput();
            continueFlag = false;
        }
    }

    public byte[] readData() {
        int b = 0;
        byte[] buf = new byte[256]; // XXX
        int index = 0;
        try {
            while (continueFlag) {
                b = inputStream.read();
                if ((b) == 0x00) {
                    while ((b = inputStream.read()) != 0xFF) {
                        buf[index++] = (byte) b;
                    }
                    break;
                } else if(b == -1) {
                    continueFlag = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] res = new byte[index];
        for (int i = 0; i < index; i++) {
            res[i] = buf[i];
        }
        return res;
    }
}
