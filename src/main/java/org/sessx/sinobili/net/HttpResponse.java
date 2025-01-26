package org.sessx.sinobili.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class HttpResponse {

    private HttpURLConnection conn;

    protected HttpResponse(HttpURLConnection conn) throws IOException {
        this.conn = conn;
        this.conn.connect();
    }

    public InputStream getInputStream() throws IOException {
        return this.conn.getInputStream();
    }

    /**
     * Get the response body as a byte array.
     * @return the response body as a byte array.
     * @throws IOException if an I/O error occurs.
     */
    public byte[] bytes() throws IOException {
        InputStream in = this.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            baos.write(b);
        }
        in.close();
        return baos.toByteArray();
    }

    /**
     * Get the response body as a string.
     * @return the response body as a string.
     * @throws IOException if an I/O error occurs.
     */
    public String text() throws IOException {
        return new String(this.bytes(), StandardCharsets.UTF_8);
    }

    /**
     * Default Gson instance for JSON parsing.
     */
    public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Get the response body as a JSON element.
     * @return the response body as a JSON element.
     * @throws IOException if an I/O error occurs.
     */
    public JsonElement json() throws IOException {
        return GSON.fromJson(this.text(), JsonElement.class);
    }

}
