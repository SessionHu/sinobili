package org.sessx.sinobili.bili;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * A Bilibili video object.
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

    public static final String API_VIEW_URL = "https://api.bilibili.com/x/web-interface/view";

    public static Video fromAid(long aid) {
        JsonObject params = new JsonObject();
        params.addProperty("aid", aid);
        JsonElement element = APIRequest.get(API_VIEW_URL, params);
        Video video = new Video();
        video.data = element.getAsJsonObject().get("data").getAsJsonObject();
        return video;
    }

    public static Video fromBvid(String bvid) {
        JsonObject params = new JsonObject();
        params.addProperty("bvid", bvid);
        JsonElement element = APIRequest.get(API_VIEW_URL, params);
        Video video = new Video();
        video.data = element.getAsJsonObject().get("data").getAsJsonObject();
        return video;
    }

    /**
     * A map of Bilibili video zone IDs to their main zone labels.
     * @see https://github.com/SessionHu/SessBilinfo/blob/main/src/java/tk/xhuoffice/sessbilinfo/Video.java
     * @see https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/video/video_zone.md
     */
    public static final Map<Short, String> ZONE = new HashMap<>();
    static {
        short[][] data = {
                { 1, 24, 25, 47, 257, 210, 86, 253, 27 }, // 动画
                { 13, 51, 152, 32, 33 }, // 番剧
                { 167, 153, 168, 169, 170, 195 }, // 国创
                { 3, 28, 31, 30, 59, 193, 29, 130, 243, 244, 194 }, // 音乐
                { 129, 20, 154, 156, 198, 199, 200, 255 }, // 舞蹈
                { 4, 17, 171, 172, 65, 173, 121, 136, 19 }, // 游戏
                { 36, 201, 124, 228, 207, 208, 209, 229, 122, 39, 96, 98 }, // 知识
                { 188, 95, 230, 231, 232, 233, 189, 190, 191 }, // 科技
                { 234, 235, 249, 164, 236, 237, 238 }, // 运动
                { 223, 258, 245, 246, 247, 248, 240, 227, 176, 224, 225, 226 }, // 汽车
                { 160, 138, 250, 251, 239, 161, 162, 21, 163, 174, 254 }, // 生活
                { 211, 76, 212, 213, 214, 215 }, // 美食
                { 217, 218, 219, 220, 221, 222, 75 }, // 动物圈
                { 119, 22, 26, 126, 216, 127 }, // 鬼畜
                { 155, 157, 252, 158, 159, 192 }, // 时尚
                { 202, 203, 204, 205, 206 }, // 资讯
                // { 165, 166 }, // 广告
                { 5, 71, 241, 242, 137, 131 }, // 娱乐
                { 181, 182, 183, 85, 184, 256 }, // 影视
                { 177, 37, 178, 179, 180 }, // 纪录片
                { 23, 147, 145, 146, 83 }, // 电影
                { 11, 185, 187 } // 电视剧
        };
        String[] labels = {
                "动画", "番剧", "国创", "音乐", "舞蹈", "游戏", "知识", "科技", "运动",
                "汽车", "生活", "美食", "动物圈", "鬼畜", "时尚", "资讯", /* "广告", */
                "娱乐", "影视", "纪录片", "电影", "电视剧"
        };
        for (short i = 0; i < data.length; i++) {
            for (short j = 0; j < data[i].length; j++) {
                ZONE.put(data[i][j], labels[i]);
            }
        }
    }
    
    /**
     * Get the main zone label of a Bilibili video zone ID.
     * @param tid The Bilibili video zone ID.
     * @return The main zone label of the video zone, or the string
     *         representation of the zone ID if it is not recognized.
     */
    public static String tidSubToMain(short tid) {
        return ZONE.getOrDefault(tid, String.valueOf(tid));
    }

}
