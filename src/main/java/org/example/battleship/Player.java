package org.example.battleship;

public class Player {
    public static final int FIELD_SIZE = 10;

    public int countShips = 0;

    private final int[][] playerGrid = new int[FIELD_SIZE][FIELD_SIZE];
    private boolean isTurn;
    private boolean isReady;

    public Player(boolean isTurn, boolean isReady) {
        this.isTurn = isTurn;
        this.isReady = isReady;
    }

    public void decrementShips() {
        countShips--;
    }

    public void hitShip(int x, int y) {
        playerGrid[x][y] = 1;
    }

    public void missShip(int x, int y) {
        playerGrid[x][y] = -1;
    }

    public void placeShip(int x, int y) {
        playerGrid[x][y] = 1;
        countShips++;
    }

    public void removeShip(int x, int y) {
        playerGrid[x][y] = 0;
        countShips--;
    }

    public boolean isShip(int x, int y) {
        return playerGrid[x][y] == 1;
    }

    public boolean isTurn() {
        return isTurn;
    }

    public void setTurn(boolean turn) {
        isTurn = turn;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public int getCountShips() {
        return countShips;
    }

    public void clearData() {
        countShips = 0;

        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                playerGrid[i][j] = 0;
            }
        }
    }
}
