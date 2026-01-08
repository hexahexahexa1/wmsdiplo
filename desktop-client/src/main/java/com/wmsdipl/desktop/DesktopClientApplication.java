package com.wmsdipl.desktop;

import com.wmsdipl.desktop.ImportServiceClient;
import com.wmsdipl.desktop.model.Location;
import com.wmsdipl.desktop.model.Pallet;
import com.wmsdipl.desktop.model.PutawayRule;
import com.wmsdipl.desktop.model.Receipt;
import com.wmsdipl.desktop.model.Scan;
import com.wmsdipl.desktop.model.Sku;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DesktopClientApplication extends Application {

    private String coreApiBase = System.getenv().getOrDefault("WMS_CORE_API_BASE", "http://localhost:8080");
    private String importApiBase = System.getenv().getOrDefault("WMS_IMPORT_BASE", "http://localhost:8090");
    private ApiClient apiClient = new ApiClient(coreApiBase);

    private BorderPane shell;
    private VBox contentHolder;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String activeModule = "receipts";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        if (!showLoginDialog()) {
            Platform.exit();
            return;
        }

        shell = new BorderPane();
        shell.setPrefSize(1100, 720);
        shell.setStyle("-fx-background-color: #1c1c1c;");

        VBox nav = buildNav();
        shell.setLeft(nav);

        contentHolder = new VBox();
        contentHolder.setPadding(new Insets(18));
        contentHolder.setSpacing(12);
        shell.setCenter(contentHolder);

        showReceiptsPane();

        Scene scene = new Scene(shell);
        applyStyles(scene);
        stage.setScene(scene);
        stage.setTitle("WMSDIPL");
        stage.show();
    }

    private VBox buildNav() {
        Label logo = new Label("WMSDIPL");
        logo.getStyleClass().add("logo-text");
        logo.setAlignment(Pos.CENTER);
        logo.setMaxWidth(Double.MAX_VALUE);

        Button receiptsBtn = navButton("Приходы", activeModule.equals("receipts"), this::showReceiptsPane);
        Button topologyBtn = navButton("Топология", activeModule.equals("topology"), this::showTopologyPane);
        Button palletsBtn = navButton("Паллеты", activeModule.equals("pallets"), this::showPalletsPane);
        Button tasksBtn = navButton("Задания", activeModule.equals("tasks"), this::showTasksPane);
        Button terminalBtn = navButton("Терминал", activeModule.equals("terminal"), this::showTerminalPane);
        Button settingsBtn = navButton("Настройки", activeModule.equals("settings"), this::showSettingsPane);

        VBox nav = new VBox(14, logo, receiptsBtn, topologyBtn, palletsBtn, tasksBtn, terminalBtn, settingsBtn);
        nav.setPadding(new Insets(24, 24, 24, 24));
        nav.setPrefWidth(210);
        nav.setAlignment(Pos.TOP_CENTER);
        nav.setStyle("-fx-background-color: #0e0e10;");
        return nav;
    }

    private Button navButton(String text, boolean selected, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        if (selected) {
            btn.getStyleClass().add("nav-button-selected");
        }
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void showReceiptsPane() {
        activeModule = "receipts";
        shell.setLeft(buildNav());

        TextField filterField = new TextField();
        filterField.setPromptText("Введите номер акта......");
        filterField.setId("searchField");
        filterField.setPrefWidth(520);
        filterField.setPrefHeight(48);
        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("refresh-btn");
        refreshBtn.setPrefWidth(200);
        refreshBtn.setPrefHeight(48);
        refreshBtn.setMinWidth(200);

        Button confirmBtn = new Button("Подтвердить");
        Button startReceivingBtn = new Button("Начать приёмку");
        Button completeReceivingBtn = new Button("Завершить приёмку");
        Button acceptBtn = new Button("Принять");
        Button startPlacementBtn = new Button("Начать размещение");
        Button tasksBtn = new Button("Задания");

        confirmBtn.getStyleClass().add("refresh-btn");
        startReceivingBtn.getStyleClass().add("refresh-btn");
        completeReceivingBtn.getStyleClass().add("refresh-btn");
        acceptBtn.getStyleClass().add("refresh-btn");
        startPlacementBtn.getStyleClass().add("refresh-btn");
        tasksBtn.getStyleClass().add("refresh-btn");

        confirmBtn.setPrefHeight(48);
        startReceivingBtn.setPrefHeight(48);
        completeReceivingBtn.setPrefHeight(48);
        acceptBtn.setPrefHeight(48);
        startPlacementBtn.setPrefHeight(48);
        tasksBtn.setPrefHeight(48);

        TableView<Receipt> table = buildReceiptTable();
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSelection = newV != null;
            confirmBtn.setDisable(!hasSelection);
            startReceivingBtn.setDisable(!hasSelection);
            completeReceivingBtn.setDisable(!hasSelection);
            acceptBtn.setDisable(!hasSelection);
            startPlacementBtn.setDisable(!hasSelection);
            tasksBtn.setDisable(!hasSelection);
        });
        confirmBtn.setDisable(true);
        startReceivingBtn.setDisable(true);
        completeReceivingBtn.setDisable(true);
        acceptBtn.setDisable(true);
        startPlacementBtn.setDisable(true);
        tasksBtn.setDisable(true);

        confirmBtn.setOnAction(e -> withSelectedReceipt(table, r -> asyncAction(() -> apiClient.confirm(r.id()), table)));
        startReceivingBtn.setOnAction(e -> withSelectedReceipt(table, r -> asyncAction(() -> apiClient.startReceiving(r.id()), table)));
        completeReceivingBtn.setOnAction(e -> withSelectedReceipt(table, r -> asyncAction(() -> apiClient.completeReceiving(r.id()), table)));
        acceptBtn.setOnAction(e -> withSelectedReceipt(table, r -> asyncAction(() -> apiClient.accept(r.id()), table)));
        startPlacementBtn.setOnAction(e -> withSelectedReceipt(table, r -> asyncAction(() -> apiClient.startPlacement(r.id()), table)));
        tasksBtn.setOnAction(e -> withSelectedReceipt(table, this::showTasksDialog));

        HBox filterRow = new HBox(14, filterField, refreshBtn);
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        HBox actionsRow = new HBox(10, confirmBtn, startReceivingBtn, completeReceivingBtn, acceptBtn, startPlacementBtn, tasksBtn);
        VBox card = new VBox(16, filterRow, actionsRow, table);
        card.setPadding(new Insets(24));
        card.setId("main-card");

        refreshBtn.setOnAction(e -> loadReceipts(table, filterField.getText()));
        filterField.setOnAction(e -> loadReceipts(table, filterField.getText()));

        setContent(new VBox(card));
        loadReceipts(table, "");
    }

    private TableView<Receipt> buildReceiptTable() {
        TableView<Receipt> table = new TableView<>();
        table.setId("receiptTable");
        table.setPlaceholder(new Label("Нет данных"));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(72);

        table.setRowFactory(tv -> {
            TableRow<Receipt> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showLinesDialog(row.getItem());
                }
            });
            return row;
        });

        TableColumn<Receipt, String> docNoCol = new TableColumn<>("docNo");
        docNoCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().docNo()));
        docNoCol.setPrefWidth(160);

        TableColumn<Receipt, String> msgCol = new TableColumn<>("messageId");
        msgCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().messageId()));
        msgCol.setPrefWidth(180);

        TableColumn<Receipt, String> statusCol = new TableColumn<>("status");
        statusCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(
            cell.getValue().status() != null ? cell.getValue().status() : ""
        ));
        statusCol.setPrefWidth(140);

        TableColumn<Receipt, String> dateCol = new TableColumn<>("docDate");
        dateCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(
            cell.getValue().docDate() != null ? dateFmt.format(cell.getValue().docDate()) : ""
        ));
        dateCol.setPrefWidth(160);

        TableColumn<Receipt, String> supplierCol = new TableColumn<>("supplier");
        supplierCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().supplier()));
        supplierCol.setPrefWidth(160);

        table.getColumns().addAll(docNoCol, msgCol, statusCol, dateCol, supplierCol);
        return table;
    }

    private void showLinesDialog(Receipt receipt) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Состав: " + receipt.docNo());

        TableView<com.wmsdipl.desktop.model.ReceiptLine> lineTable = new TableView<>();
        lineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lineTable.setPlaceholder(new Label("Загрузка..."));

        TableColumn<com.wmsdipl.desktop.model.ReceiptLine, Number> lineNoCol = new TableColumn<>("lineNo");
        lineNoCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().lineNo()));
        TableColumn<com.wmsdipl.desktop.model.ReceiptLine, Number> skuCol = new TableColumn<>("skuId");
        skuCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().skuId()));
        TableColumn<com.wmsdipl.desktop.model.ReceiptLine, Number> packCol = new TableColumn<>("packagingId");
        packCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().packagingId()));
        TableColumn<com.wmsdipl.desktop.model.ReceiptLine, String> uomCol = new TableColumn<>("uom");
        uomCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().uom()));
        TableColumn<com.wmsdipl.desktop.model.ReceiptLine, Number> qtyCol = new TableColumn<>("qtyExpected");
        qtyCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().qtyExpected()));
        TableColumn<com.wmsdipl.desktop.model.ReceiptLine, String> ssccCol = new TableColumn<>("ssccExpected");
        ssccCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().ssccExpected()));

        lineTable.getColumns().addAll(lineNoCol, skuCol, packCol, uomCol, qtyCol, ssccCol);

        VBox box = new VBox(lineTable);
        box.setPadding(new Insets(12));
        dialog.setScene(new Scene(box, 640, 420));
        dialog.show();

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return apiClient.getReceiptLines(receipt.id());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .whenComplete((lines, error) -> Platform.runLater(() -> {
                if (error != null) {
                    Alert alert = new Alert(AlertType.ERROR, "Ошибка загрузки строк: " + error.getMessage());
                    alert.initOwner(dialog);
                    alert.showAndWait();
                    lineTable.setPlaceholder(new Label("Ошибка"));
                    return;
                }
                lineTable.setItems(FXCollections.observableArrayList(lines));
                lineTable.setPlaceholder(new Label("Нет строк"));
            }));
    }

    private void loadReceipts(TableView<Receipt> table, String filter) {
        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return apiClient.listReceipts();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .whenComplete((receipts, error) -> Platform.runLater(() -> {
                if (error != null) {
                    table.setPlaceholder(new Label("Ошибка: " + error.getMessage()));
                    table.setItems(FXCollections.observableArrayList());
                    return;
                }
                List<Receipt> filtered = receipts;
                if (filter != null && !filter.isBlank()) {
                    String lower = filter.toLowerCase();
                    filtered = receipts.stream()
                        .filter(r -> r.docNo() != null && r.docNo().toLowerCase().contains(lower))
                        .toList();
                }
                table.setItems(FXCollections.observableArrayList(filtered));
            }));
    }

    private void showTopologyPane() {
        activeModule = "topology";
        shell.setLeft(buildNav());

        Label header = new Label("Топология склада");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        ListView<com.wmsdipl.desktop.model.Zone> zonesView = new ListView<>();
        zonesView.setPrefWidth(240);

        TableView<Location> locTable = new TableView<>();
        locTable.setPlaceholder(new Label("Нет ячеек"));
        TableColumn<Location, String> locCode = new TableColumn<>("code");
        locCode.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().code()));
        TableColumn<Location, String> locStatus = new TableColumn<>("status");
        locStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().status()));
        TableColumn<Location, Number> locMax = new TableColumn<>("maxPallets");
        locMax.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().maxPallets()));
        TableColumn<Location, String> locZone = new TableColumn<>("zone");
        locZone.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().zone() != null ? c.getValue().zone().code() : ""));
        locTable.getColumns().addAll(locCode, locStatus, locMax, locZone);

        VBox left = new VBox(10, new Label("Зоны"), zonesView);
        left.setPadding(new Insets(12));
        left.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        VBox right = new VBox(10, new Label("Ячейки"), locTable);
        right.setPadding(new Insets(12));
        right.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");

        HBox body = new HBox(12, left, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        setContent(new VBox(10, header, body));

        loadTopology(zonesView, locTable);
        zonesView.getSelectionModel().selectedItemProperty().addListener((obs, o, z) -> filterLocationsByZone(locTable, z));
    }

    private void loadTopology(ListView<com.wmsdipl.desktop.model.Zone> zonesView, TableView<Location> locTable) {
        CompletableFuture
            .supplyAsync(() -> {
                try {
                    var zones = apiClient.listZones();
                    var locs = apiClient.listLocations();
                    return new Object[]{zones, locs};
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .whenComplete((data, error) -> Platform.runLater(() -> {
                if (error != null) {
                    zonesView.setItems(FXCollections.observableArrayList());
                    locTable.setItems(FXCollections.observableArrayList());
                    locTable.setPlaceholder(new Label("Ошибка: " + error.getMessage()));
                    return;
                }
                @SuppressWarnings("unchecked")
                List<com.wmsdipl.desktop.model.Zone> zones = (List<com.wmsdipl.desktop.model.Zone>) data[0];
                @SuppressWarnings("unchecked")
                List<Location> locs = (List<Location>) data[1];
                zonesView.setItems(FXCollections.observableArrayList(zones));
                locTable.setUserData(locs);
                locTable.setItems(FXCollections.observableArrayList(locs));
            }));
    }

    private void filterLocationsByZone(TableView<Location> locTable, com.wmsdipl.desktop.model.Zone zone) {
        @SuppressWarnings("unchecked")
        List<Location> all = (List<Location>) locTable.getUserData();
        if (all == null) {
            return;
        }
        List<Location> filtered = zone == null ? all : all.stream()
            .filter(l -> l.zone() != null && zone.id().equals(l.zone().id()))
            .toList();
        locTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showPalletsPane() {
        activeModule = "pallets";
        shell.setLeft(buildNav());

        Label header = new Label("Паллеты");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Button refresh = new Button("Обновить");
        refresh.getStyleClass().add("refresh-btn");
        
        Button createBtn = new Button("Создать паллету");
        createBtn.getStyleClass().add("refresh-btn");
        createBtn.setOnAction(e -> showCreatePalletDialog());
        
        HBox toolbar = new HBox(12, refresh, createBtn);

        TableView<Pallet> table = new TableView<>();
        table.setPlaceholder(new Label("Нет данных"));
        table.getColumns().addAll(
            column("code", p -> p.code()),
            column("status", p -> p.status()),
            column("location", p -> p.location() != null ? p.location().code() : ""),
            column("skuId", p -> p.skuId()),
            column("qty", p -> p.quantity()),
            column("receiptId", p -> p.receipt() != null ? p.receipt().id() : null)
        );

        refresh.setOnAction(e -> loadList(table, () -> apiClient.listPallets()));
        loadList(table, () -> apiClient.listPallets());

        VBox layout = new VBox(12, header, toolbar, table);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        setContent(layout);
    }

    private void showTasksPane() {
        activeModule = "tasks";
        shell.setLeft(buildNav());

        Label header = new Label("Задания");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        TextField receiptFilter = new TextField();
        receiptFilter.setPromptText("receiptId (опционально)");
        Button refresh = new Button("Обновить");
        refresh.getStyleClass().add("refresh-btn");

        TableView<com.wmsdipl.desktop.model.Task> table = new TableView<>();
        table.setPlaceholder(new Label("Нет данных"));
        
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskIdCol = column("ID", com.wmsdipl.desktop.model.Task::id);
        taskIdCol.setPrefWidth(50);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskDocCol = column("Документ", t -> t.receiptDocNo() != null ? t.receiptDocNo() : "");
        taskDocCol.setPrefWidth(150);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskTypeCol = column("Тип", com.wmsdipl.desktop.model.Task::taskType);
        taskTypeCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskStatusCol = column("Статус", com.wmsdipl.desktop.model.Task::status);
        taskStatusCol.setPrefWidth(120);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskAssigneeCol = column("Исполнитель", com.wmsdipl.desktop.model.Task::assignee);
        taskAssigneeCol.setPrefWidth(120);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskPalletCol = column("Паллета", com.wmsdipl.desktop.model.Task::palletId);
        taskPalletCol.setPrefWidth(80);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskSourceCol = column("Откуда", com.wmsdipl.desktop.model.Task::sourceLocationId);
        taskSourceCol.setPrefWidth(80);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskTargetCol = column("Куда", com.wmsdipl.desktop.model.Task::targetLocationId);
        taskTargetCol.setPrefWidth(80);
        
        table.getColumns().addAll(taskIdCol, taskDocCol, taskTypeCol, taskStatusCol, taskAssigneeCol, taskPalletCol, taskSourceCol, taskTargetCol);

        refresh.setOnAction(e -> loadTasks(table, receiptFilter.getText()));
        loadTasks(table, null);

        HBox controls = new HBox(10, receiptFilter, refresh);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox layout = new VBox(12, header, controls, table);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        setContent(layout);
    }

    private void loadTasks(TableView<com.wmsdipl.desktop.model.Task> table, String receiptFilter) {
        Long receiptId = null;
        if (receiptFilter != null && !receiptFilter.isBlank()) {
            try {
                receiptId = Long.parseLong(receiptFilter.trim());
            } catch (NumberFormatException ex) {
                table.setPlaceholder(new Label("Неверный receiptId"));
                table.setItems(FXCollections.observableArrayList());
                return;
            }
        }
        final Long rid = receiptId;
        loadList(table, () -> apiClient.listTasks(rid));
    }

    private void showTerminalPane() {
        activeModule = "terminal";
        shell.setLeft(buildNav());

        Label header = new Label("Терминал приёмки");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Filter tabs
        TabPane filterTabs = new TabPane();
        filterTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab myTasksTab = new Tab("Мои задачи");
        Tab allTasksTab = new Tab("Все задачи");

        TableView<com.wmsdipl.desktop.model.Task> taskTable = new TableView<>();
        taskTable.setPlaceholder(new Label("Нет заданий"));
        taskTable.setFixedCellSize(60);
        
        TableColumn<com.wmsdipl.desktop.model.Task, Object> idCol = column("ID", com.wmsdipl.desktop.model.Task::id);
        idCol.setPrefWidth(50);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> typeCol = column("Тип", com.wmsdipl.desktop.model.Task::taskType);
        typeCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> statusCol = column("Статус", com.wmsdipl.desktop.model.Task::status);
        statusCol.setPrefWidth(120);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> receiptCol = column("Документ", t -> t.receiptDocNo() != null ? t.receiptDocNo() : "");
        receiptCol.setPrefWidth(180);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> qtyCol = column("Прогресс", t -> {
            BigDecimal done = t.qtyDone();
            BigDecimal assigned = t.qtyAssigned();
            if (done == null) done = BigDecimal.ZERO;
            if (assigned == null) assigned = BigDecimal.ZERO;
            return done + " / " + assigned;
        });
        qtyCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> assigneeCol = column("Исполнитель", com.wmsdipl.desktop.model.Task::assignee);
        assigneeCol.setPrefWidth(120);
        
        taskTable.getColumns().addAll(idCol, typeCol, statusCol, receiptCol, qtyCol, assigneeCol);

        // Double-click to open task
        taskTable.setRowFactory(tv -> {
            TableRow<com.wmsdipl.desktop.model.Task> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    com.wmsdipl.desktop.model.Task task = row.getItem();
                    if ("RECEIVING".equals(task.taskType()) || "PLACEMENT".equals(task.taskType())) {
                        openTaskExecutionDialog(task);
                    }
                }
            });
            return row;
        });

        // My tasks table
        TableView<com.wmsdipl.desktop.model.Task> allTasksTable = new TableView<>();
        allTasksTable.setPlaceholder(new Label("Нет заданий"));
        allTasksTable.setFixedCellSize(60);
        
        TableColumn<com.wmsdipl.desktop.model.Task, Object> allIdCol = column("ID", com.wmsdipl.desktop.model.Task::id);
        allIdCol.setPrefWidth(50);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> allTypeCol = column("Тип", com.wmsdipl.desktop.model.Task::taskType);
        allTypeCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> allStatusCol = column("Статус", com.wmsdipl.desktop.model.Task::status);
        allStatusCol.setPrefWidth(120);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> allReceiptCol = column("Документ", t -> t.receiptDocNo() != null ? t.receiptDocNo() : "");
        allReceiptCol.setPrefWidth(180);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> allQtyCol = column("Прогресс", t -> {
            BigDecimal done = t.qtyDone();
            BigDecimal assigned = t.qtyAssigned();
            if (done == null) done = BigDecimal.ZERO;
            if (assigned == null) assigned = BigDecimal.ZERO;
            return done + " / " + assigned;
        });
        allQtyCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> allAssigneeCol = column("Исполнитель", t -> t.assignee() != null ? t.assignee() : "Не назначен");
        allAssigneeCol.setPrefWidth(120);
        
        allTasksTable.getColumns().addAll(allIdCol, allTypeCol, allStatusCol, allReceiptCol, allQtyCol, allAssigneeCol);
        
        // Double-click handler for all tasks
        allTasksTable.setRowFactory(tv -> {
            TableRow<com.wmsdipl.desktop.model.Task> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    com.wmsdipl.desktop.model.Task task = row.getItem();
                    if ("RECEIVING".equals(task.taskType()) || "PLACEMENT".equals(task.taskType())) {
                        openTaskExecutionDialog(task);
                    }
                }
            });
            return row;
        });

        myTasksTab.setContent(taskTable);
        allTasksTab.setContent(allTasksTable);
        
        filterTabs.getTabs().addAll(myTasksTab, allTasksTab);

        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("refresh-btn");
        refreshBtn.setPrefHeight(48);

        // Load my tasks
        Runnable loadMyTasks = () -> {
            String currentUser = apiClient.getCurrentUsername();
            if (currentUser != null) {
                CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.getAllTasks().stream()
                        .filter(t -> currentUser.equals(t.assignee()))
                        .filter(t -> "RECEIVING".equals(t.taskType()) || "PLACEMENT".equals(t.taskType()))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                }).whenComplete((tasks, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        taskTable.setPlaceholder(new Label("Ошибка: " + error.getMessage()));
                        taskTable.setItems(FXCollections.observableArrayList());
                    } else {
                        taskTable.setItems(FXCollections.observableArrayList(tasks));
                    }
                }));
            }
        };
        
        // Load all tasks
        Runnable loadAllTasks = () -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.getAllTasks().stream()
                        .filter(t -> "RECEIVING".equals(t.taskType()) || "PLACEMENT".equals(t.taskType()))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).whenComplete((tasks, error) -> Platform.runLater(() -> {
                if (error != null) {
                    allTasksTable.setPlaceholder(new Label("Ошибка: " + error.getMessage()));
                    allTasksTable.setItems(FXCollections.observableArrayList());
                } else {
                    allTasksTable.setItems(FXCollections.observableArrayList(tasks));
                }
            }));
        };
        
        // Switch tab listener
        filterTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == myTasksTab) {
                loadMyTasks.run();
            } else if (newTab == allTasksTab) {
                loadAllTasks.run();
            }
        });

        refreshBtn.setOnAction(e -> {
            if (filterTabs.getSelectionModel().getSelectedItem() == myTasksTab) {
                loadMyTasks.run();
            } else {
                loadAllTasks.run();
            }
        });

        VBox layout = new VBox(12, header, refreshBtn, filterTabs);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        VBox.setVgrow(filterTabs, Priority.ALWAYS);

        setContent(layout);
        loadMyTasks.run();
    }

    private void showSettingsPane() {
        activeModule = "settings";
        shell.setLeft(buildNav());
        Label header = new Label("Настройки");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField coreApiField = new TextField(coreApiBase);
        TextField importApiField = new TextField(importApiBase);
        TextField importFolderField = new TextField();
        Label statusLabel = new Label("Сохраните настройки");
        statusLabel.setStyle("-fx-text-fill: white;");

        Button saveApi = new Button("Сохранить API адреса");
        saveApi.setOnAction(e -> {
            coreApiBase = coreApiField.getText();
            importApiBase = importApiField.getText();
            apiClient = new ApiClient(coreApiBase);
            statusLabel.setText("API базовые адреса сохранены");
        });

        Button saveFolder = new Button("Сохранить директорию импорта");
        saveFolder.setOnAction(e -> updateImportFolder(importApiField.getText(), importFolderField.getText(), statusLabel));

        // SKU catalog
        Label skuHeader = new Label("Справочник номенклатур (SKU)");
        skuHeader.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        TableView<Sku> skuTable = new TableView<>();
        skuTable.setPlaceholder(new Label("Нет номенклатур"));
        skuTable.setPrefHeight(250);
        skuTable.getColumns().addAll(
            column("ID", Sku::id),
            column("Код", Sku::code),
            column("Название", Sku::name),
            column("Ед.изм.", Sku::uom)
        );
        
        Button refreshSku = new Button("Обновить");
        refreshSku.getStyleClass().add("refresh-btn");
        refreshSku.setOnAction(e -> loadList(skuTable, () -> apiClient.listSkus()));
        
        Button createSku = new Button("Создать SKU");
        createSku.getStyleClass().add("refresh-btn");
        createSku.setOnAction(e -> showCreateSkuDialog(skuTable));
        
        Button deleteSku = new Button("Удалить");
        deleteSku.getStyleClass().add("refresh-btn");
        deleteSku.setDisable(true);
        
        skuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            deleteSku.setDisable(newV == null);
        });
        
        deleteSku.setOnAction(e -> {
            Sku selected = skuTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                CompletableFuture.runAsync(() -> {
                    try {
                        apiClient.deleteSku(selected.id());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }).whenComplete((v, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        Alert alert = new Alert(AlertType.ERROR, "Ошибка удаления: " + error.getMessage());
                        alert.showAndWait();
                    }
                    loadList(skuTable, () -> apiClient.listSkus());
                }));
            }
        });
        
        HBox skuButtons = new HBox(10, refreshSku, createSku, deleteSku);
        loadList(skuTable, () -> apiClient.listSkus());

        // Putaway rules list
        Label rulesHeader = new Label("Правила размещения");
        rulesHeader.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        TableView<PutawayRule> rulesTable = new TableView<>();
        rulesTable.setPlaceholder(new Label("Нет правил"));
        rulesTable.setPrefHeight(200);
        rulesTable.getColumns().addAll(
            column("priority", PutawayRule::priority),
            column("name", PutawayRule::name),
            column("strategy", PutawayRule::strategyType),
            column("zoneId", PutawayRule::zoneId),
            column("velocity", PutawayRule::velocityClass),
            column("active", PutawayRule::active)
        );
        Button refreshRules = new Button("Обновить правила");
        refreshRules.getStyleClass().add("refresh-btn");
        refreshRules.setOnAction(e -> loadList(rulesTable, () -> apiClient.listPutawayRules()));
        loadList(rulesTable, () -> apiClient.listPutawayRules());

        VBox layout = new VBox(10,
            header,
            new Label("CORE API"), coreApiField,
            new Label("Import API"), importApiField,
            new Label("Путь к папке импорта"), importFolderField,
            new HBox(8, saveApi, saveFolder),
            statusLabel,
            skuHeader,
            skuButtons,
            skuTable,
            rulesHeader,
            refreshRules,
            rulesTable
        );
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        layout.setFillWidth(true);

        setContent(layout);
    }

    private void showTasksDialog(Receipt receipt) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Задания прихода " + receipt.docNo());

        TableView<com.wmsdipl.desktop.model.Task> table = new TableView<>();
        table.setPlaceholder(new Label("Загрузка..."));
        table.getColumns().addAll(
            column("id", com.wmsdipl.desktop.model.Task::id),
            column("taskType", com.wmsdipl.desktop.model.Task::taskType),
            column("status", com.wmsdipl.desktop.model.Task::status),
            column("assignee", com.wmsdipl.desktop.model.Task::assignee),
            column("palletId", com.wmsdipl.desktop.model.Task::palletId),
            column("sourceLoc", com.wmsdipl.desktop.model.Task::sourceLocationId),
            column("targetLoc", com.wmsdipl.desktop.model.Task::targetLocationId)
        );

        dialog.setScene(new Scene(new VBox(table), 780, 420));
        dialog.show();

        loadList(table, () -> apiClient.listTasks(receipt.id()));
    }

    private void updateImportFolder(String importBase, String folder, Label importStatus) {
        if (folder == null || folder.isBlank()) {
            importStatus.setText("Укажите директорию импорта");
            return;
        }
        importStatus.setText("Сохранение директории...");
        CompletableFuture.runAsync(() -> {
            try {
                new ImportServiceClient(importBase).updateFolder(folder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((v, error) -> Platform.runLater(() -> {
            if (error != null) {
                importStatus.setText("Ошибка директории: " + error.getMessage());
            } else {
                importStatus.setText("Директория установлена: " + folder);
            }
        }));
    }

    private <T> void loadList(TableView<T> table, ThrowingSupplier<List<T>> supplier) {
        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .whenComplete((list, error) -> Platform.runLater(() -> {
                if (error != null) {
                    table.setPlaceholder(new Label("Ошибка: " + error.getMessage()));
                    table.setItems(FXCollections.observableArrayList());
                    return;
                }
                table.setItems(FXCollections.observableArrayList(list));
            }));
    }

    private void asyncAction(ThrowingRunnable action, TableView<Receipt> table) {
        CompletableFuture.runAsync(() -> {
            try {
                action.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((v, error) -> Platform.runLater(() -> {
            if (error != null) {
                new Alert(AlertType.ERROR, "Ошибка: " + error.getMessage()).showAndWait();
            }
            loadReceipts(table, "");
        }));
    }

    private void withSelectedReceipt(TableView<Receipt> table, Consumer<Receipt> consumer) {
        Receipt selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            consumer.accept(selected);
        }
    }

    private <T, R> TableColumn<T, Object> column(String title, Function<T, R> extractor) {
        TableColumn<T, Object> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new SimpleObjectProperty<>(extractor.apply(cell.getValue())));
        return col;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private void setContent(VBox node) {
        contentHolder.getChildren().setAll(node);
    }

    private boolean showLoginDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Авторизация");

        TextField userField = new TextField();
        userField.setPromptText("Логин");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Пароль");
        Label info = new Label("Введите логин и пароль");
        Button loginBtn = new Button("Войти");

        final boolean[] success = {false};
        loginBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();
            if (username.isBlank() || password.isBlank()) {
                info.setText("Заполните логин/пароль");
                return;
            }
            loginBtn.setDisable(true);
            info.setText("Проверка...");
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.login(username, password);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((ok, error) -> Platform.runLater(() -> {
                loginBtn.setDisable(false);
                if (error != null || !Boolean.TRUE.equals(ok)) {
                    info.setText("Неверный логин или пароль");
                    return;
                }
                success[0] = true;
                dialog.close();
            }));
        });

        VBox box = new VBox(8, info, userField, passField, loginBtn);
        box.setPadding(new Insets(12));
        box.setAlignment(Pos.CENTER_LEFT);
        dialog.setScene(new Scene(box, 300, 160));
        dialog.showAndWait();
        return success[0];
    }

    private void openTaskExecutionDialog(com.wmsdipl.desktop.model.Task initialTask) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        final com.wmsdipl.desktop.model.Task[] currentTask = {initialTask};
        
        // Updatable title
        Runnable updateTitle = () -> {
            String docNo = currentTask[0].receiptDocNo() != null ? currentTask[0].receiptDocNo() : "";
            dialog.setTitle("Задание #" + currentTask[0].id() + " - " + docNo + " [" + currentTask[0].status() + "]");
        };
        updateTitle.run();

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Document (expected data)
        Tab docTab = new Tab("Документ");
        VBox docContent = buildDocumentTab(currentTask[0]);
        docTab.setContent(docContent);

        // Tab 2: Fact (scan form)
        Tab factTab = new Tab("Факт");
        VBox factContent = buildFactTab(currentTask[0], dialog);
        factTab.setContent(factContent);

        tabs.getTabs().addAll(docTab, factTab);
        tabs.getSelectionModel().select(factTab); // Start on Fact tab

        // Action buttons
        Button assignBtn = new Button("Назначить");
        assignBtn.getStyleClass().add("refresh-btn");
        assignBtn.setPrefHeight(40);
        assignBtn.setPrefWidth(150);
        
        Button startBtn = new Button("Начать");
        startBtn.getStyleClass().add("refresh-btn");
        startBtn.setPrefHeight(40);
        startBtn.setPrefWidth(150);
        
        Button completeBtn = new Button("Завершить");
        completeBtn.getStyleClass().add("refresh-btn");
        completeBtn.setPrefHeight(40);
        completeBtn.setPrefWidth(150);
        
        Button releaseBtn = new Button("Освободить");
        releaseBtn.getStyleClass().add("refresh-btn");
        releaseBtn.setPrefHeight(40);
        releaseBtn.setPrefWidth(150);
        
        Label actionStatus = new Label("");
        actionStatus.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        // Update button states based on task status
        Runnable updateButtons = () -> {
            String status = currentTask[0].status();
            String currentUser = apiClient.getCurrentUsername();
            String assignee = currentTask[0].assignee();
            
            assignBtn.setDisable(!"NEW".equals(status));
            startBtn.setDisable(!"ASSIGNED".equals(status) || !currentUser.equals(assignee));
            completeBtn.setDisable(!"IN_PROGRESS".equals(status) || !currentUser.equals(assignee));
            releaseBtn.setDisable(!("ASSIGNED".equals(status) || "IN_PROGRESS".equals(status)) || !currentUser.equals(assignee));
        };
        updateButtons.run();
        
        // Assign action
        assignBtn.setOnAction(e -> {
            assignBtn.setDisable(true);
            actionStatus.setText("Назначение...");
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.assignTask(currentTask[0].id(), apiClient.getCurrentUsername());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((updatedTask, error) -> Platform.runLater(() -> {
                if (error != null) {
                    actionStatus.setText("Ошибка: " + error.getMessage());
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    assignBtn.setDisable(false);
                } else {
                    currentTask[0] = updatedTask;
                    actionStatus.setText("Задание назначено");
                    actionStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                    updateButtons.run();
                    updateTitle.run();
                    docTab.setContent(buildDocumentTab(currentTask[0]));
                }
            }));
        });
        
        // Start action
        startBtn.setOnAction(e -> {
            startBtn.setDisable(true);
            actionStatus.setText("Начало...");
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.startTask(currentTask[0].id());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((updatedTask, error) -> Platform.runLater(() -> {
                if (error != null) {
                    actionStatus.setText("Ошибка: " + error.getMessage());
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    startBtn.setDisable(false);
                } else {
                    currentTask[0] = updatedTask;
                    actionStatus.setText("Задание начато");
                    actionStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                    updateButtons.run();
                    updateTitle.run();
                    docTab.setContent(buildDocumentTab(currentTask[0]));
                }
            }));
        });
        
        // Complete action
        completeBtn.setOnAction(e -> {
            completeBtn.setDisable(true);
            actionStatus.setText("Проверка расхождений...");
            
            // First, fetch fresh task data from server to get updated qtyDone
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.getTask(currentTask[0].id());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((freshTask, fetchError) -> Platform.runLater(() -> {
                if (fetchError != null) {
                    actionStatus.setText("Ошибка загрузки задачи: " + fetchError.getMessage());
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    completeBtn.setDisable(false);
                    return;
                }
                
                // Update currentTask with fresh data
                currentTask[0] = freshTask;
                
                // Check for UNDER_QTY on client side
                boolean hasUnderQty = false;
                if (freshTask.qtyAssigned() != null && freshTask.qtyDone() != null) {
                    hasUnderQty = freshTask.qtyDone().compareTo(freshTask.qtyAssigned()) < 0;
                }
                
                // Also check for existing scan discrepancies from backend
                final boolean clientDetectedUnderQty = hasUnderQty;
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return apiClient.hasDiscrepancies(freshTask.id());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }).whenComplete((hasScanDiscrepancies, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        actionStatus.setText("Ошибка проверки: " + error.getMessage());
                        actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                        completeBtn.setDisable(false);
                        return;
                    }
                    
                    // If has any discrepancies (UNDER_QTY or existing scan discrepancies), show confirmation dialog
                    if (hasScanDiscrepancies || clientDetectedUnderQty) {
                        boolean confirmed = showDiscrepancyConfirmationDialog();
                        if (!confirmed) {
                            actionStatus.setText("Завершение отменено");
                            actionStatus.setStyle("-fx-text-fill: #FFC107; -fx-font-size: 14px;");
                            completeBtn.setDisable(false);
                            return;
                        }
                    }
                    
                    // Proceed with completion
                    actionStatus.setText("Завершение...");
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return apiClient.completeTask(currentTask[0].id());
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }).whenComplete((updatedTask, error2) -> Platform.runLater(() -> {
                        if (error2 != null) {
                            actionStatus.setText("Ошибка: " + error2.getMessage());
                            actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                            completeBtn.setDisable(false);
                        } else {
                            currentTask[0] = updatedTask;
                            actionStatus.setText("Задание завершено!");
                            actionStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                            updateButtons.run();
                            updateTitle.run();
                            docTab.setContent(buildDocumentTab(currentTask[0]));
                            // Rebuild Fact tab to refresh scan table with updated discrepancy flags
                            factTab.setContent(buildFactTab(currentTask[0], dialog));
                        }
                    }));
                }));
            }));
        });
        
        // Release action
        releaseBtn.setOnAction(e -> {
            releaseBtn.setDisable(true);
            actionStatus.setText("Освобождение...");
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.releaseTask(currentTask[0].id());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((updatedTask, error) -> Platform.runLater(() -> {
                if (error != null) {
                    actionStatus.setText("Ошибка: " + error.getMessage());
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    releaseBtn.setDisable(false);
                } else {
                    currentTask[0] = updatedTask;
                    actionStatus.setText("Задание освобождено");
                    actionStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                    updateButtons.run();
                    updateTitle.run();
                    docTab.setContent(buildDocumentTab(currentTask[0]));
                }
            }));
        });
        
        HBox actionButtons = new HBox(10, assignBtn, startBtn, completeBtn, releaseBtn, actionStatus);
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        actionButtons.setPadding(new Insets(10));
        actionButtons.setStyle("-fx-background-color: #1c1c1c;");

        VBox root = new VBox(actionButtons, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Scene scene = new Scene(root, 800, 650);
        applyStyles(scene);
        dialog.setScene(scene);
        dialog.show();
    }

    private VBox buildDocumentTab(com.wmsdipl.desktop.model.Task task) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");

        Label header = new Label("Ожидаемые данные");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label receiptLabel = new Label("Приход: " + (task.receiptDocNo() != null ? task.receiptDocNo() : "N/A"));
        Label taskTypeLabel = new Label("Тип задания: " + task.taskType());
        Label statusLabel = new Label("Статус: " + task.status());
        Label qtyLabel = new Label("Количество: " + task.qtyAssigned());
        Label assigneeLabel = new Label("Исполнитель: " + (task.assignee() != null ? task.assignee() : "не назначен"));

        receiptLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        taskTypeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        qtyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        assigneeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        content.getChildren().addAll(header, receiptLabel, taskTypeLabel, statusLabel, qtyLabel, assigneeLabel);
        return content;
    }

    private VBox buildFactTab(com.wmsdipl.desktop.model.Task task, Stage dialog) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1c1c1c;");

        Label header = new Label("Сканирование");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Scan fields (blue, 16px, with 📷 icon)
        Label palletLabel = new Label("📷 Паллета:");
        palletLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px;");
        TextField palletField = new TextField();
        palletField.setPromptText("Отсканируйте код паллеты");
        palletField.getStyleClass().add("scan-field");
        palletField.setPrefHeight(48);

        Label barcodeLabel = new Label("📷 Баркод:");
        barcodeLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px;");
        TextField barcodeField = new TextField();
        barcodeField.setPromptText("Отсканируйте баркод товара");
        barcodeField.getStyleClass().add("scan-field");
        barcodeField.setPrefHeight(48);

        // Input fields (gray, 14px, with ✏️ icon)
        Label qtyLabel = new Label("✏️ Количество:");
        qtyLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
        TextField qtyField = new TextField();
        qtyField.setPromptText("Введите количество");
        qtyField.getStyleClass().add("input-field");
        qtyField.setPrefHeight(40);
        
        // TextFormatter for integers only
        qtyField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d+")) {
                return change;
            }
            return null;
        }));

        Label commentLabel = new Label("✏️ Комментарий:");
        commentLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
        TextArea commentField = new TextArea();
        commentField.setPromptText("Комментарий (опционально)");
        commentField.getStyleClass().add("input-field");
        commentField.setPrefHeight(60);
        commentField.setMaxHeight(60);

        // Submit button
        Button submitBtn = new Button("Отправить");
        submitBtn.getStyleClass().add("refresh-btn");
        submitBtn.setPrefHeight(48);
        submitBtn.setPrefWidth(200);

        // Scan history table
        TableView<Scan> scanTable = new TableView<>();
        scanTable.setPlaceholder(new Label("Нет сканов"));
        scanTable.setPrefHeight(200);
        scanTable.getColumns().addAll(
            column("Паллета", Scan::palletCode),
            column("Баркод", Scan::barcode),
            column("Кол-во", Scan::qty),
            column("Расхождение", s -> {
                if (s.discrepancy() == null || !s.discrepancy()) {
                    return "✓ ОК";
                }
                return "⚠ Есть расхождение";
            }),
            column("Время", s -> s.scannedAt() != null ? s.scannedAt().toString() : "")
        );

        // Enter key navigation
        palletField.setOnAction(e -> barcodeField.requestFocus());
        barcodeField.setOnAction(e -> qtyField.requestFocus());
        qtyField.setOnAction(e -> {
            if (!qtyField.getText().isBlank()) {
                submitBtn.fire();
            } else {
                commentField.requestFocus();
            }
        });

        // Submit action
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        submitBtn.setOnAction(e -> {
            String palletCode = palletField.getText().trim();
            String barcode = barcodeField.getText().trim();
            String qtyStr = qtyField.getText().trim();

            if (palletCode.isEmpty()) {
                showError(statusLabel, "Код паллеты обязателен", palletField);
                return;
            }
            if (qtyStr.isEmpty()) {
                showError(statusLabel, "Количество обязательно", qtyField);
                return;
            }

            Integer qty = Integer.parseInt(qtyStr);
            String comment = commentField.getText().trim();

            submitBtn.setDisable(true);
            statusLabel.setText("Отправка...");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.recordScan(task.id(), palletCode, barcode, qty, comment);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((scan, error) -> Platform.runLater(() -> {
                submitBtn.setDisable(false);
                if (error != null) {
                    showError(statusLabel, "Ошибка: " + error.getMessage(), palletField);
                } else {
                    showSuccess(statusLabel, "Скан записан!", palletField);
                    // Clear fields
                    palletField.clear();
                    barcodeField.clear();
                    qtyField.clear();
                    commentField.clear();
                    palletField.requestFocus();
                    // Reload scans
                    loadTaskScans(task, scanTable);
                }
            }));
        });

        // Load initial scans
        loadTaskScans(task, scanTable);

        VBox form = new VBox(8, 
            palletLabel, palletField,
            barcodeLabel, barcodeField,
            qtyLabel, qtyField,
            commentLabel, commentField,
            submitBtn,
            statusLabel
        );

        content.getChildren().addAll(header, form, new Label("История сканов:"), scanTable);
        VBox.setVgrow(scanTable, Priority.ALWAYS);

        // Auto-focus pallet field
        Platform.runLater(() -> palletField.requestFocus());

        return content;
    }

    private void loadTaskScans(com.wmsdipl.desktop.model.Task task, TableView<Scan> table) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.getTaskScans(task.id());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((scans, error) -> Platform.runLater(() -> {
            if (error != null) {
                table.setPlaceholder(new Label("Ошибка: " + error.getMessage()));
            } else {
                table.setItems(FXCollections.observableArrayList(scans));
            }
        }));
    }

    private void showSuccess(Label label, String message, TextField field) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Green flash animation
        field.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 2px;");
        FadeTransition flash = new FadeTransition(Duration.millis(200), field);
        flash.setFromValue(0.5);
        flash.setToValue(1.0);
        flash.setCycleCount(2);
        flash.setAutoReverse(true);
        flash.setOnFinished(e -> field.setStyle(""));
        flash.play();
    }

    private void showError(Label label, String message, TextField field) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Red shake animation
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), field);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    private void showCreatePalletDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Создать паллету");

        Label codeLabel = new Label("Код паллеты:");
        codeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        TextField codeField = new TextField();
        codeField.setPromptText("Например: PLT-001");
        codeField.setPrefWidth(300);
        
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        Button createBtn = new Button("Создать");
        createBtn.getStyleClass().add("refresh-btn");
        createBtn.setPrefWidth(150);
        
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("refresh-btn");
        cancelBtn.setPrefWidth(150);
        cancelBtn.setOnAction(e -> dialog.close());
        
        HBox buttonBox = new HBox(12, createBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        createBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                statusLabel.setText("Введите код паллеты");
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }
            
            createBtn.setDisable(true);
            statusLabel.setText("Создание...");
            statusLabel.setStyle("-fx-text-fill: #FFC107; -fx-font-size: 12px;");
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.createPallet(code);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((pallet, error) -> Platform.runLater(() -> {
                if (error != null) {
                    statusLabel.setText("Ошибка: " + error.getCause().getMessage());
                    statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                    createBtn.setDisable(false);
                } else {
                    statusLabel.setText("Паллета создана: " + pallet.code());
                    statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
                    
                    // Auto-close after 1 second
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(() -> {
                                dialog.close();
                                showPalletsPane(); // Refresh pallets view
                            });
                        } catch (InterruptedException ex) {
                            // Ignore
                        }
                    }).start();
                }
            }));
        });
        
        VBox layout = new VBox(12, codeLabel, codeField, statusLabel, buttonBox);
        layout.setPadding(new Insets(24));
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setStyle("-fx-background-color: #1c1c1c;");
        
        Scene scene = new Scene(layout, 400, 200);
        applyStyles(scene);
        dialog.setScene(scene);
        dialog.show();
        
        codeField.requestFocus();
    }

    private void showCreateSkuDialog(TableView<Sku> skuTable) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Создать номенклатуру");

        Label codeLabel = new Label("Код SKU:");
        codeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField codeField = new TextField();
        codeField.setPromptText("Например: SKU-001");
        codeField.setPrefWidth(300);

        Label nameLabel = new Label("Название:");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField nameField = new TextField();
        nameField.setPromptText("Название товара");
        nameField.setPrefWidth(300);

        Label uomLabel = new Label("Единица измерения:");
        uomLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField uomField = new TextField("ШТ");
        uomField.setPrefWidth(300);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        Button createBtn = new Button("Создать");
        createBtn.getStyleClass().add("refresh-btn");
        createBtn.setPrefWidth(150);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("refresh-btn");
        cancelBtn.setPrefWidth(150);
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(12, createBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        createBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String uom = uomField.getText().trim();

            if (code.isEmpty()) {
                statusLabel.setText("Введите код SKU");
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }
            if (name.isEmpty()) {
                statusLabel.setText("Введите название");
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }
            if (uom.isEmpty()) {
                statusLabel.setText("Введите единицу измерения");
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }

            createBtn.setDisable(true);
            statusLabel.setText("Создание...");
            statusLabel.setStyle("-fx-text-fill: #FFC107; -fx-font-size: 12px;");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.createSku(code, name, uom);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((sku, error) -> Platform.runLater(() -> {
                if (error != null) {
                    statusLabel.setText("Ошибка: " + error.getCause().getMessage());
                    statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                    createBtn.setDisable(false);
                } else {
                    statusLabel.setText("SKU создан: " + sku.code());
                    statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                    // Auto-close after 1 second and refresh table
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(() -> {
                                dialog.close();
                                loadList(skuTable, () -> apiClient.listSkus());
                            });
                        } catch (InterruptedException ex) {
                            // Ignore
                        }
                    }).start();
                }
            }));
        });

        VBox layout = new VBox(12, 
            codeLabel, codeField,
            nameLabel, nameField,
            uomLabel, uomField,
            statusLabel, buttonBox
        );
        layout.setPadding(new Insets(24));
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setStyle("-fx-background-color: #1c1c1c;");

        Scene scene = new Scene(layout, 400, 350);
        applyStyles(scene);
        dialog.setScene(scene);
        dialog.show();

        codeField.requestFocus();
    }

    private boolean showDiscrepancyConfirmationDialog() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Обнаружены расхождения");
        alert.setHeaderText("⚠ В задании есть расхождения между ожидаемым и фактическим количеством");
        alert.setContentText(
            "Вы можете:\n\n" +
            "• Подтвердить завершение — расхождения будут одобрены и зафиксированы, " +
            "задание будет завершено, приход автоматически примет фактическое количество\n\n" +
            "• Отменить — вернуться к приёмке и допринять недостающее количество"
        );
        
        // Customize button text
        alert.getButtonTypes().setAll(
            new javafx.scene.control.ButtonType("Подтвердить завершение", javafx.scene.control.ButtonBar.ButtonData.OK_DONE),
            new javafx.scene.control.ButtonType("Отменить и вернуться к приёмке", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE)
        );
        
        // Apply styles with lighter text color
        alert.getDialogPane().getStylesheets().clear();
        var cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }
        alert.getDialogPane().setStyle(
            "-fx-background-color: #1c1c1c; " +
            "-fx-text-fill: #E0E0E0;"
        );
        
        // Apply lighter color to content text
        alert.getDialogPane().lookup(".content.label").setStyle(
            "-fx-text-fill: #E0E0E0; " +
            "-fx-font-size: 14px;"
        );
        
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE;
    }

    private void applyStyles(Scene scene) {
        var cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
