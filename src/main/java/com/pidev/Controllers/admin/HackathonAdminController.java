package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceHackathon;
import com.pidev.models.Hackathon;
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

public class HackathonAdminController implements Initializable {

    @FXML
    private TableView<Hackathon> hackathonTable;
    @FXML
    private TableColumn<Hackathon, String> colTitle, colTheme, colLocation, colStatus;
    @FXML
    private TableColumn<Hackathon, LocalDateTime> colStartAt, colEndAt;
    @FXML
    private TableColumn<Hackathon, Void> colActions;
    @FXML
    private TextField searchField;

    private ServiceHackathon serviceHackathon = new ServiceHackathon();
    private ObservableList<Hackathon> hackathonList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadData();
        setupSearch();
    }

    private void setupColumns() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colTheme.setCellValueFactory(new PropertyValueFactory<>("theme"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStartAt.setCellValueFactory(new PropertyValueFactory<>("startAt"));
        colEndAt.setCellValueFactory(new PropertyValueFactory<>("endAt"));

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
                    Hackathon h = getTableView().getItems().get(getIndex());
                    showEditForm(h);
                });

                deleteBtn.setOnAction(event -> {
                    Hackathon h = getTableView().getItems().get(getIndex());
                    handleDelete(h);
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
        hackathonList.setAll(serviceHackathon.getAll());
        hackathonTable.setItems(hackathonList);
    }

    private void setupSearch() {
        FilteredList<Hackathon> filteredData = new FilteredList<>(hackathonList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(h -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (h.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                if (h.getTheme().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });
        hackathonTable.setItems(filteredData);
    }

    @FXML
    private void showAddForm() {
        loadForm(null);
    }

    private void showEditForm(Hackathon h) {
        loadForm(h);
    }

    private void loadForm(Hackathon h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/hackathon_form.fxml"));
            Parent form = loader.load();
            HackathonFormController controller = loader.getController();
            controller.setHackathon(h);

            StackPane contentArea = (StackPane) hackathonTable.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Hackathon h) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Hackathon");
        alert.setHeaderText("Are you sure you want to delete this hackathon?");
        alert.setContentText(h.getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceHackathon.delete(h.getId());
            loadData();
        }
    }

    public void refreshTable() {
        loadData();
    }
}
