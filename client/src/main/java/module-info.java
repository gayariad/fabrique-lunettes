module com.lunettes {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.paho.client.mqttv3;
    requires com.google.gson;
    requires org.slf4j;

    // FXML instancie les controllers par réflexion
    opens com.lunettes to javafx.fxml;
    opens com.lunettes.controller to javafx.fxml;
    opens com.lunettes.model to javafx.fxml, com.google.gson;

    exports com.lunettes;
}
