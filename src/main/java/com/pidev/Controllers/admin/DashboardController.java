package com.pidev.Controllers.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class DashboardController {

	@FXML
	private void loadCourses(ActionEvent event) {
		loadAdminSection(event, BaseController::loadCourses);
	}

	@FXML
	private void loadChapters(ActionEvent event) {
		loadAdminSection(event, BaseController::loadChapters);
	}

	@FXML
	private void loadQuizzes(ActionEvent event) {
		loadAdminSection(event, BaseController::loadQuizzes);
	}

	@FXML
	private void loadQuestions(ActionEvent event) {
		loadAdminSection(event, BaseController::loadQuestions);
	}

	@FXML
	private void loadAnswers(ActionEvent event) {
		loadAdminSection(event, BaseController::loadAnswers);
	}

	private void loadAdminSection(ActionEvent event, AdminAction action) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/base_back.fxml"));
			Parent root = loader.load();
			BaseController controller = loader.getController();
			action.apply(controller);

			Scene scene = ((Node) event.getSource()).getScene();
			if (scene != null) {
				scene.setRoot(root);
			}
		} catch (IOException e) {
			System.err.println("Could not load admin section from dashboard.");
			e.printStackTrace();
		}
	}

	@FunctionalInterface
	private interface AdminAction {
		void apply(BaseController controller);
	}
}
