package net.nicovrc.dev;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import net.nicovrc.dev.server.HTTPServer;
import net.nicovrc.dev.server.TCPServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {


    private static final Pattern matcher_VideoID = Pattern.compile("(\\d+)_(.+)");
    private static final HashMap<String, VideoData> TempList = new HashMap<>();

    public static void main(String[] args) {

        final String RedisServerIP;
        final int RedisServerPort;
        final String RedisPassword;
        final String SiteHostname;

        if (new File("./config.yml").exists()){
            try {
                YamlMapping ConfigYaml = Yaml.createYamlInput(new File("./config.yml")).readYamlMapping();
                RedisServerIP = ConfigYaml.string("RedisServerIP");
                RedisServerPort = ConfigYaml.integer("RedisServerPort");
                RedisPassword = ConfigYaml.string("RedisPassword");
                SiteHostname = ConfigYaml.string("SiteHostname");
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        } else {
            YamlMappingBuilder add = Yaml.createYamlMappingBuilder()
                    .add("RedisServerIP", "127.0.0.1")
                    .add("RedisServerPort", "6379")
                    .add("RedisPassword", "pass-word")
                    .add("SiteHostname", "b.nicovrc.net");
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

                JedisPool jedisPool = new JedisPool(RedisServerIP, RedisServerPort);
                Jedis jedis = jedisPool.getResource();
                if (!RedisPassword.isEmpty()){
                    jedis.auth(RedisPassword);
                }

                for (String key : jedis.keys("nico-bili:*")){
                    final Matcher matcher = matcher_VideoID.matcher(key);
                    if (matcher.find()){
                        long l = Long.parseLong(matcher.group(1));

                        if (new Date().getTime() - l >= 86400000L){
                            jedis.del(key);
                            TempList.remove(key.split(":")[1]);
                        }
                    }
                }

                jedis.close();
                jedisPool.close();


                System.gc();
            }
        }, 0L, 3600000L);


        new HTTPServer(TempList, RedisServerIP, RedisServerPort, RedisPassword).start();
        new TCPServer(RedisServerIP, RedisServerPort, RedisPassword).start();

    }
}