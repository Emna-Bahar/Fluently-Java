module com.example.pijava_fluently {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires com.google.gson;
    // ── Ajouter ces lignes ──
    requires com.google.zxing;          // ZXing Core
    requires com.google.zxing.javase;   // ZXing JavaSE
    requires webcam.capture;            // Webcam Capture
    requires jdk.httpserver;//
    requires javafx.swing;
    requires org.json;//hedhy ia
    opens com.example.pijava_fluently.services to java.base;//hedhy google api
    requires java.net.http;

    opens com.example.pijava_fluently to javafx.fxml;
    opens com.example.pijava_fluently.controller to javafx.fxml;
    opens com.example.pijava_fluently.entites to javafx.base, javafx.fxml;
    opens com.example.pijava_fluently.utils to javafx.fxml;

    exports com.example.pijava_fluently;
    exports com.example.pijava_fluently.controller;
    exports com.example.pijava_fluently.entites;
    exports com.example.pijava_fluently.services;
    exports com.example.pijava_fluently.utils;
}