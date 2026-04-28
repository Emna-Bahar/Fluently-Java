module com.example.pijava_fluently {
    // JavaFX de base
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // Java standard
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires jdk.httpserver;

    // JSON / Sérialisation
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires org.json;

    // QR Code
    requires com.google.zxing.javase;
    requires com.google.zxing;

    // PDF (itext)
    requires itextpdf;
    requires kernel;
    requires layout;

    // HTTP Client
    requires okhttp3;

    // Sécurité
    requires jbcrypt;

    // Email
    requires jakarta.mail;
    requires javafx.swing;
    requires jdk.jsobject;

    // Ouverture pour reflexion (FXML)
    opens com.example.pijava_fluently to javafx.fxml;
    opens com.example.pijava_fluently.controller to javafx.fxml;
    opens com.example.pijava_fluently.entites to javafx.base, javafx.fxml;
    opens com.example.pijava_fluently.utils to javafx.fxml;

    // Exportations
    exports com.example.pijava_fluently;
    exports com.example.pijava_fluently.controller;
    exports com.example.pijava_fluently.entites;
    exports com.example.pijava_fluently.services;
    exports com.example.pijava_fluently.utils;
}