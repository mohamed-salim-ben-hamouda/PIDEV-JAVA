package com.pidev.Controllers.client.Challenge.Activity;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

public class ActionCell<T> extends TableCell<T, Void> {
    private final Button editBtn = new Button("Edit");
    private final Button deleteBtn = new Button("Delete");
    private final HBox container = new HBox(12, editBtn, deleteBtn);

    public ActionCell(Consumer<T> onEdit, Consumer<T> onDelete) {
        container.setStyle("-fx-alignment: CENTER;");
        editBtn.getStyleClass().add("action-btn-outline");
        deleteBtn.getStyleClass().add("action-btn-delete");

        editBtn.setOnAction(event -> {
            T selected = getSelectedRowItem();
            if (selected != null) onEdit.accept(selected);
        });

        deleteBtn.setOnAction(event -> {
            T selected = getSelectedRowItem();
            if (selected != null) onDelete.accept(selected);
        });
    }

    private T getSelectedRowItem() {
        if (getIndex() < 0 || getTableView() == null || getTableView().getItems() == null) return null;
        if (getIndex() >= getTableView().getItems().size()) return null;
        return getTableView().getItems().get(getIndex());
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : container);
    }
}
