package grpc;

import grpc.BattleShipService.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.example.battleship2.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

public class GameServer extends BattleShipServiceGrpc.BattleShipServiceImplBase {
    private final Map<String, Player> players = new HashMap<>();
    private final Map<String, StreamObserver<ConnectResponse>> observers = new HashMap<>();
    public static final int SHIPS_COUNT = 20;

    @Override
    public void connectService(ConnectRequest request,
                               StreamObserver<ConnectResponse> responseObserver) {

        String clientId = UUID.randomUUID().toString();

        System.out.println("Клиент подключился (clientId = " + clientId + ")");

        boolean isTurn = players.isEmpty();
        Player newPlayer = new Player(isTurn, false);
        players.put(clientId, newPlayer);
        observers.put(clientId, responseObserver);
        System.out.println("responseObserver = " + responseObserver);

        System.out.println("Новый игрок добавлен. Всего игроков: " + players.size());

        ConnectResponse connectResponse = ConnectResponse.newBuilder()
                .setClientId(clientId)
                .setMessage("connect")
                .setIsTurn(newPlayer.isTurn())
                .build();
        responseObserver.onNext(connectResponse);

        if (players.size() == 2) {
            System.out.println("Начало игры. Отправка сообщения обоим игрокам");
            startGameForAllPlayers();
        }
        System.out.println("Окончание отправки сообщения обоим игрокам");
    }

    private void startGameForAllPlayers() {
        System.out.println("Создали ответ сервера о начале игры. Начинаем отправку клиентам");
        for (StreamObserver<ConnectResponse> observer : observers.values()) {
            ConnectResponse connectResponse = ConnectResponse.newBuilder()
                    .setMessage("start")
                    .build();
            observer.onNext(connectResponse);
        }
    }

