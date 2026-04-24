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
    
    // Excel Exports
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    
    // PDF Exports (iText 7)
    requires kernel;
    requires layout;
    requires io;

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
