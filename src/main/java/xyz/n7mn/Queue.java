package xyz.n7mn;

import java.util.Date;

public class Queue {
    private String videoUrl;
    private Date date;

    public Queue(String videoUrl, Date date){
        this.videoUrl = videoUrl;;
        this.date = date;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public Date getDate() {
        return date;
    }
}
