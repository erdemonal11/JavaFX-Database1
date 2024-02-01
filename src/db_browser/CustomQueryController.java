//@author Birkan Durgun Erdem Onal
package db_browser;

import java.io.IOException;
import java.net.URL;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class CustomQueryController implements Initializable {

    @FXML
    private Button btnConnect;
    @FXML
    private Button btnDisplayContent;
    @FXML
    private ListView<String> lwTables;
    @FXML
    private TextField tfQuery;
    @FXML
    private Button btnExecute;
    @FXML
    private Label lblTables;

    private TableView<ObservableList<String>> tableView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateTableList();
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
                btnConnect.setText("Connected");
                btnConnect.setStyle("-fx-background-color: green;");
                setVisibility();
            } else {
                DB_Manager.closeConnection();
                btnConnect.setText("Connect");
                btnConnect.setStyle("-fx-background-color: red;");
                setVisibility();
            }
        });

        btnDisplayContent.setOnAction(e -> {
            try {
                Stage currentStage = (Stage) btnDisplayContent.getScene().getWindow();
                currentStage.close();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setMinWidth(860);
                stage.setMinHeight(550);
                stage.show();

            } catch (IOException ex) {
                showAlert("Error", "An error occurred while loading Main.fxml", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        btnExecute.setOnAction(e -> {
            String query = tfQuery.getText().trim();
            if (!query.isEmpty()) {
                executeQuery(query);
            }
        });

    }

    private void setVisibility() {
        boolean isConnected = (DB_Manager.getConnection() != null);

        lblTables.setVisible(isConnected);
        btnDisplayContent.setVisible(isConnected);
        lwTables.setVisible(isConnected);
        tfQuery.setVisible(isConnected);
        btnExecute.setVisible(isConnected);

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
                e.printStackTrace();
            }
        }
    }

    private void executeQuery(String query) {
        try {

            if (query.trim().toLowerCase().startsWith("select")) {
                executeSelectQuery(query);
            } else if (query.trim().toLowerCase().startsWith("insert")
                    || query.trim().toLowerCase().startsWith("update")
                    || query.trim().toLowerCase().startsWith("delete")) {
                executeUpdateQuery(query);
            } else {
                showUnsupportedQueryAlert();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void executeSelectQuery(String query) throws SQLException {
        tableView = new TableView<>();
        tableView.setMaxHeight(200);
        Stage currentStage = (Stage) btnExecute.getScene().getWindow();
        Scene scene = currentStage.getScene();

        Parent root = scene.getRoot();

        if (root instanceof BorderPane) {
            ((BorderPane) root).setBottom(tableView);
        }
        try {
            ResultSet resultSet = DB_Manager.getConnection().createStatement().executeQuery(query);

            ResultSetMetaData metaData = resultSet.getMetaData();
            ObservableList<String> columnNames = FXCollections.observableArrayList();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            tableView.getColumns().clear();
            for (String columnName : columnNames) {
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
                final int columnIndex = columnNames.indexOf(columnName);
                column.setCellValueFactory(cellData -> {
                    ObservableList<String> row = cellData.getValue();
                    return new SimpleStringProperty(row.get(columnIndex));
                });
                tableView.getColumns().add(column);
            }

            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            while (resultSet.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row.add(resultSet.getString(i));
                }
                data.add(row);
            }

            tableView.setItems(data);
        } catch (SQLException ex) {
            showInvalidQueryAlert(ex.getMessage());
        }
    }

    private void executeUpdateQuery(String query) {
        Stage currentStage = (Stage) btnExecute.getScene().getWindow();
        Scene scene = currentStage.getScene();

        Parent root = scene.getRoot();

        if (root instanceof BorderPane) {
            ((BorderPane) root).setBottom(null);
        }

        try {
            int rowsAffected = DB_Manager.getConnection().createStatement().executeUpdate(query);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Query Result");
            alert.setHeaderText(null);
            alert.setContentText("Query executed successfully.\nRows affected: " + rowsAffected);
            alert.showAndWait();

            updateTableList();
        } catch (SQLException ex) {
            showInvalidQueryAlert(ex.getMessage());
        }
    }

    private void showAlert(String title, String headerText, String contentText, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    private void showUnsupportedQueryAlert() {
        showAlert("Unsupported Query", null, "Only SELECT, INSERT, UPDATE, and DELETE queries are supported.", Alert.AlertType.WARNING);
    }

    private void showInvalidQueryAlert(String errorMessage) {
        showAlert("Invalid Query", null, "Invalid table name or query syntax.\n" + errorMessage, Alert.AlertType.WARNING);
    }
}
