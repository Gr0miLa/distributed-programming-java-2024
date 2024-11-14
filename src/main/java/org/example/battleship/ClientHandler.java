package org.example.battleship;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Player currentPlayer;
    private final Player opponentPlayer;
    private ClientHandler opponentHandler;

    public static final int SHIPS_COUNT = 20;

    public ClientHandler(Socket socket, Player currentPlayer, Player opponentPlayer, ClientHandler opponentHandler) {
        this.clientSocket = socket;
        this.currentPlayer = currentPlayer;
        this.opponentPlayer = opponentPlayer;
        this.opponentHandler = opponentHandler;
    }

    public void setOpponentHandler(ClientHandler opponentHandler) {
        this.opponentHandler = opponentHandler;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    @Override
    public void run() {
        DataInputStream in = null;
        DataOutputStream out = null;
        DataOutputStream otherOut = null;
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            otherOut = new DataOutputStream(opponentHandler.getClientSocket().getOutputStream());


            String message;
            while ((message = in.readUTF()) != null) {
                System.out.println("Received from " + clientSocket.getPort() + ": " + message);
                handleMessage(message, out, otherOut);
            }

        } catch (IOException e) {
            System.out.println("Клиент отключился");
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.out.println("Сервер выключен");
            }
        }
    }

    private void handleMessage(String message, DataOutputStream out, DataOutputStream otherOut) throws IOException {
        String[] parts = message.split(" ");
        String command = parts[0];

        switch (command) {
            case "CONNECT":
                handleConnect(out, otherOut);
                break;
            case "SHOT":
                handleShot(parts, out, otherOut);
                break;
            case "STAGE":
                handleStage(parts, out);
                break;
            case "READY":
                handleReady(out);
                break;
            case "END":
                handleEndGame(parts, out, otherOut);
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

    private void handleEndGame(String[] parts, DataOutputStream out, DataOutputStream otherOut) {
        boolean win = Boolean.parseBoolean(parts[1]);

        if (out != null) {
            currentPlayer.setTurn(true);
            currentPlayer.setReady(false);
            currentPlayer.clearData();
            sendMessage("END " + win, out);
        }
        if (otherOut != null) {
            opponentPlayer.setTurn(false);
            opponentPlayer.setReady(false);
            opponentPlayer.clearData();
            opponentHandler.sendMessage("END " + !win, otherOut);
        }

    }

    private void handleConnect(DataOutputStream out, DataOutputStream otherOut) {
        GameServer.countPlayers++;

        if (GameServer.countPlayers == 2) {
            sendMessage("CONNECTED", out);
            opponentHandler.sendMessage("CONNECTED", otherOut);
            System.out.println(clientSocket.getPort() + " connected");
        }
    }

    private void handleShot(String[] parts, DataOutputStream out, DataOutputStream otherOut) {
        if (parts.length != 3) {
            System.out.println("Invalid SHOT command format");
            return;
        }

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);

        boolean hit = opponentPlayer.isShip(x, y);
        if (hit) {
            opponentPlayer.decrementShips();
            opponentPlayer.hitShip(x, y);
            sendMessage("HIT " + x + " " + y + " " + true, out);
            opponentHandler.sendMessage("HIT " + x + " " + y + " " + false, otherOut);
        } else {
            opponentPlayer.missShip(x, y);
            opponentPlayer.setTurn(true);
            currentPlayer.setTurn(false);
            sendMessage("MISS " + x + " " + y + " " + true, out);
            opponentHandler.sendMessage("MISS " + x + " " + y + " " + false, otherOut);
            System.out.println("Смена хода");
        }

        System.out.println("Shot at (" + x + ", " + y + ") - " + (hit ? "HIT" : "MISS"));
    }

    private void handleStage(String[] parts, DataOutputStream out) {
        if (parts.length != 3) {
            System.out.println("Invalid STAGE command format");
            return;
        }

        System.out.println("Count ships: " + currentPlayer.getCountShips());

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);

        if (currentPlayer.isShip(x, y)) {
            System.out.println("Remove at (" + x + ", " + y + ") - ");
            sendMessage("REMOVE " + x + " " + y, out);
            currentPlayer.removeShip(x, y);
        } else {
            if (currentPlayer.getCountShips() != SHIPS_COUNT) {
                System.out.println("Place at (" + x + ", " + y + ") - ");
                sendMessage("PLACE " + x + " " + y, out);
                currentPlayer.placeShip(x, y);
            }
        }
    }

    private void handleReady(DataOutputStream out) {
        currentPlayer.setReady(true);
        if (currentPlayer.isReady() && opponentPlayer.isReady()) {
            if (currentPlayer.isTurn()) {
                sendMessage("START", out);
            } else {
                opponentHandler.sendMessage("START", out);
            }
            System.out.println("Start by " + clientSocket.getPort());
        }
    }

    public void sendMessage(String message, DataOutputStream out) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
