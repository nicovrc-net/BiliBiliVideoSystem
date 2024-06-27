package net.nicovrc.dev.server;

import com.google.gson.Gson;
import net.nicovrc.dev.RequestJson;
import net.nicovrc.dev.VideoData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class TCPServer extends Thread{

    private final String RedisServerIP;
    private final int RedisServerPort;
    private final String RedisPassword;
    private final HashMap<String, VideoData> TempList;
    private final String SiteHostname;

    public TCPServer(HashMap<String, VideoData> TempList, String RedisServerIP, int RedisServerPort, String RedisPassword, String SiteHostname){
        this.TempList = TempList;
        this.RedisServerIP = RedisServerIP;
        this.RedisServerPort = RedisServerPort;
        this.RedisPassword = RedisPassword;
        this.SiteHostname = SiteHostname;
    }

    @Override
    public void run() {

        ServerSocket socket = null;
        try {
            socket = new ServerSocket(28279);
            System.out.println("TCP Port 28279で受付サーバー受付開始");
            while (true) {
                try {
                    final Socket sock = socket.accept();

                    final InputStream in = sock.getInputStream();
                    final OutputStream out = sock.getOutputStream();

                    byte[] data = new byte[1024768];
                    int readSize = in.read(data);
                    if (readSize <= 0) {
                        sock.close();
                        continue;
                    }
                    data = Arrays.copyOf(data, readSize);

                    final String request = new String(data, StandardCharsets.UTF_8);

                    //System.out.println(request);

                    final RequestJson json;
                    try {
                        json = new Gson().fromJson(request, RequestJson.class);
                    } catch (Exception e){
                        sock.close();
                        continue;
                    }

                    final VideoData videoData = new VideoData();
                    videoData.setVRC(json.isVRC());
                    videoData.setHostname(json.getVideoURL().split("/")[2]);
                    videoData.setBilibiliType(json.getSiteType());
                    final StringBuffer sb1 = new StringBuffer("/");
                    final StringBuffer sb2 = new StringBuffer("/");
                    int i = 0;
                    for (String str : json.getVideoURL().split("/")){
                        if (i < 3){
                            i++;
                            continue;
                        }

                        sb1.append(str).append("/");

                        i++;
                    }
                    i = 0;
                    for (String str : json.getAudioURL().split("/")){
                        if (i < 3){
                            i++;
                            continue;
                        }

                        sb2.append(str).append("/");

                        i++;
                    }
                    videoData.setVideoURI(sb1.toString());
                    videoData.setAudioURI(sb2.toString());
                    videoData.setVideoDuration(json.getVideoDuration());
                    final String id = new Date().getTime() + "_" + UUID.randomUUID().toString().split("-")[0];

                    TempList.put(id, videoData);
                    new Thread(()->{
                        JedisPool jedisPool = new JedisPool(RedisServerIP, RedisServerPort);
                        Jedis jedis = jedisPool.getResource();
                        if (!RedisPassword.isEmpty()){
                            jedis.auth(RedisPassword);
                        }
                        jedis.set("nico-bili:" + id, new Gson().toJson(videoData));
                        jedis.close();
                        jedisPool.close();
                    }).start();

                    //System.out.println(new Gson().toJson(videoData));
                    //System.out.println("https://" + SiteHostname + "/video/" + id + "/main.m3u8");
                    out.write(("https://" + SiteHostname + "/video/" + id + "/main.m3u8").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    in.close();
                    out.close();

                    sock.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ex) {
                    //ex.printStackTrace();
                }
            }
        }

    }

}
