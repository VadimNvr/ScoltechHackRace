import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vadim on 04.02.16.
 */
public class Level {
    Chip cpu;
    Chip ram;
    private Integer[][] map;
    private int height;
    private int width;
    List<Integer> pins;

    public Level(int _height, int _width) {
        height = _height;
        width = _width;

        map = new Integer[height][width];

        for (int i = 0; i < height; ++i)
            for (int j = 0; j < width; ++j)
                set(0, j, i);

        cpu = new Chip();
        ram = new Chip();

        pins = new ArrayList<>();
    }

    public void set(int value, int x, int y) {
        map[y][x] = value;
    }

    public int get(int x, int y) {
        return map[y][x];
    }

    public Integer[][] cloneMap() {
        Integer[][] new_map = new Integer[height][width];

        for (int i = 0; i < height; ++i)
            for (int j = 0; j < width; ++j)
                new_map[i][j] = map[i][j];

        return new_map;
    }

    public void print(int num) {
        try {
            FileOutputStream os = new FileOutputStream("data/level_" + Integer.toString(num) + ".txt");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

            for (int i = height-1; i > 0; --i) {
                String line = "";
                for (int j = 0; j < width; ++j) {
                    switch (get(j, i)) {
                        case 0:
                            line += '.';
                            break;

                        case 1:
                            line += '*';
                            break;

                        case 2:
                            line += '-';
                            break;

                        case 3:
                            line += '|';
                            break;

                        case 5:
                            line += '$';
                            break;

                        case 8:
                            line += '^';
                            break;
                    }
                }
                writer.write(line+'\n');
            }

            writer.close();
        } catch (IOException x) {
            System.err.println(x);
        }
    }
}