    public void readyService(grpc.BattleShipService.ReadyRequest request,
                             io.grpc.stub.StreamObserver<grpc.BattleShipService.ReadyResponse> responseObserver) {
        String clientId = request.getClientId();
        if (players.containsKey(clientId)) {
            Player player = players.get(clientId);
            ReadyResponse readyResponse = ReadyResponse.newBuilder()
                    .setIsTurn(player.isTurn())
                    .build();
            responseObserver.onNext(readyResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void stageService(StageRequest request,
                             StreamObserver<StageResponse> responseObserver) {

        String clientId = request.getClientId();
        int x = request.getX();
        int y = request.getY();

        StageResponse stageResponse = StageResponse.newBuilder().setPlaced(-1).build();

        if (players.containsKey(clientId)) {
            Player player = players.get(clientId);
            if (player.isShip(x, y)) {
                System.out.println("Корабль удалён с клетки (" + x + ", " + y + ")");
                stageResponse = StageResponse.newBuilder().setPlaced(0).build();
                player.removeShip(x, y);
            } else {
                if (player.getCountShips() != SHIPS_COUNT) {
                    System.out.println("Корабль поставлен на клетку (" + x + ", " + y + ")");
                    stageResponse = StageResponse.newBuilder().setPlaced(1).build();
                    player.placeShip(x, y);
                }
            }
            System.out.println("Отмечена клетка (" + x + ", " + y + ") от клиента с ID: " + clientId);
        } else {
            System.out.println("Неизвестный клиент с ID: " + clientId);
        }

        responseObserver.onNext(stageResponse);
        responseObserver.onCompleted();
    }

    public void shotService(ShotRequest request,
                            StreamObserver<ShotResponse> responseObserver) {
        String clientId = request.getClientId();
        String shotMessage = "";
        int x = request.getX();
        int y = request.getY();
        System.out.println("Получено сообщение о выстреле от : " + clientId);
        System.out.println("Клетка (" + x + ", " + y + ")");
        
        if (players.containsKey(clientId)) {
            System.out.println("Информация об игроке есть на сервере");
            Player currentPlayer = null;
            Player opponentPlayer = null;
            for (Map.Entry<String, Player> entry : players.entrySet()) {
                if (entry.getKey().equals(clientId)) {
                    currentPlayer = entry.getValue();
                } else {
                    opponentPlayer = entry.getValue();
                }
            }
            assert opponentPlayer != null;
            boolean hit = opponentPlayer.isShip(x, y);
            shotMessage = hit ? "hit" : "miss";
            if (hit) {
                opponentPlayer.decrementShips();
                opponentPlayer.hitShip(x, y);
                System.out.println("Попадание (" + x + ", " + y + ")");
            } else {
                opponentPlayer.missShip(x, y);
                opponentPlayer.setTurn(true);
                currentPlayer.setTurn(false);
                System.out.println("Промах (" + x + ", " + y + ")");
                System.out.println("Смена хода");
            }
            System.out.println("Был совершен выстрел (" + x + ", " + y + ") " +
                    "от клиента с ID: " + clientId);
        } else {
            System.out.println("Неизвестный клиент с ID: " + clientId);
        }

        ShotResponse shotResponse = ShotResponse.newBuilder()
                .setX(x)
                .setY(y)
                .build();

        System.out.println("Создали ответ сервера о выстреле. Начинаем отправку клиентам");
        for (Map.Entry<String, StreamObserver<ConnectResponse>> entry : observers.entrySet()) {
            String observerClientId = entry.getKey();
            StreamObserver<ConnectResponse> observer = entry.getValue();
            System.out.println("Отправка " + observerClientId);

            shotResponse = ShotResponse.newBuilder()
                    .setIsMyField(observerClientId.equals(clientId))
                    .setX(x)
                    .setY(y)
                    .build();
            ConnectResponse connectResponse = ConnectResponse.newBuilder()
                    .setMessage(shotMessage)
                    .setShot(shotResponse)
                    .build();
            observer.onNext(connectResponse);
        }
        responseObserver.onNext(shotResponse);
        responseObserver.onCompleted();
    }

    public void endGameService(grpc.BattleShipService.EndGameRequest request,
                               io.grpc.stub.StreamObserver<grpc.BattleShipService.EndGameResponse> responseObserver) {
        String clientId = request.getClientId();

        EndGameResponse endGameResponse = EndGameResponse.newBuilder().build();
        if (players.containsKey(clientId)) {
            for (Map.Entry<String, StreamObserver<ConnectResponse>> entry : observers.entrySet()) {
                String observerClientId = entry.getKey();
                StreamObserver<ConnectResponse> observer = entry.getValue();
                System.out.println("Отправка " + observerClientId);
                Player player = players.get(observerClientId);
                player.clearData();

                boolean win = observerClientId.equals(request.getClientId()) == request.getWin();

                endGameResponse = EndGameResponse.newBuilder()
                        .setWin(win)
                        .build();
                ConnectResponse connectResponse = ConnectResponse.newBuilder()
                        .setMessage("end")
                        .setEnd(endGameResponse)
                        .build();
                observer.onNext(connectResponse);
            }
            System.out.println("Игра окончена");
        }
        responseObserver.onNext(endGameResponse);
        responseObserver.onCompleted();
    }

    public void closeStreamService(grpc.BattleShipService.CloseStreamRequest request,
                                   io.grpc.stub.StreamObserver<grpc.BattleShipService.CloseStreamResponse> responseObserver) {
        String clientId = request.getClientId();

        CloseStreamResponse closeStreamResponse = CloseStreamResponse.newBuilder().build();
        if (players.containsKey(clientId)) {
            Iterator<Map.Entry<String, StreamObserver<ConnectResponse>>> iterator = observers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, StreamObserver<ConnectResponse>> entry = iterator.next();
                String observerClientId = entry.getKey();
                StreamObserver<ConnectResponse> observer = entry.getValue();

                if (observerClientId.equals(clientId)) {
                    players.remove(clientId);
                    iterator.remove();
                    observer.onCompleted();

                    System.out.println("Игрок отключился: " + observerClientId);
                } else {
                    ConnectResponse connectResponse = ConnectResponse.newBuilder()
                            .setMessage("reset")
                            .build();
                    observer.onNext(connectResponse);
                }
            }
        }

        responseObserver.onNext(closeStreamResponse);
        responseObserver.onCompleted();
    }

}
