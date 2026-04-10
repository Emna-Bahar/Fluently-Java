module com.example.pijava_fluently {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.example.pijava_fluently to javafx.fxml;
    exports com.example.pijava_fluently;

    exports com.example.pijava_fluently.utils;
    opens com.example.pijava_fluently.utils to javafx.fxml;

    exports com.example.pijava_fluently.models;
    opens com.example.pijava_fluently.models to javafx.fxml, javafx.base;

    exports com.example.pijava_fluently.services;
    opens com.example.pijava_fluently.services to javafx.fxml;

    exports com.example.pijava_fluently.controller;
    opens com.example.pijava_fluently.controller to javafx.fxml;
}
