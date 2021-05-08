package com.saisumanth.documenttoaudioconverter;

import com.google.firebase.Timestamp;

public class Item {

    private String filename;
    private String link;
    private Timestamp time;
    private String userid;

    public Item(String filename, String link, Timestamp time, String userid) {
        this.filename = filename;
        this.link = link;
        this.time = time;
        this.userid = userid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    @Override
    public String toString() {
        return "Item{" +
                "filename='" + filename + '\'' +
                ", link='" + link + '\'' +
                ", time=" + time +
                ", userid='" + userid + '\'' +
                '}';
    }
}