package org.sessx.sinobili.bili;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.sessx.sinobili.Main;

import com.google.gson.JsonObject;

public class Monitor {

    public static void video(long aid, String tty, String csv) {
        Main.stopall = false;
        Runnable r = () -> {
            // open csv file
            Writer csvWriter = null;
            try {
                if (csv != null) {
                    OutputStream os = new FileOutputStream(csv);
                    csvWriter = new OutputStreamWriter(csv.endsWith("gz") ? new GZIPOutputStream(os) : os);
                    csvWriter.write("time,view,danmaku,like,coin,favorite,share,reply\n");
                }
            } catch (IOException e) {
                Main.logger().log(2, Main.logger().xcpt2str(e));
            }
            // get initial stat
            long view, danmaku, like, coin, share, fav, reply;
            Video video = Video.fromAid(aid);
            JsonObject stat = video.getData().get("stat").getAsJsonObject();
            view = stat.get("view").getAsLong();
            danmaku = stat.get("danmaku").getAsLong();
            like = stat.get("like").getAsLong();
            coin = stat.get("coin").getAsLong();
            fav = stat.get("favorite").getAsLong();
            share = stat.get("share").getAsLong();
            reply = stat.get("reply").getAsLong();
            // sleep time
            long sleepTime = 0;
            // loop
            while (!Main.stopall) {
                // get latest stat
                long newView, newDanmaku, newLike, newCoin, newFav, newShare, newReply;
                try {
                    stat = (video = Video.fromAid(aid)).getData().get("stat").getAsJsonObject();
                } catch (RuntimeException e) {
                    Main.logger().log(2, Main.logger().xcpt2str(e));
                }
                newView = stat.get("view").getAsLong();
                newDanmaku = stat.get("danmaku").getAsLong();
                newLike = stat.get("like").getAsLong();
                newCoin = stat.get("coin").getAsLong();
                newFav = stat.get("favorite").getAsLong();
                newShare = stat.get("share").getAsLong();
                newReply = stat.get("reply").getAsLong();
                // diff
                long diffView = newView - view;
                long diffDanmaku = newDanmaku - danmaku;
                long diffLike = newLike - like;
                long diffCoin = newCoin - coin;
                long diffFav = newFav - fav;
                long diffShare = newShare - share;
                long diffReply = newReply - reply;
                // format diff
                String diffViewStr = diffView > 0 ? "(+" + diffView + ")" : diffView == 0 ? "" : "(" + diffView + ")";
                String diffDanmakuStr = diffDanmaku > 0 ? "(+" + diffDanmaku + ")" : diffDanmaku == 0 ? "" : "(" + diffDanmaku + ")";
                String diffLikeStr = diffLike > 0 ? "(+" + diffLike + ")" : diffLike == 0 ? "" : "(" + diffLike + ")";
                String diffCoinStr = diffCoin > 0 ? "(+" + diffCoin + ")" : diffCoin == 0 ? "" : "(" + diffCoin + ")";
                String diffFavStr = diffFav > 0 ? "(+" + diffFav + ")" : diffFav == 0 ? "" : "(" + diffFav + ")";
                String diffShareStr = diffShare > 0 ? "(+" + diffShare + ")" : diffShare == 0 ? "" : "(" + diffShare + ")";
                String diffReplyStr = diffReply > 0 ? "(+" + diffReply + ")" : diffReply == 0 ? "" : "(" + diffReply + ")";
                // print
                String formatted = "Status of video AV" + aid + "\n" +
                        "    View:     " + view + diffViewStr + "\n" +
                        "    Danmaku:  " + danmaku + diffDanmakuStr + "\n" +
                        "    Like:     " + like + diffLikeStr + "\n" +
                        "    Coin:     " + coin + diffCoinStr + "\n" +
                        "    Favorite: " + fav + diffFavStr + "\n" +
                        "    Share:    " + share + diffShareStr + "\n" +
                        "    Reply:    " + reply + diffReplyStr + "\n" +
                        "    Updated " + sleepTime + "ms ago";
                if (tty != null) {
                    try (Writer out = new OutputStreamWriter(new FileOutputStream(tty))) {
                        out.write("\033[2J\033[H");
                        out.write(formatted);
                        out.flush();
                    } catch (IOException e) {
                        Main.logger().log(2, e.toString());
                    }
                } else {
                    Main.logger().log(1, formatted);
                }
                if (csvWriter != null) {
                    try {
                        StringBuffer sb = new StringBuffer();
                        sb.append(System.currentTimeMillis()).append(',');
                        sb.append(newView).append(',');
                        sb.append(newDanmaku).append(',');
                        sb.append(newLike).append(',');
                        sb.append(newCoin).append(',');
                        sb.append(newFav).append(',');
                        sb.append(newShare).append(',');
                        sb.append(newReply).append('\n');
                        csvWriter.write(sb.toString());
                        csvWriter.flush();
                    } catch (IOException e) {
                        Main.logger().log(2, e.toString());
                    }
                }
                // update
                view = newView;
                danmaku = newDanmaku;
                like = newLike;
                coin = newCoin;
                fav = newFav;
                share = newShare;
                reply = newReply;
                // sleep for [1, 5] seconds
                try {
                    Thread.sleep(sleepTime = (long) (Math.random() * 4000) + 1000);
                } catch (InterruptedException e) {
                    // interrupted, skip to continue
                }
            }
            try {
                if (csvWriter != null) csvWriter.close();
            } catch (IOException e) {
                Main.logger().log(2, Main.logger().xcpt2str(e));
            }
        };
        Thread t = new Thread(r, "VideoMonitor-AV" + aid);
        t.start();
        if (tty != null) {
            Main.logger().log(1, "Video monitor for AV" + aid + " has redirected output to " + tty);
        }
    }

}
