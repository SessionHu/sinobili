package org.sessx.sinobili.bili;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * @see https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/video/info.md
 */
public class Video {

    @SerializedName("data")
    private JsonObject data;
    public JsonObject getData() {return this.data;}

    private User uploader;
    public User getUploader() {
        if (this.uploader == null) {
            this.uploader = User.fromMid(this.data.getAsJsonObject("owner").get("mid").getAsLong());
        }
        return this.uploader;
    }

    private Video() {}

    public static Video fromAid(long aid) {
        JsonObject params = new JsonObject();
        params.addProperty("aid", aid);
        JsonElement element = APIRequest.get("https://api.bilibili.com/x/web-interface/view", params);
        Video video = new Video();
        video.data = element.getAsJsonObject().get("data").getAsJsonObject();
        return video;
    }

}
