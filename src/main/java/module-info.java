module pidev {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.management;
    requires spring.context;
    requires jakarta.persistence;
    requires com.fasterxml.jackson.databind;
    requires javafx.web;
    // Noms de modules automatiques pour iTextPDF
    requires kernel;    // pour kernel-7.2.5.jar
    requires layout;    // pour layout-7.2.5.jar
    requires io;
    requires jbcrypt;
    requires org.hibernate.orm.core;
    requires org.slf4j;

    requires com.google.zxing;
    requires com.google.zxing.javase;        // pour io-7.2.5.jar

    requires javafx.swing;
    requires twilio;
    requires stripe.java;
    requires jdk.jsobject;
    requires Java.WebSocket;
    requires com.google.api.client;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.gson;        // pour io-7.2.5.jar

    opens org.example.pidev to javafx.fxml;
    opens org.example.pidev.controllers to javafx.fxml;
    opens org.example.pidev.entities to javafx.base;
    opens org.example.pidev.utils to javafx.fxml;
    opens org.example.pidev.test to javafx.graphics;

    exports org.example.pidev;
}