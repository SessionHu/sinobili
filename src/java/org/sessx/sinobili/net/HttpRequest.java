package org.sessx.sinobili.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringJoiner;

import com.google.gson.JsonObject;

public class HttpRequest {

    public static final String FIREFOX_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0";

    protected HttpRequest(String uri) throws IOException, URISyntaxException {
        this(uri, "GET");
    }

    protected HttpRequest(String uri, String method) throws IOException, URISyntaxException {
        this.conn = (HttpURLConnection) new URI(uri).toURL().openConnection();
        this.conn.setRequestMethod(method);
    }

    public HttpRequest header(JsonObject headers) {
        for (String key : headers.keySet()) {
            this.conn.setRequestProperty(key, headers.get(key).getAsString());
        }
        return this;
    }

    public HttpRequest cookie(JsonObject cookies) {
        StringJoiner joiner = new StringJoiner("; ");
        for (String key : cookies.keySet()) {
            joiner.add(key + "=" + cookies.get(key).getAsString());
        }
        this.conn.setRequestProperty("Cookie", joiner.toString());
        return this;
    }

    private HttpURLConnection conn;

    public HttpURLConnection conn() {
        return this.conn;
    }

    public HttpResponse response() throws IOException {
        return new HttpResponse(this.conn);
    }

}
