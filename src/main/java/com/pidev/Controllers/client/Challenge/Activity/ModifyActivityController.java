package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.models.Activity;
import com.pidev.models.MemberActivity;
import com.pidev.models.ProblemSolution;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class ModifyActivityController {
    @FXML
    private VBox mainActivityContainer;
    private Activity a;
    ServiceMemberActivity serviceMA = new ServiceMemberActivity();

    public void initData(Activity a) {
        this.a = a;
        if (a != null) {
            loadMemberActivity();
        }


    }

    public void loadMemberActivity() {
        mainActivityContainer.getChildren().clear();
        List<MemberActivity> ma = serviceMA.SelectMActivity(a.getId());
        for (MemberActivity m_activity : ma) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/MembersActivityModifCards.fxml"));
                VBox card = loader.load();
                MembersActivityModifCardsController controller = loader.getController();
                controller.initData(m_activity, () -> {
                });
                mainActivityContainer.getChildren().add(card);


            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }
    }
}
