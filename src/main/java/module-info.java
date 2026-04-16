module com.pidev.challenge_module {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires java.desktop;
    requires org.kordamp.ikonli.core;
    requires java.management;
    requires javafx.web;
    requires org.apache.pdfbox;
    requires org.json;
    opens challenge_module.evaluation_pdf to javafx.graphics, javafx.web;
    opens com.pidev.Controllers.admin.Challenge to javafx.fxml;
    opens com.pidev.Controllers.admin.Challenge.Activity to javafx.fxml;
    opens com.pidev.Controllers.admin.Challenge.Evaluation to javafx.fxml;


    opens com.pidev to javafx.fxml;
    opens com.pidev.Controllers.client.User to javafx.fxml;
    opens com.pidev.Controllers.client to javafx.fxml;
    opens com.pidev.Controllers.admin to javafx.fxml;
    opens com.pidev.Controllers.client.Challenge.Activity to javafx.fxml;
    opens com.pidev.Controllers.client.Challenge.Evaluation to javafx.fxml;
    opens com.pidev.Controllers.client.Challenge to javafx.fxml;

    exports com.pidev.Controllers.client.Challenge to javafx.fxml;
    exports com.pidev.Controllers.client.Challenge.Activity to javafx.fxml;
    exports com.pidev.Controllers.client.Challenge.Evaluation;
    exports com.pidev;
    exports com.pidev.Controllers.admin;
    exports com.pidev.Controllers.client;
    exports com.pidev.models;
    //exports com.pidev.Views;
}
