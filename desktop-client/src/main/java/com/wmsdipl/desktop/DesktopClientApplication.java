package com.wmsdipl.desktop;

import com.wmsdipl.desktop.ImportServiceClient;
import com.wmsdipl.desktop.model.Receipt;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        Button ordersBtn = navButton("Заказы", activeModule.equals("orders"), this::showOrdersStub);
        Button tasksBtn = navButton("Задания", activeModule.equals("tasks"), this::showTasksStub);
        Button catalogBtn = navButton("Номенклатура", activeModule.equals("catalog"), this::showCatalogStub);

        VBox nav = new VBox(14, logo, receiptsBtn, ordersBtn, tasksBtn, catalogBtn);
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

        TableView<Receipt> table = buildReceiptTable();

        HBox filterRow = new HBox(14, filterField, refreshBtn);
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, filterRow, table);
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

        VBox layout = new VBox(10,
            header,
            new Label("CORE API"), coreApiField,
            new Label("Import API"), importApiField,
            new Label("Путь к папке импорта"), importFolderField,
            new HBox(8, saveApi, saveFolder),
            statusLabel
        );
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        layout.setFillWidth(true);

        setContent(layout);
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

    private void showTasksStub() {
        activeModule = "tasks";
        shell.setLeft(buildNav());
        VBox layout = new VBox(10,
            new Label("Задания"),
            new Label("Заглушка: экран задач будет позже")
        );
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        setContent(layout);
    }

    private void showOrdersStub() {
        activeModule = "orders";
        shell.setLeft(buildNav());
        VBox layout = new VBox(10,
            new Label("Заказы"),
            new Label("Заглушка: экран заказов будет позже")
        );
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        setContent(layout);
    }

    private void showCatalogStub() {
        activeModule = "catalog";
        shell.setLeft(buildNav());
        VBox layout = new VBox(10,
            new Label("Номенклатура"),
            new Label("Заглушка: экран каталога будет позже")
        );
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        setContent(layout);
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
            if (userField.getText().isBlank() || passField.getText().isBlank()) {
                info.setText("Заполните логин/пароль");
                return;
            }
            if ("admin".equals(userField.getText()) && "admin".equals(passField.getText())) {
                success[0] = true;
                dialog.close();
            } else {
                info.setText("Неверный логин или пароль");
            }
        });

        VBox box = new VBox(8, info, userField, passField, loginBtn);
        box.setPadding(new Insets(12));
        box.setAlignment(Pos.CENTER_LEFT);
        dialog.setScene(new Scene(box, 300, 160));
        dialog.showAndWait();
        return success[0];
    }

    private void applyStyles(Scene scene) {
        var cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
