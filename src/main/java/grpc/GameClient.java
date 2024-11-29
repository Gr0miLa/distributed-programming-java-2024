package grpc;

import io.grpc.stub.StreamObserver;
import org.example.battleship2.SceneController;
import grpc.BattleShipService.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GameClient {
    private final BattleShipServiceGrpc.BattleShipServiceBlockingStub blockingStub;
    private final BattleShipServiceGrpc.BattleShipServiceStub asyncStub;
    private final SceneController sceneController;
    private String ClientId;

    public String getClientId() {
        return ClientId;
    }

    public GameClient(SceneController sceneController, String clientId) {
        this.sceneController = sceneController;
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();
        this.blockingStub = BattleShipServiceGrpc.newBlockingStub(channel);
        this.asyncStub = BattleShipServiceGrpc.newStub(channel);
        this.ClientId = clientId;
    }

    public void connectToServer() {
        try {
            ConnectRequest connectRequest = ConnectRequest.newBuilder().build();
            System.out.println("Подключение к серверу...");

            asyncStub.connectService(connectRequest, new StreamObserver<>() {
                @Override
                public void onNext(ConnectResponse connectResponse) {
                    if (connectResponse.getMessage().equals("connect")) {
                        System.out.println("Соединение с сервером установлено!");

                        ClientId = connectResponse.getClientId();
                        sceneController.setTurn(connectResponse.getIsTurn());
                    } else if (connectResponse.getMessage().equals("start")) {
                        System.out.println("Начало игры");

                        sceneController.disableButton(false);
                    } else if (connectResponse.getMessage().equals("hit")) {
                        ShotResponse shotResponse = connectResponse.getShot();

                        System.out.println("Ответ о выстреле получен - попадание");
                        System.out.println("Ответ получен: (" +
                                shotResponse.getX() + ", " + shotResponse.getY() + ")");

                        sceneController.hitShip(shotResponse.getX(), shotResponse.getY(),
                                shotResponse.getIsMyField());
                    } else if (connectResponse.getMessage().equals("miss")) {
                        ShotResponse shotResponse = connectResponse.getShot();

                        System.out.println("Ответ о выстреле получен - промах");
                        System.out.println("1 Ответ получен: (" +
                                shotResponse.getX() + ", " + shotResponse.getY() + ")");

                        sceneController.missShip(shotResponse.getX(), shotResponse.getY(),
                                shotResponse.getIsMyField());
                    } else if (connectResponse.getMessage().equals("end")) {
                        EndGameResponse endGameResponse = connectResponse.getEnd();

                        System.out.println("Ответ об окончании игры получен: " + endGameResponse.getWin());

                        sceneController.endGame(endGameResponse.getWin());
                    } else if (connectResponse.getMessage().equals("reset")) {
                        System.out.println("Противник отключился");
                        sceneController.disableButton(true);
                        sceneController.setOpponentFieldDisabled(true);
                    }
                    System.out.println("-------------------------------------------------------");
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Ошибка: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Закрытие потока");
                }
            });
        } catch (Exception e) {
            System.err.println("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    public void sendShipStage(int x, int y) {
        try {
            StageRequest stageRequest = StageRequest.newBuilder()
                    .setClientId(this.ClientId)
                    .setX(x)
                    .setY(y)
                    .build();
            System.out.println("Отправлено сообщение о состоянии корабля");
            StageResponse stageResponse = blockingStub.stageService(stageRequest);
            System.out.println("Ответ получен: " + stageResponse.getPlaced());

            if (stageResponse.getPlaced() == 1) {
                sceneController.setPlace(x, y);
            } else if (stageResponse.getPlaced() == 0) {
                sceneController.removePlace(x, y);
            } else {
                System.out.println("Размещено максимальное количество кораблей");
            }
        } catch (Exception e) {
            System.err.println("Ошибка постановки корабля: " + e.getMessage());
        }
    }

    public void sendShotMessage(int x, int y) {
        try {
            ShotRequest shotRequest = ShotRequest.newBuilder()
                    .setClientId(this.ClientId)
                    .setX(x)
                    .setY(y)
                    .build();
            System.out.println("Сообщение отправлено: (" + x + ", " + y + ")");
            ShotResponse shotResponse = blockingStub.shotService(shotRequest);
        } catch (Exception e) {
            System.err.println("Ошибка выстрела: " + e.getMessage());
        }
    }

    public void sendReadyMessage() {
        try {
            ReadyRequest readyRequest = ReadyRequest.newBuilder()
                    .setClientId(this.ClientId)
                    .build();
            System.out.println("Сообщение о готовности отправлено");
            ReadyResponse readyResponse = blockingStub.readyService(readyRequest);
            System.out.println("Ответ получен: " + readyResponse.getIsTurn());
            if (readyResponse.getIsTurn()) {
                sceneController.setTurn(true);
                sceneController.setOpponentFieldDisabled(false);
            }
        } catch (Exception e) {
            System.err.println("Ошибка готовности: " + e.getMessage());
        }
    }

    public void sendEndGame(boolean win) {
        try {
            EndGameRequest endGameRequest = EndGameRequest.newBuilder()
                    .setClientId(this.ClientId)
                    .setWin(win)
                    .build();
            System.out.println("Сообщение об окончании игры");
            EndGameResponse endGameResponse = blockingStub.endGameService(endGameRequest);
            System.out.println("Ответ получен. Игра окончена");
        } catch (Exception e) {
            System.err.println("Ошибка окончания игры: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            CloseStreamRequest closeStreamRequest = CloseStreamRequest.newBuilder()
                    .setClientId(this.ClientId)
                    .build();
            System.out.println("Сообщение о закрытии потока");
            CloseStreamResponse closeStreamResponse =
                    blockingStub.closeStreamService(closeStreamRequest);
            System.out.println("Ответ получен. Поток закрыт");
        } catch (Exception e) {
            System.err.println("Ошибка закрытия потока: " + e.getMessage());
        }
    }
}
