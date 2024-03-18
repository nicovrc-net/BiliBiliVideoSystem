package xyz.n7mn;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static String ffmpegPass = "";
    private static HashMap<String, Queue> queueList = new HashMap<>();


    public static void main(String[] args) {

        if (new File("./config.yml").exists()){
            try {
                YamlMapping ConfigYaml = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                ffmpegPass = ConfigYaml.string("ffmpegPass");
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        } else {
            YamlMappingBuilder add = Yaml.createYamlMappingBuilder()
                    .add("ffmpegPass", "/bin/ffmpeg");
            YamlMapping build = add.build();

            try {
                new File("./config.yml").createNewFile();
                PrintWriter writer = new PrintWriter("./config.yml");
                writer.print(build.toString());
                writer.close();

                System.out.println("[Info] config.ymlを設定してください。");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File file = new File("./temp/");
                for (File f : Objects.requireNonNull(file.listFiles())) {
                    Matcher matcher = Pattern.compile("(\\d+)_(.+)").matcher(f.getName());
                    if (matcher.find()){
                        Long l = Long.parseLong(matcher.group(1));
                        long time = new Date().getTime();

                        if ((time - l) >= 86400000L){
                            //System.out.println(time - l);
                            if (f.isFile()){
                                continue;
                            }

                            File[] files = f.listFiles();
                            for (File fi : files){
                                if (fi.getName().startsWith(".")){
                                    continue;
                                }

                                if (fi.isDirectory()){
                                    for (File fil : fi.listFiles()){
                                        if (fil.isDirectory()){
                                            for (File file1 : fi.listFiles()){
                                                file1.delete();
                                            }
                                            fi.delete();
                                        } else {
                                            fil.delete();
                                        }
                                    }
                                    fi.delete();
                                } else {
                                    fi.delete();
                                }

                                f.delete();
                            }
                            f.delete();
                        }
                    }
                }
            }
        }, 0L, 10000L);

        // HTTP通信を受け取る
        new Thread(()->{
            ServerSocket socket = null;
            try {
                socket = new ServerSocket(28280);
                while (true) {
                    Socket sock = socket.accept();
                    System.gc();
                    new Thread(() -> {
                        try {
                            InputStream in = sock.getInputStream();
                            OutputStream out = sock.getOutputStream();
                            byte[] data = new byte[100000000];

                            int readSize = in.read(data);
                            data = Arrays.copyOf(data, readSize);
                            String text = new String(data, StandardCharsets.UTF_8);
                            Matcher matcher1 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);

                            Matcher matcher2_1 = Pattern.compile("(GET|HEAD) (.+) HTTP").matcher(text);
                            Matcher matcher2_2 = Pattern.compile("GET /video/(.*) HTTP").matcher(text);

                            String httpVersion = "1.1";
                            if (matcher1.find()){
                                httpVersion = "1."+matcher1.group(1);
                            }

                            boolean match1 = matcher2_1.find();
                            boolean match2 = matcher2_2.find();

                            if (match1 && !match2){
                                //System.out.println("!");
                                Matcher matcher = Pattern.compile("&com=true").matcher(text);
                                if (matcher.find()){
                                    OkHttpClient client = new OkHttpClient();
                                    String[] split = matcher2_1.group(2).split("&com=true");

                                    Request request;
                                    if (split.length != 2) {
                                         request = new Request.Builder()
                                                .url("https://upos-sz-mirroraliov.bilivideo.com" + split[0])
                                                .addHeader("Referer", "https://www.bilibili.com/")
                                                .build();
                                    } else {
                                        request = new Request.Builder()
                                                .url("https://upos-hz-mirrorakam.akamaized.net" + split[0])
                                                .addHeader("Referer", "https://www.bilibili.com/")
                                                .build();
                                    }


                                    Response response = client.newCall(request).execute();
                                    if (response.body() != null) {
                                        //System.out.println(response.code());
                                        if (response.code() == 200){
                                            out.write(("HTTP/" + httpVersion + " 200 OK\r\n" +
                                                    "Content-Type: "+response.header("Content-Type")+"\r\n" +
                                                    "\r\n").getBytes(StandardCharsets.UTF_8));

                                            out.write(response.body().bytes());
                                            out.flush();
                                        } else {
                                            out.write(("HTTP/" + httpVersion + " 404 Not Found\r\n" +
                                                    "Content-Type: text/plain\r\n" +
                                                    "\r\n" +
                                                    "404").getBytes(StandardCharsets.UTF_8));
                                            out.flush();
                                        }
                                    }
                                    response.close();
                                }

                                in.close();
                                out.close();
                                sock.close();

                                return;
                            }

                            if (match2){

                                File file = new File("./temp/" + matcher2_2.group(1).replaceAll("\\.\\./",""));
                                //System.out.println("./temp/" + matcher2.group(1).replaceAll("\\.\\./",""));

                                String ContentType = "application/octet-stream";
                                if (file.getName().endsWith("m3u8")){
                                    ContentType = "application/vnd.apple.mpegurl";
                                }
                                if (file.getName().endsWith("ts")){
                                    ContentType = "video/mp2t";
                                }
                                if (file.getName().endsWith("mp4")){
                                    ContentType = "video/mp4";
                                }

                                if (!file.exists()){
                                    out.write(("HTTP/"+httpVersion+" 404 Not Found\n" +
                                            "Content-Type: text/plain\n" +
                                            "\n404").getBytes(StandardCharsets.UTF_8));
                                } else {
                                    FileInputStream stream = new FileInputStream(file);

                                    out.write(("HTTP/" + httpVersion + " 200 OK\r\n" +
                                            "Date: " + new Date() + "\r\n" +
                                            "Content-Type: "+ContentType+"\r\n" +
                                            "\r\n").getBytes(StandardCharsets.UTF_8));

                                    out.write(stream.readAllBytes());

                                    stream.close();

                                }
                                out.flush();
                            } else {
                                out.write(("HTTP/" + httpVersion + " 405 Method Not Allowed").getBytes(StandardCharsets.UTF_8));
                                out.flush();
                            }
                            in.close();
                            out.close();
                            sock.close();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }).start();

        // 処理受付
        ServerSocket socket2 = null;
        try {
            socket2 = new ServerSocket(28279);
            while (true) {
                //System.out.println("a");
                Socket sock = socket2.accept();
                System.gc();
                new Thread(() -> {
                    try {
                        InputStream in = sock.getInputStream();
                        OutputStream out = sock.getOutputStream();
                        byte[] data = new byte[100000000];

                        int readSize = in.read(data);
                        data = Arrays.copyOf(data, readSize);

                        byte[] bytes = data;
                        if (bytes.length == 0){
                            sock.close();
                            return;
                        }

                        //System.out.println(new String(bytes, StandardCharsets.UTF_8));

                        RequestJson json = new Gson().fromJson(new String(bytes, StandardCharsets.UTF_8), RequestJson.class);

                        String url;
                        if (json.getSiteType().equals("com")){
                            if (json.getProxy() != null){
                                url = new BiliBiliCom().run(json.getVideoURL(), json.getAudioURL(), json.getVideoDuration(), json.getProxy().split(":")[0], Integer.parseInt(json.getProxy().split(":")[1]));
                            } else {
                                url = new BiliBiliCom().run(json.getVideoURL(), json.getAudioURL(), json.getVideoDuration(), "", 0);
                            }
                        } else {
                            if (json.getProxy() != null){
                                url = new BiliBiliTv().run(json.getVideoURL(), json.getAudioURL(), json.getProxy().split(":")[0], Integer.parseInt(json.getProxy().split(":")[1]));
                            } else {
                                url = new BiliBiliTv().run(json.getVideoURL(), json.getAudioURL(), "", 0);
                            }
                        }

                        url = "https://b.nicovrc.net/video/" + url;

                        out.write(url.getBytes());
                        out.flush();

                        //System.out.println("b : " + url);
                        in.close();
                        out.close();
                        sock.close();

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public static String getffmpegPass(){
        return ffmpegPass;
    }
}