package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.example.pidev.entities.Commande;
import org.example.pidev.services.CommandeService;
import org.example.pidev.utils.MyDatabase;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class CalendarController implements Initializable {

    @FXML private VBox calendarContainer;
    @FXML private Label monthYearLabel;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;
    @FXML private Button prevYearBtn;
    @FXML private Button nextYearBtn;
    @FXML private Button todayBtn;

    private YearMonth currentYearMonth;
    private final CommandeService commandeService = new CommandeService(MyDatabase.getInstance().getMyConnection());
    private List<Commande> commandes;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentYearMonth = YearMonth.now();
        commandes = commandeService.getAllCommandees();

        // Initialize ComboBoxes
        initializeMonthComboBox();
        initializeYearComboBox();

        // Setup navigation buttons
        setupNavigationButtons();

        // Display initial calendar
        updateCalendar();
    }

    private void initializeMonthComboBox() {
        monthComboBox.getItems().clear();
        for (int i = 1; i <= 12; i++) {
            monthComboBox.getItems().add(
                    LocalDate.of(2000, i, 1)
                            .getMonth()
                            .getDisplayName(TextStyle.FULL, Locale.FRENCH)
            );
        }
        monthComboBox.getSelectionModel().select(currentYearMonth.getMonthValue() - 1);
        monthComboBox.setOnAction(e -> {
            int selectedMonth = monthComboBox.getSelectionModel().getSelectedIndex() + 1;
            currentYearMonth = YearMonth.of(currentYearMonth.getYear(), selectedMonth);
            updateCalendar();
        });
    }

    private void initializeYearComboBox() {
        yearComboBox.getItems().clear();
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 10; year <= currentYear + 10; year++) {
            yearComboBox.getItems().add(year);
        }
        yearComboBox.getSelectionModel().select((Integer) currentYear);
        yearComboBox.setOnAction(e -> {
            int selectedYear = yearComboBox.getValue();
            currentYearMonth = YearMonth.of(selectedYear, currentYearMonth.getMonthValue());
            updateCalendar();
        });
    }

    private void setupNavigationButtons() {
        prevMonthBtn.setOnAction(e -> navigateMonth(-1));
        nextMonthBtn.setOnAction(e -> navigateMonth(1));
        prevYearBtn.setOnAction(e -> navigateYear(-1));
        nextYearBtn.setOnAction(e -> navigateYear(1));
        todayBtn.setOnAction(e -> navigateToToday());
    }

    private void navigateMonth(int months) {
        currentYearMonth = currentYearMonth.plusMonths(months);
        updateComboBoxes();
        updateCalendar();
    }

    private void navigateYear(int years) {
        currentYearMonth = currentYearMonth.plusYears(years);
        updateComboBoxes();
        updateCalendar();
    }

    private void navigateToToday() {
        currentYearMonth = YearMonth.now();
        updateComboBoxes();
        updateCalendar();
    }

    private void updateComboBoxes() {
        monthComboBox.getSelectionModel().select(currentYearMonth.getMonthValue() - 1);
        yearComboBox.getSelectionModel().select((Integer) currentYearMonth.getYear());
    }

    private void updateCalendar() {
        // Update month/year label
        monthYearLabel.setText(
                currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase() +
                        " " + currentYearMonth.getYear()
        );

        // Create calendar grid
        GridPane calendarGrid = new GridPane();
        calendarGrid.setAlignment(Pos.CENTER);
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        calendarGrid.setPadding(new Insets(10));
        calendarGrid.getStyleClass().add("calendar-grid");

        // Add day headers
        String[] dayNames = {"LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM"};
        for (int i = 0; i < dayNames.length; i++) {
            Label header = new Label(dayNames[i]);
            header.setAlignment(Pos.CENTER);
            header.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            header.getStyleClass().add("calendar-header");
            calendarGrid.add(header, i, 0);
        }

        // Calculate first day of month and offset
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        DayOfWeek firstDayOfWeek = firstOfMonth.getDayOfWeek();
        int dayOfWeekValue = firstDayOfWeek.getValue(); // Monday=1 to Sunday=7

        // Fill calendar
        int row = 1;
        int col = dayOfWeekValue - 1;

        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate currentDate = currentYearMonth.atDay(day);
            VBox dayCell = createDayCell(day, currentDate);
            calendarGrid.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        // Clear and add new calendar
        calendarContainer.getChildren().clear();
        calendarContainer.getChildren().addAll(monthYearLabel, calendarGrid);
    }

    private VBox createDayCell(int day, LocalDate date) {
        VBox dayCell = new VBox(2);
        dayCell.setAlignment(Pos.TOP_CENTER);
        dayCell.setPadding(new Insets(5));
        dayCell.getStyleClass().add("calendar-day");

        // Highlight current day
        if (currentYearMonth.equals(YearMonth.now()) && day == LocalDate.now().getDayOfMonth()) {
            dayCell.getStyleClass().add("current-day");
        }

        // Day number
        Text dayNumber = new Text(String.valueOf(day));
        dayNumber.getStyleClass().add("day-number");
        dayCell.getChildren().add(dayNumber);

        // Add check icon if there are commandes for this day
        List<Commande> commandesForDay = getCommandesForDate(date);
        if (!commandesForDay.isEmpty()) {
            // Create check icon (green circle with check mark)
            StackPane checkIcon = createCheckIcon();
            dayCell.getChildren().add(checkIcon);
        }

        // Add click handler
        dayCell.setOnMouseClicked(e -> handleDayClick(date, commandesForDay));

        return dayCell;
    }

    private StackPane createCheckIcon() {
        // Create a green circle
        Circle circle = new Circle(8);
        circle.setFill(Color.GREEN);

        // Create a check mark (✓)
        Text checkMark = new Text("✓");
        checkMark.setFill(Color.WHITE);
        checkMark.setFont(Font.font("Arial", FontWeight.BOLD, 10));

        // Stack them together
        StackPane checkIcon = new StackPane(circle, checkMark);
        checkIcon.setAlignment(Pos.CENTER);

        return checkIcon;
    }

    private List<Commande> getCommandesForDate(LocalDate date) {
        return commandes.stream()
                .filter(c -> c.getDateCommande().toLocalDate().equals(date))
                .toList();
    }

    private void handleDayClick(LocalDate date, List<Commande> commandesForDay) {
        if (!commandesForDay.isEmpty()) {
            // Create dialog with pagination
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails des commandes");
            dialog.setHeaderText("Commandes du " + date);

            // Create pagination
            int itemsPerPage = 3;
            int pageCount = (int) Math.ceil((double) commandesForDay.size() / itemsPerPage);

            Pagination pagination = new Pagination(pageCount, 0);
            pagination.setPageFactory(pageIndex -> {
                VBox pageContent = new VBox(5);
                pageContent.setPadding(new Insets(10));

                int fromIndex = pageIndex * itemsPerPage;
                int toIndex = Math.min(fromIndex + itemsPerPage, commandesForDay.size());

                for (int i = fromIndex; i < toIndex; i++) {
                    Commande commande = commandesForDay.get(i);
                    Label commandeLabel = new Label(
                            "Commande #" + commande.getId() +
                                    "\nClient: " + commande.getClient().getName() +
                                    "\nTotal: " + commande.getTotal() + " DT"
                    );
                    commandeLabel.setStyle("-fx-border-color: lightgray; -fx-border-width: 0 0 1 0; -fx-padding: 5;");
                    commandeLabel.setWrapText(true);
                    pageContent.getChildren().add(commandeLabel);
                }

                return new ScrollPane(pageContent);
            });

            // Add pagination to dialog
            dialog.getDialogPane().setContent(pagination);
            dialog.getDialogPane().setPrefSize(400, 300);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        }
    }
}