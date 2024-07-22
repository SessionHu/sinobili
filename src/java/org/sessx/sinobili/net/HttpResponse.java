package org.sessx.sinobili.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class HttpResponse {

    private HttpURLConnection conn;

    protected HttpResponse(HttpURLConnection conn) throws IOException {
        this.conn = conn;
        this.conn.connect();
    }

    public byte[] bytes() throws IOException {
        InputStream in = this.conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            baos.write(b);
        }
        in.close();
        return baos.toByteArray();
    }

    public String text() throws IOException {
        return new String(this.bytes(), StandardCharsets.UTF_8);
    }

    public static Gson GSON = new Gson();

    public JsonElement json() throws IOException {
        return GSON.fromJson(this.text(), JsonElement.class);
    }

}
