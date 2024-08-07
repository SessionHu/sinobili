package org.sessx.sinobili.bili;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.sessx.sinobili.Main;
import org.sessx.sinobili.net.HttpClient;
import org.sessx.sinobili.net.HttpRequest;
import org.sessx.sinobili.net.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Uploader {

    public Uploader(File file) {
        // check params
        if (!file.exists()) {
            Main.logger().log(2, file.getPath() + " does not exist!");
            return;
        } else if (!file.isFile()) {
            Main.logger().log(2, file.getPath() + " is not a file!");
            return;
        } else if (!file.canRead()) {
            Main.logger().log(2, "Cannot read " + file.getPath());
            return;
        } else if (file.isDirectory()) {
            Main.logger().log(2, file.getPath() + " is a directory and you can upload it???");
            return;
        } else {
            Main.logger().log(1, "Will upload " + file.getName() + ". Are you sure? (Y/n)");
            if (!Main.readYesOrNo()) {
                Main.logger().log(1, "Aborted.");
                return;
            } else {
                this.file = file;
            }
        }
    }

    private File file;

    private String uploadUrl;

    private String xUposAuth;

    private long bizId;

    private int chunkSize;

    private String uploadId;

    private int parts;

    public void upload() {
        long ts = System.currentTimeMillis();
        // preupload
        Main.logger().log(1, "Preuploading " + this.file.getName());
        this.preupload();
        // upload meta
        Main.logger().log(1, "Uploading meta of " + this.file.getName());
        this.uploadMeta();
        // digest
        try {
            this.sha256 = MessageDigest.getInstance("SHA-256");
            this.sha256.reset();
        } catch (NoSuchAlgorithmException e) {
            Main.logger().log(2, "Failed to get SHA-256 algorithm: " + e.getMessage());
        }
        // upload chunks
        try {
            Main.logger().log(1, "Uploading chunks of " + this.file.getName());
            this.uploadChunks();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        // finish
        Main.logger().log(1, "Finishing uploading " + this.file.getName());
        this.finish();
        // done
        Main.logger().log(1, "Done uploading " + this.file.getName() + "(" + (System.currentTimeMillis() - ts) + "ms)");
        // sharelink
        Main.logger().log(1, "Sharelink: " + this.genSharelink());
    }

    private MessageDigest sha256;

    public String genSharelink() {
        StringBuilder shareuri = new StringBuilder("ssb://");
        JsonObject data = new JsonObject();
        data.addProperty("name", this.file.getName());
        data.addProperty("size", this.file.length());
        data.addProperty("auth", this.xUposAuth);
        data.addProperty("url", this.uploadUrl);
        data.addProperty("sha256", BiliSign.bytesToHex(this.sha256.digest()));
        shareuri.append(BiliSign.bytesToBase64(data.toString().getBytes(StandardCharsets.UTF_8)));
        return shareuri.toString();
    }

    private static class NumVal {
        public volatile long val = 0;
    }

    public static void fromSharelink(String sharelink) {
        // parse sharelink
        if (!sharelink.startsWith("ssb://")) {
            Main.logger().log(2, "Invalid sharelink: " + sharelink);
            return;
        }
        String base64 = sharelink.substring(6);
        byte[] data = BiliSign.base64ToBytes(base64);
        JsonObject json = HttpResponse.GSON.fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
        // print json
        Main.logger().log(1, "Sharelink JSON:\n" + HttpResponse.GSON.toJson(json));
        // file
        File file = new File(System.getProperty("user.home") + "/Downloads", json.get("name").getAsString());
        long size = json.get("size").getAsLong();
        String sha256 = json.get("sha256").getAsString();
        NumVal nowprogress = new NumVal();
        // headers
        JsonObject headers = new JsonObject();
        headers.add("X-Upos-Auth", json.get("auth"));
        // download
        try {
            Main.logger().log(1, "Downloading " + file.getPath());
            // download
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            sha256Digest.reset();
            HttpRequest req = HttpClient.get().request(json.get("url").getAsString(), "GET");
            InputStream in = req.header(headers).timeout(5000).response().getInputStream();
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[65536];
            int len;
            long startts = System.currentTimeMillis() - 1;
            // report thread
            Thread reportThread = new Thread(() -> {
                while (nowprogress.val < size && nowprogress.val >= 0) {
                    Main.logger().log(1, "speed: " + (nowprogress.val / (System.currentTimeMillis() - startts)) + "bytes/s");
                    Main.logger().log(1, "progress: " + nowprogress.val + "/" + size + " (" + (nowprogress.val * 100 / size) + "%)");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "DownloadReporter-" + file.getName());
            reportThread.setDaemon(true);
            reportThread.start();
            while ((len = in.read(buffer)) > 0) {
                // tips
                nowprogress.val += len;
                // digest & write
                sha256Digest.update(buffer, 0, len);
                out.write(buffer, 0, len);
            }
            out.close();
            // check sha256
            String localSha256 = BiliSign.bytesToHex(sha256Digest.digest());
            Main.logger().log(1, "SHA-256 of downloaded file: " + localSha256);
            if (!sha256.equals(localSha256)) {
                Main.logger().log(2, "SHA-256 of downloaded file does not match!");
            } else {
                Main.logger().log(1, "Done! (" + (System.currentTimeMillis() - startts) + "ms)");
            }
        } catch (IOException | URISyntaxException | NoSuchAlgorithmException e) {
            Main.logger().log(2, "Failed to download file!");
            Main.logger().log(2, Main.logger().xcpt2str(e));
        } finally {
            nowprogress.val = -1;
        }
    }
    

    public static final String API_PREUPLOAD_URL = "https://member.bilibili.com/preupload";

    private void preupload() {
        JsonObject params = new JsonObject();
        params.addProperty("name", this.file.getName());
        params.addProperty("size", this.file.length()); // not necessary
        params.addProperty("r", "upos");
        params.addProperty("profile", "ugcfx/bup");
        JsonObject resp = APIRequest.post(API_PREUPLOAD_URL, params).getAsJsonObject();
        // upload url
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https:").append(resp.get("endpoint").getAsString());
        urlBuilder.append(resp.get("upos_uri").getAsString().replaceFirst("upos:/", ""));
        this.uploadUrl = urlBuilder.toString();
        // x-upos-auth
        this.xUposAuth = resp.get("auth").getAsString();
        // biz_id
        this.bizId = resp.get("biz_id").getAsLong();
        // chunk size
        this.chunkSize = resp.get("chunk_size").getAsInt();
    }

    private void uploadMeta() {
        // params
        JsonObject params = new JsonObject();
        params.addProperty("uploads", ""); // not typo, or will HTTP 404
        params.addProperty("output", "json");
        params.addProperty("profile", "ugcfx/bup");
        params.addProperty("filesize", this.file.length());
        params.addProperty("partsize", this.chunkSize);
        params.addProperty("biz_id", this.bizId);
        // headers
        JsonObject headers = new JsonObject();
        headers.addProperty("X-Upos-Auth", this.xUposAuth);
        // upload
        JsonElement resp = APIRequest.post(this.uploadUrl, params, headers, null);
        this.uploadId = resp.getAsJsonObject().get("upload_id").getAsString();
    }

    private void uploadChunks() throws IOException, URISyntaxException {
        long startts = System.currentTimeMillis() - 1;
        // prepare to read
        long length = this.file.length();
        byte[] buffer = new byte[this.chunkSize];
        int size = 0;
        int chunks = (int) Math.ceil(length / (double) buffer.length);
        InputStream in = new FileInputStream(this.file);
        // params
        JsonObject params = new JsonObject();
        params.addProperty("uploadId", this.uploadId);
        params.addProperty("total", length);
        // headers
        JsonObject headers = new JsonObject();
        headers.addProperty("X-Upos-Auth", this.xUposAuth);
        headers.addProperty("Content-Type", "application/octet-stream");
        // read and upload
        for (int chunk = 0; chunk < chunks; chunk++) {
            // tips
            Main.logger().log(1, "speed: " + (chunk * buffer.length) / (System.currentTimeMillis() - startts) + "bytes/s");
            Main.logger().log(1, "chunk: " + (chunk + 1) + "/" + chunks);
            // read
            size = in.read(buffer, 0, buffer.length);
            if (size == -1) {
                break;
            }
            // digest
            this.sha256.update(buffer, 0, size);
            // params
            params.addProperty("partNumber", String.valueOf(chunk + 1));
            params.addProperty("chunk", String.valueOf(chunk));
            params.addProperty("chunks", String.valueOf(chunks));
            params.addProperty("size", String.valueOf(size));
            params.addProperty("start", String.valueOf(chunk * buffer.length));
            params.addProperty("end", String.valueOf((chunk) * buffer.length + size));
            // headers
            headers.addProperty("Content-Length", String.valueOf(size));
            // upload
            byte[] data = new byte[size];
            System.arraycopy(buffer, 0, data, 0, size);
            HttpRequest request = HttpClient.get().request(APIRequest.buildurl(this.uploadUrl, params), "PUT");
            String response = request.header(headers).data(data).timeout(5000).response().text();
            if (!response.contains("MULTIPART_PUT_SUCCESS")) {
                // retry
                Main.logger().log(2, "Failed to upload chunk " + (chunk + 1) + ", retrying...");
                chunk--;
            }
        }
        in.close();
        this.parts = chunks;
    }

    private void finish() {
        // params
        JsonObject params = new JsonObject();
        params.addProperty("output", "json");
        params.addProperty("name", this.file.getName());
        params.addProperty("profile", "ugcfx/bup");
        params.addProperty("uploadId", this.uploadId);
        params.addProperty("biz_id", this.bizId);
        // headers
        JsonObject headers = new JsonObject();
        headers.addProperty("X-Upos-Auth", this.xUposAuth);
        // data
        JsonArray partsArr = new JsonArray();
        for (int i = 1; i <= this.parts; i++) {
            JsonObject part = new JsonObject();
            part.addProperty("partNumber", i);
            part.addProperty("eTag", "etag");
            partsArr.add(part);
        }
        JsonObject obj = new JsonObject();
        obj.add("parts", partsArr);
        byte[] data = obj.toString().getBytes(StandardCharsets.UTF_8);
        // request
        APIRequest.post(this.uploadUrl, params, headers, data).getAsJsonObject();
    }

}
