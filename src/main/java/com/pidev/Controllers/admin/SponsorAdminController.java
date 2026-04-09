package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceSponsor;
import com.pidev.models.Sponsor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

public class SponsorAdminController implements Initializable {

    @FXML private TableView<Sponsor> sponsorTable;
    @FXML private TableColumn<Sponsor, String> colName, colWebsite;
    @FXML private TableColumn<Sponsor, LocalDateTime> colCreatedAt;
    @FXML private TableColumn<Sponsor, Void> colActions;
    @FXML private TextField searchField;

    private ServiceSponsor serviceSponsor = new ServiceSponsor();
    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadData();
        setupSearch();
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colWebsite.setCellValueFactory(new PropertyValueFactory<>("websiteUrl"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox container = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("action-btn-edit");
                editBtn.setGraphic(new FontIcon("fas-edit"));
                deleteBtn.getStyleClass().add("action-btn-delete");
                deleteBtn.setGraphic(new FontIcon("fas-trash"));

                editBtn.setOnAction(event -> {
                    Sponsor s = getTableView().getItems().get(getIndex());
                    showEditForm(s);
                });

                deleteBtn.setOnAction(event -> {
                    Sponsor s = getTableView().getItems().get(getIndex());
                    handleDelete(s);
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
        sponsorList.setAll(serviceSponsor.getAll());
        sponsorTable.setItems(sponsorList);
    }

    private void setupSearch() {
        FilteredList<Sponsor> filteredData = new FilteredList<>(sponsorList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(s -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return s.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        sponsorTable.setItems(filteredData);
    }

    @FXML
    private void showAddForm() {
        loadForm(null);
    }

    private void showEditForm(Sponsor s) {
        loadForm(s);
    }

    private void loadForm(Sponsor s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/sponsor_form.fxml"));
            Parent form = loader.load();
            SponsorFormController controller = loader.getController();
            controller.setSponsor(s);

            StackPane contentArea = (StackPane) sponsorTable.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Sponsor s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Sponsor");
        alert.setHeaderText("Are you sure you want to delete this sponsor?");
        alert.setContentText(s.getName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceSponsor.delete(s.getId());
            loadData();
        }
    }
}
