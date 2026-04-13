package com.pidev.Controllers.admin.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.models.Evaluation;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EvaluationBackofficeController implements Initializable {
    @FXML
    private VBox EvaluationCardsContainer;
    private ServiceEvaluation serviceEva=new ServiceEvaluation();
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        displayEvaluation();
    }

    public void displayEvaluation(){
        EvaluationCardsContainer.getChildren().clear();
        List<Evaluation> evaluations = serviceEva.displayAll();
        for (Evaluation e : evaluations){
            try{
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/Evaluation/EvaluationBackCards.fxml"));
                HBox card = loader.load();
                EvaluationBackCardsController cardController = loader.getController();
                cardController.initData(e,v->displayEvaluation());
                EvaluationCardsContainer.getChildren().add(card);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

    }
}
