module org.example.battleship2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.grpc;
    requires io.netty.buffer;
    requires io.grpc.protobuf;
    requires protobuf.java;
    requires io.grpc.stub;
    requires com.google.common;
    requires java.annotation;
    requires com.google.gson;

    opens org.example.battleship2 to javafx.fxml;
    exports org.example.battleship2;
}