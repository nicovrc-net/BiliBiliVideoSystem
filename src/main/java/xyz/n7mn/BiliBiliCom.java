package xyz.n7mn;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class BiliBiliCom {

    public String run(String videoUrl, String audioUrl, long duration, String proxyAddress, int proxyPort){
        if (!new File("./temp").exists()){
            new File("./temp").mkdir();
        }

        String fileId;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((videoUrl + audioUrl + UUID.randomUUID().toString()).getBytes(StandardCharsets.UTF_8));
            byte[] cipher_byte = md.digest();
            StringBuilder sb = new StringBuilder(2 * cipher_byte.length);
            for(byte b: cipher_byte) {
                sb.append(String.format("%02x", b&0xff) );
            }
            fileId = new Date().getTime()+"_"+sb.substring(0, 16);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }

        //System.out.println(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
        if (new File("./temp/"+fileId).mkdir()){


            String m3u8_main = "#EXTM3U\n" +
                    "#EXT-X-VERSION:6\n" +
                    "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                    "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"/video/"+fileId+"/audio.m3u8\"\n" +
                    "#EXT-X-STREAM-INF:AUDIO=\"audio\"\n" +
                    "/video/"+fileId+"/video.m3u8";

            String m3u8_sub = "#EXTM3U\n" +
                    "#EXT-X-VERSION:6\n" +
                    "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                    "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"/video/"+fileId+"/audio.m3u8\"\n" +
                    "#EXT-X-STREAM-INF:AUDIO=\"audio\"\n" +
                    "/video/"+fileId+"/sub.m3u8";

            String m3u8_video = "#EXTM3U\n" +
                    "#EXT-X-VERSION:6\n" +
                    "#EXT-X-TARGETDURATION:0\n" +
                    "#EXT-X-PLAYLIST-TYPE:VOD\n" +
                    "#EXT-X-MEDIA-SEQUENCE:1\n" +
                    "#EXTINF:"+duration+",\n" +
                    videoUrl.replaceAll("https://upos-sz-mirroraliov\\.bilivideo\\.com", "").replaceAll("https://upos-hz-mirrorakam\\.akamaized\\.net", "") + "&com=true" + (videoUrl.startsWith("https://upos-hz-mirrorakam\\.akamaized\\.net") ? "&com=aka" : "") + "\n" +
                    "#EXT-X-ENDLIST";

            String m3u8_audio = "#EXTM3U\n" +
                    "#EXT-X-VERSION:6\n" +
                    "#EXT-X-TARGETDURATION:0\n" +
                    "#EXT-X-PLAYLIST-TYPE:VOD\n" +
                    "#EXT-X-MEDIA-SEQUENCE:1\n" +
                    "#EXTINF:"+duration+",\n" +
                    audioUrl.replaceAll("https://upos-sz-mirroraliov\\.bilivideo\\.com", "").replaceAll("https://upos-hz-mirrorakam\\.akamaized\\.net", "") + "&com=true" + (audioUrl.startsWith("https://upos-hz-mirrorakam\\.akamaized\\.net") ? "&com=aka" : "") + "\n" +
                    "#EXT-X-ENDLIST";

            try {
                FileOutputStream stream = new FileOutputStream("./temp/" + fileId + "/sub.m3u8");
                stream.write(m3u8_main.getBytes(StandardCharsets.UTF_8));
                stream.flush();
                stream.close();

                FileOutputStream stream2 = new FileOutputStream("./temp/" + fileId + "/main.m3u8");
                stream2.write(m3u8_sub.getBytes(StandardCharsets.UTF_8));
                stream2.flush();
                stream2.close();

                FileOutputStream stream3 = new FileOutputStream("./temp/" + fileId + "/video.m3u8");
                stream3.write(m3u8_video.getBytes(StandardCharsets.UTF_8));
                stream3.flush();
                stream3.close();

                FileOutputStream stream4 = new FileOutputStream("./temp/" + fileId + "/audio.m3u8");
                stream4.write(m3u8_audio.getBytes(StandardCharsets.UTF_8));
                stream4.flush();
                stream4.close();
            } catch (Exception e){
                //e.printStackTrace();
            }
        }

        return fileId+"/main.m3u8";
    }

}
