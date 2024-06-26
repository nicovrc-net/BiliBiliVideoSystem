package xyz.n7mn;

import java.util.regex.Pattern;

public class Main {

    private static final Pattern matcher_httpVersion = Pattern.compile("HTTP/1\\.(\\d)");
    private static final Pattern matcher_Request = Pattern.compile("(GET|HEAD) (.+) HTTP");
    private static final Pattern matcher_Request2 = Pattern.compile("(GET|HEAD) /video/(.+) HTTP");
    private static final Pattern matcher_resoniteUA = Pattern.compile("[uU]ser-[aA]gent: (VLC|vlc)");
    private static final Pattern matcher_mode_com = Pattern.compile("&com=true");


    public static void main(String[] args) {



    }
}