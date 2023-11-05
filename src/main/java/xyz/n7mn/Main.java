package xyz.n7mn;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;

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
                        Matcher matcher1 = Pattern.compile("GET /\\?url=(.*) HTTP").matcher(text);
                        Matcher matcher2 = Pattern.compile("HTTP/1\\.(\\d)").matcher(text);
                        Matcher matcher3 = Pattern.compile("GET /video/(.*) HTTP").matcher(text);

                        String httpVersion = "1.1";
                        if (matcher2.find()){
                            httpVersion = "1."+matcher2.group(1);
                        }

                        if (matcher1.find()){

                            String str = matcher1.group(1);
                            String url = str.split("&cc&")[0];
                            String mode = "";
                            if (str.split("&cc&").length >= 2){
                                mode = str.split("&cc&")[1];
                            }
                            String proxy = ":";
                            if (str.split("&cc&").length == 3){
                                proxy = str.split("&cc&")[2];
                            }

                            String[] temp = url.split(",,");
                            String[] temp2;
                            if (!proxy.equals(":")){
                                temp2 = proxy.split(":");
                            } else {
                                temp2 = new String[]{"","0"};
                            }

                            String run = "";
                            if (mode.equals("com")) {
                                run = new BiliBiliCom().run(temp[0], temp[1], temp2[0], Integer.parseInt(temp2[1]));
                            } else {
                                run = new BiliBiliTv().run(temp[0], temp[1], temp2[0], Integer.parseInt(temp2[1]));
                            }

                            queueList.put(url, new Queue(url, new Date()));

                            out.write(("HTTP/" + httpVersion + " 200 OK\n" +
                                    "Content-Type: text/plain\n" +
                                    "Date: " + new Date() + "\n\n" +
                                    "/video/" + run).replaceAll("\0", "").getBytes(StandardCharsets.UTF_8));
                            out.flush();
                            in.close();
                            out.close();
                            sock.close();

                            return;
                        }

                        if (matcher3.find()){

                            File file = new File("./temp/" + matcher3.group(1).replaceAll("\\.\\./",""));
                            //System.out.println("./temp/" + matcher3.group(1).replaceAll("\\.\\./",""));

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
    }

    public static String getffmpegPass(){
        return ffmpegPass;
    }
}