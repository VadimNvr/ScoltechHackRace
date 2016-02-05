import java.util.ArrayList;

/**
 * Created by vadim on 03.02.16.
 */
public class Chip {
    public Pin[] pins;
    public int[] connections;

    public Chip() {
        pins = new Pin[40];
        connections = new int[40];

        for (int i = 0; i < 40; ++i)
            connections[i] = -1;
    }
}
