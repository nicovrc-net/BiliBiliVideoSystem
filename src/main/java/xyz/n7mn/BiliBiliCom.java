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

    public String run(String videoUrl, String audioUrl, String proxyAddress, int proxyPort){
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
            fileId = sb.substring(0, 16);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = !proxyAddress.isEmpty() ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort))).build() : new OkHttpClient();

        //System.out.println(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
        if (new File("./temp"+fileId).mkdir()){

            String[] split = videoUrl.split("\\?")[0].split("/");
            String fileName = split[split.length - 1];
            //System.out.println("video : "+ fileName);

            String[] split2 = audioUrl.split("\\?")[0].split("/");
            String fileName2 = split2[split2.length - 1];

            new Thread(()-> {

                Request request = new Request.Builder()
                        .url(videoUrl)
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    if (response.body() != null) {
                        FileOutputStream stream = new FileOutputStream("./temp" + fileId + "/" + fileName);
                        stream.write(response.body().bytes());
                        stream.flush();
                        stream.close();
                    }
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }).start();
            new Thread(()->{
                //System.out.println("audio : "+ fileName2);
                Request request2 = new Request.Builder()
                        .url(audioUrl)
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .build();
                try {
                    Response response2 = client.newCall(request2).execute();
                    if (response2.body() != null) {
                        FileOutputStream stream = new FileOutputStream("./temp" + fileId + "/" + fileName2);
                        stream.write(response2.body().bytes());
                        stream.flush();
                        stream.close();
                    }
                    response2.close();
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }).start();

            //System.out.println("DL待機 : " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
            boolean isOK = false;
            File file = new File("./temp" + fileId + "/" + fileName);
            File file1 = new File("./temp" + fileId + "/" + fileName2);
            while (!isOK){
                //System.out.println(new File("./temp"+fileId+"/"+fileName).exists() + " : " + new File("./temp"+fileId+"/"+fileName2).exists());
                isOK = file.exists() && file1.exists();
                if (file.length() == 0 || file1.length() == 0){
                    isOK = false;
                }
            }

            //System.out.println(isOK);
            //System.out.println("DL完了 : "+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));

            new File("./temp"+fileId).mkdir();
            new File("./temp/"+fileId).mkdir();
            String str = Main.getffmpegPass()+" -i ./temp"+fileId+"/"+fileName+" -i ./temp"+fileId+"/"+fileName2+" -c:v copy -c:a copy -f hls -hls_time 5 -hls_playlist_type vod -hls_segment_filename ./temp/"+fileId+"/%3d.ts ./temp/"+fileId+"/main.m3u8"; //./temp/"+fileId+".mp4";
            //System.out.println(str);
            new Thread(()->{
                try {
                    //System.out.println("変換処理開始 : "+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
                    Runtime runtime = Runtime.getRuntime();
                    Process exec = runtime.exec(str);
                    exec.waitFor();
                    //System.out.println("変換完了 : "+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
                    new File("./temp"+fileId+"/"+fileName).delete();
                    new File("./temp"+fileId+"/"+fileName2).delete();
                    new File("./temp"+fileId).delete();
                    //System.out.println("実施結果 : "+exec.waitFor());
                    //InputStream errorStream = exec.getErrorStream();
                    //InputStream inputStream = exec.getInputStream();
                    //System.out.println(new String(inputStream.readAllBytes()));
                    //System.out.println(new String(errorStream.readAllBytes()));
                } catch (Exception e) {
                    e.fillInStackTrace();
                }
            }).start();
            try {
                Thread.sleep(1500L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return fileId+"/main.m3u8";
    }

}
