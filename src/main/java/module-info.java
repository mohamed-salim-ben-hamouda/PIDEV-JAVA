module com.pidev.challenge_module {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires java.desktop;
    requires javafx.swing;
    requires org.kordamp.ikonli.core;
    requires cloudinary.http44;
    requires cloudinary.core;
    requires org.apache.commons.lang3;
    requires stripe.java;
    requires jakarta.mail;
    requires com.google.gson;
    requires java.net.http;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires kernel;
    requires layout;
    requires io;
    requires java.prefs;
    requires jbcrypt;
    requires itextpdf;
    requires jdk.httpserver;
    requires java.mail;
    requires activation;
    requires org.bytedeco.opencv;
    requires org.bytedeco.javacpp;
    requires org.bytedeco.openblas;
    requires io.github.cdimascio.dotenv.java;

    opens com.pidev to javafx.fxml;
    opens com.pidev.Controllers.client.User to javafx.fxml;
    opens com.pidev.Controllers.client to javafx.fxml;
    opens com.pidev.Controllers.admin to javafx.fxml;
    opens com.pidev.models to com.google.gson, javafx.base;

    exports com.pidev;
    exports com.pidev.Controllers.admin;
    exports com.pidev.Controllers.client;
    exports com.pidev.models;
}
