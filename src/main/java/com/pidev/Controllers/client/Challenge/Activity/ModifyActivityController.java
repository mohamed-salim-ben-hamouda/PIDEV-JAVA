package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.Activity;
import com.pidev.models.MemberActivity;
import com.pidev.models.ProblemSolution;
import com.pidev.utils.OpenPdfUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class ModifyActivityController {
    @FXML
    private VBox mainActivityContainer;
    @FXML VBox problemsModifContainer;
    @FXML
    private Button newFileBtn;
    private Activity a;
    ServiceMemberActivity serviceMA = new ServiceMemberActivity();
    ServiceProblemSolution servicePS = new ServiceProblemSolution();
    private File selectedPdf;
    ServiceActivity serviceA = new ServiceActivity();


    public void initData(Activity a) {
        this.a = a;
        if (a != null) {
            loadMemberActivity();
            loadProblemSolutions();
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
    public void loadProblemSolutions(){
        problemsModifContainer.getChildren().clear();
        List<ProblemSolution> ps = servicePS.display(a.getId());
        for (ProblemSolution problem_solution:ps){
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/ActivityModifProblemSolutionCards.fxml"));
                VBox card = loader.load();
                ActivityModifProblemSolutionCardsController controller = loader.getController();
                controller.initData(problem_solution,() -> {});
                problemsModifContainer.getChildren().add(card);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
    @FXML
    public void openPdfSubmission(){
        if(a == null || a.getSubmissionFile() == null || a.getSubmissionFile().isBlank()){
            showError("No Submission file is available for this activity yet.");
            return;
        }
        try {
            OpenPdfUtil.openPdfInApp(a.getSubmissionFile(), "Submission PDF");
        } catch (IOException ex) {
            showError("Could not open submission PDF:\n" + ex.getMessage());
        }
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Open PDF");
        alert.setHeaderText("Unable to open feedback PDF");
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    public void onChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            newFileBtn.setText(selectedPdf.getName());
        }
    }
    public void onUpdateFile(){
        try {
            Path destDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "activity_pdf");
            Files.createDirectories(destDir);
            Path destFile = destDir.resolve(selectedPdf.getName());
            Files.copy(selectedPdf.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            a.setSubmissionFile("challenge_module/activity_pdf/"+selectedPdf.getName());
            serviceA.updateActivityFile(a);
            BaseController.getInstance().loadOldActivities();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
