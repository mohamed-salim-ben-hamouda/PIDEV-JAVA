package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceSponsorHackathon;
import com.pidev.models.SponsorHackathon;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class SponsorHackathonAdminController implements Initializable {

    @FXML private TableView<SponsorHackathon> relationTable;
    @FXML private TableColumn<SponsorHackathon, String> colHackathon, colSponsor;
    @FXML private TableColumn<SponsorHackathon, Void> colActions;
    @FXML private TextField searchField;

    private ServiceSponsorHackathon serviceSH = new ServiceSponsorHackathon();
    private ObservableList<SponsorHackathon> relationList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadData();
        setupSearch();
    }

    private void setupColumns() {
        colHackathon.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getHackathon().getTitle()));
        colSponsor.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSponsor().getName()));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button();
            private final HBox container = new HBox(10, deleteBtn);

            {
                deleteBtn.getStyleClass().add("action-btn-delete");
                deleteBtn.setGraphic(new FontIcon("fas-trash"));
                deleteBtn.setOnAction(event -> {
                    SponsorHackathon sh = getTableView().getItems().get(getIndex());
                    handleDelete(sh);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadData() {
        relationList.setAll(serviceSH.getAll());
        relationTable.setItems(relationList);
    }

    private void setupSearch() {
        FilteredList<SponsorHackathon> filteredData = new FilteredList<>(relationList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(sh -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return sh.getHackathon().getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       sh.getSponsor().getName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        relationTable.setItems(filteredData);
    }

    @FXML
    private void showAddForm() {
        try {
            Parent form = FXMLLoader.load(getClass().getResource("/Fxml/admin/sponsor_hackathon_form.fxml"));
            StackPane contentArea = (StackPane) relationTable.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(SponsorHackathon sh) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Assignment");
        alert.setHeaderText("Remove sponsor from hackathon?");
        alert.setContentText(sh.getSponsor().getName() + " -> " + sh.getHackathon().getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceSH.delete(sh.getId());
            loadData();
        }
    }
}
