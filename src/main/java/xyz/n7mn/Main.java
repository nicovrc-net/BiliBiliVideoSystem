package xyz.n7mn;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
                            Matcher matcher2 = Pattern.compile("GET /video/(.*) HTTP").matcher(text);

                            String httpVersion = "1.1";
                            if (matcher1.find()){
                                httpVersion = "1."+matcher1.group(1);
                            }

                            if (matcher2.find()){

                                File file = new File("./temp/" + matcher2.group(1).replaceAll("\\.\\./",""));
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