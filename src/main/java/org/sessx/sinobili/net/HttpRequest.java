package org.sessx.sinobili.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringJoiner;

import com.google.gson.JsonObject;

public class HttpRequest {

    /**
     * The default user agent for Firefox on Linux.
     */
    public static final String FIREFOX_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0";

    protected HttpRequest(String uri) throws IOException, URISyntaxException {
        this(uri, "GET");
    }

    protected HttpRequest(String uri, String method) throws IOException, URISyntaxException {
        this.conn = (HttpURLConnection) new URI(uri).toURL().openConnection();
        this.conn.setRequestMethod(method);
    }

    /**
     * Set or add the request headers.
     * @param headers the headers to be set or added
     * @return this
     */
    public HttpRequest header(JsonObject headers) {
        for (String key : headers.keySet()) {
            this.conn.setRequestProperty(key, headers.get(key).getAsString());
        }
        return this;
    }

    /**
     * Set the cookies.
     * @param cookies the cookies
     * @return this
     */
    public HttpRequest cookie(JsonObject cookies) {
        StringJoiner joiner = new StringJoiner("; ");
        for (String key : cookies.keySet()) {
            String value = cookies.get(key).getAsString();
            joiner.add(key + "=" + HttpClient.get().encode(HttpClient.get().decode(value)));
        }
        this.conn.setRequestProperty("Cookie", joiner.toString());
        return this;
    }

    private HttpURLConnection conn;

    /**
     * Get the underlying HttpURLConnection.
     * @return the HttpURLConnection
     */
    public HttpURLConnection conn() {
        return this.conn;
    }

    /**
     * Send the request and return the response.
     * @return the response
     * @throws IOException if an I/O error occurs
     */
    public HttpResponse response() throws IOException {
        return new HttpResponse(this.conn);
    }

    /**
     * Set the request method.
     * @param m the request method
     * @return this
     * @throws IOException if an I/O error occurs
     */
    public HttpRequest method(String m) throws IOException {
        this.conn.setRequestMethod(m);
        return this;
    }

    /**
     * Set the request body data.
     * @param data
     * @return this
     * @throws IOException if an I/O error occurs
     */
    public HttpRequest data(byte[] data) throws IOException {
        if (data == null) return this;
        this.conn.setDoOutput(true);
        this.conn.getOutputStream().write(data);
        this.conn.getOutputStream().flush();
        this.conn.getOutputStream().close();
        return this;
    }

    /**
     * Set the request timeout.
     * @param timeout the timeout in milliseconds
     * @return this
     */
    public HttpRequest timeout(int timeout) {
        this.conn.setReadTimeout(timeout);
        this.conn.setConnectTimeout(timeout);
        return this;
    }

}
