package com.pidev.models;

public class Answer {
    private Integer id;
    private Question question;
    private String content;
    private boolean isCorrect;

    public Answer() {
    }

    public Answer(int id) {
        this.id = id;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    @Override
    public String toString() {
        return content != null ? content : "Answer #" + id;
    }
}
