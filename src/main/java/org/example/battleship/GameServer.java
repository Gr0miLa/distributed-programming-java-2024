package org.example.battleship;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    private static final int PORT = 5555;
    private Socket clientSocket1, clientSocket2;

    private static final Player player1 = new Player(true, false);
    private static final Player player2 = new Player(false, false);

    public static int countPlayers = 0;

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен и ожидает подключений...");

            clientSocket1 = serverSocket.accept();
            System.out.println("Первый игрок подключен.");

            clientSocket2 = serverSocket.accept();
            System.out.println("Второй игрок подключен.");

            ClientHandler handler1 = new ClientHandler(clientSocket1, player1, player2, null);
            ClientHandler handler2 = new ClientHandler(clientSocket2, player2, player1, handler1);

            handler1.setOpponentHandler(handler2);

            new Thread(handler1).start();
            new Thread(handler2).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

