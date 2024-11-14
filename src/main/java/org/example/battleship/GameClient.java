package org.example.battleship;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class GameClient {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private SceneController sceneController;
    private boolean connected = true;

    public GameClient(SceneController sceneController) {
        this.sceneController = sceneController;
    }

    public void connectToServer() {
        try {
            System.out.println("Подключение к серверу...");
            socket = new Socket("localhost", 5555);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            System.out.println("Соединение с сервером установлено!");

            sendConnectMessage();

            new Thread(() -> {
                try {
                    while (connected) {
                        String message = input.readUTF();
                        handleMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        try {
            System.out.println("Command: " + message);
            output.writeUTF(message);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendConnectMessage() {
        sendMessage("CONNECT");
    }

    public void sendShotMessage(int x, int y) {
        sendMessage("SHOT " + x + " " + y);
    }

    public void sendShipStageMessage(int x, int y) {
        sendMessage("STAGE " + x + " " + y);
    }

    public void sendReadyMessage() {
        sendMessage("READY");
    }

    public void sendEndGame(boolean win) {
        sendMessage("END " + win);
    }

    private void handleMessage(String message) {
        String[] parts = message.split(" ");
        String command = parts[0];
        int x;
        int y;
        boolean isMyField;
        switch (command) {
            case "CONNECTED":
                System.out.println("Подключение установлено");
                sceneController.disableButton(false);
                break;
            case "PLACE":
                x = Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
                sceneController.setPlace(x, y);
                System.out.println("Корабль установлен");
                break;
            case "REMOVE":
                System.out.println("Корабль удалён");
                x = Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
                sceneController.removePlace(x, y);
                break;
            case "START":
                System.out.println("Игра началась");
                sceneController.setTurn(true);
                sceneController.setOpponentFieldDisabled(false);
                break;
            case "HIT":
                System.out.println("Попадание!");
                x = Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
                isMyField = Boolean.parseBoolean(parts[3]);
                sceneController.hitShip(x, y, isMyField);
                break;
            case "MISS":
                System.out.println("Промах!");
                x = Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
                isMyField = Boolean.parseBoolean(parts[3]);
                sceneController.missShip(x, y, isMyField);
                break;
            case "END":
                System.out.println("Игра окончена");
                boolean win = Boolean.parseBoolean(parts[1]);
                sceneController.endGame(win);
                break;
            default:
                System.out.println("Неизвестное сообщение: " + message);
                break;
        }
    }

    public void closeConnection() {
        connected = false;
        try {
            input.close();
            output.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
