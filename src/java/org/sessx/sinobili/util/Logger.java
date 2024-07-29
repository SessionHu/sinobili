package org.sessx.sinobili.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

import org.sessx.sinobili.Main;

public class Logger {

    private Writer out;

    /**
     * Get a logger instance.
     * @return a logger instance.
     */
    public static Logger get() {
        File dir = new File(Main.BASE_DIR, "logs");
        File latest = new File(dir, "latest.log");
        if (latest.exists()) {
            // gzip
            String datetime = LocalDateTime.ofInstant(Instant.ofEpochMilli(latest.lastModified()), ZoneOffset.systemDefault())
                    .format(DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss"));
            try (InputStream in  = new FileInputStream(latest);
                OutputStream out = new GZIPOutputStream(new FileOutputStream(new File(dir, datetime + ".log.gz"))))
            {
                for (long pos = 0; pos < latest.length(); pos++) {
                    out.write(in.read());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // rm
            latest.delete();
        } else {
            dir.mkdirs();
        }
        return new Logger(latest);
    }

    private Logger(File file) {
        try {
            this.out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        } catch (java.io.FileNotFoundException e) {
            // null writer
            this.out = new Writer() {
                @Override public void close() {}
                @Override public void write(char[] cbuf, int off, int len) {}
                @Override public void flush() {}
            };
            this.log(3, e.toString());
        }
    }

    private static final String[] LEVELS = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
    private static final String[] COLORS = {"\033[36m", "\033[0m", "\033[93m", "\033[31m", "\033[91;1m"};

    /**
     * Log a message.
     * @param level the log level (0-4). 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL. Default is 0.
     * @param text the message text.
     */
    public void log(int level, String text) {
        // check
        if (level < 0 || level >= LEVELS.length) level = 0;
        // class & method
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        String className = ste.getClassName().substring(ste.getClassName().lastIndexOf('.') + 1);
        String methodName = ste.getMethodName();
        int lineNum = ste.getLineNumber();
        // time
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        // format & print & write
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            // format
            String result = String.format("%s[%s %s][%s.%s:%d] %s\033[0m",
                    COLORS[level], time, LEVELS[level], className, methodName, lineNum, line);
            // print
            Main.printAbove(result);
            // write
            try {
                this.out.write(result);
                this.out.write('\n');
                this.out.flush();
            } catch (IOException e) {
                // do nothing...
            }
        }
    }

    /**
     * Convert an {@link Throwable} to a string.
     * Format with 4 spaces for each level of the stack trace.
     * 
     * @param t the {@link Throwable} to convert.
     * @return the string representation of the {@link Throwable} and its stack trace.
     */
    public String xcpt2str(Throwable t) {
        StackTraceElement[] elements = t.getStackTrace();
        StringBuilder sb = new StringBuilder(t.toString()).append('\n');
        for (StackTraceElement elem : elements) {
            sb.append("    ").append(elem).append('\n');
        }
        return sb.toString();
    }

}
