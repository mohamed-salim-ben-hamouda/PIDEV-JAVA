package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MemberGitCardController {
    @FXML
    private Label memberName;
    @FXML
    private TextField gitUserName;
    @FXML
    private Button submitGitBtn;
    private Runnable onUpdateCallback;
    private User u;
    private ServiceActivity serviceAct = new ServiceActivity();
    public void initData(User user,Runnable onUpdate){
        this.u=user;
        this.onUpdateCallback=onUpdate;
        memberName.setText(u.getPrenom() + " " + u.getNom());
        if (u.getGit_username() != null && !u.getGit_username().isBlank()) {
            gitUserName.setText(u.getGit_username());
            submitGitBtn.setText("Modify");
        } else {
            submitGitBtn.setText("Add");
        }
    }
    @FXML
    public void onSubmitGitBtn(){
        String git_username = gitUserName.getText();
        try {
            serviceAct.updateGitUserName(u,git_username);
            u.setGit_username(git_username);
            submitGitBtn.setText("Modify");
            gitUserName.setText(u.getGit_username());
            if (onUpdateCallback != null) {
                onUpdateCallback.run();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
