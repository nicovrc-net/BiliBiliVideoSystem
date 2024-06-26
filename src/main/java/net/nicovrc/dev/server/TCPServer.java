package net.nicovrc.dev.server;

import net.nicovrc.dev.VideoData;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TCPServer extends Thread{

    private final HashMap<String, VideoData> TempList;
    private final String RedisServerIP;
    private final int RedisServerPort;
    private final String RedisPassword;

    public TCPServer(HashMap<String, VideoData> TempList, String RedisServerIP, int RedisServerPort, String RedisPassword){
        this.TempList = TempList;
        this.RedisServerIP = RedisServerIP;
        this.RedisServerPort = RedisServerPort;
        this.RedisPassword = RedisPassword;
    }

    @Override
    public void run() {

        ServerSocket socket = null;
        try {
            socket = new ServerSocket(28279);
            while (true) {
                System.out.println("TCP Port 28279で受付サーバー受付開始");
                try {
                    final Socket sock = socket.accept();


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
