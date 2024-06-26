package net.nicovrc.dev.server;

import com.google.gson.Gson;
import net.nicovrc.dev.VideoData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPServer extends Thread{
    private final Pattern matcher_httpVersion = Pattern.compile("HTTP/(\\d)\\.(\\d)");
    private final Pattern matcher_Request = Pattern.compile("(GET|HEAD) (.+) HTTP");
    private final Pattern matcher_VideoID = Pattern.compile("(\\d+)_(.+)");
    private final Pattern matcher_resoniteUA = Pattern.compile("[uU]ser-[aA]gent: (VLC|vlc|Vlc|vLc|vlC|VLc|VlC|vLC)");

    private final HashMap<String, VideoData> TempList;
    private final String RedisServerIP;
    private final int RedisServerPort;
    private final String RedisPassword;

    public HTTPServer(HashMap<String, VideoData> TempList, String RedisServerIP, int RedisServerPort, String RedisPassword){
        this.TempList = TempList;
        this.RedisServerIP = RedisServerIP;
        this.RedisServerPort = RedisServerPort;
        this.RedisPassword = RedisPassword;
    }

    @Override
    public void run() {

        ServerSocket socket = null;
        try {
            socket = new ServerSocket(28280);
            while (true) {
                System.out.println("TCP Port 28280でHTTPサーバー受付開始");
                try {
                    final Socket sock = socket.accept();

                    final InputStream in = sock.getInputStream();
                    final OutputStream out = sock.getOutputStream();

                    byte[] data = new byte[1024768];
                    int readSize = in.read(data);
                    if (readSize <= 0) {
                        sock.close();
                        return;
                    }
                    data = Arrays.copyOf(data, readSize);

                    final String httpRequest = new String(data, StandardCharsets.UTF_8);

                    System.out.println(httpRequest);

                    final Matcher matcher1 = matcher_httpVersion.matcher(httpRequest);
                    final String httpVersion = matcher1.find() ? matcher1.group(1) + "." + matcher1.group(2) : "1.1";

                    final Matcher matcher2 = matcher_Request.matcher(httpRequest);

                    if (matcher2.find()) {
                        final String Mode = matcher2.group(1);
                        final String URI = matcher2.group(2);

                        final Matcher matcher = matcher_VideoID.matcher(URI);
                        final String tempId = matcher.find() ? matcher.group(1) + "_" + matcher.group(2) : null;
                        VideoData videoData = null;
                        if (tempId != null){
                            videoData = TempList.get(tempId);
                            if (videoData == null){
                                JedisPool jedisPool = new JedisPool(RedisServerIP, RedisServerPort);
                                Jedis jedis = jedisPool.getResource();
                                if (!RedisPassword.isEmpty()){
                                    jedis.auth(RedisPassword);
                                }
                                String s = jedis.get("nico-bili:" + tempId);
                                if (s != null && !s.isEmpty()){
                                    videoData = new Gson().fromJson(s, VideoData.class);
                                }
                                jedis.close();
                                jedisPool.close();
                                TempList.put(tempId, videoData);
                            }
                        }

                        if (URI.toLowerCase(Locale.ROOT).startsWith("/video") && videoData == null){
                            out.write(("HTTP/"+httpVersion+" 404 Not Found\nAccess-Control-Allow-Origin: *\nContent-Type: text/plain; charset=utf-8\n\n").getBytes(StandardCharsets.UTF_8));
                            if (Mode.equals("GET")){
                                out.write("404".getBytes(StandardCharsets.UTF_8));
                            }
                            in.close();
                            out.close();
                            sock.close();

                            return;
                        }

                        final String[] split1 = URI.split("&hostc=");
                        final String[] split2 = URI.split("&hostv=");

                        if (URI.toLowerCase(Locale.ROOT).startsWith("/video") && videoData != null){

                            if (matcher.find()){
                                final String m3u8;
                                if (URI.toLowerCase(Locale.ROOT).startsWith("/video/main.m3u8")){
                                    if (videoData.isVRC() || matcher_resoniteUA.matcher(httpRequest).find()){
                                        m3u8 = "#EXTM3U\n" +
                                                "#EXT-X-VERSION:6\n" +
                                                "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"/video/"+tempId+"/audio.m3u8\"\n" +
                                                "#EXT-X-STREAM-INF:AUDIO=\"audio\"\n" +
                                                "/video/"+tempId+"/sub.m3u8";
                                    } else {
                                        m3u8 = "#EXTM3U\n" +
                                                "#EXT-X-VERSION:6\n" +
                                                "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"/video/"+tempId+"/audio.m3u8\"\n" +
                                                "#EXT-X-STREAM-INF:AUDIO=\"audio\"\n" +
                                                "/video/"+tempId+"/video.m3u8";
                                    }

                                    out.write(("HTTP/"+httpVersion+" 200 OK\nAccess-Control-Allow-Origin: *\nContent-Type: application/vnd.apple.mpegurl;\n\n").getBytes(StandardCharsets.UTF_8));
                                    if (Mode.equals("GET")){
                                        out.write(m3u8.getBytes(StandardCharsets.UTF_8));
                                    }
                                    out.flush();
                                    in.close();
                                    out.close();
                                    sock.close();
                                    return;
                                } else if (URI.toLowerCase(Locale.ROOT).startsWith("/video/sub.m3u8")){
                                    m3u8 = "#EXTM3U\n" +
                                            "#EXT-X-VERSION:6\n" +
                                            "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                                            "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Main Audio\",DEFAULT=YES,URI=\"/video/"+tempId+"/audio.m3u8\"\n" +
                                            "#EXT-X-STREAM-INF:AUDIO=\"audio\"\n" +
                                            "/video/"+tempId+"/video.m3u8";

                                    out.write(("HTTP/"+httpVersion+" 200 OK\nAccess-Control-Allow-Origin: *\nContent-Type: application/vnd.apple.mpegurl;\n\n").getBytes(StandardCharsets.UTF_8));
                                    if (Mode.equals("GET")){
                                        out.write(m3u8.getBytes(StandardCharsets.UTF_8));
                                    }
                                    out.flush();
                                    in.close();
                                    out.close();
                                    sock.close();
                                    return;
                                } else if (URI.toLowerCase(Locale.ROOT).startsWith("/video/video.m3u8")){
                                    m3u8 = "#EXT-X-VERSION:6\n" +
                                            "#EXT-X-TARGETDURATION:0\n" +
                                            "#EXT-X-PLAYLIST-TYPE:VOD\n" +
                                            "#EXT-X-MEDIA-SEQUENCE:1\n" +
                                            "#EXTINF:"+videoData.getVideoDuration()+",\n" +
                                            videoData.getVideoURI()+"&hostc=" + videoData.getHostname() + "\n" +
                                            "#EXT-X-ENDLIST";

                                    out.write(("HTTP/"+httpVersion+" 200 OK\nAccess-Control-Allow-Origin: *\nContent-Type: application/vnd.apple.mpegurl;\n\n").getBytes(StandardCharsets.UTF_8));
                                    if (Mode.equals("GET")){
                                        out.write(m3u8.getBytes(StandardCharsets.UTF_8));
                                    }
                                    out.flush();
                                    in.close();
                                    out.close();
                                    sock.close();
                                    return;
                                } else if (URI.toLowerCase(Locale.ROOT).startsWith("/video/audio.m3u8")){
                                    m3u8 = "#EXT-X-VERSION:6\n" +
                                            "#EXT-X-TARGETDURATION:0\n" +
                                            "#EXT-X-PLAYLIST-TYPE:VOD\n" +
                                            "#EXT-X-MEDIA-SEQUENCE:1\n" +
                                            "#EXTINF:"+videoData.getVideoDuration()+",\n" +
                                            videoData.getAudioURI()+"&hostc=" + videoData.getHostname() + "\n" +
                                            "#EXT-X-ENDLIST";

                                    out.write(("HTTP/"+httpVersion+" 200 OK\nAccess-Control-Allow-Origin: *\nContent-Type: application/vnd.apple.mpegurl;\n\n").getBytes(StandardCharsets.UTF_8));
                                    if (Mode.equals("GET")){
                                        out.write(m3u8.getBytes(StandardCharsets.UTF_8));
                                    }
                                    out.flush();
                                    in.close();
                                    out.close();
                                    sock.close();
                                    return;
                                } else {

                                    out.write(("HTTP/"+httpVersion+" 404 Not Found\nAccess-Control-Allow-Origin: *\nContent-Type: text/plain; charset=utf-8\n\n").getBytes(StandardCharsets.UTF_8));
                                    if (Mode.equals("GET")){
                                        out.write("404".getBytes(StandardCharsets.UTF_8));
                                    }
                                    in.close();
                                    out.close();
                                    sock.close();

                                    return;
                                }
                            }
                        } else if (split1.length == 2) {

                            final OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://" + split1[1] + split1[0])
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0")
                                    .addHeader("Referer", "https://www.bilibili.com/")
                                    .build();
                            Response response = client.newCall(request).execute();
                            if (response.body() != null) {
                                if (response.code() == 200) {
                                    out.write(("HTTP/" + httpVersion + " 200 OK\n" +
                                            "Access-Control-Allow-Origin: *\n" +
                                            "Content-Type: " + response.header("Content-Type") + "\n" +
                                            "\n").getBytes(StandardCharsets.UTF_8));

                                    if (Mode.equals("GET")) {
                                        out.write(response.body().bytes());
                                    }
                                } else {
                                    out.write(("HTTP/" + httpVersion + " 404 Not Found\nAccess-Control-Allow-Origin: *\nContent-Type: text/plain; charset=utf-8\n\n").getBytes(StandardCharsets.UTF_8));
                                    if (Mode.equals("GET")) {
                                        out.write("404".getBytes(StandardCharsets.UTF_8));
                                    }
                                }
                                out.flush();
                                in.close();
                                out.close();
                                sock.close();
                            }
                            response.close();
                            return;
                        } else if (split2.length == 2){

                            final OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://" + split2[1] + split2[0])
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0")
                                    .addHeader("Referer", "https://www.bilibili.tv/")
                                    .build();
                            Response response = client.newCall(request).execute();
                            if (response.body() != null) {
                                if (response.code() == 200){
                                    out.write(("HTTP/" + httpVersion + " 200 OK\n" +
                                            "Access-Control-Allow-Origin: *\n" +
                                            "Content-Type: "+response.header("Content-Type")+"\n" +
                                            "\n").getBytes(StandardCharsets.UTF_8));

                                    if (Mode.equals("GET")){
                                        out.write(response.body().bytes());
                                    }
                                } else {
                                    out.write(("HTTP/"+httpVersion+" 404 Not Found\nAccess-Control-Allow-Origin: *\nContent-Type: text/plain; charset=utf-8\n\n").getBytes(StandardCharsets.UTF_8));
                                    if (Mode.equals("GET")){
                                        out.write("404".getBytes(StandardCharsets.UTF_8));
                                    }
                                }
                                out.flush();
                                in.close();
                                out.close();
                                sock.close();
                            }
                            response.close();
                            return;

                        } else {
                            out.write(("HTTP/"+httpVersion+" 404 Not Found\nAccess-Control-Allow-Origin: *\nContent-Type: text/plain; charset=utf-8\n\n").getBytes(StandardCharsets.UTF_8));
                            if (Mode.equals("GET")){
                                out.write("404".getBytes(StandardCharsets.UTF_8));
                            }
                            in.close();
                            out.close();
                            sock.close();

                            return;
                        }

                    }


                    sock.close();
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e){
            if (socket != null){
                try {
                    socket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            e.printStackTrace();
        }

    }
}
