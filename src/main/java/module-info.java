module com.example.pijava_fluently {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.example.pijava_fluently to javafx.fxml;
    exports com.example.pijava_fluently;
}