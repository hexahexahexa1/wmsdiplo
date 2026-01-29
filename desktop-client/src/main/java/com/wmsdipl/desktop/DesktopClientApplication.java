package com.wmsdipl.desktop;

import com.wmsdipl.desktop.ImportServiceClient;
import com.wmsdipl.desktop.model.Location;
import com.wmsdipl.desktop.model.Pallet;
import com.wmsdipl.desktop.model.PutawayRule;
import com.wmsdipl.desktop.model.Receipt;
import com.wmsdipl.desktop.model.Scan;
import com.wmsdipl.desktop.model.Sku;
import com.wmsdipl.desktop.model.StockItem;
import com.wmsdipl.desktop.model.StockMovement;
import com.wmsdipl.desktop.model.User;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
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
import java.util.Map;
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

        contentHolder = new VBox();
        contentHolder.setPadding(new Insets(18));
        contentHolder.setSpacing(12);
        shell.setCenter(contentHolder);

        // Determine start page based on role
        String role = apiClient.getCurrentUser().role();
        
        Platform.runLater(() -> {
            if ("OPERATOR".equalsIgnoreCase(role)) {
                activeModule = "terminal";
                showTerminalPane();
            } else {
                activeModule = "receipts";
                showReceiptsPane();
            }
            
            Scene scene = new Scene(shell);
            applyStyles(scene);
            stage.setScene(scene);
            stage.setTitle("WMSDIPL - " + apiClient.getCurrentUser().username() + " [" + role + "]");
            stage.show();
        });
    }

    private VBox buildNav() {
        Label logo = new Label("WMSDIPL");
        logo.getStyleClass().add("logo-text");
        logo.setAlignment(Pos.CENTER);
        logo.setMaxWidth(Double.MAX_VALUE);

        String role = apiClient.getCurrentUser() != null ? apiClient.getCurrentUser().role() : "OPERATOR";

        VBox nav = new VBox(14, logo);
        nav.setPadding(new Insets(24, 24, 24, 24));
        nav.setPrefWidth(210);
        nav.setAlignment(Pos.TOP_CENTER);
        nav.setStyle("-fx-background-color: #0e0e10;");

        if (!"OPERATOR".equalsIgnoreCase(role)) {
            nav.getChildren().add(navButton(I18n.get("nav.receipts"), activeModule.equals("receipts"), this::showReceiptsPane));
            nav.getChildren().add(navButton(I18n.get("nav.topology"), activeModule.equals("topology"), this::showTopologyPane));
            nav.getChildren().add(navButton(I18n.get("nav.pallets"), activeModule.equals("pallets"), this::showPalletsPane));
            nav.getChildren().add(navButton(I18n.get("nav.stock"), activeModule.equals("stock"), this::showStockPane));
        }

        if (!"OPERATOR".equalsIgnoreCase(role) && !"PC_OPERATOR".equalsIgnoreCase(role)) {
            nav.getChildren().add(navButton(I18n.get("nav.tasks"), activeModule.equals("tasks"), this::showTasksPane));
        }

        if (!"OPERATOR".equalsIgnoreCase(role)) {
            nav.getChildren().add(navButton(I18n.get("nav.analytics"), activeModule.equals("analytics"), this::showAnalyticsPane));
        }

        if (!"OPERATOR".equalsIgnoreCase(role) && !"PC_OPERATOR".equalsIgnoreCase(role)) {
            nav.getChildren().add(navButton(I18n.get("nav.skus"), activeModule.equals("skus"), this::showSkusPane));
        }

        if (!"PC_OPERATOR".equalsIgnoreCase(role)) {
            nav.getChildren().add(navButton(I18n.get("nav.terminal"), activeModule.equals("terminal"), this::showTerminalPane));
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            nav.getChildren().add(navButton(I18n.get("nav.users"), activeModule.equals("users"), this::showUsersPane));
        }
        
        nav.getChildren().add(navButton(I18n.get("nav.settings"), activeModule.equals("settings"), this::showSettingsPane));
        
        // Spacer to push logout button to bottom
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        nav.getChildren().add(spacer);
        
        Button logoutBtn = new Button(I18n.get("nav.logout"));
        logoutBtn.getStyleClass().add("nav-button");
        logoutBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white;");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> handleLogout());
        nav.getChildren().add(logoutBtn);

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
        filterField.setPromptText(I18n.get("receipts.filter.placeholder"));
        filterField.setId("searchField");
        filterField.setPrefWidth(520);
        filterField.setPrefHeight(48);
        Button refreshBtn = new Button(I18n.get("btn.refresh"));
        refreshBtn.getStyleClass().add("refresh-btn");
        refreshBtn.setPrefWidth(200);
        refreshBtn.setPrefHeight(48);
        refreshBtn.setMinWidth(200);

        Button confirmBtn = new Button(I18n.get("receipts.btn.confirm"));
        Button startReceivingBtn = new Button(I18n.get("receipts.btn.start_receiving"));
        Button completeReceivingBtn = new Button(I18n.get("receipts.btn.complete_receiving"));
        Button acceptBtn = new Button(I18n.get("receipts.btn.accept"));
        Button startPlacementBtn = new Button(I18n.get("receipts.btn.start_placement"));
        Button tasksBtn = new Button(I18n.get("receipts.btn.tasks"));

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
        
        startReceivingBtn.setOnAction(e -> withSelectedReceipt(table, r -> {
            CompletableFuture.runAsync(() -> {
                try {
                    int count = apiClient.startReceiving(r.id());
                    Platform.runLater(() -> showNotification(I18n.format("tasks.created.notification", count)));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((v, error) -> Platform.runLater(() -> {
                if (error != null) {
                    new Alert(AlertType.ERROR, I18n.format("common.error", error.getMessage())).showAndWait();
                }
                loadReceipts(table, "");
            }));
        }));
        
        completeReceivingBtn.setOnAction(e -> withSelectedReceipt(table, r -> asyncAction(() -> apiClient.completeReceiving(r.id()), table)));
        acceptBtn.setOnAction(e -> withSelectedReceipt(table, r -> asyncAction(() -> apiClient.accept(r.id()), table)));
        
        startPlacementBtn.setOnAction(e -> withSelectedReceipt(table, r -> {
            CompletableFuture.runAsync(() -> {
                try {
                    int count = apiClient.startPlacement(r.id());
                    Platform.runLater(() -> showNotification(I18n.format("tasks.created.notification", count)));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((v, error) -> Platform.runLater(() -> {
                if (error != null) {
                    new Alert(AlertType.ERROR, I18n.format("common.error", error.getMessage())).showAndWait();
                }
                loadReceipts(table, "");
            }));
        }));
        
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
        table.setPlaceholder(new Label(I18n.get("common.no_data")));
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

        TableColumn<Receipt, String> docNoCol = new TableColumn<>(I18n.get("receipts.table.doc_no"));
        docNoCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().docNo()));
        docNoCol.setPrefWidth(160);

        TableColumn<Receipt, String> msgCol = new TableColumn<>(I18n.get("receipts.table.message_id"));
        msgCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().messageId()));
        msgCol.setPrefWidth(180);

        TableColumn<Receipt, String> statusCol = new TableColumn<>(I18n.get("receipts.table.status"));
        statusCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(
            translateStatus(cell.getValue().status())
        ));
        statusCol.setPrefWidth(140);

        TableColumn<Receipt, String> dateCol = new TableColumn<>(I18n.get("receipts.table.doc_date"));
        dateCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(
            cell.getValue().docDate() != null ? dateFmt.format(cell.getValue().docDate()) : ""
        ));
        dateCol.setPrefWidth(160);

        TableColumn<Receipt, String> supplierCol = new TableColumn<>(I18n.get("receipts.table.supplier"));
        supplierCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().supplier()));
        supplierCol.setPrefWidth(160);

        TableColumn<Receipt, String> crossDockCol = new TableColumn<>(I18n.get("receipts.table.cross_dock"));
        crossDockCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(
            cell.getValue().crossDock() != null && cell.getValue().crossDock() ? "🚀 Cross-Dock" : ""
        ));
        crossDockCol.setPrefWidth(120);

        table.getColumns().addAll(docNoCol, msgCol, statusCol, dateCol, supplierCol, crossDockCol);
        return table;
    }

    private void showLinesDialog(Receipt receipt) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(I18n.format("receipts.dialog.lines_title", receipt.docNo()));

        TableView<com.wmsdipl.desktop.model.ReceiptLine> lineTable = new TableView<>();
        lineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lineTable.setPlaceholder(new Label(I18n.get("common.loading")));

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
                    Alert alert = new Alert(AlertType.ERROR, I18n.format("common.error", error.getMessage()));
                    alert.initOwner(dialog);
                    alert.showAndWait();
                    lineTable.setPlaceholder(new Label(I18n.get("common.alert.error")));
                    return;
                }
                lineTable.setItems(FXCollections.observableArrayList(lines));
                lineTable.setPlaceholder(new Label(I18n.get("common.no_data")));
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
                    table.setPlaceholder(new Label(I18n.format("common.error", error.getMessage())));
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

    private String translateStatus(String status) {
        if (status == null) return "";
        String key = "status." + status;
        String val = I18n.get(key);
        return val.startsWith("!") ? status : val;
    }

    private void showTopologyPane() {
        activeModule = "topology";
        shell.setLeft(buildNav());

        String role = apiClient.getCurrentUser() != null ? apiClient.getCurrentUser().role() : "OPERATOR";
        boolean isAdmin = "ADMIN".equals(role);
        boolean isSupervisor = "SUPERVISOR".equals(role);
        boolean canEdit = isAdmin;
        boolean canBlock = isAdmin || isSupervisor;

        Label header = new Label(I18n.get("topology.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        ListView<com.wmsdipl.desktop.model.Zone> zonesView = new ListView<>();
        zonesView.setPrefWidth(240);

        // Zone management buttons
        HBox zoneBtns = new HBox(8);
        zoneBtns.setPadding(new Insets(8, 0, 0, 0));

        if (canEdit) {
            Button createZoneBtn = new Button(I18n.get("topology.btn.create_zone"));
            createZoneBtn.getStyleClass().add("btn-success");
            createZoneBtn.setOnAction(e -> openZoneCreationDialog(zonesView));
            
            Button editZoneBtn = new Button(I18n.get("topology.btn.edit"));
            editZoneBtn.getStyleClass().add("btn-primary");
            editZoneBtn.setDisable(true);
            editZoneBtn.setOnAction(e -> openZoneEditDialog(zonesView, zonesView.getSelectionModel().getSelectedItem()));
            
            Button deleteZoneBtn = new Button(I18n.get("topology.btn.delete"));
            deleteZoneBtn.getStyleClass().add("btn-danger");
            deleteZoneBtn.setDisable(true);
            deleteZoneBtn.setOnAction(e -> deleteZone(zonesView, zonesView.getSelectionModel().getSelectedItem()));
            
            zonesView.getSelectionModel().selectedItemProperty().addListener((obs, o, z) -> {
                boolean selected = z != null;
                editZoneBtn.setDisable(!selected);
                deleteZoneBtn.setDisable(!selected);
            });
            
            zoneBtns.getChildren().addAll(createZoneBtn, editZoneBtn, deleteZoneBtn);
        }

        TableView<Location> locTable = new TableView<>();
        locTable.setPlaceholder(new Label(I18n.get("placeholder.no_cells")));
        
        TableColumn<Location, String> locCode = new TableColumn<>(I18n.get("topology.table.code"));
        locCode.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().code()));
        
        TableColumn<Location, String> locType = new TableColumn<>(I18n.get("topology.table.type"));
        locType.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().locationType() != null ? c.getValue().locationType() : ""));
        
        TableColumn<Location, String> locStatus = new TableColumn<>(I18n.get("topology.table.status"));
        locStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(translateStatus(c.getValue().status())));
        
        TableColumn<Location, Number> locMax = new TableColumn<>(I18n.get("topology.table.max_pallets"));
        locMax.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().maxPallets()));
        
        TableColumn<Location, String> locZone = new TableColumn<>(I18n.get("topology.table.zone"));
        locZone.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().zoneCode() != null ? c.getValue().zoneCode() : ""));
        
        locTable.getColumns().addAll(locCode, locType, locStatus, locMax, locZone);

        // Location management buttons
        HBox locBtns = new HBox(8);
        locBtns.setPadding(new Insets(8, 0, 0, 0));

        Button editLocBtn = null;
        Button deleteLocBtn = null;
        Button blockLocBtn = null;

        if (canEdit) {
            Button createLocBtn = new Button(I18n.get("topology.btn.create_location"));
            createLocBtn.getStyleClass().add("btn-success");
            createLocBtn.setOnAction(e -> openLocationCreationDialog(locTable, zonesView));
            
            editLocBtn = new Button(I18n.get("topology.btn.edit"));
            editLocBtn.getStyleClass().add("btn-primary");
            editLocBtn.setDisable(true);
            Button finalEditBtn = editLocBtn;
            editLocBtn.setOnAction(e -> openLocationEditDialog(locTable, locTable.getSelectionModel().getSelectedItem(), zonesView));
            
            deleteLocBtn = new Button(I18n.get("topology.btn.delete"));
            deleteLocBtn.getStyleClass().add("btn-danger");
            deleteLocBtn.setDisable(true);
            Button finalDeleteBtn = deleteLocBtn;
            deleteLocBtn.setOnAction(e -> deleteLocation(locTable, locTable.getSelectionModel().getSelectedItem()));
            
            locBtns.getChildren().addAll(createLocBtn, editLocBtn, deleteLocBtn);
        }

        if (canBlock) {
            blockLocBtn = new Button(I18n.get("topology.btn.block"));
            blockLocBtn.getStyleClass().add("btn-warning");
            blockLocBtn.setDisable(true);
            Button finalBlockBtn = blockLocBtn;
            blockLocBtn.setOnAction(e -> toggleLocationBlock(locTable, locTable.getSelectionModel().getSelectedItem()));
            locBtns.getChildren().add(blockLocBtn);
        }
        
        Button finalEditLocBtn = editLocBtn;
        Button finalDeleteLocBtn = deleteLocBtn;
        Button finalBlockLocBtn = blockLocBtn;

        locTable.getSelectionModel().selectedItemProperty().addListener((obs, o, loc) -> {
            boolean selected = loc != null;
            if (finalEditLocBtn != null) finalEditLocBtn.setDisable(!selected);
            if (finalDeleteLocBtn != null) finalDeleteLocBtn.setDisable(!selected);
            
            if (finalBlockLocBtn != null) {
                if (selected) {
                    boolean isBlocked = "BLOCKED".equals(loc.status());
                    finalBlockLocBtn.setText(isBlocked ? I18n.get("topology.btn.unblock") : I18n.get("topology.btn.block"));
                    finalBlockLocBtn.setDisable(false);
                } else {
                    finalBlockLocBtn.setDisable(true);
                }
            }
        });

        VBox left = new VBox(10, new Label(I18n.get("topology.zones")), zonesView, zoneBtns);
        left.setPadding(new Insets(12));
        left.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        VBox right = new VBox(10, new Label(I18n.get("topology.locations")), locTable, locBtns);
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
                    var zones = zonesView != null ? apiClient.listZones() : null;
                    var locs = locTable != null ? apiClient.listLocations() : null;
                    return new Object[]{zones, locs};
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .whenComplete((data, error) -> Platform.runLater(() -> {
                if (error != null) {
                    if (zonesView != null) zonesView.setItems(FXCollections.observableArrayList());
                    if (locTable != null) {
                        locTable.setItems(FXCollections.observableArrayList());
                        locTable.setPlaceholder(new Label(I18n.format("common.error", error.getMessage())));
                    }
                    return;
                }
                if (data[0] != null && zonesView != null) {
                    @SuppressWarnings("unchecked")
                    List<com.wmsdipl.desktop.model.Zone> zones = (List<com.wmsdipl.desktop.model.Zone>) data[0];
                    zonesView.setItems(FXCollections.observableArrayList(zones));
                }
                if (data[1] != null && locTable != null) {
                    @SuppressWarnings("unchecked")
                    List<Location> locs = (List<Location>) data[1];
                    locTable.setUserData(locs);
                    locTable.setItems(FXCollections.observableArrayList(locs));
                }
            }));
    }

    private void filterLocationsByZone(TableView<Location> locTable, com.wmsdipl.desktop.model.Zone zone) {
        @SuppressWarnings("unchecked")
        List<Location> all = (List<Location>) locTable.getUserData();
        if (all == null) {
            return;
        }
        List<Location> filtered = zone == null ? all : all.stream()
            .filter(l -> l.zoneId() != null && zone.id().equals(l.zoneId()))
            .toList();
        locTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showPalletsPane() {
        activeModule = "pallets";
        shell.setLeft(buildNav());

        Label header = new Label(I18n.get("pallets.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Button refresh = new Button(I18n.get("btn.refresh"));
        refresh.getStyleClass().add("refresh-btn");
        
        Button createBtn = new Button(I18n.get("pallets.btn.create"));
        createBtn.getStyleClass().add("refresh-btn");
        createBtn.setOnAction(e -> showCreatePalletDialog());
        
        Button bulkCreateBtn = new Button(I18n.get("pallets.btn.bulk_create"));
        bulkCreateBtn.getStyleClass().add("refresh-btn");
        bulkCreateBtn.setOnAction(e -> showBulkCreatePalletsDialog());
        
        HBox toolbar = new HBox(12, refresh, createBtn, bulkCreateBtn);

        TableView<Pallet> table = new TableView<>();
        table.setPlaceholder(new Label(I18n.get("placeholder.no_data")));
        
        TableColumn<Pallet, Object> codeCol = column(I18n.get("pallets.table.code"), p -> p.code());
        TableColumn<Pallet, Object> statusCol = column(I18n.get("pallets.table.status"), p -> translateStatus(p.status()));
        TableColumn<Pallet, Object> locCol = column(I18n.get("pallets.table.location"), p -> p.location() != null ? p.location().code() : "");
        TableColumn<Pallet, Object> skuCol = column(I18n.get("pallets.table.sku"), p -> p.skuId());
        TableColumn<Pallet, Object> qtyCol = column(I18n.get("pallets.table.qty"), p -> p.quantity());
        TableColumn<Pallet, Object> receiptCol = column(I18n.get("pallets.table.receipt"), p -> p.receipt() != null ? p.receipt().id() : null);
        
        table.getColumns().addAll(codeCol, statusCol, locCol, skuCol, qtyCol, receiptCol);

        refresh.setOnAction(e -> loadList(table, () -> apiClient.listPallets()));
        loadList(table, () -> apiClient.listPallets());

        VBox layout = new VBox(12, header, toolbar, table);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        setContent(layout);
    }

    private void showStockPane() {
        activeModule = "stock";
        shell.setLeft(buildNav());

        Label header = new Label(I18n.get("stock.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        // Filter fields
        TextField skuCodeFilter = new TextField();
        skuCodeFilter.setPromptText(I18n.get("stock.filter.sku"));
        skuCodeFilter.setPrefWidth(150);
        
        TextField locationCodeFilter = new TextField();
        locationCodeFilter.setPromptText(I18n.get("stock.filter.location"));
        locationCodeFilter.setPrefWidth(150);
        
        TextField palletBarcodeFilter = new TextField();
        palletBarcodeFilter.setPromptText(I18n.get("stock.filter.pallet"));
        palletBarcodeFilter.setPrefWidth(150);
        
        TextField receiptIdFilter = new TextField();
        receiptIdFilter.setPromptText(I18n.get("stock.filter.receipt"));
        receiptIdFilter.setPrefWidth(150);
        
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("", "EMPTY", "RECEIVED", "PLACED", "PICKED");
        statusFilter.setValue("");
        statusFilter.setPromptText(I18n.get("stock.filter.status"));
        statusFilter.setPrefWidth(150);
        
        Button searchBtn = new Button(I18n.get("stock.btn.search"));
        searchBtn.getStyleClass().add("filter-btn");
        
        Button clearBtn = new Button(I18n.get("stock.btn.clear"));
        clearBtn.getStyleClass().add("filter-btn");
        
        // Filters row (first line)
        HBox filtersRow = new HBox(10, skuCodeFilter, locationCodeFilter, palletBarcodeFilter, receiptIdFilter, statusFilter);
        filtersRow.setAlignment(Pos.CENTER_LEFT);
        
        // Buttons row (second line)
        HBox buttonsRow = new HBox(10, searchBtn, clearBtn);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);
        
        // Stock table
        TableView<StockItem> stockTable = new TableView<>();
        stockTable.setPlaceholder(new Label(I18n.get("placeholder.no_data")));
        stockTable.setPrefHeight(500);
        stockTable.setFixedCellSize(50);
        
        TableColumn<StockItem, Object> palletIdCol = column(I18n.get("stock.table.id"), StockItem::palletId);
        palletIdCol.setPrefWidth(60);
        
        TableColumn<StockItem, Object> palletCodeCol = column(I18n.get("stock.table.pallet"), StockItem::palletCode);
        palletCodeCol.setPrefWidth(120);
        
        TableColumn<StockItem, Object> skuCodeCol = column(I18n.get("stock.table.sku"), StockItem::skuCode);
        skuCodeCol.setPrefWidth(120);
        
        TableColumn<StockItem, Object> skuNameCol = column(I18n.get("stock.table.name"), StockItem::skuName);
        skuNameCol.setPrefWidth(200);
        
        TableColumn<StockItem, Object> qtyCol = column(I18n.get("stock.table.qty"), StockItem::quantity);
        qtyCol.setPrefWidth(80);
        
        TableColumn<StockItem, Object> uomCol = column(I18n.get("stock.table.uom"), StockItem::uom);
        uomCol.setPrefWidth(70);
        
        TableColumn<StockItem, Object> locationCol = column(I18n.get("stock.table.location"), s -> s.locationCode() != null ? s.locationCode() : I18n.get("stock.value.not_placed"));
        locationCol.setPrefWidth(120);
        
        TableColumn<StockItem, Object> statusCol = column(I18n.get("stock.table.status"), s -> translateStatus(s.palletStatus()));
        statusCol.setPrefWidth(100);
        
        TableColumn<StockItem, Object> receiptCol = column(I18n.get("stock.table.receipt"), s -> s.receiptDocNumber() != null ? s.receiptDocNumber() : "");
        receiptCol.setPrefWidth(150);
        
        TableColumn<StockItem, Object> lotCol = column(I18n.get("stock.table.lot"), s -> s.lotNumber() != null ? s.lotNumber() : "");
        lotCol.setPrefWidth(100);
        
        TableColumn<StockItem, Object> expiryCol = column(I18n.get("stock.table.expiry"), s -> s.expiryDate() != null ? s.expiryDate().toString() : "");
        expiryCol.setPrefWidth(100);
        
        stockTable.getColumns().addAll(palletIdCol, palletCodeCol, skuCodeCol, skuNameCol, 
                                       qtyCol, uomCol, locationCol, statusCol, receiptCol, lotCol, expiryCol);
        
        // Double-click to show movement history
        stockTable.setRowFactory(tv -> {
            TableRow<StockItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showMovementHistoryDialog(row.getItem());
                }
            });
            return row;
        });
        
        // Pagination controls
        Label pageLabel = new Label(I18n.format("stock.pagination.page", 1, 1));
        pageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        Button prevBtn = new Button(I18n.get("stock.btn.prev"));
        prevBtn.getStyleClass().add("refresh-btn");
        prevBtn.setPrefHeight(36);
        prevBtn.setDisable(true);
        
        Button nextBtn = new Button(I18n.get("stock.btn.next"));
        nextBtn.getStyleClass().add("refresh-btn");
        nextBtn.setPrefHeight(36);
        
        Label totalLabel = new Label(I18n.format("stock.pagination.total", 0));
        totalLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        HBox paginationBox = new HBox(12, prevBtn, pageLabel, nextBtn, totalLabel);
        paginationBox.setAlignment(Pos.CENTER_LEFT);
        paginationBox.setPadding(new Insets(10, 0, 0, 0));
        
        // State for pagination
        final int[] currentPage = {0};
        final int pageSize = 50;
        
        // Load stock function
        Runnable loadStock = () -> {
            String skuCode = skuCodeFilter.getText().trim();
            String locationCode = locationCodeFilter.getText().trim();
            String palletBarcode = palletBarcodeFilter.getText().trim();
            String receiptIdStr = receiptIdFilter.getText().trim();
            String status = statusFilter.getValue();
            
            Long receiptId = null;
            if (!receiptIdStr.isEmpty()) {
                try {
                    receiptId = Long.parseLong(receiptIdStr);
                } catch (NumberFormatException ex) {
                    stockTable.setPlaceholder(new Label(I18n.get("placeholder.invalid_receipt")));
                    return;
                }
            }
            
            final Long finalReceiptId = receiptId;
            stockTable.setPlaceholder(new Label(I18n.get("placeholder.loading")));
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.listStock(
                        skuCode.isEmpty() ? null : skuCode,
                        locationCode.isEmpty() ? null : locationCode,
                        palletBarcode.isEmpty() ? null : palletBarcode,
                        finalReceiptId,
                        status.isEmpty() ? null : status,
                        currentPage[0],
                        pageSize
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).whenComplete((page, error) -> Platform.runLater(() -> {
                if (error != null) {
                    stockTable.setPlaceholder(new Label(I18n.format("common.error", error.getMessage())));
                    stockTable.setItems(FXCollections.observableArrayList());
                    return;
                }
                
                stockTable.setItems(FXCollections.observableArrayList(page.content));
                pageLabel.setText(I18n.format("stock.pagination.page", currentPage[0] + 1, Math.max(1, page.totalPages)));
                totalLabel.setText(I18n.format("stock.pagination.total", page.totalElements));
                
                prevBtn.setDisable(currentPage[0] == 0);
                nextBtn.setDisable(currentPage[0] >= page.totalPages - 1);
                
                stockTable.setPlaceholder(new Label(I18n.get("placeholder.no_data")));
            }));
        };
        
        // Button actions
        searchBtn.setOnAction(e -> {
            currentPage[0] = 0;
            loadStock.run();
        });
        
        clearBtn.setOnAction(e -> {
            skuCodeFilter.clear();
            locationCodeFilter.clear();
            palletBarcodeFilter.clear();
            receiptIdFilter.clear();
            statusFilter.setValue("");
            currentPage[0] = 0;
            loadStock.run();
        });
        
        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadStock.run();
            }
        });
        
        nextBtn.setOnAction(e -> {
            currentPage[0]++;
            loadStock.run();
        });
        
        // Enter key on filters triggers search
        skuCodeFilter.setOnAction(e -> searchBtn.fire());
        locationCodeFilter.setOnAction(e -> searchBtn.fire());
        palletBarcodeFilter.setOnAction(e -> searchBtn.fire());
        receiptIdFilter.setOnAction(e -> searchBtn.fire());
        
        VBox layout = new VBox(12, header, filtersRow, buttonsRow, stockTable, paginationBox);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        VBox.setVgrow(stockTable, Priority.ALWAYS);
        
        setContent(layout);
        
        // Load initial data
        loadStock.run();
    }
    
    private void showMovementHistoryDialog(StockItem stockItem) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(I18n.format("movement.dialog.title", stockItem.palletCode()));
        
        TableView<StockMovement> historyTable = new TableView<>();
        historyTable.setPlaceholder(new Label(I18n.get("common.loading")));
        historyTable.setPrefHeight(400);
        
        TableColumn<StockMovement, Object> idCol = column(I18n.get("movement.col.id"), StockMovement::id);
        idCol.setPrefWidth(50);
        
        TableColumn<StockMovement, Object> typeCol = column(I18n.get("movement.col.type"), StockMovement::movementType);
        typeCol.setPrefWidth(100);
        
        TableColumn<StockMovement, Object> fromLocCol = column(I18n.get("movement.col.from"), m -> m.fromLocationCode() != null ? m.fromLocationCode() : "-");
        fromLocCol.setPrefWidth(120);
        
        TableColumn<StockMovement, Object> toLocCol = column(I18n.get("movement.col.to"), m -> m.toLocationCode() != null ? m.toLocationCode() : "-");
        toLocCol.setPrefWidth(120);
        
        TableColumn<StockMovement, Object> qtyCol = column(I18n.get("movement.col.qty"), StockMovement::quantity);
        qtyCol.setPrefWidth(80);
        
        TableColumn<StockMovement, Object> movedByCol = column(I18n.get("movement.col.by"), StockMovement::movedBy);
        movedByCol.setPrefWidth(120);
        
        TableColumn<StockMovement, Object> movedAtCol = column(I18n.get("movement.col.when"), m -> {
            if (m.movedAt() != null) {
                return m.movedAt().toString().substring(0, 19).replace('T', ' ');
            }
            return "";
        });
        movedAtCol.setPrefWidth(150);
        
        TableColumn<StockMovement, Object> taskCol = column(I18n.get("movement.col.task"), m -> m.taskId() != null ? m.taskId().toString() : "");
        taskCol.setPrefWidth(80);
        
        historyTable.getColumns().addAll(idCol, typeCol, fromLocCol, toLocCol, qtyCol, movedByCol, movedAtCol, taskCol);
        
        Button closeBtn = new Button(I18n.get("common.close"));
        closeBtn.getStyleClass().add("refresh-btn");
        closeBtn.setPrefWidth(150);
        closeBtn.setPrefHeight(35);
        closeBtn.setOnAction(e -> dialog.close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        
        VBox content = new VBox(10, historyTable, buttonBox);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: #1c1c1c;");
        
        Scene scene = new Scene(content, 900, 500);
        applyStyles(scene);
        dialog.setScene(scene);
        dialog.show();
        
        // Load movement history
        CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.getPalletHistory(stockItem.palletId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((movements, error) -> Platform.runLater(() -> {
            if (error != null) {
                // Extract meaningful error message
                String errorMsg = error.getMessage();
                if (errorMsg != null && errorMsg.contains("RuntimeException")) {
                    // Try to extract the actual IOException message
                    Throwable cause = error.getCause();
                    if (cause != null && cause.getMessage() != null) {
                        errorMsg = cause.getMessage();
                    }
                }
                
                Label errorLabel = new Label(I18n.format("movement.placeholder.error", errorMsg));
                errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-wrap-text: true;");
                historyTable.setPlaceholder(errorLabel);
            } else if (movements == null || movements.isEmpty()) {
                // No movements yet - show friendly message
                Label emptyLabel = new Label(I18n.get("movement.placeholder.empty"));
                emptyLabel.setStyle("-fx-text-fill: #888; -fx-text-alignment: center;");
                historyTable.setPlaceholder(emptyLabel);
            } else {
                // Success - populate table
                historyTable.setItems(FXCollections.observableArrayList(movements));
                historyTable.setPlaceholder(new Label(I18n.get("placeholder.no_movements")));
            }
        }));
    }

    private void showTasksPane() {
        activeModule = "tasks";
        shell.setLeft(buildNav());

        Label header = new Label(I18n.get("tasks.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        TextField receiptFilter = new TextField();
        receiptFilter.setPromptText(I18n.get("tasks.filter.receipt"));
        Button refresh = new Button(I18n.get("btn.refresh"));
        refresh.getStyleClass().add("refresh-btn");
        
        String role = apiClient.getCurrentUser() != null ? apiClient.getCurrentUser().role() : "OPERATOR";
        boolean canBulk = "ADMIN".equals(role) || "SUPERVISOR".equals(role);
        
        Button bulkOpsBtn = new Button(I18n.get("tasks.btn.bulk"));
        bulkOpsBtn.getStyleClass().add("btn-primary");
        bulkOpsBtn.setDisable(true);
        
        if (!canBulk) {
            bulkOpsBtn.setVisible(false);
            bulkOpsBtn.setManaged(false);
        }

        TableView<com.wmsdipl.desktop.model.Task> table = new TableView<>();
        table.setPlaceholder(new Label(I18n.get("placeholder.no_data")));
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Enable bulk ops button when tasks are selected
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (canBulk) {
                bulkOpsBtn.setDisable(table.getSelectionModel().getSelectedItems().isEmpty());
            }
        });
        
        if (canBulk) {
            bulkOpsBtn.setOnAction(e -> showBulkOperationsDialog(table));
        }
        
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskIdCol = column(I18n.get("tasks.table.id"), com.wmsdipl.desktop.model.Task::id);
        taskIdCol.setPrefWidth(50);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskDocCol = column(I18n.get("tasks.table.doc"), t -> t.receiptDocNo() != null ? t.receiptDocNo() : "");
        taskDocCol.setPrefWidth(150);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskTypeCol = column(I18n.get("tasks.table.type"), com.wmsdipl.desktop.model.Task::taskType);
        taskTypeCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskStatusCol = column(I18n.get("tasks.table.status"), t -> translateStatus(t.status()));
        taskStatusCol.setPrefWidth(120);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskAssigneeCol = column(I18n.get("tasks.table.assignee"), com.wmsdipl.desktop.model.Task::assignee);
        taskAssigneeCol.setPrefWidth(120);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskPalletCol = column(I18n.get("tasks.table.pallet"), com.wmsdipl.desktop.model.Task::palletId);
        taskPalletCol.setPrefWidth(80);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskSourceCol = column(I18n.get("tasks.table.source"), com.wmsdipl.desktop.model.Task::sourceLocationId);
        taskSourceCol.setPrefWidth(80);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> taskTargetCol = column(I18n.get("tasks.table.target"), com.wmsdipl.desktop.model.Task::targetLocationId);
        taskTargetCol.setPrefWidth(80);
        
        table.getColumns().addAll(taskIdCol, taskDocCol, taskTypeCol, taskStatusCol, taskAssigneeCol, taskPalletCol, taskSourceCol, taskTargetCol);

        refresh.setOnAction(e -> loadTasks(table, receiptFilter.getText()));
        loadTasks(table, null);

        HBox controls = new HBox(10, receiptFilter, refresh);
        if (canBulk) {
            controls.getChildren().add(bulkOpsBtn);
        }
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox layout = new VBox(12, header, controls, table);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        setContent(layout);
    }

    private void showUsersPane() {
        activeModule = "users";
        shell.setLeft(buildNav());

        Label header = new Label(I18n.get("users.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        TableView<User> userTable = new TableView<>();
        userTable.setPlaceholder(new Label(I18n.get("placeholder.no_users")));
        userTable.getColumns().addAll(
            column(I18n.get("users.table.id"), User::id),
            column(I18n.get("users.table.username"), User::username),
            column(I18n.get("users.table.fullname"), User::fullName),
            column(I18n.get("users.table.email"), User::email),
            column(I18n.get("users.table.role"), User::role),
            column(I18n.get("users.table.active"), User::active)
        );

        // User management buttons
        Button createUserBtn = new Button(I18n.get("users.btn.create"));
        createUserBtn.getStyleClass().add("btn-success");
        createUserBtn.setOnAction(e -> openUserCreationDialog(userTable));

        Button editUserBtn = new Button(I18n.get("btn.edit"));
        editUserBtn.getStyleClass().add("btn-primary");
        editUserBtn.setDisable(true);
        editUserBtn.setOnAction(e -> openUserEditDialog(userTable, userTable.getSelectionModel().getSelectedItem()));

        Button changePasswordBtn = new Button(I18n.get("users.btn.change_pass"));
        changePasswordBtn.getStyleClass().add("btn-warning");
        changePasswordBtn.setDisable(true);
        changePasswordBtn.setOnAction(e -> openPasswordChangeDialog(userTable, userTable.getSelectionModel().getSelectedItem()));

        Button deleteUserBtn = new Button(I18n.get("btn.delete"));
        deleteUserBtn.getStyleClass().add("btn-danger");
        deleteUserBtn.setDisable(true);
        deleteUserBtn.setOnAction(e -> deleteUser(userTable, userTable.getSelectionModel().getSelectedItem()));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, o, user) -> {
            boolean selected = user != null;
            editUserBtn.setDisable(!selected);
            changePasswordBtn.setDisable(!selected);
            deleteUserBtn.setDisable(!selected);
        });

        Button refreshBtn = new Button(I18n.get("btn.refresh"));
        refreshBtn.getStyleClass().add("refresh-btn");
        refreshBtn.setOnAction(e -> loadList(userTable, () -> apiClient.listUsers()));

        HBox userBtns = new HBox(8, createUserBtn, editUserBtn, changePasswordBtn, deleteUserBtn, refreshBtn);
        userBtns.setPadding(new Insets(8, 0, 8, 0));

        VBox layout = new VBox(10, header, userBtns, userTable);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");

        setContent(layout);

        loadList(userTable, () -> apiClient.listUsers());
    }

    private void openUserCreationDialog(TableView<User> userTable) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("user.create.title"));
        dialog.setHeaderText(I18n.get("user.create.header"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText(I18n.get("user.create.username_prompt"));
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(I18n.get("user.create.password_prompt"));
        TextField fullNameField = new TextField();
        fullNameField.setPromptText(I18n.get("user.create.fullname_prompt"));
        TextField emailField = new TextField();
        emailField.setPromptText(I18n.get("user.create.email_prompt"));
        
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("OPERATOR", "SUPERVISOR", "ADMIN");
        roleCombo.setValue("OPERATOR");
        
        CheckBox activeCheck = new CheckBox(I18n.get("user.create.active_label"));
        activeCheck.setSelected(true);

        grid.add(new Label(I18n.get("user.create.username_label")), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label(I18n.get("user.create.password_label")), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label(I18n.get("user.create.fullname_label")), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new Label(I18n.get("user.create.email_label")), 0, 3);
        grid.add(emailField, 1, 3);
        grid.add(new Label(I18n.get("user.create.role_label")), 0, 4);
        grid.add(roleCombo, 1, 4);
        grid.add(activeCheck, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String username = usernameField.getText();
                String password = passwordField.getText();
                String fullName = fullNameField.getText();
                String email = emailField.getText();
                String role = roleCombo.getValue();
                Boolean active = activeCheck.isSelected();

                if (username == null || username.isBlank() || password == null || password.isBlank()) {
                    showError(I18n.get("user.create.error.required"));
                    return;
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        apiClient.createUser(username, password, fullName, email, role, active);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).whenComplete((v, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showError(I18n.format("user.create.error.failed", error.getMessage()));
                    } else {
                        showInfo(I18n.get("user.create.success"));
                        loadList(userTable, () -> apiClient.listUsers());
                    }
                }));
            }
        });
    }

    private void openUserEditDialog(TableView<User> userTable, User user) {
        if (user == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("user.edit.title"));
        dialog.setHeaderText(I18n.format("user.edit.header", user.username()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField fullNameField = new TextField(user.fullName() != null ? user.fullName() : "");
        fullNameField.setPromptText(I18n.get("user.edit.fullname_prompt"));
        TextField emailField = new TextField(user.email() != null ? user.email() : "");
        emailField.setPromptText(I18n.get("user.edit.email_prompt"));
        
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("OPERATOR", "SUPERVISOR", "ADMIN");
        roleCombo.setValue(user.role() != null ? user.role() : "OPERATOR");
        
        CheckBox activeCheck = new CheckBox(I18n.get("user.edit.active_label"));
        activeCheck.setSelected(user.active() != null ? user.active() : true);

        grid.add(new Label(I18n.get("user.edit.fullname_label")), 0, 0);
        grid.add(fullNameField, 1, 0);
        grid.add(new Label(I18n.get("user.edit.email_label")), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label(I18n.get("user.edit.role_label")), 0, 2);
        grid.add(roleCombo, 1, 2);
        grid.add(activeCheck, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String fullName = fullNameField.getText();
                String email = emailField.getText();
                String role = roleCombo.getValue();
                Boolean active = activeCheck.isSelected();

                CompletableFuture.runAsync(() -> {
                    try {
                        apiClient.updateUser(user.id(), fullName, email, role, active);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).whenComplete((v, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showError(I18n.format("user.edit.error.failed", error.getMessage()));
                    } else {
                        showInfo(I18n.get("user.edit.success"));
                        loadList(userTable, () -> apiClient.listUsers());
                    }
                }));
            }
        });
    }

    private void openPasswordChangeDialog(TableView<User> userTable, User user) {
        if (user == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("user.password.title"));
        dialog.setHeaderText(I18n.format("user.password.header", user.username()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText(I18n.get("user.password.new_prompt"));

        grid.add(new Label(I18n.get("user.password.new_label")), 0, 0);
        grid.add(newPasswordField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String newPassword = newPasswordField.getText();

                if (newPassword == null || newPassword.isBlank()) {
                    showError(I18n.get("user.password.error.empty"));
                    return;
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        apiClient.changePassword(user.id(), newPassword);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).whenComplete((v, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showError(I18n.format("user.password.error.failed", error.getMessage()));
                    } else {
                        showInfo(I18n.get("user.password.success"));
                    }
                }));
            }
        });
    }

    private void deleteUser(TableView<User> userTable, User user) {
        if (user == null) return;

        if (!showConfirm(I18n.get("user.delete.title"), I18n.format("user.delete.confirm", user.username()))) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                apiClient.deleteUser(user.id());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((v, error) -> Platform.runLater(() -> {
            if (error != null) {
                showError(I18n.format("user.delete.error.failed", error.getMessage()));
            } else {
                showInfo(I18n.get("user.delete.success"));
                loadList(userTable, () -> apiClient.listUsers());
            }
        }));
    }

    private void loadTasks(TableView<com.wmsdipl.desktop.model.Task> table, String receiptFilter) {
        Long receiptId = null;
        if (receiptFilter != null && !receiptFilter.isBlank()) {
            try {
                receiptId = Long.parseLong(receiptFilter.trim());
            } catch (NumberFormatException ex) {
                table.setPlaceholder(new Label(I18n.get("placeholder.invalid_receipt")));
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

        Label header = new Label(I18n.get("terminal.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        String role = apiClient.getCurrentUser() != null ? apiClient.getCurrentUser().role() : "OPERATOR";

        // Filter tabs
        TabPane filterTabs = new TabPane();
        filterTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab myTasksTab = new Tab(I18n.get("terminal.tab.my_tasks"));
        Tab allTasksTab = new Tab(I18n.get("terminal.tab.all_tasks"));

        TableView<com.wmsdipl.desktop.model.Task> taskTable = new TableView<>();
        taskTable.setPlaceholder(new Label(I18n.get("placeholder.no_tasks")));
        taskTable.setFixedCellSize(60);
        
        TableColumn<com.wmsdipl.desktop.model.Task, Object> idCol = column(I18n.get("tasks.table.id"), com.wmsdipl.desktop.model.Task::id);
        idCol.setPrefWidth(50);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> typeCol = column(I18n.get("tasks.table.type"), com.wmsdipl.desktop.model.Task::taskType);
        typeCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> statusCol = column(I18n.get("tasks.table.status"), t -> translateStatus(t.status()));
        statusCol.setPrefWidth(120);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> receiptCol = column(I18n.get("tasks.table.doc"), t -> t.receiptDocNo() != null ? t.receiptDocNo() : "");
        receiptCol.setPrefWidth(180);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> qtyCol = column(I18n.get("pallets.table.qty"), t -> {
            BigDecimal done = t.qtyDone();
            BigDecimal assigned = t.qtyAssigned();
            if (done == null) done = BigDecimal.ZERO;
            if (assigned == null) assigned = BigDecimal.ZERO;
            return done + " / " + assigned;
        });
        qtyCol.setPrefWidth(100);
        TableColumn<com.wmsdipl.desktop.model.Task, Object> assigneeCol = column(I18n.get("tasks.table.assignee"), com.wmsdipl.desktop.model.Task::assignee);
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

        myTasksTab.setContent(taskTable);
        filterTabs.getTabs().add(myTasksTab);

        TableView<com.wmsdipl.desktop.model.Task> allTasksTable = null;
        if (allTasksTab != null) {
            allTasksTable = new TableView<>();
            allTasksTable.setPlaceholder(new Label(I18n.get("placeholder.no_tasks")));
            allTasksTable.setFixedCellSize(60);
            
            TableColumn<com.wmsdipl.desktop.model.Task, Object> allIdCol = column(I18n.get("tasks.table.id"), com.wmsdipl.desktop.model.Task::id);
            allIdCol.setPrefWidth(50);
            TableColumn<com.wmsdipl.desktop.model.Task, Object> allTypeCol = column(I18n.get("tasks.table.type"), com.wmsdipl.desktop.model.Task::taskType);
            allTypeCol.setPrefWidth(100);
            TableColumn<com.wmsdipl.desktop.model.Task, Object> allStatusCol = column(I18n.get("tasks.table.status"), t -> translateStatus(t.status()));
            allStatusCol.setPrefWidth(120);
            TableColumn<com.wmsdipl.desktop.model.Task, Object> allReceiptCol = column(I18n.get("tasks.table.doc"), t -> t.receiptDocNo() != null ? t.receiptDocNo() : "");
            allReceiptCol.setPrefWidth(180);
            TableColumn<com.wmsdipl.desktop.model.Task, Object> allQtyCol = column(I18n.get("pallets.table.qty"), t -> {
                BigDecimal done = t.qtyDone();
                BigDecimal assigned = t.qtyAssigned();
                if (done == null) done = BigDecimal.ZERO;
                if (assigned == null) assigned = BigDecimal.ZERO;
                return done + " / " + assigned;
            });
            allQtyCol.setPrefWidth(100);
            TableColumn<com.wmsdipl.desktop.model.Task, Object> allAssigneeCol = column(I18n.get("tasks.table.assignee"), t -> t.assignee() != null ? t.assignee() : I18n.get("tasks.value.unassigned"));
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
            
            allTasksTab.setContent(allTasksTable);
            filterTabs.getTabs().add(allTasksTab);
        }

        Button refreshBtn = new Button(I18n.get("btn.refresh"));
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
                        taskTable.setPlaceholder(new Label(I18n.format("common.error", error.getMessage())));
                        taskTable.setItems(FXCollections.observableArrayList());
                    } else {
                        taskTable.setItems(FXCollections.observableArrayList(tasks));
                    }
                }));
            }
        };
        
        // Load all tasks
        final TableView<com.wmsdipl.desktop.model.Task> finalAllTasksTable = allTasksTable;
        Runnable loadAllTasks = () -> {
            if (finalAllTasksTable == null) return;
            CompletableFuture.supplyAsync(() -> {
                try {
                    String currentUser = apiClient.getCurrentUsername();
                    String currentRole = apiClient.getCurrentUser().role();
                    
                    return apiClient.getAllTasks().stream()
                        .filter(t -> "RECEIVING".equals(t.taskType()) || "PLACEMENT".equals(t.taskType()))
                        .filter(t -> {
                            if ("ADMIN".equalsIgnoreCase(currentRole) || "SUPERVISOR".equalsIgnoreCase(currentRole)) {
                                return true;
                            }
                            // Operators see NEW tasks or their own
                            return t.assignee() == null || currentUser.equals(t.assignee());
                        })
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).whenComplete((tasks, error) -> Platform.runLater(() -> {
                if (error != null) {
                    finalAllTasksTable.setPlaceholder(new Label(I18n.format("common.error", error.getMessage())));
                    finalAllTasksTable.setItems(FXCollections.observableArrayList());
                } else {
                    finalAllTasksTable.setItems(FXCollections.observableArrayList(tasks));
                }
            }));
        };
        
        // Switch tab listener
        Tab finalAllTasksTab = allTasksTab;
        filterTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == myTasksTab) {
                loadMyTasks.run();
            } else if (newTab == finalAllTasksTab) {
                loadAllTasks.run();
            }
        });

        refreshBtn.setOnAction(e -> {
            if (filterTabs.getSelectionModel().getSelectedItem() == myTasksTab) {
                loadMyTasks.run();
            } else if (filterTabs.getSelectionModel().getSelectedItem() == finalAllTasksTab) {
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

    private void showSkusPane() {
        activeModule = "skus";
        shell.setLeft(buildNav());
        
        Label header = new Label(I18n.get("skus.header"));
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        TableView<Sku> skuTable = new TableView<>();
        skuTable.setPlaceholder(new Label(I18n.get("common.no_data")));
        skuTable.setPrefHeight(400);
        skuTable.getColumns().addAll(
            column(I18n.get("skus.table.id"), Sku::id),
            column(I18n.get("skus.table.code"), Sku::code),
            column(I18n.get("skus.table.name"), Sku::name),
            column(I18n.get("skus.table.uom"), Sku::uom)
        );
        
        Button refreshSku = new Button(I18n.get("btn.refresh"));
        refreshSku.getStyleClass().add("refresh-btn");
        refreshSku.setPrefHeight(48);
        refreshSku.setPrefWidth(150);
        refreshSku.setOnAction(e -> loadList(skuTable, () -> apiClient.listSkus()));
        
        Button createSku = new Button(I18n.get("skus.btn.create"));
        createSku.getStyleClass().add("refresh-btn");
        createSku.setPrefHeight(48);
        createSku.setPrefWidth(150);
        createSku.setOnAction(e -> showCreateSkuDialog(skuTable));
        
        Button deleteSku = new Button(I18n.get("btn.delete"));
        deleteSku.getStyleClass().add("refresh-btn");
        deleteSku.setPrefHeight(48);
        deleteSku.setPrefWidth(150);
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
                        Alert alert = new Alert(AlertType.ERROR, I18n.format("common.error", error.getMessage()));
                        alert.showAndWait();
                    }
                    loadList(skuTable, () -> apiClient.listSkus());
                }));
            }
        });
        
        HBox buttons = new HBox(10, refreshSku, createSku, deleteSku);
        buttons.setPadding(new Insets(10, 0, 10, 0));
        
        VBox layout = new VBox(15, header, buttons, skuTable);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        layout.setFillWidth(true);
        
        setContent(layout);
        loadList(skuTable, () -> apiClient.listSkus());
    }

    private void showSettingsPane() {
        activeModule = "settings";
        shell.setLeft(buildNav());
        Label header = new Label(I18n.get("settings.header"));
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Language selector
        Label langLabel = new Label(I18n.get("settings.language"));
        langLabel.setStyle("-fx-text-fill: white;");
        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll(I18n.get("settings.lang.russian"), I18n.get("settings.lang.english"));
        
        String currentLang = I18n.getCurrentLang();
        langCombo.setValue("en".equals(currentLang) ? I18n.get("settings.lang.english") : I18n.get("settings.lang.russian"));
        
        langCombo.setOnAction(e -> {
            String selected = langCombo.getValue();
            String code = "English".equals(selected) || (selected != null && selected.contains("English")) || (selected != null && selected.contains("en")) ? "en" : "ru";
            // Check based on localized string
            if (I18n.get("settings.lang.english").equals(selected)) code = "en";
            else if (I18n.get("settings.lang.russian").equals(selected)) code = "ru";

            if (!code.equals(currentLang)) {
                I18n.setLocale(code);
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle(I18n.get("settings.header"));
                alert.setHeaderText(I18n.get("settings.restart_required"));
                alert.setContentText(I18n.get("settings.restart_message"));
                alert.showAndWait();
            }
        });

        VBox layout = new VBox(10, header, langLabel, langCombo);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white;");
        layout.setFillWidth(true);

        String role = apiClient.getCurrentUser().role();
        if ("ADMIN".equalsIgnoreCase(role)) {
            TextField coreApiField = new TextField(coreApiBase);
            TextField importApiField = new TextField(importApiBase);
            TextField importFolderField = new TextField();
            Label statusLabel = new Label(I18n.get("settings.status_default"));
            statusLabel.setStyle("-fx-text-fill: white;");

            // Load current import folder
            CompletableFuture.runAsync(() -> {
                try {
                    String currentFolder = new ImportServiceClient(importApiBase).getCurrentFolder();
                    Platform.runLater(() -> {
                        if (currentFolder != null && !currentFolder.isBlank()) {
                            importFolderField.setText(currentFolder);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText(I18n.format("settings.current_folder_error", e.getMessage()));
                    });
                }
            });

            Button saveApi = new Button(I18n.get("settings.save_api"));
            saveApi.setOnAction(e -> {
                coreApiBase = coreApiField.getText();
                importApiBase = importApiField.getText();
                apiClient = new ApiClient(coreApiBase);
                statusLabel.setText(I18n.get("settings.api_saved"));
            });

            Button saveFolder = new Button(I18n.get("settings.save_folder"));
            saveFolder.setOnAction(e -> updateImportFolder(importApiField.getText(), importFolderField.getText(), statusLabel));

            // Putaway rules list
            Label rulesHeader = new Label(I18n.get("settings.rules_header"));
            rulesHeader.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            TableView<PutawayRule> rulesTable = new TableView<>();
            rulesTable.setPlaceholder(new Label(I18n.get("settings.no_rules")));
            rulesTable.setPrefHeight(200);
            rulesTable.getColumns().addAll(
                column("priority", PutawayRule::priority),
                column("name", PutawayRule::name),
                column("strategy", PutawayRule::strategyType),
                column("zoneId", PutawayRule::zoneId),
                column("velocity", PutawayRule::velocityClass),
                column("active", PutawayRule::active)
            );
            Button refreshRules = new Button(I18n.get("settings.refresh_rules"));
            refreshRules.getStyleClass().add("refresh-btn");
            refreshRules.setOnAction(e -> loadList(rulesTable, () -> apiClient.listPutawayRules()));
            loadList(rulesTable, () -> apiClient.listPutawayRules());

            layout.getChildren().addAll(
                new Separator(),
                new Label(I18n.get("settings.core_api")), coreApiField,
                new Label(I18n.get("settings.import_api")), importApiField,
                new Label(I18n.get("settings.import_folder")), importFolderField,
                new HBox(8, saveApi, saveFolder),
                statusLabel,
                rulesHeader,
                refreshRules,
                rulesTable
            );
        }

        setContent(layout);
    }

    private void showTasksDialog(Receipt receipt) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(I18n.format("receipts.dialog.tasks_title", receipt.docNo()));

        TableView<com.wmsdipl.desktop.model.Task> table = new TableView<>();
        table.setPlaceholder(new Label(I18n.get("common.loading")));
        table.getColumns().addAll(
            column(I18n.get("tasks.table.id"), com.wmsdipl.desktop.model.Task::id),
            column(I18n.get("tasks.table.type"), com.wmsdipl.desktop.model.Task::taskType),
            column(I18n.get("tasks.table.status"), t -> translateStatus(t.status())),
            column(I18n.get("tasks.table.assignee"), com.wmsdipl.desktop.model.Task::assignee),
            column(I18n.get("tasks.table.pallet"), com.wmsdipl.desktop.model.Task::palletId),
            column(I18n.get("tasks.table.source"), com.wmsdipl.desktop.model.Task::sourceLocationId),
            column(I18n.get("tasks.table.target"), com.wmsdipl.desktop.model.Task::targetLocationId)
        );

        dialog.setScene(new Scene(new VBox(table), 780, 420));
        dialog.show();

        loadList(table, () -> apiClient.listTasks(receipt.id()));
    }

    private void updateImportFolder(String importBase, String folder, Label importStatus) {
        if (folder == null || folder.isBlank()) {
            importStatus.setText(I18n.get("settings.import_folder_prompt"));
            return;
        }
        importStatus.setText(I18n.get("settings.import_folder_saving"));
        CompletableFuture.runAsync(() -> {
            try {
                new ImportServiceClient(importBase).updateFolder(folder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((v, error) -> Platform.runLater(() -> {
            if (error != null) {
                importStatus.setText(I18n.format("settings.folder_error", error.getMessage()));
            } else {
                importStatus.setText(I18n.format("settings.folder_saved", folder));
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
                    table.setPlaceholder(new Label(I18n.format("common.error", error.getMessage())));
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
                new Alert(AlertType.ERROR, I18n.format("common.error", error.getMessage())).showAndWait();
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

    private void showNotification(String message) {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        Label label = new Label(message);
        label.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15px; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);");
        label.setMinWidth(200);
        label.setAlignment(Pos.CENTER);
        
        popup.getContent().add(label);
        popup.setAutoHide(true);
        
        Stage stage = (Stage) shell.getScene().getWindow();
        // Position bottom right
        popup.show(stage, 
            stage.getX() + stage.getWidth() - 220, 
            stage.getY() + stage.getHeight() - 80);
            
        // Auto-hide
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(popup::hide);
            }
        }, 3000);
    }

    private boolean showLoginDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(I18n.get("login.title"));

        TextField userField = new TextField();
        userField.setPromptText(I18n.get("login.username"));
        PasswordField passField = new PasswordField();
        passField.setPromptText(I18n.get("login.password"));
        Label info = new Label(I18n.get("login.prompt"));
        Button loginBtn = new Button(I18n.get("login.button"));

        final boolean[] success = {false};
        loginBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();
            if (username.isBlank() || password.isBlank()) {
                info.setText(I18n.get("login.error.empty"));
                return;
            }
            loginBtn.setDisable(true);
            info.setText(I18n.get("login.checking"));
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.login(username, password);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((user, error) -> Platform.runLater(() -> {
                loginBtn.setDisable(false);
                if (error != null || user == null) {
                    info.setText(I18n.get("login.error.invalid"));
                    return;
                }
                success[0] = true;
                I18n.loadForUser(username);
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
            dialog.setTitle(I18n.format("task_exec.title", currentTask[0].id(), docNo, currentTask[0].status()));
        };
        updateTitle.run();

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Document (expected data)
        Tab docTab = new Tab(I18n.get("task_exec.tab.document"));
        VBox docContent = buildDocumentTab(currentTask[0]);
        docTab.setContent(docContent);

        // Tab 2: Fact (scan form)
        Tab factTab = new Tab(I18n.get("task_exec.tab.fact"));
        VBox factContent = buildFactTab(currentTask[0], dialog);
        factTab.setContent(factContent);

        tabs.getTabs().addAll(docTab, factTab);
        tabs.getSelectionModel().select(factTab); // Start on Fact tab

        // Action buttons
        Button assignBtn = new Button(I18n.get("task_exec.btn.assign"));
        assignBtn.getStyleClass().add("refresh-btn");
        assignBtn.setPrefHeight(40);
        assignBtn.setPrefWidth(150);
        
        Button startBtn = new Button(I18n.get("task_exec.btn.start"));
        startBtn.getStyleClass().add("refresh-btn");
        startBtn.setPrefHeight(40);
        startBtn.setPrefWidth(150);
        
        Button completeBtn = new Button(I18n.get("task_exec.btn.complete"));
        completeBtn.getStyleClass().add("refresh-btn");
        completeBtn.setPrefHeight(40);
        completeBtn.setPrefWidth(150);
        
        Button releaseBtn = new Button(I18n.get("task_exec.btn.release"));
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
            String userRole = apiClient.getCurrentUser().role();
            
            boolean isOperator = "OPERATOR".equalsIgnoreCase(userRole);
            
            if (isOperator) {
                assignBtn.setDisable(!"NEW".equals(status));
            } else {
                assignBtn.setDisable(!"NEW".equals(status) && !"ASSIGNED".equals(status) && !"IN_PROGRESS".equals(status));
            }

            startBtn.setDisable(!"ASSIGNED".equals(status) || !currentUser.equals(assignee));
            completeBtn.setDisable(!"IN_PROGRESS".equals(status) || !currentUser.equals(assignee));
            releaseBtn.setDisable(!("ASSIGNED".equals(status) || "IN_PROGRESS".equals(status)) || !currentUser.equals(assignee));
        };
        updateButtons.run();
        
        // Assign action
        assignBtn.setOnAction(e -> {
            String userRole = apiClient.getCurrentUser().role();
            if ("OPERATOR".equalsIgnoreCase(userRole)) {
                if ("NEW".equals(currentTask[0].status())) {
                    assignToSelf(currentTask, assignBtn, actionStatus, updateButtons, updateTitle, docTab, factTab, dialog);
                }
            } else {
                showAssignDialog(currentTask, assignBtn, actionStatus, updateButtons, updateTitle, docTab, factTab, dialog);
            }
        });
        
        // Start action
        startBtn.setOnAction(e -> {
            startBtn.setDisable(true);
            actionStatus.setText(I18n.get("task_exec.status.starting"));
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.startTask(currentTask[0].id());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((updatedTask, error) -> Platform.runLater(() -> {
                if (error != null) {
                    actionStatus.setText(I18n.format("common.error", error.getMessage()));
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    startBtn.setDisable(false);
                } else {
                    currentTask[0] = updatedTask;
                    actionStatus.setText(I18n.get("task_exec.status.started"));
                    actionStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                    updateButtons.run();
                    updateTitle.run();
                    docTab.setContent(buildDocumentTab(currentTask[0]));
                    factTab.setContent(buildFactTab(currentTask[0], dialog));
                }
            }));
        });
        
        // Complete action
        completeBtn.setOnAction(e -> {
            completeBtn.setDisable(true);
            actionStatus.setText(I18n.get("task_exec.status.checking"));
            
            // First, fetch fresh task data from server to get updated qtyDone
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.getTask(currentTask[0].id());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((freshTask, fetchError) -> Platform.runLater(() -> {
                if (fetchError != null) {
                    actionStatus.setText(I18n.format("task_exec.error.load", fetchError.getMessage()));
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    completeBtn.setDisable(false);
                    return;
                }
                
                // Update currentTask with fresh data
                currentTask[0] = freshTask;
                
                // Check for discrepancies on client side
                boolean hasDiscrepancy = false;
                if (freshTask.qtyAssigned() != null && freshTask.qtyDone() != null) {
                    hasDiscrepancy = freshTask.qtyDone().compareTo(freshTask.qtyAssigned()) != 0;
                }
                
                final boolean clientDetectedDiscrepancy = hasDiscrepancy;
                final boolean isPlacement = "PLACEMENT".equals(freshTask.taskType());

                // Also check for existing scan discrepancies from backend
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return apiClient.hasDiscrepancies(freshTask.id());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }).whenComplete((hasScanDiscrepancies, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        actionStatus.setText(I18n.format("task_exec.error.check", error.getMessage()));
                        actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                        completeBtn.setDisable(false);
                        return;
                    }
                    
                    // Logic for PLACEMENT: Hard stop if discrepancies exist
                    if (isPlacement && (clientDetectedDiscrepancy || hasScanDiscrepancies)) {
                        new Alert(AlertType.ERROR, I18n.get("error.placement_discrepancy")).showAndWait();
                        actionStatus.setText(I18n.get("task_exec.status.cancelled"));
                        completeBtn.setDisable(false);
                        return;
                    }

                    // Logic for RECEIVING: Confirmation dialog for discrepancies
                    if (!isPlacement && (hasScanDiscrepancies || clientDetectedDiscrepancy)) {
                        boolean confirmed = showDiscrepancyConfirmationDialog();
                        if (!confirmed) {
                            actionStatus.setText(I18n.get("task_exec.status.cancelled"));
                            actionStatus.setStyle("-fx-text-fill: #FFC107; -fx-font-size: 14px;");
                            completeBtn.setDisable(false);
                            return;
                        }
                    }
                    
                    // Proceed with completion
                    actionStatus.setText(I18n.get("task_exec.status.completing"));
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return apiClient.completeTask(currentTask[0].id());
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }).whenComplete((updatedTask, error2) -> Platform.runLater(() -> {
                        if (error2 != null) {
                            actionStatus.setText(I18n.format("common.error", error2.getMessage()));
                            actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                            completeBtn.setDisable(false);
                        } else {
                            currentTask[0] = updatedTask;
                            actionStatus.setText(I18n.get("task_exec.status.completed"));
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
            actionStatus.setText(I18n.get("task_exec.status.releasing"));
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.releaseTask(currentTask[0].id());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((updatedTask, error) -> Platform.runLater(() -> {
                if (error != null) {
                    actionStatus.setText(I18n.format("common.error", error.getMessage()));
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    releaseBtn.setDisable(false);
                } else {
                    currentTask[0] = updatedTask;
                    actionStatus.setText(I18n.get("task_exec.status.released"));
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

        Label header = new Label(I18n.get("doc_tab.header"));
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label receiptLabel = new Label(I18n.format("doc_tab.receipt", (task.receiptDocNo() != null ? task.receiptDocNo() : "N/A")));
        Label taskTypeLabel = new Label(I18n.format("doc_tab.task_type", task.taskType()));
        Label statusLabel = new Label(I18n.format("doc_tab.status", task.status()));
        Label qtyLabel = new Label(I18n.format("doc_tab.qty", task.qtyAssigned()));
        
        Label skuCodeLabel = new Label(I18n.format("doc_tab.barcode", (task.skuCode() != null ? task.skuCode() : "N/A")));
        skuCodeLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label palletCodeLabel = null;
        if ("PLACEMENT".equals(task.taskType())) {
            palletCodeLabel = new Label(I18n.format("doc_tab.pallet", (task.palletCode() != null ? task.palletCode() : "N/A")));
            palletCodeLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px; -fx-font-weight: bold;");
        }

        Label assigneeLabel = new Label(I18n.format("doc_tab.assignee", (task.assignee() != null ? task.assignee() : I18n.get("doc_tab.unassigned"))));

        receiptLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        taskTypeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        qtyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        assigneeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        content.getChildren().addAll(header, receiptLabel, taskTypeLabel, statusLabel, qtyLabel, skuCodeLabel);
        if (palletCodeLabel != null) {
            content.getChildren().add(palletCodeLabel);
        }
        content.getChildren().add(assigneeLabel);
        return content;
    }

    private VBox buildFactTab(com.wmsdipl.desktop.model.Task task, Stage dialog) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1c1c1c;");

        Label header = new Label(I18n.get("fact_tab.header"));
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Show target location for PLACEMENT tasks
        Label targetLocationInfo = null;
        if ("PLACEMENT".equals(task.taskType()) && task.targetLocationCode() != null) {
            targetLocationInfo = new Label(I18n.format("fact_tab.target_location", task.targetLocationCode()));
            targetLocationInfo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-padding: 8px; -fx-background-color: rgba(76,175,80,0.1); -fx-background-radius: 4px;");
        }

        // Scan fields (blue, 16px, with 📷 icon)
        Label palletLabel = new Label(I18n.get("terminal.scan.pallet"));
        palletLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px;");
        TextField palletField = new TextField();
        palletField.setPromptText(I18n.get("fact_tab.prompt.pallet"));
        palletField.getStyleClass().add("scan-field");
        palletField.setPrefHeight(48);

        Label barcodeLabel = new Label(I18n.get("terminal.scan.barcode"));
        barcodeLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px;");
        TextField barcodeField = new TextField();
        barcodeField.setPromptText(I18n.get("fact_tab.prompt.barcode"));
        barcodeField.getStyleClass().add("scan-field");
        barcodeField.setPrefHeight(48);

        // Input fields (gray, 14px, with ✏️ icon)
        Label qtyLabel = new Label(I18n.get("terminal.scan.qty"));
        qtyLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
        TextField qtyField = new TextField();
        qtyField.setPromptText(I18n.get("fact_tab.prompt.qty"));
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

        // Damage tracking section
        Label damageHeaderLabel = new Label(I18n.get("fact_tab.damage.header"));
        damageHeaderLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 14px; -fx-font-weight: bold;");

        CheckBox damageCheckBox = new CheckBox(I18n.get("fact_tab.damage.checkbox"));
        damageCheckBox.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        ComboBox<String> damageTypeCombo = new ComboBox<>();
        damageTypeCombo.getItems().addAll("PHYSICAL", "WATER", "EXPIRED", "OTHER");
        damageTypeCombo.setPromptText(I18n.get("fact_tab.damage.type_prompt"));
        damageTypeCombo.setPrefHeight(40);
        damageTypeCombo.getStyleClass().add("input-field");
        damageTypeCombo.setDisable(true);

        TextArea damageDescriptionField = new TextArea();
        damageDescriptionField.setPromptText(I18n.get("fact_tab.damage.desc_prompt"));
        damageDescriptionField.setPrefHeight(60);
        damageDescriptionField.setMaxHeight(60);
        damageDescriptionField.getStyleClass().add("input-field");
        damageDescriptionField.setDisable(true);

        // Enable/disable damage fields based on checkbox
        damageCheckBox.selectedProperty().addListener((obs, old, selected) -> {
            damageTypeCombo.setDisable(!selected);
            damageDescriptionField.setDisable(!selected);
            if (!selected) {
                damageTypeCombo.setValue(null);
                damageDescriptionField.clear();
            }
        });

        // Lot tracking section
        Label lotHeaderLabel = new Label(I18n.get("fact_tab.lot.header"));
        lotHeaderLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px; -fx-font-weight: bold;");

        TextField lotNumberField = new TextField();
        lotNumberField.setPromptText(I18n.get("fact_tab.lot.number_prompt"));
        lotNumberField.getStyleClass().add("input-field");
        lotNumberField.setPrefHeight(40);

        DatePicker expiryDatePicker = new DatePicker();
        expiryDatePicker.setPromptText(I18n.get("fact_tab.lot.expiry_prompt"));
        expiryDatePicker.setPrefHeight(40);
        expiryDatePicker.getStyleClass().add("input-field");

        // Location field (only for PLACEMENT tasks)
        Label locationLabel = null;
        TextField locationField = null;
        boolean isPlacementTask = "PLACEMENT".equals(task.taskType());
        
        if (isPlacementTask) {
            locationLabel = new Label(I18n.get("fact_tab.location_label"));
            locationLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px;");
            locationField = new TextField();
            locationField.setPromptText(I18n.get("fact_tab.location_prompt"));
            locationField.getStyleClass().add("scan-field");
            locationField.setPrefHeight(48);
        }

        Label commentLabel = new Label(I18n.get("fact_tab.comment_label"));
        commentLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
        TextArea commentField = new TextArea();
        commentField.setPromptText(I18n.get("fact_tab.comment_prompt"));
        commentField.getStyleClass().add("input-field");
        commentField.setPrefHeight(60);
        commentField.setMaxHeight(60);

        // Submit button
        Button submitBtn = new Button(I18n.get("fact_tab.submit"));
        submitBtn.getStyleClass().add("refresh-btn");
        submitBtn.setPrefHeight(48);
        submitBtn.setPrefWidth(200);

        // Disable all fields if task is not STARTED
        boolean isStarted = "IN_PROGRESS".equals(task.status());
        palletField.setDisable(!isStarted);
        barcodeField.setDisable(!isStarted);
        qtyField.setDisable(!isStarted);
        commentField.setDisable(!isStarted);
        submitBtn.setDisable(!isStarted);
        damageCheckBox.setDisable(!isStarted);
        lotNumberField.setDisable(!isStarted);
        expiryDatePicker.setDisable(!isStarted);
        if (locationField != null) {
            locationField.setDisable(!isStarted);
        }

        // Scan history table
        TableView<Scan> scanTable = new TableView<>();
        scanTable.setPlaceholder(new Label(I18n.get("fact_tab.no_scans")));
        scanTable.setPrefHeight(200);
        scanTable.getColumns().addAll(
            column(I18n.get("fact_tab.col.pallet"), Scan::palletCode),
            column(I18n.get("fact_tab.col.barcode"), Scan::barcode),
            column(I18n.get("fact_tab.col.qty"), Scan::qty),
            column(I18n.get("fact_tab.col.discrepancy"), s -> {
                if (s.discrepancy() == null || !s.discrepancy()) {
                    return I18n.get("fact_tab.discrepancy.ok");
                }
                return I18n.get("fact_tab.discrepancy.yes");
            }),
            column(I18n.get("fact_tab.col.damage"), s -> {
                if (s.damageFlag() != null && s.damageFlag()) {
                    return "🚨 " + (s.damageType() != null ? s.damageType() : I18n.get("fact_tab.damage.yes"));
                }
                return "";
            }),
            column(I18n.get("fact_tab.col.lot"), s -> s.lotNumber() != null ? s.lotNumber() : ""),
            column(I18n.get("fact_tab.col.expiry"), s -> s.expiryDate() != null ? s.expiryDate().toString() : ""),
            column(I18n.get("fact_tab.col.time"), s -> s.scannedAt() != null ? s.scannedAt().toString() : "")
        );

        // Enter key navigation
        TextField finalLocationField = locationField;
        palletField.setOnAction(e -> barcodeField.requestFocus());
        barcodeField.setOnAction(e -> qtyField.requestFocus());
        qtyField.setOnAction(e -> {
            if (isPlacementTask && finalLocationField != null) {
                finalLocationField.requestFocus();
            } else if (!qtyField.getText().isBlank()) {
                submitBtn.fire();
            } else {
                commentField.requestFocus();
            }
        });
        
        if (locationField != null) {
            locationField.setOnAction(e -> submitBtn.fire());
        }

        // Submit action
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        TextField finalLocationField2 = locationField;
        submitBtn.setOnAction(e -> {
            String palletCode = palletField.getText().trim();
            String barcode = barcodeField.getText().trim();
            String qtyStr = qtyField.getText().trim();
            String locationCode = finalLocationField2 != null ? finalLocationField2.getText().trim() : null;

            if (palletCode.isEmpty()) {
                showError(statusLabel, I18n.get("fact_tab.error.pallet_required"), palletField);
                return;
            }
            if (qtyStr.isEmpty()) {
                showError(statusLabel, I18n.get("fact_tab.error.qty_required"), qtyField);
                return;
            }
            
            // Validate location for PLACEMENT tasks
            if (isPlacementTask && (locationCode == null || locationCode.isEmpty())) {
                showError(statusLabel, I18n.get("fact_tab.error.location_required"), finalLocationField2);
                return;
            }

            Integer qty = Integer.parseInt(qtyStr);
            String comment = commentField.getText().trim();
            
            // Extract damage tracking fields
            Boolean damageFlag = damageCheckBox.isSelected();
            String damageType = damageCheckBox.isSelected() ? damageTypeCombo.getValue() : null;
            String damageDesc = damageCheckBox.isSelected() ? damageDescriptionField.getText().trim() : null;
            
            // Extract lot tracking fields
            String lotNumber = lotNumberField.getText().trim();
            String expiryDate = expiryDatePicker.getValue() != null ? expiryDatePicker.getValue().toString() : null;

            submitBtn.setDisable(true);
            statusLabel.setText(I18n.get("fact_tab.status.sending"));

            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.recordScan(task.id(), palletCode, barcode, qty, comment, locationCode,
                                               damageFlag, damageType, damageDesc, lotNumber, expiryDate);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((scan, error) -> Platform.runLater(() -> {
                submitBtn.setDisable(!isStarted);
                if (error != null) {
                    showError(statusLabel, I18n.format("common.error", error.getMessage()), palletField);
                } else {
                    showSuccess(statusLabel, I18n.get("fact_tab.status.saved"), palletField);
                    // Clear fields
                    palletField.clear();
                    barcodeField.clear();
                    qtyField.clear();
                    commentField.clear();
                    damageCheckBox.setSelected(false);
                    damageTypeCombo.setValue(null);
                    damageDescriptionField.clear();
                    lotNumberField.clear();
                    expiryDatePicker.setValue(null);
                    if (finalLocationField2 != null) {
                        finalLocationField2.clear();
                    }
                    palletField.requestFocus();
                    // Reload scans
                    loadTaskScans(task, scanTable);
                }
            }));
        });

        // Load initial scans
        loadTaskScans(task, scanTable);

        // Build form with conditional location field
        VBox form;
        if (isPlacementTask && locationLabel != null && locationField != null) {
            form = new VBox(8, 
                palletLabel, palletField,
                barcodeLabel, barcodeField,
                qtyLabel, qtyField,
                locationLabel, locationField,
                commentLabel, commentField,
                submitBtn,
                statusLabel
            );
        } else {
            form = new VBox(8, 
                palletLabel, palletField,
                barcodeLabel, barcodeField,
                qtyLabel, qtyField,
                damageHeaderLabel, damageCheckBox, damageTypeCombo, damageDescriptionField,
                lotHeaderLabel, lotNumberField, expiryDatePicker,
                commentLabel, commentField,
                submitBtn,
                statusLabel
            );
        }

        if (targetLocationInfo != null) {
            content.getChildren().addAll(header, targetLocationInfo, form, new Label(I18n.get("fact_tab.scan_history")), scanTable);
        } else {
            content.getChildren().addAll(header, form, new Label(I18n.get("fact_tab.scan_history")), scanTable);
        }
        VBox.setVgrow(scanTable, Priority.ALWAYS);

        // Auto-focus pallet field if task is started
        if (isStarted) {
            Platform.runLater(() -> palletField.requestFocus());
        }

        // Wrap content in ScrollPane
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: #1c1c1c; -fx-background-color: #1c1c1c;");
        
        VBox root = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return root;
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
                table.setPlaceholder(new Label(I18n.format("common.error", error.getMessage())));
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
        dialog.setTitle(I18n.get("pallet.create.title"));

        Label codeLabel = new Label(I18n.get("pallet.create.code_label"));
        codeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        TextField codeField = new TextField();
        codeField.setPromptText(I18n.get("pallet.create.code_prompt"));
        codeField.setPrefWidth(300);
        
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        Button createBtn = new Button(I18n.get("common.create"));
        createBtn.getStyleClass().add("refresh-btn");
        createBtn.setPrefWidth(150);
        
        Button cancelBtn = new Button(I18n.get("common.cancel"));
        cancelBtn.getStyleClass().add("refresh-btn");
        cancelBtn.setPrefWidth(150);
        cancelBtn.setOnAction(e -> dialog.close());
        
        HBox buttonBox = new HBox(12, createBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        createBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                statusLabel.setText(I18n.get("pallet.create.error.code_required"));
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }
            
            createBtn.setDisable(true);
            statusLabel.setText(I18n.get("pallet.create.status.creating"));
            statusLabel.setStyle("-fx-text-fill: #FFC107; -fx-font-size: 12px;");
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.createPallet(code);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((pallet, error) -> Platform.runLater(() -> {
                if (error != null) {
                    statusLabel.setText(I18n.format("common.error", error.getCause().getMessage()));
                    statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                    createBtn.setDisable(false);
                } else {
                    statusLabel.setText(I18n.format("pallet.create.status.created", pallet.code()));
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
        dialog.setTitle(I18n.get("sku.create.title"));

        Label codeLabel = new Label(I18n.get("sku.create.code_label"));
        codeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField codeField = new TextField();
        codeField.setPromptText(I18n.get("sku.create.code_prompt"));
        codeField.setPrefWidth(300);

        Label nameLabel = new Label(I18n.get("sku.create.name_label"));
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("sku.create.name_prompt"));
        nameField.setPrefWidth(300);

        Label uomLabel = new Label(I18n.get("sku.create.uom_label"));
        uomLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField uomField = new TextField(I18n.get("sku.field.uom_default"));
        uomField.setPrefWidth(300);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        Button createBtn = new Button(I18n.get("common.create"));
        createBtn.getStyleClass().add("refresh-btn");
        createBtn.setPrefWidth(150);

        Button cancelBtn = new Button(I18n.get("common.cancel"));
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
                statusLabel.setText(I18n.get("sku.create.error.code_required"));
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }
            if (name.isEmpty()) {
                statusLabel.setText(I18n.get("sku.create.error.name_required"));
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }
            if (uom.isEmpty()) {
                statusLabel.setText(I18n.get("sku.create.error.uom_required"));
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                return;
            }

            createBtn.setDisable(true);
            statusLabel.setText(I18n.get("sku.create.status.creating"));
            statusLabel.setStyle("-fx-text-fill: #FFC107; -fx-font-size: 12px;");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.createSku(code, name, uom);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((sku, error) -> Platform.runLater(() -> {
                if (error != null) {
                    statusLabel.setText(I18n.format("common.error", error.getCause().getMessage()));
                    statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                    createBtn.setDisable(false);
                } else {
                    statusLabel.setText(I18n.format("sku.create.status.created", sku.code()));
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
        alert.setTitle(I18n.get("alert.discrepancy.title"));
        alert.setHeaderText(I18n.get("alert.discrepancy.header"));
        alert.setContentText(I18n.get("alert.discrepancy.content"));
        
        // Customize button text
        ButtonType confirmBtn = new ButtonType(I18n.get("alert.discrepancy.btn.confirm"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType(I18n.get("alert.discrepancy.btn.cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmBtn;
    }

    // ============================================================
    // ZONE MANAGEMENT METHODS
    // ============================================================

    private void openZoneCreationDialog(ListView<com.wmsdipl.desktop.model.Zone> zonesView) {
        Dialog<com.wmsdipl.desktop.model.Zone> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("zone.create.title"));
        dialog.setHeaderText(I18n.get("zone.create.header"));
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField codeField = new TextField();
        codeField.setPromptText(I18n.get("zone.create.code_prompt"));
        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("zone.create.name_prompt"));
        TextField priorityField = new TextField("100");
        priorityField.setPromptText(I18n.get("zone.create.priority_prompt"));
        TextArea descField = new TextArea();
        descField.setPromptText(I18n.get("zone.create.desc_prompt"));
        descField.setPrefRowCount(3);
        
        grid.add(new Label(I18n.get("zone.create.code_label")), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label(I18n.get("zone.create.name_label")), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label(I18n.get("zone.create.priority_label")), 0, 2);
        grid.add(priorityField, 1, 2);
        grid.add(new Label(I18n.get("zone.create.desc_label")), 0, 3);
        grid.add(descField, 1, 3);
        
        dialogPane.setContent(grid);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String code = codeField.getText().trim();
                    String name = nameField.getText().trim();
                    Integer priority = priorityField.getText().trim().isEmpty() ? 100 : Integer.parseInt(priorityField.getText().trim());
                    String desc = descField.getText().trim();
                    
                    if (code.isEmpty() || name.isEmpty()) {
                        showError(I18n.get("zone.create.error.required"));
                        return null;
                    }
                    
                    com.wmsdipl.desktop.model.Zone created = apiClient.createZone(code, name, priority, desc.isEmpty() ? null : desc);
                    showInfo(I18n.format("zone.create.success", created.code()));
                    loadTopology(zonesView, null);
                    return created;
                } catch (Exception ex) {
                    showError(I18n.format("zone.create.error.failed", ex.getMessage()));
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private void openZoneEditDialog(ListView<com.wmsdipl.desktop.model.Zone> zonesView, com.wmsdipl.desktop.model.Zone zone) {
        if (zone == null) return;
        
        Dialog<com.wmsdipl.desktop.model.Zone> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("zone.edit.title"));
        dialog.setHeaderText(I18n.format("zone.edit.header", zone.code()));
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField codeField = new TextField(zone.code());
        TextField nameField = new TextField(zone.name());
        TextField priorityField = new TextField(zone.priorityRank() != null ? zone.priorityRank().toString() : "100");
        TextArea descField = new TextArea(zone.description() != null ? zone.description() : "");
        descField.setPrefRowCount(3);
        CheckBox activeCheck = new CheckBox(I18n.get("zone.edit.active_label"));
        activeCheck.setSelected(zone.active() != null ? zone.active() : true);
        
        grid.add(new Label(I18n.get("zone.edit.code_label")), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label(I18n.get("zone.edit.name_label")), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label(I18n.get("zone.edit.priority_label")), 0, 2);
        grid.add(priorityField, 1, 2);
        grid.add(new Label(I18n.get("zone.edit.desc_label")), 0, 3);
        grid.add(descField, 1, 3);
        grid.add(activeCheck, 1, 4);
        
        dialogPane.setContent(grid);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String code = codeField.getText().trim();
                    String name = nameField.getText().trim();
                    Integer priority = priorityField.getText().trim().isEmpty() ? 100 : Integer.parseInt(priorityField.getText().trim());
                    String desc = descField.getText().trim();
                    Boolean active = activeCheck.isSelected();
                    
                    if (code.isEmpty() || name.isEmpty()) {
                        showError(I18n.get("zone.edit.error.required"));
                        return null;
                    }
                    
                    com.wmsdipl.desktop.model.Zone updated = apiClient.updateZone(zone.id(), code, name, priority, desc.isEmpty() ? null : desc, active);
                    showInfo(I18n.get("zone.edit.success"));
                    loadTopology(zonesView, null);
                    return updated;
                } catch (Exception ex) {
                    showError(I18n.format("zone.edit.error.failed", ex.getMessage()));
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private void deleteZone(ListView<com.wmsdipl.desktop.model.Zone> zonesView, com.wmsdipl.desktop.model.Zone zone) {
        if (zone == null) return;
        
        if (!showConfirm(I18n.format("zone.delete.title", zone.code()), I18n.get("zone.delete.message"))) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                apiClient.deleteZone(zone.id());
            } catch (Exception ex) {
                Platform.runLater(() -> showError(I18n.format("zone.delete.error", ex.getMessage())));
                return;
            }
            Platform.runLater(() -> {
                showInfo(I18n.get("zone.delete.success"));
                loadTopology(zonesView, null);
            });
        });
    }

    // ============================================================
    // LOCATION MANAGEMENT METHODS
    // ============================================================

    private void openLocationCreationDialog(TableView<Location> locTable, ListView<com.wmsdipl.desktop.model.Zone> zonesView) {
        Dialog<Location> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("location.create.title"));
        dialog.setHeaderText(I18n.get("location.create.header"));
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<com.wmsdipl.desktop.model.Zone> zoneCombo = new ComboBox<>(zonesView.getItems());
        zoneCombo.setPromptText(I18n.get("location.create.zone_prompt"));
        zoneCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(com.wmsdipl.desktop.model.Zone item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.code() + " - " + item.name());
            }
        });
        zoneCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(com.wmsdipl.desktop.model.Zone item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.code() + " - " + item.name());
            }
        });
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("RECEIVING", "STORAGE", "SHIPPING", "CROSS_DOCK", "DAMAGED", "QUARANTINE");
        typeCombo.setPromptText(I18n.get("location.create.type_prompt"));
        typeCombo.setValue("STORAGE"); // Default value
        
        TextField codeField = new TextField();
        codeField.setPromptText(I18n.get("location.create.code_prompt"));
        TextField aisleField = new TextField();
        aisleField.setPromptText(I18n.get("location.create.aisle_prompt"));
        TextField bayField = new TextField();
        bayField.setPromptText(I18n.get("location.create.bay_prompt"));
        TextField levelField = new TextField();
        levelField.setPromptText(I18n.get("location.create.level_prompt"));
        TextField maxPalletsField = new TextField("1");
        maxPalletsField.setPromptText(I18n.get("location.create.capacity_prompt"));
        maxPalletsField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d+")) {
                return change;
            }
            return null;
        }));
        
        grid.add(new Label(I18n.get("location.create.zone_label")), 0, 0);
        grid.add(zoneCombo, 1, 0);
        grid.add(new Label(I18n.get("location.create.code_label")), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label(I18n.get("location.create.type_label")), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label(I18n.get("location.create.capacity_label")), 0, 3);
        grid.add(maxPalletsField, 1, 3);
        grid.add(new Label(I18n.get("location.create.aisle_label")), 0, 4);
        grid.add(aisleField, 1, 4);
        grid.add(new Label(I18n.get("location.create.bay_label")), 0, 5);
        grid.add(bayField, 1, 5);
        grid.add(new Label(I18n.get("location.create.level_label")), 0, 6);
        grid.add(levelField, 1, 6);
        
        dialogPane.setContent(grid);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    com.wmsdipl.desktop.model.Zone zone = zoneCombo.getValue();
                    String code = codeField.getText().trim();
                    String locationType = typeCombo.getValue();
                    String aisle = aisleField.getText().trim();
                    String bay = bayField.getText().trim();
                    String level = levelField.getText().trim();
                    Integer maxPallets = maxPalletsField.getText().trim().isEmpty() ? 1 : Integer.parseInt(maxPalletsField.getText().trim());
                    
                    if (zone == null || code.isEmpty()) {
                        showError(I18n.get("location.create.error.required"));
                        return null;
                    }
                    
                    if (maxPallets <= 0) {
                        showError(I18n.get("location.create.error.capacity"));
                        return null;
                    }
                    
                    Location created = apiClient.createLocation(
                        zone.id(),
                        code,
                        locationType,
                        aisle.isEmpty() ? null : aisle,
                        bay.isEmpty() ? null : bay,
                        level.isEmpty() ? null : level,
                        maxPallets
                    );
                    showInfo(I18n.format("location.create.success", created.code()));
                    loadTopology(zonesView, locTable);
                    return created;
                } catch (Exception ex) {
                    showError(I18n.format("location.create.error.failed", ex.getMessage()));
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private void openLocationEditDialog(TableView<Location> locTable, Location location, ListView<com.wmsdipl.desktop.model.Zone> zonesView) {
        if (location == null) return;
        
        Dialog<Location> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("location.edit.title"));
        dialog.setHeaderText(I18n.format("location.edit.header", location.code()));
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<com.wmsdipl.desktop.model.Zone> zoneCombo = new ComboBox<>(zonesView.getItems());
        zoneCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(com.wmsdipl.desktop.model.Zone item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.code() + " - " + item.name());
            }
        });
        zoneCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(com.wmsdipl.desktop.model.Zone item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.code() + " - " + item.name());
            }
        });
        if (location.zoneId() != null) {
            zoneCombo.setValue(zonesView.getItems().stream()
                .filter(z -> z.id().equals(location.zoneId()))
                .findFirst().orElse(null));
        }
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("RECEIVING", "STORAGE", "SHIPPING", "QUARANTINE");
        typeCombo.setValue(location.locationType() != null ? location.locationType() : "STORAGE");
        
        TextField codeField = new TextField(location.code());
        TextField aisleField = new TextField(location.aisle() != null ? location.aisle() : "");
        TextField bayField = new TextField(location.bay() != null ? location.bay() : "");
        TextField levelField = new TextField(location.level() != null ? location.level() : "");
        TextField maxPalletsField = new TextField(location.maxPallets() != null ? location.maxPallets().toString() : "1");
        maxPalletsField.setPromptText(I18n.get("location.edit.capacity_prompt"));
        maxPalletsField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d+")) {
                return change;
            }
            return null;
        }));
        CheckBox activeCheck = new CheckBox(I18n.get("location.edit.active_label"));
        activeCheck.setSelected(location.active() != null ? location.active() : true);
        
        grid.add(new Label(I18n.get("location.edit.zone_label")), 0, 0);
        grid.add(zoneCombo, 1, 0);
        grid.add(new Label(I18n.get("location.edit.code_label")), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label(I18n.get("location.edit.type_label")), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label(I18n.get("location.edit.capacity_label")), 0, 3);
        grid.add(maxPalletsField, 1, 3);
        grid.add(new Label(I18n.get("location.edit.aisle_label")), 0, 4);
        grid.add(aisleField, 1, 4);
        grid.add(new Label(I18n.get("location.edit.bay_label")), 0, 5);
        grid.add(bayField, 1, 5);
        grid.add(new Label(I18n.get("location.edit.level_label")), 0, 6);
        grid.add(levelField, 1, 6);
        grid.add(activeCheck, 1, 7);
        
        dialogPane.setContent(grid);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    com.wmsdipl.desktop.model.Zone zone = zoneCombo.getValue();
                    String code = codeField.getText().trim();
                    String locationType = typeCombo.getValue();
                    String aisle = aisleField.getText().trim();
                    String bay = bayField.getText().trim();
                    String level = levelField.getText().trim();
                    Integer maxPallets = maxPalletsField.getText().trim().isEmpty() ? 1 : Integer.parseInt(maxPalletsField.getText().trim());
                    Boolean active = activeCheck.isSelected();
                    
                    if (zone == null || code.isEmpty()) {
                        showError(I18n.get("location.edit.error.required"));
                        return null;
                    }
                    
                    if (maxPallets <= 0) {
                        showError(I18n.get("location.edit.error.capacity"));
                        return null;
                    }
                    
                    Location updated = apiClient.updateLocation(
                        location.id(),
                        zone.id(),
                        code,
                        locationType,
                        aisle.isEmpty() ? null : aisle,
                        bay.isEmpty() ? null : bay,
                        level.isEmpty() ? null : level,
                        maxPallets,
                        null, // Don't update status here
                        active
                    );
                    showInfo(I18n.get("location.edit.success"));
                    loadTopology(zonesView, locTable);
                    return updated;
                } catch (Exception ex) {
                    showError(I18n.format("location.edit.error.failed", ex.getMessage()));
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private void toggleLocationBlock(TableView<Location> locTable, Location location) {
        if (location == null) return;
        
        boolean isBlocked = "BLOCKED".equals(location.status());
        String action = isBlocked ? I18n.get("location.action.unblock") : I18n.get("location.action.block");
        
        if (!showConfirm(
            I18n.format(isBlocked ? "location.confirm.unblock" : "location.confirm.block", location.code()),
            I18n.get(isBlocked ? "location.confirm.unblock_msg" : "location.confirm.block_msg"))) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                if (isBlocked) {
                    apiClient.unblockLocation(location.id());
                } else {
                    apiClient.blockLocation(location.id());
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showError(I18n.format("location.error.operation", ex.getMessage())));
                return;
            }
            Platform.runLater(() -> {
                showInfo(I18n.get(isBlocked ? "location.success.unblocked" : "location.success.blocked"));
                loadTopology(null, locTable);
            });
        });
    }

    private void deleteLocation(TableView<Location> locTable, Location location) {
        if (location == null) return;
        
        if (!showConfirm(I18n.format("location.confirm.delete", location.code()), I18n.get("location.confirm.delete_msg"))) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                apiClient.deleteLocation(location.id());
            } catch (Exception ex) {
                Platform.runLater(() -> showError(I18n.format("location.error.delete", ex.getMessage())));
                return;
            }
            Platform.runLater(() -> {
                showInfo(I18n.get("location.success.deleted"));
                loadTopology(null, locTable);
            });
        });
    }

    // Simple alert helpers for topology management
    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(I18n.get("common.alert.info"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(I18n.get("common.alert.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showConfirm(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("common.alert.confirm"));
        alert.setHeaderText(title);
        alert.setContentText(message);
        
        java.util.Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    private void handleLogout() {
        if (!showConfirm(I18n.get("logout.title"), I18n.get("logout.message"))) {
            return;
        }
        
        apiClient.logout();
        
        // Get the current stage
        Stage currentStage = (Stage) shell.getScene().getWindow();
        
        // Show login dialog again
        if (showLoginDialog()) {
            // Refresh the navigation after successful re-login
            shell.setLeft(buildNav());
            showReceiptsPane();
        } else {
            // If login cancelled or failed, close the application
            Platform.exit();
        }
    }

    private void showAnalyticsPane() {
        activeModule = "analytics";
        shell.setLeft(buildNav());

        Label header = new Label(I18n.get("analytics.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Period selection
        ComboBox<String> periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll(I18n.get("analytics.range.today"), I18n.get("analytics.range.week"), I18n.get("analytics.range.month"));
        periodCombo.setValue(I18n.get("analytics.range.today"));
        periodCombo.setPrefHeight(40);

        Button refreshBtn = new Button(I18n.get("analytics.btn.refresh"));
        refreshBtn.getStyleClass().add("refresh-btn");
        refreshBtn.setPrefHeight(40);

        HBox controls = new HBox(10, new Label(I18n.get("analytics.lbl.period")), periodCombo, refreshBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setStyle("-fx-text-fill: white;");

        // Analytics display area
        VBox analyticsBox = new VBox(16);
        analyticsBox.setPadding(new Insets(16));
        analyticsBox.setStyle("-fx-background-color: #2c2c2c; -fx-background-radius: 8px;");

        Label loadingLabel = new Label(I18n.get("analytics.lbl.loading"));
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        analyticsBox.getChildren().add(loadingLabel);

        Runnable loadAnalytics = () -> {
            String period = periodCombo.getValue();
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    if (period.equals(I18n.get("analytics.range.today"))) {
                        return apiClient.getTodayAnalytics();
                    } else if (period.equals(I18n.get("analytics.range.week"))) {
                        return apiClient.getWeekAnalytics();
                    } else if (period.equals(I18n.get("analytics.range.month"))) {
                        return apiClient.getMonthAnalytics();
                    } else {
                        return apiClient.getTodayAnalytics();
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((data, error) -> Platform.runLater(() -> {
                analyticsBox.getChildren().clear();
                
                if (error != null) {
                    Label errorLabel = new Label(I18n.format("common.error", error.getMessage()));
                    errorLabel.setStyle("-fx-text-fill: #FF5252; -fx-font-size: 14px;");
                    analyticsBox.getChildren().add(errorLabel);
                    return;
                }

                // Cast data to Map
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;

                // Display analytics data
                VBox metricsBox = new VBox(12);
                metricsBox.setStyle("-fx-text-fill: white;");

                // Receipts by status
                Label receiptsHeader = new Label(I18n.get("analytics.lbl.receipts_by_status"));
                receiptsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
                
                @SuppressWarnings("unchecked")
                Map<String, Integer> receiptsByStatus = (Map<String, Integer>) dataMap.get("receiptsByStatus");
                if (receiptsByStatus != null) {
                    receiptsByStatus.forEach((status, count) -> {
                        Label statusLabel = new Label("  " + status + ": " + count);
                        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                        metricsBox.getChildren().add(statusLabel);
                    });
                }

                // Discrepancies by type
                Label discrepanciesHeader = new Label(I18n.get("analytics.lbl.discrepancies"));
                discrepanciesHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FF9800;");
                
                @SuppressWarnings("unchecked")
                Map<String, Integer> discrepanciesByType = (Map<String, Integer>) dataMap.get("discrepanciesByType");
                if (discrepanciesByType != null) {
                    discrepanciesByType.forEach((type, count) -> {
                        Label typeLabel = new Label("  " + type + ": " + count);
                        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                        metricsBox.getChildren().add(typeLabel);
                    });
                }

                // Pallets by status
                Label palletsHeader = new Label(I18n.get("analytics.lbl.pallets_by_status"));
                palletsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                
                @SuppressWarnings("unchecked")
                Map<String, Integer> palletsByStatus = (Map<String, Integer>) dataMap.get("palletsByStatus");
                if (palletsByStatus != null) {
                    palletsByStatus.forEach((status, count) -> {
                        Label statusLabel = new Label("  " + status + ": " + count);
                        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                        metricsBox.getChildren().add(statusLabel);
                    });
                }

                // Key metrics
                Label metricsHeader = new Label(I18n.get("analytics.lbl.key_metrics"));
                metricsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #9C27B0;");
                
                Object discrepancyRateObj = dataMap.get("discrepancyRate");
                Double discrepancyRate = discrepancyRateObj instanceof Number 
                    ? ((Number) discrepancyRateObj).doubleValue() 
                    : 0.0;
                Label discrepancyLabel = new Label(String.format("  " + I18n.get("analytics.lbl.discrepancy_rate") + ": %.2f%%", discrepancyRate));
                discrepancyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                
                Object damagedRateObj = dataMap.get("damagedPalletsRate");
                Double damagedRate = damagedRateObj instanceof Number 
                    ? ((Number) damagedRateObj).doubleValue() 
                    : 0.0;
                Label damagedLabel = new Label(String.format("  " + I18n.get("analytics.lbl.damage_rate") + ": %.2f%%", damagedRate));
                damagedLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                
                Object avgTimeObj = dataMap.get("avgReceivingTimeHours");
                Double avgTime = avgTimeObj instanceof Number 
                    ? ((Number) avgTimeObj).doubleValue() 
                    : 0.0;
                Label avgTimeLabel = new Label(String.format("  " + I18n.get("analytics.lbl.avg_time") + ": %.2f " + I18n.get("analytics.lbl.hours"), avgTime));
                avgTimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

                metricsBox.getChildren().addAll(
                    receiptsHeader, 
                    new Label(""), // spacer
                    discrepanciesHeader, 
                    new Label(""), // spacer
                    palletsHeader,
                    new Label(""), // spacer
                    metricsHeader,
                    discrepancyLabel,
                    damagedLabel,
                    avgTimeLabel
                );

                analyticsBox.getChildren().add(metricsBox);
            }));
        };

        refreshBtn.setOnAction(e -> loadAnalytics.run());
        loadAnalytics.run(); // Initial load

        VBox layout = new VBox(16, header, controls, analyticsBox);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #1c1c1c;");
        VBox.setVgrow(analyticsBox, Priority.ALWAYS);

        setContent(layout);
    }

    private ComboBox<String> createSearchableUserComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.setPromptText(I18n.get("common.user_prompt"));
        
        CompletableFuture.runAsync(() -> {
            try {
                List<User> users = apiClient.listUsers();
                List<String> names = users.stream().map(User::username).sorted().collect(Collectors.toList());
                Platform.runLater(() -> {
                    comboBox.getItems().addAll(names);
                    new AutoCompleteComboBoxListener<>(comboBox);
                });
            } catch (Exception e) {
                // Ignore errors (maybe no permission to list users)
            }
        });
        
        return comboBox;
    }

    private void showAssignDialog(com.wmsdipl.desktop.model.Task[] currentTaskWrapper, Button assignBtn, Label actionStatus, 
                                  Runnable updateButtons, Runnable updateTitle, Tab docTab, Tab factTab, Stage parentDialog) {
        Stage dialog = new Stage();
        dialog.setTitle(I18n.get("assign.title"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentDialog);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1c1c1c;");

        Label label = new Label(I18n.get("assign.label"));
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        ComboBox<String> userCombo = createSearchableUserComboBox();
        userCombo.setPrefWidth(250);

        Button confirmBtn = new Button(I18n.get("assign.button"));
        confirmBtn.getStyleClass().add("btn-success");
        confirmBtn.setOnAction(e -> {
            String assignee = userCombo.getEditor().getText();
            if (assignee == null || assignee.isBlank()) {
                showError(I18n.get("assign.error.empty"));
                return;
            }
            
            assignBtn.setDisable(true);
            actionStatus.setText(I18n.get("assign.status.assigning"));
            dialog.close();
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.assignTask(currentTaskWrapper[0].id(), assignee);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((updatedTask, error) -> Platform.runLater(() -> {
                if (error != null) {
                    actionStatus.setText(I18n.format("assign.status.error", error.getMessage()));
                    actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                    assignBtn.setDisable(false);
                } else {
                    currentTaskWrapper[0] = updatedTask;
                    actionStatus.setText(I18n.format("assign.status.success", updatedTask.assignee()));
                    actionStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                    updateButtons.run();
                    updateTitle.run();
                    docTab.setContent(buildDocumentTab(updatedTask));
                    factTab.setContent(buildFactTab(updatedTask, parentDialog));
                }
            }));
        });

        content.getChildren().addAll(label, userCombo, confirmBtn);
        Scene scene = new Scene(content, 300, 200);
        applyStyles(scene);
        dialog.setScene(scene);
        dialog.show();
    }

    private void assignToSelf(com.wmsdipl.desktop.model.Task[] currentTaskWrapper, Button assignBtn, Label actionStatus,
                              Runnable updateButtons, Runnable updateTitle, Tab docTab, Tab factTab, Stage dialog) {
        assignBtn.setDisable(true);
        actionStatus.setText(I18n.get("assign.status.assigning"));
        CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.assignTask(currentTaskWrapper[0].id(), apiClient.getCurrentUsername());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).whenComplete((updatedTask, error) -> Platform.runLater(() -> {
            if (error != null) {
                actionStatus.setText(I18n.format("assign.status.error", error.getMessage()));
                actionStatus.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
                assignBtn.setDisable(false);
            } else {
                currentTaskWrapper[0] = updatedTask;
                actionStatus.setText(I18n.get("assign.status.success_self"));
                actionStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                updateButtons.run();
                updateTitle.run();
                docTab.setContent(buildDocumentTab(updatedTask));
                factTab.setContent(buildFactTab(updatedTask, dialog));
            }
        }));
    }

    private void showBulkOperationsDialog(TableView<com.wmsdipl.desktop.model.Task> taskTable) {
        Stage dialog = new Stage();
        dialog.setTitle(I18n.get("bulk.title"));
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1c1c1c;");

        Label header = new Label(I18n.get("bulk.header"));
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Selected tasks info
        var selectedTasks = taskTable.getSelectionModel().getSelectedItems();
        Label infoLabel = new Label(I18n.format("bulk.selected_count", selectedTasks.size()));
        infoLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: #2c2c2c;");

        // Tab 1: Bulk Assign
        Tab assignTab = new Tab(I18n.get("bulk.tab.assign"));
        assignTab.setClosable(false);
        VBox assignBox = new VBox(12);
        assignBox.setPadding(new Insets(16));
        assignBox.setStyle("-fx-background-color: #2c2c2c;");

        Label assignLabel = new Label(I18n.get("bulk.assign.label"));
        assignLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        ComboBox<String> assignCombo = createSearchableUserComboBox();
        assignCombo.setPrefWidth(300);
        assignCombo.setPrefHeight(40);

        Button assignBtn = new Button(I18n.get("bulk.assign.button"));
        assignBtn.getStyleClass().add("btn-success");
        assignBtn.setPrefHeight(40);
        assignBtn.setOnAction(e -> {
            String assignee = assignCombo.getEditor().getText().trim();
            if (assignee.isEmpty()) {
                showError(I18n.get("bulk.assign.error.empty"));
                return;
            }
            List<Long> taskIds = selectedTasks.stream().map(com.wmsdipl.desktop.model.Task::id).toList();
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.bulkAssignTasks(taskIds, assignee);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((result, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error.getMessage());
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) result;
                    showInfo(I18n.format("bulk.assign.success", map.get("successCount")));
                    dialog.close();
                    loadTasks(taskTable, null);
                }
            }));
        });

        assignBox.getChildren().addAll(assignLabel, assignCombo, assignBtn);
        assignTab.setContent(assignBox);

        // Tab 2: Bulk Priority
        Tab priorityTab = new Tab(I18n.get("bulk.tab.priority"));
        priorityTab.setClosable(false);
        VBox priorityBox = new VBox(12);
        priorityBox.setPadding(new Insets(16));
        priorityBox.setStyle("-fx-background-color: #2c2c2c;");

        Label priorityLabel = new Label(I18n.get("bulk.priority.label"));
        priorityLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField priorityField = new TextField();
        priorityField.setPromptText(I18n.get("bulk.priority.prompt"));
        priorityField.setPrefHeight(40);

        Button priorityBtn = new Button(I18n.get("bulk.priority.button"));
        priorityBtn.getStyleClass().add("btn-primary");
        priorityBtn.setPrefHeight(40);
        priorityBtn.setOnAction(e -> {
            String priorityStr = priorityField.getText().trim();
            if (priorityStr.isEmpty()) {
                showError(I18n.get("bulk.priority.error.empty"));
                return;
            }
            Integer priority = Integer.parseInt(priorityStr);
            List<Long> taskIds = selectedTasks.stream().map(com.wmsdipl.desktop.model.Task::id).toList();
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.bulkSetPriority(taskIds, priority);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((result, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error.getMessage());
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) result;
                    showInfo(I18n.format("bulk.priority.success", map.get("successCount")));
                    dialog.close();
                    loadTasks(taskTable, null);
                }
            }));
        });

        priorityBox.getChildren().addAll(priorityLabel, priorityField, priorityBtn);
        priorityTab.setContent(priorityBox);

        // Tab 3: Cancel Tasks
        Tab cancelTab = new Tab(I18n.get("bulk.tab.cancel"));
        cancelTab.setClosable(false);
        VBox cancelBox = new VBox(12);
        cancelBox.setPadding(new Insets(16));
        cancelBox.setStyle("-fx-background-color: #2c2c2c;");

        Label cancelWarning = new Label(I18n.format("bulk.cancel.warning", selectedTasks.size()));
        cancelWarning.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button cancelTasksBtn = new Button(I18n.get("bulk.cancel.button"));
        cancelTasksBtn.getStyleClass().add("btn-danger");
        cancelTasksBtn.setPrefHeight(40);
        cancelTasksBtn.setOnAction(e -> {
            if (!showConfirm(I18n.get("bulk.cancel.confirm_title"), I18n.get("bulk.cancel.confirm_message"))) {
                return;
            }
            List<Long> taskIds = selectedTasks.stream().map(com.wmsdipl.desktop.model.Task::id).toList();
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.bulkCancelTasks(taskIds);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((result, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error.getMessage());
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) result;
                    showInfo(I18n.format("bulk.cancel.success", map.get("successCount")));
                    dialog.close();
                    loadTasks(taskTable, null);
                }
            }));
        });

        cancelBox.getChildren().addAll(cancelWarning, cancelTasksBtn);
        cancelTab.setContent(cancelBox);

        tabs.getTabs().addAll(assignTab, priorityTab, cancelTab);

        Button closeBtn = new Button(I18n.get("bulk.close_button"));
        closeBtn.setPrefHeight(40);
        closeBtn.setOnAction(e -> dialog.close());

        content.getChildren().addAll(header, infoLabel, tabs, closeBtn);

        Scene scene = new Scene(content, 500, 600);
        applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static class AutoCompleteComboBoxListener<T> implements javafx.event.EventHandler<javafx.scene.input.KeyEvent> {

        private ComboBox<T> comboBox;
        private javafx.collections.ObservableList<T> data;
        private boolean moveCaretToPos = false;
        private int caretPos;

        public AutoCompleteComboBoxListener(final ComboBox<T> comboBox) {
            this.comboBox = comboBox;
            this.data = comboBox.getItems();

            this.comboBox.setEditable(true);
            this.comboBox.setOnKeyPressed(new javafx.event.EventHandler<javafx.scene.input.KeyEvent>() {

                @Override
                public void handle(javafx.scene.input.KeyEvent t) {
                    comboBox.hide();
                }
            });
            this.comboBox.setOnKeyReleased(AutoCompleteComboBoxListener.this);
        }

        @Override
        public void handle(javafx.scene.input.KeyEvent event) {
            if (event.getCode() == javafx.scene.input.KeyCode.UP) {
                caretPos = -1;
                moveCaret(comboBox.getEditor().getText().length());
                return;
            } else if (event.getCode() == javafx.scene.input.KeyCode.DOWN) {
                if (!comboBox.isShowing()) {
                    comboBox.show();
                }
                caretPos = -1;
                moveCaret(comboBox.getEditor().getText().length());
                return;
            } else if (event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                moveCaretToPos = true;
                caretPos = comboBox.getEditor().getCaretPosition();
            } else if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                moveCaretToPos = true;
                caretPos = comboBox.getEditor().getCaretPosition();
            }

            if (event.getCode() == javafx.scene.input.KeyCode.RIGHT || event.getCode() == javafx.scene.input.KeyCode.LEFT
                    || event.isControlDown() || event.getCode() == javafx.scene.input.KeyCode.HOME
                    || event.getCode() == javafx.scene.input.KeyCode.END || event.getCode() == javafx.scene.input.KeyCode.TAB) {
                return;
            }

            javafx.collections.ObservableList<T> list = FXCollections.observableArrayList();
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).toString().toLowerCase().contains(
                        AutoCompleteComboBoxListener.this.comboBox.getEditor().getText().toLowerCase())) {
                    list.add(data.get(i));
                }
            }
            String t = comboBox.getEditor().getText();

            comboBox.setItems(list);
            comboBox.getEditor().setText(t);
            if (!moveCaretToPos) {
                caretPos = -1;
            }
            moveCaret(t.length());
            if (!list.isEmpty()) {
                comboBox.show();
            }
        }

        private void moveCaret(int textLength) {
            if (caretPos == -1) {
                comboBox.getEditor().positionCaret(textLength);
            } else {
                comboBox.getEditor().positionCaret(caretPos);
            }
            moveCaretToPos = false;
        }
    }

    private void showBulkCreatePalletsDialog() {
        Stage dialog = new Stage();
        dialog.setTitle(I18n.get("pallet.bulk.title"));
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #2c2c2c;");

        Label header = new Label(I18n.get("pallet.bulk.header"));
        header.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label prefixLabel = new Label(I18n.get("pallet.bulk.prefix_label"));
        prefixLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField prefixField = new TextField("PLT");
        prefixField.setPrefHeight(40);

        Label startLabel = new Label(I18n.get("pallet.bulk.start_label"));
        startLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField startField = new TextField("100");
        startField.setPrefHeight(40);

        Label countLabel = new Label(I18n.get("pallet.bulk.count_label"));
        countLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        TextField countField = new TextField("50");
        countField.setPrefHeight(40);

        Button createBtn = new Button(I18n.get("pallet.bulk.create_button"));
        createBtn.getStyleClass().add("btn-success");
        createBtn.setPrefHeight(40);
        createBtn.setOnAction(e -> {
            String prefix = prefixField.getText().trim();
            Integer start = Integer.parseInt(startField.getText().trim());
            Integer count = Integer.parseInt(countField.getText().trim());
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.bulkCreatePallets(prefix, start, count);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((result, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error.getMessage());
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) result;
                    showInfo(I18n.format("pallet.bulk.success", map.get("createdCount")));
                    dialog.close();
                }
            }));
        });

        content.getChildren().addAll(header, prefixLabel, prefixField, startLabel, startField, countLabel, countField, createBtn);
        Scene scene = new Scene(content, 400, 450);
        applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void applyStyles(Scene scene) {
        var cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
