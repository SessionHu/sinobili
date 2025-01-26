package org.sessx.sinobili.net;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.sessx.sinobili.Main;

public class HttpClient {

    private HttpClient() {}
    private static HttpClient instance = new HttpClient();
    /**
     * Singleton instance of HttpClient
     * @return HttpClient instance
     */
    public static HttpClient get() {
        return instance;
    }

    /**
     * Create a new HttpRequest instance
     * @param uri the URI to request
     * @return a new HttpRequest instance
     * @throws IOException if an I/O error occurs
     * @throws URISyntaxException if the URI is invalid
     */
    public HttpRequest request(String uri) throws IOException, URISyntaxException {
        return new HttpRequest(uri);
    }

    /**
     * Create a new HttpRequest instance with a specific HTTP method
     * @param uri the URI to request
     * @param method the HTTP method to use
     * @return a new HttpRequest instance
     * @throws IOException if an I/O error occurs
     * @throws URISyntaxException if the URI is invalid
     */
    public HttpRequest request(String uri, String method) throws IOException, URISyntaxException {
        return new HttpRequest(uri, method);
    }

    /**
     * Encode a string for use in a URL
     * @param str the string to encode
     * @return the encoded string
     */
    public String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Main.logger().log(3, Main.logger().xcpt2str(e));
            return str;
        }
    }

    /**
     * Decode a string from a URL format
     * @param str the string to decode
     * @return the decoded string
     */
    public String decode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Main.logger().log(3, Main.logger().xcpt2str(e));
            return str;
        }
    }

}
