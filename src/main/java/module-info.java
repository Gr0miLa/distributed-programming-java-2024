module org.example.battleship {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens org.example.battleship to javafx.fxml;
    exports org.example.battleship;
}