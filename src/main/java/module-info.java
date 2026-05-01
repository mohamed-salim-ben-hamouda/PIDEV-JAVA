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
    requires java.mail;
    requires com.github.librepdf.openpdf;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires org.apache.pdfbox;
    requires org.json;
    requires java.net.http;
    opens com.pidev to javafx.fxml;
    opens com.pidev.Controllers.client.User to javafx.fxml;
    opens com.pidev.Controllers.client to javafx.fxml;
    opens com.pidev.Controllers.admin to javafx.fxml;

    exports com.pidev;
    exports com.pidev.Controllers.admin;
    exports com.pidev.Controllers.client;
    exports com.pidev.models;
    //exports com.pidev.Views;
}
