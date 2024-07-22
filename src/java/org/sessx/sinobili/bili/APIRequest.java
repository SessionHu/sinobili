package org.sessx.sinobili.bili;

import java.io.IOException;
import java.util.StringJoiner;

import org.sessx.sinobili.Main;
import org.sessx.sinobili.net.HttpClient;
import org.sessx.sinobili.net.HttpRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class APIRequest {

    private static String buildurl(String url, JsonObject params) {
        // check
        if (params.isEmpty()) return url;
        // wbi
        if (url.contains("wbi")) {
            BiliSign.wbiSign(params);
        }
        // build
        StringJoiner sj = new StringJoiner("&", url + "?", "");
        for (String param : params.keySet()) {
            sj.add(param + "=" + HttpClient.get().encode(params.get(param).getAsString()));
        }
        return sj.toString();
    }

    public static JsonElement get(String url) {
        return get(url, new JsonObject());
    }

    public static JsonElement get(String url, JsonObject params) {
        // request
        String url0 = buildurl(url, params);
        // response
        JsonElement response = null;
        for (int i = 0; i < 3; i++) {
            Main.logger().log(0, "GET " + url0);
            try {
                HttpRequest request = HttpClient.get().request(url0).header(BiliSign.DEFAULT_HEADERS);
                response = request.cookie(BiliSign.DEFAULT_COOKIES).response().json();
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
        // code
        int code = response.getAsJsonObject().get("code").getAsShort();
        if (code != 0 && !(code == -101 && url.equals("https://api.bilibili.com/x/web-interface/nav"))) {
            String message = response.getAsJsonObject().get("message").getAsString();
            throw new BiliException(code + " " + message + ": " + url);
        }
        // return
        return response.getAsJsonObject();
    }

}
