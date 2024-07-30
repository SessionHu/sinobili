package org.sessx.sinobili;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.sessx.sinobili.bili.BiliSign;
import org.sessx.sinobili.bili.Uploader;
import org.sessx.sinobili.bili.Video;
import org.sessx.sinobili.util.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Main class of SinoBili.
 */
public class Main {

    /**
     * Base directory of SinoBili.
     */
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

    private Main() {} // no instance

    private static void printHelp() {
        String str = 
                "avaliable commands:\n" +
                "  video <aid | bvid>        - get video info\n" +
                "  wbi                       - update wbi sign keys\n" +
                "  biliticket [csrf]         - get bili ticket, 'csrf' is optional\n" +
                "  netdisk <file> <cookies>  - upload file to Bilibili as netdisk\n" +
                "  sharelink <link>          - download file from SSB share link\n" +
                "  clear                     - clear screen\n" +
                "  help                      - show help\n" +
                "  exit                      - exit program\n";
        logger().log(1, str);
    }

    private static LineReader lineReader;

    public static void printAbove(String str) {
        if (lineReader == null) {
            System.out.println(str);
        } else {
            lineReader.printAbove(str + '\n');
        }
    }

    public static boolean readYesOrNo() {
        return lineReader.readLine(">> ").equalsIgnoreCase("y");
    }

    /**
     * main method
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // welcome
        logger().log(1, "Welcome to SinoBili!\nEnter 'help' to get help.");
        try {
            // init terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            lineReader = LineReaderBuilder.builder().terminal(terminal).build();
            String line;
            // loop
            while (true) {
                // read line
                try {
                    line = lineReader.readLine("SinoBili> ");
                } catch (org.jline.reader.EndOfFileException e) {
                    // ^D
                    line = "exit";
                } catch (org.jline.reader.UserInterruptException e) {
                    // ^C
                    continue;
                }
                // parse
                String[] tokens = line.split("\\s+");
                if (tokens.length == 2 && tokens[0].equals("video")) {
                    try {
                        logger().log(1, basicVideoInfo(tokens[1]));
                    } catch (Exception e) {
                        logger().log(3, logger().xcpt2str(e));
                    }
                } else if (tokens.length == 1 && tokens[0].equals("wbi")) {
                    BiliSign.clearMixinKeyCache();
                    BiliSign.wbiSign(new JsonObject());
                    logger().log(1, "wbi sign keys updated");
                } else if (tokens.length > 0 && tokens.length < 3 && tokens[0].equals("biliticket")) {
                    String csrf = tokens.length == 2 ? tokens[1] : "";
                    String ticket = BiliSign.getBiliTicket(csrf);
                    logger().log(1, "bili ticket: " + ticket);
                } else if (tokens.length >= 1 && tokens[0].equals("netdisk")) {
                    if (tokens.length > 2) {
                        BiliSign.clearBiliTicketCache();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < tokens.length; i++) {
                            sb.append(tokens[i]).append("; ");
                        }
                        new Uploader(new File(tokens[1]), sb.toString()).upload();
                    } else if (tokens.length == 2) {
                        logger().log(2, "missing SESSDATA");
                    } else {
                        logger().log(2, "missing file path");
                    }
                } else if (tokens.length > 0 && tokens[0].equals("sharelink")) {
                    if (tokens.length > 1) {
                        Uploader.fromSharelink(tokens[1]);
                    } else {
                        logger().log(2, "missing share link");
                    }
                } else if (tokens.length == 1 && tokens[0].equals("clear")) {
                    terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
                } else if (tokens.length == 1 && tokens[0].equals("help")) {
                    printHelp();
                } else if (tokens.length == 1 && tokens[0].equals("exit")) {
                    logger().log(1, "bye!");
                    break;
                } else {
                    logger().log(2, "unknown command '" + line + "'");
                }
            }
        } catch (Exception e) {
            logger().log(4, logger().xcpt2str(e));
        }
    }

    /**
     * get basic video info by aid or bvid
     * @param aidOrBvid aid or bvid of the video
     * @return basic video info
     */
    public static String basicVideoInfo(String aidOrBvid) {
        // get
        Video video;
        if (aidOrBvid.toLowerCase().startsWith("av")) {
            video = Video.fromAid(Long.parseLong(aidOrBvid.substring(2)));
        } else if (aidOrBvid.toLowerCase().startsWith("bv") && aidOrBvid.length() == 12) {
            video = Video.fromBvid(aidOrBvid);
        } else if (aidOrBvid.matches("\\d+")) {
            video = Video.fromAid(Long.parseLong(aidOrBvid));
        } else {
            throw new IllegalArgumentException("invalid aid or bvid: " + aidOrBvid);
        }
        // build
        StringBuilder sb = new StringBuilder();
        JsonObject videoData = video.getData();
        // title
        sb.append(videoData.get("title").getAsString()).append('\n');
        // aid / bvid
        sb.append("AV").append(videoData.get("aid").getAsLong()).append(" / ");
        sb.append(videoData.get("bvid").getAsString()).append('\n');
        // duration
        Duration duration = Duration.ofSeconds(videoData.get("duration").getAsLong());
        sb.append("时长 ");
        if (duration.toHours() > 0) sb.append(duration.toHours()).append(':');
        sb.append(String.format("%02d:%02d  ", duration.toMinutes() % 60, duration.getSeconds() % 60));
        // view danmaku pubdate
        JsonObject stat = videoData.get("stat").getAsJsonObject();
        sb.append("播放 ").append(stat.get("view").getAsLong()).append("  ");
        sb.append("弹幕 ").append(stat.get("danmaku").getAsLong()).append("  ");
        OffsetDateTime pubdate = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(videoData.get("pubdate").getAsLong()), ZoneOffset.systemDefault());
        sb.append(pubdate.format(DateTimeFormatter.ISO_DATE_TIME)).append('\n');
        // cover
        sb.append("封面 ").append(videoData.get("pic").getAsString()).append('\n');
        // like coin fav share reply
        sb.append("点赞 ").append(stat.get("like").getAsLong()).append("  ");
        sb.append("投币 ").append(stat.get("coin").getAsLong()).append("  ");
        sb.append("收藏 ").append(stat.get("favorite").getAsLong()).append("  ");
        sb.append("分享 ").append(stat.get("share").getAsLong()).append("  ");
        sb.append("评论 ").append(stat.get("reply").getAsLong()).append('\n');
        // tag
        JsonArray tags = video.getTags();
        if (tags.size() > 0) {
            StringJoiner sj = new StringJoiner(" ", "Tags ", "\n");
            for (JsonElement elem : tags) {
                sj.add(elem.getAsJsonObject().get("tag_name").getAsString());
            }
            sb.append(sj);
        }
        // desc
        String desc = videoData.get("desc").getAsString();
        if (!desc.isEmpty()) sb.append(desc).append('\n');
        // return
        return sb.toString();
    }

}
