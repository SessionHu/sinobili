package org.sessx.sinobili.net;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.sessx.sinobili.Main;

public class HttpClient {

    private HttpClient() {}
    private static HttpClient instance = new HttpClient();
    public static HttpClient get() {
        return instance;
    }

    public HttpRequest request(String uri) throws IOException, URISyntaxException {
        return new HttpRequest(uri);
    }
    public HttpRequest request(String uri, String method) throws IOException, URISyntaxException {
        return new HttpRequest(uri, method);
    }

    public String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Main.logger().log(3, Main.logger().xcpt2str(e));
            return str;
        }
    }

}
