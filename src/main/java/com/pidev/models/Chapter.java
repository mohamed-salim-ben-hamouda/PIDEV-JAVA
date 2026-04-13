package com.pidev.models;

public class Chapter {
    private Integer id;
    private Course course;
    private int chapterOrder;
    private String status;
    private float minScore;
    private String content;
    private String title;

    public Chapter() {
    }

    public Chapter(int id) {
        this.id = id;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public int getChapterOrder() { return chapterOrder; }
    public void setChapterOrder(int chapterOrder) { this.chapterOrder = chapterOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public float getMinScore() { return minScore; }
    public void setMinScore(float minScore) { this.minScore = minScore; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Override
    public String toString() {
        return title != null ? title : "Chapter #" + id;
    }
}
