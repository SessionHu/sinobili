package org.sessx.sinobili.bili;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.StringJoiner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.sessx.sinobili.Main;
import org.sessx.sinobili.net.HttpClient;
import org.sessx.sinobili.net.HttpRequest;

import com.google.gson.JsonObject;

/**
 * Bilibili API sign methods.
 */
public class BiliSign {

    private static final byte[] MIXIN_KEY_ENC_TAB = {
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    };

    private static String mixinKeyCache = null;
    private static long mixinKeyCacheTimeMillis = 0;

    public static void clearMixinKeyCache() {
        mixinKeyCache = null;
        mixinKeyCacheTimeMillis = 0;
    }

    public static final String API_NAV_URL = "https://api.bilibili.com/x/web-interface/nav";

    /**
     * Get the mixin key from the Bilibili API.
     * @return The mixin key as a string.
     */
    private static String getMixinKey() {
        // cache (8 hour)
        if (mixinKeyCache!= null && System.currentTimeMillis() - mixinKeyCacheTimeMillis < 8L * 3600000L) {
            return mixinKeyCache;
        }
        // get imgKey and subKey
        JsonObject nav = APIRequest.get(API_NAV_URL).getAsJsonObject();
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

    /**
     * Sign a request to the Bilibili API using the Wbi Sign method.
     * @param params The request parameters to sign.
     * @return The signed request parameters.
     */
    public static JsonObject wbiSign(JsonObject params) {
        // clear sign
        params.remove("w_rid");
        params.remove("wts");
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
            params.addProperty("w_rid", bytesToHex(md5));
        } catch (java.security.NoSuchAlgorithmException e) {
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

    /**
     * Generate a HMAC-SHA256 hash of the given message string using the given key
     * string.
     * 
     * @param key     The key string to use for the HMAC-SHA256 hash.
     * @param message The message string to hash.
     * @return The HMAC-SHA256 hash of the given message string using the given key
     *         string.
     *         {@code null} if an error occurs during the hash generation.
     */
    public static String hmacSha256(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            Main.logger().log(3, Main.logger().xcpt2str(e));
            return null;
        }
    }

    /**
     * Convert a byte array to a hex string.
     * 
     * @param bytes
     * @return The hex string representation of the given byte array.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    
    public static final String API_GEN_WEB_TICKET = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket";

    private static String biliTicketCache = null;
    private static long biliTicketCacheTimeMillis = 0;
    private static String biliCsrfCache = null;

    /**
     * Get a Bilibili web ticket for the given CSRF token.
     * 
     * @param csrf The CSRF token to use for the web ticket, can be {@code null} or
     *             empty.
     * @return The Bilibili web ticket for the given CSRF token.
     * @see https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/misc/sign/bili_ticket.md
     */
    public static String getBiliTicket(String csrf) {
        // cache (3 days)
        if (biliTicketCache != null && System.currentTimeMillis() - biliTicketCacheTimeMillis < 259260000L
                && (csrf == null || csrf.equals(biliCsrfCache))) {
            return biliTicketCache;
        }
        // generate web ticket
        long ts = System.currentTimeMillis() / 1000;
        String hexSign = hmacSha256("XgwSnGZ1p", "ts" + ts);
        JsonObject params = new JsonObject();
        params.addProperty("key_id", "ec02");
        params.addProperty("hexsign", hexSign);
        params.addProperty("context[ts]", ts);
        params.addProperty("csrf", csrf == null ? "" : csrf);
        JsonObject response = APIRequest.post(API_GEN_WEB_TICKET, params).getAsJsonObject();
        // add cache
        biliTicketCacheTimeMillis = System.currentTimeMillis();
        biliTicketCache = response.get("data").getAsJsonObject().get("ticket").getAsString();
        biliCsrfCache = csrf;
        // return
        return biliTicketCache;
    }

    public static void clearBiliTicketCache() {
        biliTicketCache = null;
        biliTicketCacheTimeMillis = 0;
        biliCsrfCache = null;
    }

    public static String bytesToBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    public static byte[] base64ToBytes(String base64) {
        return Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
    }

}