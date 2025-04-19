module pidev {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.management;
    requires spring.context;
    requires jakarta.persistence;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;

    // Noms de modules automatiques pour iTextPDF
    requires kernel;    // pour kernel-7.2.5.jar
    requires layout;    // pour layout-7.2.5.jar
    requires io;
    requires jbcrypt;
    requires org.hibernate.orm.core;
    requires org.slf4j;        // pour io-7.2.5.jar

    opens org.example.pidev to javafx.fxml;
    opens org.example.pidev.controllers to javafx.fxml;
    opens org.example.pidev.entities to javafx.base;
    opens org.example.pidev.utils to javafx.fxml;
    opens org.example.pidev.test to javafx.graphics;

    exports org.example.pidev;
}