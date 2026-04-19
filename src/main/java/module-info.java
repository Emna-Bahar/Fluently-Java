module com.example.pijava_fluently {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires org.slf4j;
    requires javafx.media;
    requires jdk.jsobject;



    opens com.example.pijava_fluently to javafx.fxml;
    opens com.example.pijava_fluently.controller to javafx.fxml;
    opens com.example.pijava_fluently.entites to javafx.base, javafx.fxml, com.fasterxml.jackson.databind;
    opens com.example.pijava_fluently.utils to javafx.fxml;
    opens com.example.pijava_fluently.services to com.fasterxml.jackson.databind;

    exports com.example.pijava_fluently;
    exports com.example.pijava_fluently.controller;
    exports com.example.pijava_fluently.entites;
    exports com.example.pijava_fluently.services;
    exports com.example.pijava_fluently.utils;
}