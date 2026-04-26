module com.example.pijava_fluently {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires webcam.capture;
    requires jdk.httpserver;
    requires javafx.swing;
    requires org.json;
    requires java.net.http;
    requires javafx.web;
    requires jdk.jsobject;

    opens com.example.pijava_fluently.services to java.base;
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