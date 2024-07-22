package org.sessx.sinobili.bili;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringJoiner;

import org.sessx.sinobili.Main;
import org.sessx.sinobili.net.HttpClient;
import org.sessx.sinobili.net.HttpRequest;

import com.google.gson.JsonObject;

public class BiliSign {

    private static final byte[] MIXIN_KEY_ENC_TAB = {
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    };

    private static String mixinKeyCache = null;
    private static long mixinKeyCacheTimeMillis = 0;

    private static String getMixinKey() {
        // cache
        if (mixinKeyCache!= null && System.currentTimeMillis() - mixinKeyCacheTimeMillis < 3600000) {
            return mixinKeyCache;
        }
        // get imgKey and subKey
        JsonObject nav = APIRequest.get("https://api.bilibili.com/x/web-interface/nav").getAsJsonObject();
        JsonObject wbiImg = nav.get("data").getAsJsonObject().get("wbi_img").getAsJsonObject();
        String imgUrl = wbiImg.get("img_url").getAsString();
        String imgKey = imgUrl.substring(imgUrl.lastIndexOf("/") + 1, imgUrl.lastIndexOf("."));
        String subUrl = wbiImg.get("sub_url").getAsString();
        String subKey = subUrl.substring(subUrl.lastIndexOf("/") + 1, subUrl.lastIndexOf("."));
        // mixin key
        String s = imgKey + subKey;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            key.append(s.charAt(MIXIN_KEY_ENC_TAB[i]));
        }
        // return
        mixinKeyCache = key.toString();
        mixinKeyCacheTimeMillis = System.currentTimeMillis();
        return mixinKeyCache;
    }

    public static JsonObject wbiSign(JsonObject params) {
        // mixin key
        String mixinKey = getMixinKey();
        // add timestamp
        JsonObject copy = params.deepCopy();
        copy.addProperty("wts", System.currentTimeMillis() / 1000);
        // message to be signed
        StringJoiner joiner = new StringJoiner("&");
        for (String key : copy.keySet()) {
            String encoded = HttpClient.get().encode(copy.get(key).getAsString());
            joiner.add(key + "=" + encoded.replace("+", "%20"));
        }
        String s = joiner + mixinKey;
        // md5
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : md5) {
                hex.append(Integer.toHexString(0xff & b));
            }
            params.addProperty("w_rid", hex.toString());
        } catch (NoSuchAlgorithmException e) {
            Main.logger().log(3, Main.logger().xcpt2str(e));
        }
        params.addProperty("wts", copy.get("wts").getAsLong());
        return params;
    }

    public static final JsonObject DEFAULT_HEADERS;
    static {
        JsonObject headers = new JsonObject();
        headers.addProperty("User-Agent", HttpRequest.FIREFOX_USER_AGENT);
        headers.addProperty("Referer", "https://www.bilibili.com");
        // headers.addProperty("Accept-Encoding", "gzip, deflate, br"); // not implemented yet
        headers.addProperty("Accept", "application/json, text/plain, */*");
        headers.addProperty("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.addProperty("DNT", "1"); // even if it do nothing
        DEFAULT_HEADERS = headers;
    }

    public static final JsonObject DEFAULT_COOKIES;
    static {
        JsonObject cookies = new JsonObject();
        // from https://github.com/SocialSisterYi/bilibili-API-collect/issues/795
        // from https://api.bilibili.com/x/frontend/finger/spi
        cookies.addProperty("buvid3", "3F681212-EA56-FB84-EACC-E634CD872D8A53844infoc");
        cookies.addProperty("buvid4", "6AA36083-722B-5229-A5D5-BACD43BAA83F53844-024072206-v5mYMm9dUeVGOADNkrbD6Q==");
        DEFAULT_COOKIES = cookies;
    }

}