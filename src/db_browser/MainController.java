//@author Birkan Durgun Erdem Onal
package db_browser;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainController implements Initializable {

    @FXML
    private Button btnConnect;
    @FXML
    private Button btnAddNew;
    @FXML
    private Button btnUpdateSelected;
    @FXML
    private Button btnDeleteSelected;
    @FXML
    private Button btnCustomQuery;
    @FXML
    private ListView<String> lwTables;
    @FXML
    private TableView<ObservableList<String>> twData;
    @FXML
    private Label lblTables;

    private ResultSetMetaData metaData;

    private Button btnCreatedAdd;

    private Button btnCreatedUpdate;

    private ResultSet resultSet;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateTableList();
        updateTableView();
        if (DB_Manager.getConnection() != null) {
            btnConnect.setText("Connected");
            btnConnect.setStyle("-fx-background-color: green;");
        } else {
            btnConnect.setText("Connect");
            btnConnect.setStyle("-fx-background-color: red;");
        }

        btnConnect.setOnAction(e -> {
            if (DB_Manager.getConnection() == null) {
                DB_Manager.connectToDatabase();
                if (DB_Manager.isConnected()) {
                    btnConnect.setText("Connected");
                    btnConnect.setStyle("-fx-background-color: green;");
                    updateTableList();
                }
            } else {
                lwTables.getItems().clear();
                twData.getColumns().clear();
                btnConnect.setText("Connect");
                btnConnect.setStyle("-fx-background-color: red;");
                DB_Manager.closeConnection();
            }
            updateTableView();
            setVisibility();
        });

        lwTables.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Stage currentStage = (Stage) lwTables.getScene().getWindow();
            Scene scene = currentStage.getScene();
            Parent root = scene.getRoot();
            if (root instanceof BorderPane) {
                BorderPane borderPane = (BorderPane) root;
                borderPane.setBottom(null);
            }
            updateTableView();
        });

        setVisibility();

        btnAddNew.setOnAction(e -> {
            Stage currentStage = (Stage) btnAddNew.getScene().getWindow();
            Scene scene = currentStage.getScene();

            Parent root = scene.getRoot();

            if (root instanceof BorderPane) {
                BorderPane borderPane = (BorderPane) root;
                HBox hbox = new HBox();
                borderPane.setBottom(hbox);
                ArrayList<TextField> textFields = new ArrayList<>();
                try {
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        TextField textField = new TextField();
                        textField.setMaxWidth(150);
                        HBox.setMargin(textField, new Insets(10, 0, 0, 10));
                        hbox.getChildren().add(textField);
                        textFields.add(textField);
                    }
                    btnCreatedAdd = new Button();
                    btnCreatedAdd.setText("Add");
                    btnCreatedAdd.setMaxWidth(150);
                    HBox.setMargin(btnCreatedAdd, new Insets(10, 0, 0, 10));
                    hbox.getChildren().add(btnCreatedAdd);
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }

                btnCreatedAdd.setOnAction(a -> {
                    try {
                        resultSet.moveToInsertRow();

                        for (int i = 0; i < textFields.size(); i++) {
                            String columnName = metaData.getColumnName(i + 1);
                            String columnValue = textFields.get(i).getText();

                            if (columnValue.isEmpty()) {
                                resultSet.updateNull(columnName);
                            } else {
                                resultSet.updateString(columnName, columnValue);
                            }
                        }

                        resultSet.insertRow();
                        resultSet.moveToCurrentRow();

                        updateTableView();
                        if (root instanceof BorderPane) {
                            borderPane.setBottom(null);
                        }

                    } catch (SQLException ex) {
                        showAlert("Error", "Error adding new record", ex.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }
        });

        btnUpdateSelected.setOnAction(e -> {
            Stage currentStage = (Stage) btnAddNew.getScene().getWindow();
            Scene scene = currentStage.getScene();

            Parent root = scene.getRoot();

            if (root instanceof BorderPane) {
                BorderPane borderPane = (BorderPane) root;
                HBox hbox = new HBox();
                borderPane.setBottom(hbox);

                try {
                    ObservableList<String> selectedRow = twData.getSelectionModel().getSelectedItem();

                    if (selectedRow != null) {
                        ArrayList<TextField> textFields = new ArrayList<>();

                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            TextField textField = new TextField();
                            textField.setMaxWidth(150);
                            HBox.setMargin(textField, new Insets(10, 0, 0, 10));
                            textField.setText(selectedRow.get(i - 1));
                            hbox.getChildren().add(textField);
                            textFields.add(textField);
                        }

                        btnCreatedUpdate = new Button();
                        btnCreatedUpdate.setText("Update");
                        btnCreatedUpdate.setMaxWidth(150);
                        HBox.setMargin(btnCreatedUpdate, new Insets(10, 0, 0, 10));
                        hbox.getChildren().add(btnCreatedUpdate);

                        btnCreatedUpdate.setOnAction(a -> {
                            try {
                                resultSet.absolute(twData.getSelectionModel().getSelectedIndex() + 1);

                                for (int i = 0; i < textFields.size(); i++) {
                                    String columnName = metaData.getColumnName(i + 1);
                                    String columnValue = textFields.get(i).getText();

                                    if (columnValue.isEmpty()) {
                                        resultSet.updateNull(columnName);
                                    } else {
                                        resultSet.updateString(columnName, columnValue);
                                    }
                                }

                                resultSet.updateRow();
                                updateTableView();
                                if (root instanceof BorderPane) {
                                    borderPane.setBottom(null);
                                }

                            } catch (SQLException ex) {
                                showAlert("Error", "Error updating selected record", ex.getMessage(), Alert.AlertType.ERROR);
                            }
                        });
                    }
                } catch (SQLException ex) {
                    showAlert("Error", "Error accessing selected record", ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        btnDeleteSelected.setOnAction(e -> {
            int selectedIndex = twData.getSelectionModel().getSelectedIndex();

            if (selectedIndex >= 0) {
                try {
                    DatabaseMetaData metaData = DB_Manager.getConnection().getMetaData();
                    String selectedTableName = lwTables.getSelectionModel().getSelectedItem();

                    if (selectedTableName != null && !selectedTableName.isEmpty()) {
                        ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, selectedTableName);
                        primaryKeys.next();
                        String primaryKeyColumn = primaryKeys.getString("COLUMN_NAME");
                        primaryKeys.close();

                        Statement statement = DB_Manager.getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        ResultSet resultSet = statement.executeQuery("SELECT * FROM " + selectedTableName);

                        resultSet.absolute(selectedIndex + 1);
                        resultSet.deleteRow();

                        twData.getItems().remove(selectedIndex);
                    }
                } catch (SQLException ex) {
                    showAlert("Error", "Error deleting selected record", ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        btnCustomQuery.setOnAction(e -> {
            try {
                Stage mainStage = (Stage) btnCustomQuery.getScene().getWindow();
                mainStage.close();

                Parent root = FXMLLoader.load(getClass().getResource("CustomQuery.fxml"));
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setMinHeight(600);
                stage.setMinWidth(980);
                stage.show();
            } catch (IOException ex) {
                showAlert("Error", "Error opening custom query window", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void updateTableList() {
        if (DB_Manager.getConnection() != null) {
            try {
                DatabaseMetaData metaData = DB_Manager.getConnection().getMetaData();
                ResultSet tables = metaData.getTables(null, null, null, new String[]{"TABLE"});

                ObservableList<String> tableList = FXCollections.observableArrayList();
                while (tables.next()) {
                    tableList.add(tables.getString("TABLE_NAME"));
                }

                lwTables.setItems(tableList);

            } catch (SQLException e) {
                showAlert("Error", "Error updating table list", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void updateTableView() {
        String selectedTableName = lwTables.getSelectionModel().getSelectedItem();

        twData.getColumns().clear();
        twData.getItems().clear();

        if (selectedTableName != null && !selectedTableName.isEmpty()) {
            try {
                String query = "SELECT * FROM " + selectedTableName;
                Statement statement = DB_Manager.getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                resultSet = statement.executeQuery(query);

                metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    final int j = i - 1;
                    TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
                    column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(j)));
                    twData.getColumns().add(column);
                }

                while (resultSet.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (int i = 1; i <= columnCount; i++) {
                        String value = resultSet.getString(i);

                        if (resultSet.wasNull()) {
                            value = "";
                        }

                        row.add(value);
                    }
                    twData.getItems().add(row);
                }

            } catch (SQLException e) {
                showAlert("Error", "Error updating table view", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void setVisibility() {
        boolean isConnected = (DB_Manager.getConnection() != null);

        lblTables.setVisible(isConnected);
        lwTables.setVisible(isConnected);
        twData.setVisible(isConnected);
        btnAddNew.setVisible(isConnected);
        btnUpdateSelected.setVisible(isConnected);
        btnDeleteSelected.setVisible(isConnected);
        btnCustomQuery.setVisible(isConnected);
    }

    private void showAlert(String title, String headerText, String contentText, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

}
