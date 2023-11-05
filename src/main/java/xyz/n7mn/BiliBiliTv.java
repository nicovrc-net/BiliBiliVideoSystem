package xyz.n7mn;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public class BiliBiliTv {

    public String run(String videoUrl, String audioUrl, String proxyAddress, int proxyPort){

        if (!new File("./temp").exists()){
            System.out.println(new File("./temp").mkdir());
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

        new File("./temp/"+fileId).mkdir();
        String str = Main.getffmpegPass()+" -i "+videoUrl+" -i "+audioUrl+" -c:v copy -c:a copy -f hls -hls_time 5 -hls_playlist_type vod -hls_segment_filename ./temp/"+fileId+"/%3d.ts ./temp/"+fileId+"/main.m3u8";
        System.out.println(str);

        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(str);
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return fileId+"/main.m3u8";
    }

}
