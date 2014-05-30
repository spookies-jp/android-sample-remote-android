package jp.co.spookies.android.a3.websocket.server;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class HttpRequest {
    private final String CRLF = "\r\n";
    private String action;
    private String connection;
    private byte[] key1;
    private byte[] key2;
    private byte[] key3;
    private String upgrade;
    private String protocol;
    private String origin;
    private String host;

    public HttpRequest(InputStream stream) throws IOException {
        try {
            byte[] b = new byte[10000]; // XXX
            int length = stream.read(b);
            List<String> lines = Arrays.asList(StringUtils.split(new String(b, 0, length), CRLF));
            action = StringUtils.split(lines.get(0))[1];
            for (String line : lines.subList(1, lines.size() - 1)) {
                if (!StringUtils.contains(line, ':')) {
                    continue;
                }
                int separatorIndex = line.indexOf(':');
                String name = StringUtils.strip(line.substring(0, separatorIndex));
                String value = StringUtils.strip(line.substring(separatorIndex + 1));
                if (name.equals("Host")) {
                    host = value;
                } else if (name.equals("Connection")) {
                    connection = value;
                } else if (name.equals("Sec-WebSocket-Key1")) {
                    key1 = getKey(value);
                } else if (name.equals("Sec-WebSocket-Key2")) {
                    key2 = getKey(value);
                } else if (name.equals("Upgrade")) {
                    upgrade = value;
                } else if (name.equals("Sec-WebSocket-Protocol")) {
                    protocol = value;
                } else if (name.equals("Origin")) {
                    origin = value;
                }
            }
            if (isWebSocket()) {
                key3 = new byte[8];
                for (int i = 0; i < 8; i++) {
                    key3[i] = b[length - 8 + i];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException();
        }
    }

    public String getAction() {
        return action;
    }

    public Boolean isWebSocket() {
        return "WebSocket".equals(upgrade);
    }

    public byte[] getChallenge() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] buf = new byte[key1.length + key2.length + key3.length];
        int i = 0;
        for (int j = 0; j < key1.length; j++) {
            buf[i++] = key1[j];
        }
        for (int j = 0; j < key2.length; j++) {
            buf[i++] = key2[j];
        }
        for (int j = 0; j < key3.length; j++) {
            buf[i++] = key3[j];
        }
        return digest.digest(buf);
    }

    public String getOrigin() {
        return origin;
    }

    public String getLocation() {
        return "ws://" + host + action;
    }

    public String getProtocol() {
        return protocol;
    }

    private byte[] getKey(String str) throws Exception {
        StringBuilder builder = new StringBuilder();
        long spaceCount = 0;
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                builder.append(c);
            } else if (c == ' ') {
                spaceCount++;
            }
        }
        String strNum = builder.toString();
        long num = Long.parseLong(strNum);
        if (num % spaceCount != 0) {
            throw new Exception();
        }
        return new BigInteger(new Long(num / spaceCount).toString()).toByteArray();
    }

    public String getConnection() {
        return connection;
    }
}
