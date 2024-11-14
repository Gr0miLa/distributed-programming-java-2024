package org.example.battleship;

public class ServerApp {
    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.startServer();
    }
}
