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
    requires itextpdf;
    requires java.mail;
    requires com.google.zxing;
    requires com.google.zxing.javase;        // pour io-7.2.5.jar

    requires javafx.swing;
    requires twilio;
    requires stripe.java;
  
    requires itextpdf;        // pour io-7.2.5.jar

    requires com.google.gson;


    requires jdk.jsobject;        // pour io-7.2.5.jar
    requires javax.mail;
    requires Java.WebSocket;        // pour io-7.2.5.jar
    requires java.prefs;

    requires webcam.capture;
      
    requires java.net.http;



    opens org.example.pidev to javafx.fxml;
    opens org.example.pidev.controllers to javafx.fxml;
    opens org.example.pidev.entities to javafx.base;
    opens org.example.pidev.utils to javafx.fxml;
    opens org.example.pidev.test to javafx.graphics;
    opens org.example.pidev.services to javafx.fxml;

    exports org.example.pidev;
}