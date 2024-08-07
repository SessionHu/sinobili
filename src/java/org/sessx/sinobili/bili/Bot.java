package org.sessx.sinobili.bili;

import java.util.UUID;

import org.sessx.sinobili.Main;

import com.google.gson.JsonObject;

public class Bot {

    public static final String API_WEB_H5_VIEW = "https://api.bilibili.com/x/click-interface/click/web/h5";

    public static void fakeviewer(long aid, long cid) {
        Main.stopall = false;
        Runnable r = () -> {
            JsonObject params = new JsonObject();
            params.addProperty("w_aid", aid);
            params.addProperty("w_part", 1);
            params.addProperty("w_type", 3);
            params.addProperty("web_location", 1315873);
            JsonObject form = new JsonObject();
            form.addProperty("mid", APIRequest.getDedeUserID());
            form.addProperty("aid", aid);
            form.addProperty("cid", cid);
            form.addProperty("part", 1);
            form.addProperty("type", 3);
            form.addProperty("sub_type", 0);
            form.addProperty("refer_url", "https://www.bilibili.com/");
            form.addProperty("outer", 0);
            form.addProperty("spmid", "333.788.0.0");
            form.addProperty("from_spmid", "");
            form.addProperty("csrf", APIRequest.getCsrf());
            // loop
            while (!Main.stopall) {
                params.addProperty("w_ftime", System.currentTimeMillis() / 1000L);
                params.addProperty("w_stime", System.currentTimeMillis() / 1000L);
                form.add("ftime", params.get("w_ftime"));
                form.add("stime", params.get("w_stime"));
                form.addProperty("start_ts", System.currentTimeMillis() / 1000L);
                form.addProperty("session", UUID.randomUUID().toString().replace("-", ""));
                JsonObject json = APIRequest.post(API_WEB_H5_VIEW, params, form, false).getAsJsonObject();
                int code = json.get("code").getAsInt();
                if (code == 0) {
                    Main.logger().log(1, "Request sucessfully! Return 0");
                } else {
                    Main.logger().log(2, "Request failed! Response: " + json.toString());
                }
                try {
                    // sleep for [0, 120) s
                    Thread.sleep((int) (Math.random() * 120 * 1000));
                } catch (InterruptedException e) {
                    // do nothing...
                }
            }
        };
        Thread t = new Thread(r, "Fakeviewer-AV" + aid + "-CID" + cid);
        t.setDaemon(true);
        t.start();
    }

}
