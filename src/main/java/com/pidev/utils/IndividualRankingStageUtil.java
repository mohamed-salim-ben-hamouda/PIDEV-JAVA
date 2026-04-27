package com.pidev.utils;

import com.pidev.models.IndividualRankingEntry;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;

public final class IndividualRankingStageUtil {
    private IndividualRankingStageUtil() {
    }

    public static void showRanking(Window owner,
                                   String challengeTitle,
                                   List<IndividualRankingEntry> ranking,
                                   int currentUserId) throws IOException {
        FXMLLoader loader = new FXMLLoader(IndividualRankingStageUtil.class.getResource("/Fxml/utils/IndividualRankingStage.fxml"));
        Scene scene = new Scene(loader.load());

        IndividualRankingStageController controller = loader.getController();
        controller.setData(challengeTitle, ranking, currentUserId);

        Stage stage = new Stage();
        stage.setTitle("Individual Ranking");
        stage.initModality(Modality.NONE);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setMinWidth(760);
        stage.setMinHeight(620);
        stage.setWidth(860);
        stage.setHeight(680);
        stage.setScene(scene);
        stage.show();
    }
}
