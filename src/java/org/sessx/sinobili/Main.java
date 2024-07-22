package org.sessx.sinobili;

import java.io.File;

import org.sessx.sinobili.bili.User;
import org.sessx.sinobili.bili.Video;
import org.sessx.sinobili.util.Logger;

public class Main {

    public static final File BASE_DIR;
    static {
        BASE_DIR = new File(System.getProperty("user.home") + "/.sessx/sinobili");
        BASE_DIR.mkdirs();
    }

    private static Logger logger;
    static {
        logger = Logger.get();
    }

    public static Logger logger() {
        return logger;
    }

    public static void main(String[] args) {
        logger().log(1, "Welcome to SinoBili!");
        try {
            logger().log(1, example());
        } catch (Exception e) {
            logger().log(4, logger().xcpt2str(e));
        }
    }

    public static String example() {
        Video video = Video.fromAid(2);
        byte aid = 2;
        String title = video.getData().get("title").getAsString();
        User user = video.getUploader();
        String upname = user.getCard().get("card").getAsJsonObject().get("name").getAsString();
        String upsign = user.getCard().get("card").getAsJsonObject().get("sign").getAsString();
        return title + "\nav" + aid + "\n" + upname + "\n" + upsign;
    }

}
