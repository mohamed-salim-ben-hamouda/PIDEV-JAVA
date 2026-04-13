package com.pidev.models;

public class Question {
    private Integer id;
    private Quiz quiz;
    private String content;
    private String type;
    private float point;

    public Question() {
    }

    public Question(int id) {
        this.id = id;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public float getPoint() { return point; }
    public void setPoint(float point) { this.point = point; }

    @Override
    public String toString() {
        if (content == null || content.isBlank()) {
            return "Question #" + id;
        }
        return content.length() > 40 ? content.substring(0, 37) + "..." : content;
    }
}
