package com.pidev.models;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private Integer id;
    private Quiz quiz;
    private String content;
    private String type;
    private float point;
    private List<Answer> answers;
    private List<Object> studentResponses;

    public Question() {
        this.answers = new ArrayList<>();
        this.studentResponses = new ArrayList<>();
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

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    public List<Object> getStudentResponses() { return studentResponses; }
    public void setStudentResponses(List<Object> studentResponses) { this.studentResponses = studentResponses; }

    @Override
    public String toString() {
        if (content == null || content.isBlank()) {
            return "Question #" + id;
        }
        return content.length() > 40 ? content.substring(0, 37) + "..." : content;
    }
}
