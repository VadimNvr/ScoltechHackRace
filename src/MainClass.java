import java.io.*;
import java.nio.file.Path;

/**
 * Created by vadim on 03.02.16.
 */
public class MainClass {

    public static void main(String[] args){
        Board board = new Board();
        board.getPins();
        board.getConnections();
    }
}
