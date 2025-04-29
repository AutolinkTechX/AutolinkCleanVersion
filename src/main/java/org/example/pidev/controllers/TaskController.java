package org.example.pidev.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.example.pidev.entities.TaskWorkshop;
import org.example.pidev.entities.User;
import org.example.pidev.entities.Workshop;
import org.example.pidev.services.ServiceTaskWorkshop;
import org.example.pidev.services.ServiceWorkshop;
import javax.mail.*;
import javax.mail.internet.*;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TaskController {

    @FXML
    private FlowPane workshopContainer;
    @FXML
    private Button refreshButton;
    @FXML
    private HBox pageNumbersContainer;
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private ComboBox<String> workshopNameComboBox;
    private User currentUser;
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    private ServiceWorkshop workshopService = new ServiceWorkshop();
    private int currentPage = 1;
    private int itemsPerPage = 6;
    private int totalPages = 1;
    private List<Workshop> allWorkshops = workshopService.getAllWorkshops(); // Cache all workshops
    private String currentSearchTerm = "";
    private String currentSortOption = "date_croissante";
    private String currentSelectedWorkshopName = "";

    @FXML
    private void initialize() {
        // Initialiser les options de tri
        sortComboBox.getItems().addAll(
                "Date (croissant)",
                "Date (décroissant)",
                "Nom (A-Z)",
                "Nom (Z-A)"
        );
        sortComboBox.getSelectionModel().select(0); // Sélectionner la première option par défaut

        // Initialiser le ComboBox des noms de workshops
        updateWorkshopNamesComboBox();
        workshopNameComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSelectedWorkshopName = newVal.equals("Tous les workshops") ? "" : newVal;
                currentPage = 1;
                loadWorkshops();
            }
        });

        // Ajouter un écouteur pour le changement de tri
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            handleSortChange();
        });

        // Charger les workshops avec le tri initial
        sortWorkshops();
        loadWorkshops();
        updatePaginationControls();
        setupComboBoxEffects();
    }

    private void setupComboBoxEffects() {
        // Animation lors de l'ouverture
        workshopNameComboBox.showingProperty().addListener((obs, oldVal, isShowing) -> {
            if (isShowing) {
                FadeTransition ft = new FadeTransition(Duration.millis(200),
                        workshopNameComboBox.getEditor());
                ft.setFromValue(0.8);
                ft.setToValue(1);
                ft.play();

                ScaleTransition st = new ScaleTransition(Duration.millis(200),
                        workshopNameComboBox.getScene().getRoot());
                st.setFromX(0.98);
                st.setFromY(0.98);
                st.setToX(1);
                st.setToY(1);
                st.play();
            }
        });

        // Effet au survol
        workshopNameComboBox.setOnMouseEntered(e -> {
            workshopNameComboBox.setEffect(new Glow(0.2));
        });

        workshopNameComboBox.setOnMouseExited(e -> {
            workshopNameComboBox.setEffect(null);
        });
    }

    private void updateWorkshopNamesComboBox() {
        workshopNameComboBox.getItems().clear();
        workshopNameComboBox.getItems().add("Tous les workshops");

        // Récupérer tous les noms distincts de workshops
        List<String> workshopNames = allWorkshops.stream()
                .map(Workshop::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        workshopNameComboBox.getItems().addAll(workshopNames);
        workshopNameComboBox.getSelectionModel().selectFirst();
    }

    private void sortWorkshops() {
        switch (currentSortOption) {
            case "date_croissante":
                allWorkshops.sort(Comparator.comparing(Workshop::getStartsAt));
                break;
            case "date_decroissante":
                allWorkshops.sort(Comparator.comparing(Workshop::getStartsAt).reversed());
                break;
            case "nom_croissant":
                allWorkshops.sort(Comparator.comparing(Workshop::getName));
                break;
            case "nom_decroissant":
                allWorkshops.sort(Comparator.comparing(Workshop::getName).reversed());
                break;
        }
    }

    private void handleSortChange() {
        String selectedOption = sortComboBox.getSelectionModel().getSelectedItem();
        switch (selectedOption) {
            case "Date (croissant)":
                currentSortOption = "date_croissante";
                break;
            case "Date (décroissant)":
                currentSortOption = "date_decroissante";
                break;
            case "Nom (A-Z)":
                currentSortOption = "nom_croissant";
                break;
            case "Nom (Z-A)":
                currentSortOption = "nom_decroissant";
                break;
        }
        sortWorkshops();
        loadWorkshops();
    }

    private void updatePaginationControls() {
        pageNumbersContainer.getChildren().clear();

        prevPageButton.setDisable(currentPage == 1);
        nextPageButton.setDisable(currentPage == totalPages);

        pageInfoLabel.setText("Page " + currentPage + " sur " + totalPages);

        int maxVisiblePages = 5;
        int startPage = Math.max(1, currentPage - maxVisiblePages / 2);
        int endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

        if (endPage - startPage + 1 < maxVisiblePages) {
            if (startPage == 1) {
                endPage = Math.min(totalPages, maxVisiblePages);
            } else {
                startPage = Math.max(1, endPage - maxVisiblePages + 1);
            }
        }

        if (startPage > 1) {
            Button firstPageButton = createPageButton(1);
            pageNumbersContainer.getChildren().add(firstPageButton);
            if (startPage > 2) {
                Label ellipsis = new Label("...");
                ellipsis.getStyleClass().add("pagination-ellipsis");
                pageNumbersContainer.getChildren().add(ellipsis);
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = createPageButton(i);
            if (i == currentPage) {
                pageButton.getStyleClass().add("pagination-button-active");
            }
            pageNumbersContainer.getChildren().add(pageButton);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                Label ellipsis = new Label("...");
                ellipsis.getStyleClass().add("pagination-ellipsis");
                pageNumbersContainer.getChildren().add(ellipsis);
            }
            Button lastPageButton = createPageButton(totalPages);
            pageNumbersContainer.getChildren().add(lastPageButton);
        }
    }

    private Button createPageButton(int pageNumber) {
        Button button = new Button(String.valueOf(pageNumber));
        button.getStyleClass().add("pagination-button");
        button.setOnAction(e -> goToPage(pageNumber));
        return button;
    }

    @FXML
    private void goToPage(int page) {
        if (page >= 1 && page <= totalPages && page != currentPage) {
            currentPage = page;
            loadWorkshops();
        }
    }

    private void loadWorkshops() {
        workshopContainer.getChildren().clear();
        List<Workshop> workshopsToDisplay = allWorkshops;

        // Appliquer le filtre par nom de workshop
        if (!currentSelectedWorkshopName.isEmpty()) {
            workshopsToDisplay = workshopsToDisplay.stream()
                    .filter(workshop -> workshop.getName().equals(currentSelectedWorkshopName))
                    .collect(Collectors.toList());
        }

        // Appliquer le filtre de recherche
        if (currentSearchTerm != null && !currentSearchTerm.isEmpty()) {
            workshopsToDisplay = workshopsToDisplay.stream()
                    .filter(workshop -> workshop.getName().toLowerCase().contains(currentSearchTerm.toLowerCase()))
                    .collect(Collectors.toList());
        }

        totalPages = (int) Math.ceil((double) workshopsToDisplay.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        updatePaginationControls();

        if (workshopsToDisplay.isEmpty()) {
            Text noWorkshops = new Text("Aucun workshop disponible");
            noWorkshops.getStyleClass().add("no-workshops-text");
            workshopContainer.getChildren().add(noWorkshops);
            return;
        }

        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, workshopsToDisplay.size());
        List<Workshop> workshopsForPage = workshopsToDisplay.subList(fromIndex, toIndex);

        for (Workshop workshop : workshopsForPage) {
            VBox card = createWorkshopCard(workshop);
            workshopContainer.getChildren().add(card);
        }
    }

    private VBox createWorkshopCard(Workshop workshop) {
        VBox card = new VBox();
        card.getStyleClass().add("workshop-card");
        card.setSpacing(10);
        card.setPadding(new Insets(15));
        card.setMinWidth(350);
        card.setMaxWidth(350);

        // Créer le conteneur des boutons
        HBox buttonsContainer = new HBox();
        buttonsContainer.setSpacing(10);
        buttonsContainer.setAlignment(Pos.CENTER);

        // Créer les boutons
        Button registerButton = new Button("S'inscrire");
        registerButton.getStyleClass().add("register-button");
        registerButton.setMinWidth(120);
        registerButton.setMaxWidth(120);
        registerButton.setOnAction(e -> handleRegistration(workshop));

        Button tasksButton = new Button("Voir les tâches");
        tasksButton.getStyleClass().add("tasks-button");
        tasksButton.setMinWidth(120);
        tasksButton.setMaxWidth(120);
        tasksButton.setOnAction(e -> handleShowTasks(workshop));

        buttonsContainer.getChildren().addAll(registerButton, tasksButton);

        if (workshop.getImage() != null && !workshop.getImage().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image("file:" + workshop.getImage()));
                imageView.setFitWidth(280);
                imageView.setFitHeight(150);
                imageView.setPreserveRatio(true);
                card.getChildren().add(imageView);
            } catch (Exception e) {
                System.err.println("Erreur de chargement de l'image: " + e.getMessage());
            }
        }

        Text name = new Text(workshop.getName() != null ? workshop.getName() : "Nom non spécifié");
        name.getStyleClass().add("workshop-name");

        Text description = new Text(workshop.getDescription() != null ? workshop.getDescription() : "Pas de description");
        description.getStyleClass().add("workshop-description");
        description.setWrappingWidth(280);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String startDate = "Date non spécifiée";
        String endDate = "Date non spécifiée";

        if (workshop.getStartsAt() != null) {
            startDate = workshop.getStartsAt().toLocalDateTime().format(formatter);
        }
        if (workshop.getEndsAt() != null) {
            endDate = workshop.getEndsAt().toLocalDateTime().format(formatter);
        }

        Text dates = new Text("Du " + startDate + " au " + endDate);
        dates.getStyleClass().add("workshop-dates");

        Text location = new Text("Lieu: " + (workshop.getLocation() != null ? workshop.getLocation() : "Non spécifié"));
        location.getStyleClass().add("workshop-location");

        Text price = new Text("Prix: " + (workshop.getPrice() > 0 ? workshop.getPrice() + " €" : "Gratuit"));
        price.getStyleClass().add("workshop-price");

        Text places = new Text("Places disponibles: " + workshop.getAvailablePlaces());
        places.getStyleClass().add("workshop-places");

        // Ajouter tous les éléments à la carte
        card.getChildren().addAll(name, description, dates, location, price, places, buttonsContainer);

        // Style dynamique
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (workshop.getEndsAt() != null && workshop.getEndsAt().before(now)) {
            card.getStyleClass().add("past-workshop");
        } else if (workshop.getStartsAt() != null && workshop.getStartsAt().after(now)) {
            if (workshop.getAvailablePlaces() > 0) {
                card.getStyleClass().add("future-workshop");
            } else {
                card.getStyleClass().add("full-workshop");
            }
        } else if (workshop.getStartsAt() != null && workshop.getEndsAt() != null) {
            card.getStyleClass().add("current-workshop");
        } else {
            card.getStyleClass().add("unknown-workshop");
        }

        return card;
    }

    private void handleRegistration(Workshop workshop) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Inscription au workshop");
        dialog.setHeaderText("Inscription à: " + workshop.getName());
        dialog.setContentText("Veuillez entrer votre email:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(email -> {
            if (!isValidEmail(email)) {
                showAlert(Alert.AlertType.ERROR,
                        "Email invalide",
                        "Veuillez entrer une adresse email valide");
                return;
            }

            try {
                boolean sent = sendRegistrationEmail(email, workshop);
                if (sent) {
                    showAlert(Alert.AlertType.INFORMATION,
                            "Inscription réussie",
                            "Un email de confirmation a été envoyé à " + email);
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur d'envoi",
                        "Une erreur est survenue: " + e.getMessage());
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private boolean sendRegistrationEmail(String email, Workshop workshop) {
        // Configuration pour Gmail
        final String username = "maramkaouech51@gmail.com";
        final String password = "jluj keud baas fnxn";
        final String smtpHost = "smtp.gmail.com";
        final int smtpPort = 587;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", smtpHost);

        try {
            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Confirmation d'inscription: " + workshop.getName());

            String content = "Bonjour,\n\n" +
                    "Vous êtes inscrit au workshop suivant:\n\n" +
                    "Nom: " + workshop.getName() + "\n" +
                    "Description: " + workshop.getDescription() + "\n" +
                    "Dates: Du " + workshop.getStartsAt() + " au " + workshop.getEndsAt() + "\n" +
                    "Lieu: " + workshop.getLocation() + "\n" +
                    "Prix: " + workshop.getPrice() + " €\n\n" +
                    "Merci pour votre inscription!\n\n" +
                    "Cordialement,\nL'équipe des workshops";

            message.setText(content);

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "Erreur d'envoi",
                    "Erreur lors de l'envoi de l'email: " + e.getMessage());
            return false;
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        currentPage = 1;
        currentSearchTerm = "";
        currentSelectedWorkshopName = "";
        searchField.clear();
        allWorkshops = workshopService.getAllWorkshops();
        updateWorkshopNamesComboBox();
        sortWorkshopsByStartDate();
        loadWorkshops();
    }

    @FXML
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadWorkshops();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadWorkshops();
        }
    }

    public void handleSearch() {
        String searchText = searchField.getText().toLowerCase();

        if (searchText.isEmpty()) {
            displayWorkshops(allWorkshops);
            return;
        }

        List<Workshop> filteredWorkshops = allWorkshops.stream()
                .filter(workshop -> workshop.getName().toLowerCase().startsWith(searchText))
                .collect(Collectors.toList());

        displayWorkshops(filteredWorkshops);
    }

    private void displayWorkshops(List<Workshop> workshops) {
        workshopContainer.getChildren().clear();

        for (Workshop workshop : workshops) {
            workshopContainer.getChildren().add(createWorkshopCard(workshop));
        }
    }

    private void showRegistrationDialog(Workshop workshop) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Inscription au Workshop");
        dialog.setHeaderText("Inscrivez-vous à : " + workshop.getName());

        ButtonType registerButtonType = new ButtonType("S'inscrire", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        TextField emailField = new TextField();
        emailField.setPromptText("Entrez votre adresse email");
        emailField.getStyleClass().add("text-field");

        VBox content = new VBox(12);
        content.setPadding(new Insets(15));
        content.getChildren().addAll(new Label("Email :"), emailField);

        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/gestion_workshop/styles.css").toExternalForm());

        dialog.setOnShown(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(400), dialog.getDialogPane());
            dialog.getDialogPane().setOpacity(0);
            ft.setToValue(1.0);
            ft.play();
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                return emailField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(email -> {
            if (!isValidEmail(email)) {
                showStyledAlert(Alert.AlertType.ERROR, "Email invalide", "Veuillez entrer une adresse email valide");
                return;
            }

            try {
                boolean sent = sendRegistrationEmail(email, workshop);
                if (sent) {
                    showStyledAlert(Alert.AlertType.INFORMATION, "Succès", "Un email de confirmation a été envoyé à " + email);
                }
            } catch (Exception e) {
                showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'envoi: " + e.getMessage());
            }
        });
    }

    private void showStyledAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/gestion_workshop/styles.css").toExternalForm());
        alert.showAndWait();
    }

    private void sortWorkshopsByStartDate() {
        Collections.sort(allWorkshops, Comparator.comparing(Workshop::getStartsAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    @FXML
    public void setAllWorkshops(List<Workshop> allWorkshops) {
        this.allWorkshops = allWorkshops;
        sortWorkshopsByStartDate();
        loadWorkshops();
    }

    private void handleShowTasks(Workshop workshop) {
        try {
            ServiceTaskWorkshop taskService = new ServiceTaskWorkshop();
            List<TaskWorkshop> tasks = taskService.getTasksForWorkshop(workshop.getId());

            // Création de la fenêtre
            Stage taskStage = new Stage();
            taskStage.setTitle("Tâches pour " + workshop.getName());
            taskStage.initStyle(StageStyle.TRANSPARENT);

            // Conteneur principal avec effet de verre
            StackPane root = new StackPane();
            root.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95);" +
                    "-fx-background-radius: 20;" +
                    "-fx-border-radius: 20;" +
                    "-fx-border-color: #4a90e2;" +
                    "-fx-border-width: 2px;");

            // Effet d'ombre
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.2));
            shadow.setRadius(25);
            root.setEffect(shadow);

            // Contenu principal
            VBox mainContent = new VBox(20);
            mainContent.setPadding(new Insets(25));
            mainContent.setAlignment(Pos.TOP_CENTER);

            // ScrollPane pour le contenu
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

            if (tasks == null || tasks.isEmpty()) {
                // Affichage quand il n'y a pas de tâches
                VBox emptyContent = new VBox(15);
                emptyContent.setAlignment(Pos.CENTER);

                // Icône illustrative
                Text emptyIcon = new Text("📭");
                emptyIcon.setStyle("-fx-font-size: 60px;");

                // Animation de l'icône
                RotateTransition rotate = new RotateTransition(Duration.seconds(2), emptyIcon);
                rotate.setFromAngle(-5);
                rotate.setToAngle(5);
                rotate.setAutoReverse(true);
                rotate.setCycleCount(Animation.INDEFINITE);
                rotate.play();

                Label emptyLabel = new Label("Aucune tâche disponible");
                emptyLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

                Label suggestionLabel = new Label("Aucune tâche n'a été créée pour ce workshop.\nRevenez plus tard ou contactez l'organisateur.");
                suggestionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
                suggestionLabel.setTextAlignment(TextAlignment.CENTER);

                emptyContent.getChildren().addAll(emptyIcon, emptyLabel, suggestionLabel);
                mainContent.getChildren().add(emptyContent);
            } else {
                // Affichage des tâches existantes
                Label titleLabel = new Label("Tâches pour " + workshop.getName());
                titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                VBox tasksContainer = new VBox(15);
                tasksContainer.setAlignment(Pos.TOP_CENTER);

                // Animation d'apparition séquentielle
                ParallelTransition allTransitions = new ParallelTransition();

                for (int i = 0; i < tasks.size(); i++) {
                    TaskWorkshop task = tasks.get(i);

                    // Carte de tâche
                    VBox taskCard = new VBox(10);
                    taskCard.setPadding(new Insets(15));
                    taskCard.setStyle("-fx-background-color: #ffffff;" +
                            "-fx-background-radius: 15;" +
                            "-fx-border-radius: 15;" +
                            "-fx-border-color: #e0e0e0;" +
                            "-fx-border-width: 1px;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");

                    // Effet au survol
                    taskCard.setOnMouseEntered(e -> {
                        taskCard.setStyle("-fx-background-color: rgba(236, 240, 241, 0.9);" +
                                "-fx-border-color: rgba(74, 144, 226, 0.5);");
                        taskCard.setEffect(new Glow(0.1));
                    });

                    taskCard.setOnMouseExited(e -> {
                        taskCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);" +
                                "-fx-border-color: rgba(74, 144, 226, 0.3);");
                        taskCard.setEffect(null);
                    });

                    // Contenu de la carte
                    Label taskName = new Label("📌 " + task.getNom());
                    taskName.setStyle("-fx-font-size: 16px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #2c3e50;");

                    Label taskDesc = new Label(task.getDescription());
                    taskDesc.setStyle("-fx-font-size: 14px; " +
                            "-fx-text-fill: #34495e; " +
                            "-fx-wrap-text: true;");
                    taskDesc.setWrapText(true);
                    taskDesc.setMaxWidth(400);

                    // Formatage des dates
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String startDate = task.getStartsAt() != null ?
                            task.getStartsAt().toLocalDateTime().format(formatter) : "Non spécifié";
                    String endDate = task.getEndsAt() != null ?
                            task.getEndsAt().toLocalDateTime().format(formatter) : "Non spécifié";

                    Label taskDates = new Label("🗓 Du " + startDate + " au " + endDate);
                    taskDates.setStyle("-fx-font-size: 13px; " +
                            "-fx-text-fill: #7f8c8d;");

                    // Statut avec couleur dynamique
                    String statusColor = task.getStatus().equals("Terminé") ? "#2ecc71" :
                            task.getStatus().equals("En cours") ? "#f39c12" : "#e74c3c";

                    Label taskStatus = new Label("Statut: " + task.getStatus());
                    taskStatus.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");

                    taskCard.getChildren().addAll(taskName, taskDesc, taskDates, taskStatus);
                    tasksContainer.getChildren().add(taskCard);

                    // Animation pour chaque carte
                    FadeTransition fade = new FadeTransition(Duration.millis(400), taskCard);
                    fade.setFromValue(0);
                    fade.setToValue(1);
                    fade.setDelay(Duration.millis(i * 150));

                    ScaleTransition scale = new ScaleTransition(Duration.millis(400), taskCard);
                    scale.setFromX(0.9);
                    scale.setFromY(0.9);
                    scale.setToX(1);
                    scale.setToY(1);
                    scale.setDelay(Duration.millis(i * 150));

                    allTransitions.getChildren().addAll(fade, scale);
                }

                allTransitions.play();
                mainContent.getChildren().addAll(titleLabel, tasksContainer);
            }

            // Bouton de fermeture
            Button closeButton = new Button("Fermer");
            closeButton.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-font-weight: bold;");

            closeButton.setOnMouseEntered(e -> {
                closeButton.setStyle("-fx-background-color: #3a7bc8; -fx-text-fill: white;");
                closeButton.setEffect(new Glow(0.2));
            });

            closeButton.setOnMouseExited(e -> {
                closeButton.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white;");
                closeButton.setEffect(null);
            });

            closeButton.setOnAction(e -> taskStage.close());

            mainContent.getChildren().add(closeButton);
            scrollPane.setContent(mainContent);
            root.getChildren().add(scrollPane);

            // Animation d'ouverture
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(500), root);
            scaleIn.setFromX(0.95);
            scaleIn.setFromY(0.95);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            ParallelTransition openTransition = new ParallelTransition(fadeIn, scaleIn);
            openTransition.play();

            Scene scene = new Scene(root, 500, 600);
            scene.setFill(Color.TRANSPARENT);
            taskStage.setScene(scene);
            taskStage.centerOnScreen();
            taskStage.show();

        } catch (Exception e) {
            showStyledErrorAlert("Erreur",
                    "Impossible d'afficher les tâches",
                    "Une erreur est survenue : " + e.getMessage());
        }
    }

    private void showStyledErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Style personnalisé
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom, #fff1f2, #fecaca);" +
                "-fx-border-color: #f87171;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;");

        // Effet d'ombre
        DropShadow alertShadow = new DropShadow();
        alertShadow.setColor(Color.rgb(220, 38, 38, 0.3));
        alertShadow.setRadius(20);
        dialogPane.setEffect(alertShadow);

        // Animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), dialogPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        alert.showAndWait();
    }

}