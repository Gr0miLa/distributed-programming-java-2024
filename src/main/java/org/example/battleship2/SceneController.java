package org.example.battleship2;

import grpc.GameClient;

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

    public boolean isTurn = false;

    public int countShips = 0;
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
            gameClient = new GameClient(this, null);
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
            if (!isValidShipPlacement()) {
                showAlert("Ошибка", "Неверная расстановка кораблей. Проверьте расположение и количество.");
                return;
            }
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
            startButton.setDisable(false);

            countShips = 0;
            numberShipsHit = 0;
            isTurn = false;

            startButton.setText("Начать игру");

            String contentText = win ? "Поздравляем! Вы победили!" : "Поздравляем! Вы проиграли!";
            showAlert("Игра завершена", contentText);
        });
    }

    public void disableButton(boolean disable) {
        startButton.setDisable(disable);
    }

    public void setOpponentFieldDisabled(boolean disabled) {
        opponentField.setDisable(disabled);
    }

    private static GridPane createField(boolean isMyField) {
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
            gameClient.sendShipStage(x, y);
        } else {
            Rectangle cell = (Rectangle) opponentPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
            if (cell.getFill().equals(Color.valueOf("#ffffff"))) {
                gameClient.sendShotMessage(x, y);
            }
        }
    }

    public void removePlace(int x, int y) {
        Rectangle cell = (Rectangle) playerPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
        cell.setFill(Color.valueOf("#ffffff"));
        countShips--;
        System.out.println("countShips = " + countShips);

        if (countShips != SHIPS_COUNT) {
            startButton.setDisable(true);
        }
    }

    public void setPlace(int x, int y) {
        Rectangle cell = (Rectangle) playerPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
        cell.setFill(Color.valueOf("#3f72af"));
        countShips++;
        System.out.println("countShips = " + countShips);

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

    private static void initializeField(Pane pane, boolean isMyField) {
        GridPane field = createField(isMyField);
        pane.getChildren().clear();
        pane.getChildren().add(field);
        if (isMyField) {
            playerPaneGrid = field;
        } else {
            opponentPaneGrid = field;
        }
    }

    private boolean isValidShipPlacement() {
        int[][] grid = new int[FIELD_SIZE][FIELD_SIZE];
        for (int x = 0; x < FIELD_SIZE; x++) {
            for (int y = 0; y < FIELD_SIZE; y++) {
                Rectangle cell = (Rectangle) playerPaneGrid.getChildren().get((x + 1) * (FIELD_SIZE + 1) + (y + 1));
                if (cell.getFill().equals(Color.valueOf("#3f72af"))) {
                    grid[x][y] = 1;
                } else {
                    grid[x][y] = 0;
                }
            }
        }

        int[] shipCount = new int[5];

        boolean[][] visited = new boolean[FIELD_SIZE][FIELD_SIZE];

        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    int size = dfs(grid, visited, i, j);
                    if (size > 4) return false;
                    shipCount[size]++;
                }
            }
        }

        return shipCount[1] == 4 && shipCount[2] == 3 && shipCount[3] == 2 && shipCount[4] == 1;
    }

    private int dfs(int[][] grid, boolean[][] visited, int x, int y) {
        if (x < 0 || y < 0 || x >= FIELD_SIZE || y >= FIELD_SIZE) return 0;
        if (visited[x][y] || grid[x][y] == 0) return 0;

        visited[x][y] = true;
        int size = 1;

        boolean isHorizontal = checkDirection(grid, x, y, 0, 1) || checkDirection(grid, x, y, 0, -1);
        boolean isVertical = checkDirection(grid, x, y, 1, 0) || checkDirection(grid, x, y, -1, 0);

        if (isHorizontal && isVertical) {
            return 0;
        }

        if (isHorizontal) {
            size += dfs(grid, visited, x, y + 1);
            size += dfs(grid, visited, x, y - 1);
        }

        if (isVertical) {
            size += dfs(grid, visited, x + 1, y);
            size += dfs(grid, visited, x - 1, y);
        }

        int xy = dfs(grid, visited, x + 1, y + 1);
        int xxy_ = dfs(grid, visited, x + 1, y - 1);
        int x_yy = dfs(grid, visited, x - 1, y + 1);
        int x_y_ = dfs(grid, visited, x - 1, y - 1);

        if (xy + xxy_ + x_yy + x_y_ > 0) return 5;

        return size;
    }

    private boolean checkDirection(int[][] grid, int x, int y, int dx, int dy) {
        int newX = x + dx;
        int newY = y + dy;
        return newX >= 0 && newY >= 0 && newX < FIELD_SIZE && newY < FIELD_SIZE && grid[newX][newY] == 1;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void handleCloseRequest() {
        gameClient.closeConnection();
    }
}
