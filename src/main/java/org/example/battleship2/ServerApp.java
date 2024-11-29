package org.example.battleship2;

import grpc.GameServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerApp {
    public static void main(String[] args) throws Exception {
        grpc.GameServer BattleShipServer = new GameServer();
        Server server = ServerBuilder.forPort(8080).addService(BattleShipServer).build();

        System.out.println("Сервер запущен на порту 8080");

        server.start();
        server.awaitTermination();
    }
}
