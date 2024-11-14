package org.example.battleship;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Objects;

public class SceneController {
    @FXML
    private Pane playerField;
    @FXML
    private Pane opponentField;
    @FXML
    private Button startButton;

    private static GameClient gameClient;

    private static GridPane playerPaneGrid;
    private static GridPane opponentPaneGrid;

    public static boolean isTurn = false;

    public static int countShips = 0;
    public static int numberShipsHit = 0;

    public static final int FIELD_SIZE = 10;
    public static final int SHIPS_COUNT = 20;
    public static final int CELL_SIZE = 39;

    @FXML
    public void initialize() {
        startButton.setOnMouseExited(event -> startButton.setStyle("-fx-background-color: #f9f7f7; -fx-font-size: 14px;"));
        startButton.setOnMouseEntered(event -> startButton.setStyle("-fx-background-color: #dbe2ef; -fx-font-size: 14px;"));
        startButton.setOnMousePressed(event -> startButton.setStyle("-fx-background-color: #dbe2ef; -fx-font-size: 15px;"));
        startButton.setOnMouseReleased(event -> startButton.setStyle("-fx-background-color: #dbe2ef; -fx-font-size: 14px;"));
        startButton.setDisable(true);

        if (gameClient == null) {
            gameClient = new GameClient(this);
            gameClient.connectToServer();
        }
    }

    @FXML
    public void onStartButtonClicked() {
        if (Objects.equals(startButton.getText(), "Начать игру")) {
            initializeField(playerField, true);
            initializeField(opponentField, false);
            playerField.setDisable(false);
            opponentField.setDisable(true);
            startButton.setText("Готов");
            startButton.setDisable(true);
        } else if (Objects.equals(startButton.getText(), "Готов")) {
            playerField.setDisable(true);
            gameClient.sendReadyMessage();
            startButton.setText("Сдаться");
        } else if (Objects.equals(startButton.getText(), "Сдаться")) {
            gameClient.sendEndGame(false);
        }
    }

    public void endGame(boolean win) {
        Platform.runLater(() -> {
            playerField.setDisable(true);
            opponentField.setDisable(true);

            countShips = 0;
            numberShipsHit = 0;
            isTurn = false;

            startButton.setText("Начать игру");

            String contentText = win ? "Поздравляем! Вы победили!" : "Поздравляем! Вы проиграли!";
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Игра завершена");
            alert.setHeaderText(null);
            alert.setContentText(contentText);
            alert.showAndWait();
        });
    }

    public void disableButton(boolean disable) {
        startButton.setDisable(disable);
    }

    public void setOpponentFieldDisabled(boolean disabled) {
        opponentField.setDisable(disabled);
    }

    public static GridPane createField(boolean isMyField) {
        GridPane grid = new GridPane();
        grid.setStyle("-fx-border-width: 0;");
        grid.setPrefSize(440, 440);

        String[] letters = {"А", "Б", "В", "Г", "Д", "Е", "Ж", "З", "И", "К"};
        int gridSize = FIELD_SIZE + 1;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                if (row == 0 || col == 0) {
                    cell.setFill(Color.valueOf("#dbe2ef"));
                } else {
                    cell.setFill(Color.valueOf("#ffffff"));
                }
                cell.setStroke(Color.valueOf("#dbe2ef"));
                cell.setStrokeWidth(1);

                final int y = col - 1;
                final int x = row - 1;
                if (y >= 0 && x >= 0) {
                    cell.setOnMouseClicked(event -> onCellClicked(y, x, isMyField));
                }

                grid.add(cell, col, row);
            }
        }

        for (int col = 1; col < gridSize; col++) {
            StackPane stackPane = new StackPane();
            Text letter = new Text(letters[col - 1]);
            letter.setFont(Font.font(20));
            stackPane.getChildren().add(letter);
            grid.add(stackPane, col, 0);
        }

        for (int row = 1; row < gridSize; row++) {
            StackPane stackPane = new StackPane();
            Text number = new Text(String.valueOf(row));
            number.setFont(Font.font(20));
            stackPane.getChildren().add(number);
            grid.add(stackPane, 0, row);
        }

        return grid;
    }


    private static void onCellClicked(int y, int x, boolean isMyField) {
        if (isMyField) {
            gameClient.sendShipStageMessage(x, y);
        } else {
            gameClient.sendShotMessage(x, y);
        }
    }

    public void removePlace(int x, int y) {
        Rectangle cell = (Rectangle) playerPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
        cell.setFill(Color.valueOf("#ffffff"));
        countShips--;

        if (countShips != SHIPS_COUNT) {
            startButton.setDisable(true);
        }
    }

    public void setPlace(int x, int y) {
        Rectangle cell = (Rectangle) playerPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
        cell.setFill(Color.valueOf("#3f72af"));
        countShips++;

        if (countShips == SHIPS_COUNT) {
            startButton.setDisable(false);
        }
    }

    public void hitShip(int x, int y, boolean isMyField) {
        if (isMyField) {
            Rectangle cell = (Rectangle) opponentPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
            cell.setFill(Color.valueOf("#e23e57"));
            numberShipsHit++;

            if (numberShipsHit == SHIPS_COUNT) {
                gameClient.sendEndGame(true);
            }
        } else {
            Rectangle cell = (Rectangle) playerPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
            cell.setFill(Color.valueOf("#e23e57"));
        }
    }

    public void missShip(int x, int y, boolean isMyField) {
        if (isMyField) {
            Rectangle cell = (Rectangle) opponentPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
            cell.setFill(Color.valueOf("#c9d6df"));
            opponentField.setDisable(true);
        } else {
            Rectangle cell = (Rectangle) playerPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
            cell.setFill(Color.valueOf("#c9d6df"));
            opponentField.setDisable(false);
        }
        isTurn = !isTurn;
    }

    public void setTurn(boolean turn) {
        isTurn = turn;
    }

    public static void initializeField(Pane pane, boolean isMyField) {
        GridPane field = createField(isMyField);
        pane.getChildren().clear();
        pane.getChildren().add(field);
        if (isMyField) {
            playerPaneGrid = field;
        } else {
            opponentPaneGrid = field;
        }
    }
}
