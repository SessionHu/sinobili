package org.sessx.sinobili.bili;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class User {

    private User() {}

    public static User fromMid(long mid) {
        // request
        JsonObject params = new JsonObject();
        params.addProperty("mid", mid);
        JsonElement cardjson = APIRequest.get("https://api.bilibili.com/x/web-interface/card", params);
        // parse
        User user = new User();
        user.card = cardjson.getAsJsonObject().get("data").getAsJsonObject();
        return user;
    }

    private JsonObject card;
    public JsonObject getCard() {
        return this.card;
    }

}
