package org.sessx.sinobili.bili;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import org.sessx.sinobili.Main;
import org.sessx.sinobili.net.HttpClient;
import org.sessx.sinobili.net.HttpRequest;
import org.sessx.sinobili.net.HttpResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A class for sending API requests to the Bilibili API.
 */
public class APIRequest {

    protected static String buildurl(String url, JsonObject params) {
        // check
        if (params.isEmpty()) return url;
        // build
        StringJoiner sj = new StringJoiner("&", url + "?", "");
        for (String param : params.keySet()) {
            sj.add(param + "=" + HttpClient.get().encode(params.get(param).getAsString()));
        }
        return sj.toString();
    }

    /**
     * Send a GET request to the specified URL.
     * 
     * @param url The URL to send the request to.
     * @return The response from the server as a {@code JsonElement}.
     * @see #get(String, JsonObject)
     */
    public static JsonElement get(String url) {
        return get(url, new JsonObject());
    }

    /**
     * Send a GET request to the specified URL with the specified parameters.
     * 
     * @param url    The URL to send the request to.
     * @param params The parameters to send with the request.
     * @return The response from the server as a {@code JsonElement}.
     */
    public static JsonElement get(String url, JsonObject params) {
        return sendRequest("GET", url, params, new JsonObject(), null);
    }

    /**
     * Send a POST request to the specified URL with the specified parameters.
     * 
     * @param url    The URL to send the request to.
     * @param params The parameters to send with the request.
     * @return The response from the server as a {@code JsonElement}.
     */
    public static JsonElement post(String url, JsonObject params) {
        return sendRequest("POST", url, params, new JsonObject(), null);
    }

    /**
     * Send a POST request to the specified URL with the specified parameters, headers,
     * and data.
     * 
     * @param url     The URL to send the request to.
     * @param params  The parameters to send with the request.
     * @param data    The data to send with the request, can be null.
     * @param headers The headers to send with the request.
     * @return The response from the server as a {@code JsonElement}.
     */
    public static JsonElement post(String url, JsonObject params, JsonObject headers, byte[] data) {
        return sendRequest("POST", url, params, headers, data);
    }

    /**
     * Send a POST request to the specified URL with the specified parameters and
     * form data.
     * 
     * @param url    The URL to send the request to.
     * @param params The parameters to send with the request.
     * @param form   The form data to send with the request.
     * @return The response from the server as a {@code JsonElement}.
     */
    public static JsonElement post(String url, JsonObject params, JsonObject form, boolean retry) {
        JsonObject headers = new JsonObject();
        headers.addProperty("Content-Type", "application/x-www-form-urlencoded");
        StringJoiner sj = new StringJoiner("&");
        for (String key : form.keySet()) {
            sj.add(key + "=" + HttpClient.get().encode(form.get(key).getAsString()));
        }
        // Main.logger().log(0, "Form data: " + sj);
        return sendRequest("POST", url, params, headers, sj.toString().getBytes(StandardCharsets.UTF_8), retry);
    }

    /**
     * Send a request to the specified URL with the specified parameters, headers,
     * and data.
     * 
     * @param method  The HTTP method to use for the request.
     * @param url     The URL to send the request to.
     * @param params  The parameters to send with the request.
     * @param headers The headers to send with the request.
     * @param data    The data to send with the request.
     * @return The response from the server as a {@code JsonElement}.
     */
    private static JsonElement sendRequest(String method, String url, JsonObject params, JsonObject headers, byte[] data) {
        return sendRequest(method, url, params, headers, data, true);
    }

