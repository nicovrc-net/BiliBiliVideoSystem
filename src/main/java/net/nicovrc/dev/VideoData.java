package net.nicovrc.dev;

public class VideoData {

    private String VideoURI;
    private String AudioURI;
    private String Hostname;
    private Long VideoDuration;
    private boolean isVRC;
    private String bilibiliType;

    public String getVideoURI() {
        return VideoURI;
    }

    public void setVideoURI(String videoURI) {
        VideoURI = videoURI;
    }

    public String getAudioURI() {
        return AudioURI;
    }

    public void setAudioURI(String audioURI) {
        AudioURI = audioURI;
    }

    public String getHostname() {
        return Hostname;
    }

    public void setHostname(String hostname) {
        Hostname = hostname;
    }

    public Long getVideoDuration() {
        return VideoDuration;
    }

    public void setVideoDuration(Long videoDuration) {
        VideoDuration = videoDuration;
    }

    public boolean isVRC() {
        return isVRC;
    }

    public void setVRC(boolean VRC) {
        isVRC = VRC;
    }

    public String getBilibiliType() {
        return bilibiliType;
    }

    public void setBilibiliType(String bilibiliType) {
        this.bilibiliType = bilibiliType;
    }
}