    /**
     * Send a request to the specified URL with the specified parameters, headers,
     * data and retry option.
     * 
     * @param method  The HTTP method to use for the request.
     * @param url     The URL to send the request to.
     * @param params  The parameters to send with the request.
     * @param headers The headers to send with the request.
     * @param data    The data to send with the request.
     * @param retry   Whether to retry the request if it fails.
     * @return The response from the server as a {@code JsonElement}.
     */
    private static JsonElement sendRequest(String method, String url, JsonObject params, JsonObject headers,
            byte[] data, boolean retry) {
        // request
        String url0 = buildurl(url, params);
        // response
        JsonElement response = null;
        for (int i = 0; i < (retry ? 3 : 1); i++) {
            // Main.logger().log(0, method + " " + url0);
            if (i > 0) {
                Main.logger().log(1, "Retrying... (" + (i + 1) + "/3)");
            }
            try {
                HttpRequest request = HttpClient.get().request(url0).method(method);
                request.header(BiliSign.DEFAULT_HEADERS).header(headers).cookie(getCookies(url));
                request.data(data);
                response = request.response().json();
                break;
            } catch (IOException | java.net.URISyntaxException e) {
                String st = Main.logger().xcpt2str(e);
                if (i < 2) {
                    Main.logger().log(2, st);
                } else {
                    throw new BiliException(e);
                }
            }
        }
        // check control
        if (!retry) return response;
        // code
        if (!response.isJsonObject() || !response.getAsJsonObject().has("code")) return response;
        int code = response.getAsJsonObject().get("code").getAsInt();
        if (code != 0 && !(code == -101 && url.equals(BiliSign.API_NAV_URL))) {
            String message = response.getAsJsonObject().get("message").getAsString();
            // Main.logger().log(0, response.toString());
            throw new BiliException(code + " " + message + ": " + url);
        }
        // return
        return response;
    }

    private static JsonObject getCookies(String url) {
        JsonObject cookies = BiliSign.DEFAULT_COOKIES.deepCopy();
        for (String key : LOCAL_COOKIES.keySet()) {
            cookies.addProperty(key, LOCAL_COOKIES.get(key).getAsString());
        }
        if (!url.equals(BiliSign.API_GEN_WEB_TICKET)) {
            String csrf = cookies.get("bili_jct") == null ? "" : cookies.get("bili_jct").getAsString();
            cookies.addProperty("bili_ticket", BiliSign.getBiliTicket(csrf));
        }
        // Main.logger().log(0, "Cookies: " + cookies.toString());
        return cookies;
    }

    private static final JsonObject LOCAL_COOKIES = new JsonObject();

    public static void login(String[] cookie) {
        for (String c : cookie) {
            String k, v;
            k = HttpClient.get().decode(c.substring(0, c.indexOf("=")));
            v = HttpClient.get().decode(c.substring(c.indexOf("=") + 1));
            if (k.equals("SESSDATA") || k.equals("bili_jct") || k.equals("DedeUserID") || k.equals("DedeUserID__ckMd5")) {
                LOCAL_COOKIES.addProperty(k, v);
            }
        }
        BiliSign.clearBiliTicketCache();
        try (OutputStream out = new FileOutputStream(Main.BASE_DIR + "/cookies.json")) {
            out.write(LOCAL_COOKIES.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Main.logger().log(2, "Failed to save cookies: " + e);
        }
        Main.logger().log(1, "Login success.");
    }

    public static void login(File file) throws IOException {
        JsonObject fc = HttpResponse.GSON.fromJson(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), JsonObject.class);
        for (String k : fc.keySet()) {
            LOCAL_COOKIES.addProperty(k, fc.get(k).getAsString());
        }
        BiliSign.clearBiliTicketCache();
        Main.logger().log(1, "Login success.");
    }

    public static String getCsrf() {
        JsonElement csrf = LOCAL_COOKIES.get("bili_jct");
        if (csrf == null) {
            Main.logger().log(2, "Not logged in, csrf is empty");
            return "";
        }
        return csrf.getAsString();
    }

    public static String getDedeUserID() {
        JsonElement mid =  LOCAL_COOKIES.get("DedeUserID");
        if (mid == null) {
            Main.logger().log(2, "Not logged in, DedeUserID is 0");
            return "0";
        }
        return mid.getAsString();
    }

}
