import java.io.*;
import java.util.*;

/**
 * Created by vadim on 03.02.16.
 */
public class Board {

    Grid grid;
    List<Level> levels;
    List<Pair> jumps;

    private final boolean SORT_BY_LENGTH = true;
    private final boolean FIRST_OPTIMIZATION = false;

    public Board() {
        grid = new Grid();
        levels = new ArrayList<>();
        jumps = new ArrayList<>();
    }

    public void initGrid() {
        float x_start = -4.0f;
        float y_start = -4.05f;

        float dy = 0.2f;
        float dx;

        float y = y_start;

        for (int i = 0; ; ++i) {
            if (y > 20)
                break;

            float x = x_start;
            for (int j = 0; ; ++j) {
                if (x > 18)
                    break;

                dx = (x < 12 ? 0.25f : 0.2f);

                if (i == 0)
                    grid.x_steps.add(dx);

                x += dx;
            }

            grid.y_steps.add(dy);
            y += dy;
        }

        grid.height = grid.y_steps.size();
        grid.width  = grid.x_steps.size();
    }

    public void init1Level() {
        levels.add(new Level(grid.height, grid.width));
        Level level = levels.get(0);

        getPins(level);
        getConnections(level);
        findPinsOnGrid(level);

        for (Pin pin : level.cpu.pins)
            level.set(1, pin.grid_x, pin.grid_y);

        for (Pin pin : level.ram.pins)
            level.set(1, pin.grid_x, pin.grid_y);
    }

    private void findPinsOnGrid(Level level) {
        float x_start = -4.0f;
        float y_start = -4.05f;

        float x, y;

        y = y_start;
        for (int i = 0; i < grid.height; ++i) {

            x = x_start;
            for (int j = 0; j < grid.width; ++j) {

                intersects(level, x, y, i, j);
                x += grid.x_steps.get(j);
            }

            y += grid.y_steps.get(i);
        }
    }

    private boolean intersects(Level level, float x, float y, int map_i, int map_j) {

        for (Pin pin: level.cpu.pins) {
            if ((Math.abs(pin.x - x) <= 0.1) && (Math.abs(pin.y - y) <= 0.1)) {
                pin.grid_x = map_j;
                pin.grid_y = map_i;
                return true;
            }
        }

        for (Pin pin: level.ram.pins) {
            if ((Math.abs(pin.x - x) <= 0.1) && (Math.abs(pin.y - y) <= 0.1)) {
                pin.grid_x = map_j;
                pin.grid_y = map_i;
                return true;
            }
        }

        return false;
    }

    public void getPins(Level level) {
        try {
            FileInputStream is = new FileInputStream("data/coordinates.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while((line = reader.readLine()) != null)
            {
                String[] ar = line.split(";");

                int pin_num = Integer.parseInt(ar[0]) - 1;
                int chip_num = Integer.parseInt(ar[1]);
                double x = Double.parseDouble(ar[2].replace(',', '.'));
                double y = Double.parseDouble(ar[3].replace(',', '.'));

                if (chip_num == 1)
                    level.cpu.pins[pin_num] = new Pin(x, y);
                else
                    level.ram.pins[pin_num] = new Pin(x, y);
            }
        } catch (IOException x) {
            System.err.println(x);
        }
    }

    public void getConnections(Level level) {
        try {
            FileInputStream is = new FileInputStream("data/connect.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while((line = reader.readLine()) != null)
            {
                String[] ar = line.split(";");

                int cpu_pin = Integer.parseInt(ar[0]) - 1;
                int ram_pin = Integer.parseInt(ar[1]) - 1;

                level.cpu.connections[cpu_pin] = ram_pin;
                level.ram.connections[ram_pin] = cpu_pin;
            }
        } catch (IOException x) {
            System.err.println(x);
        }
    }

    private List<Integer> findInserted(Level level, List<Integer> array, List<Integer> passed) {
        int parent = passed.get(passed.size() - 1);
        int parent_pin = array.get(parent);

        List<Integer> max_set = new ArrayList<>(passed);

        for (int i = parent+1; i < array.size(); ++i) {
            int cur_pin = array.get(i);
            int parent_ram_pin = level.ram.connections[parent_pin];
            int cur_ram_pin = level.ram.connections[cur_pin];

            if (level.cpu.pins[cur_ram_pin].x > level.cpu.pins[parent_ram_pin].x) {
                List<Integer> new_set = new ArrayList<>(passed);
                new_set.add(i);

                new_set = findInserted(level, array, new_set);
                if (new_set.size() > max_set.size())
                    max_set = new_set;
            }
        }

        return max_set;
    }

    private final int FREE = -2;
    private final int BUSY = -1;

    private boolean getTrack(Level level, Pair src, Pair dst) {

        int x0 = src.x;
        int y0 = src.y;

        int x1 = dst.x;
        int y1 = dst.y;

        Integer[][] map = level.cloneMap();

        for (int i = 0; i < grid.height; ++i) {
            for (int j = 0; j < grid.width; ++j) {
                map[i][j] = (map[i][j] == 0 ? FREE : BUSY);
            }
        }

        map[y0][x0] = 0;
        map[y1][x1] = FREE;

        LinkedList<Pair> queue = new LinkedList<>();
        queue.add(new Pair(x0, y0));

        while (!queue.isEmpty()) {
            Pair coord = queue.pollFirst();
            int x = coord.x;
            int y = coord.y;
            int step = map[y][x];

            if ((x == x1) && (y == y1)) {
                level.set(5, x, y);

                while (true) {
                    if ((x == x0) && (y == y0)) {
                        level.set(5, x, y);
                        return true;
                    }

                    if (map[y+1][x] == map[y][x] - 1) {
                        level.set(3, x, y+1);
                        y += 1;
                    }
                    else if (map[y-1][x] == map[y][x] - 1) {
                        level.set(3, x, y-1);
                        y -= 1;
                    }
                    else if (map[y][x+1] == map[y][x] - 1) {
                        level.set(2, x+1, y);
                        x += 1;
                    }
                    else if (map[y][x-1] == map[y][x] - 1) {
                        level.set(2, x-1, y);
                        x -= 1;
                    }
                }
            }

            if ((x > 0) && (map[y][x-1] == FREE)) {
                queue.add(new Pair(x - 1, y));
                map[y][x-1] = step+1;
            }

            if ((y > 0) && (map[y-1][x] == FREE)) {
                queue.add(new Pair(x, y-1));
                map[y-1][x] = step+1;
            }

            if ((y < grid.height - 1) && (map[y+1][x] == FREE)) {
                queue.add(new Pair(x, y + 1));
                map[y+1][x] = step+1;
            }

            if ((x < grid.width - 1) && (map[y][x+1] == FREE)) {
                queue.add(new Pair(x + 1, y));
                map[y][x+1] = step+1;
            }
        }

        return false;
    }

    private void getTranspositions(List<List<Integer>> tofill, Integer[] elems, List<Integer> path) {

        if (path.size() == elems.length) {
            tofill.add(path);
            return;
        }

        for (int elem : elems) {
            if (!path.contains(elem)) {
                List<Integer> new_path = new ArrayList<>(path);
                new_path.add(elem);

                getTranspositions(tofill, elems, new_path);
            }
        }
    }

    private double getDistance(int ram_pin) {
        Level level = levels.get(0);

        int x0 = level.ram.pins[ram_pin].grid_x;
        int y0 = level.ram.pins[ram_pin].grid_y;

        int x1 = level.cpu.pins[level.ram.connections[ram_pin]].grid_x;
        int y1 = level.cpu.pins[level.ram.connections[ram_pin]].grid_y;

        return Math.sqrt(Math.pow(x1-x0, 2) + Math.pow(y1-y0, 2));
    }

    public void drawTracks() {
        Level level = levels.get(levels.size()-1);

        List<Integer> initial = new ArrayList<>();

        int left = 20, right = 40;
        boolean ping_pong = false;
        int cur;

        while ((cur = ping_pong ? --right: --left) >= 0) {
            if (level.ram.connections[cur] != -1) {
                initial.add(cur);
            }
            ping_pong = !ping_pong;
        }

        if (FIRST_OPTIMIZATION) {
            List<Integer> success = new ArrayList<>();

            boolean reversed = true;
            for (int k = 0; k < 2; ++k) {
                List<Integer> max_set = new ArrayList<>();

                for (int i = 0; i < initial.size(); ++i) {
                    List<Integer> new_set = new ArrayList<>();
                    new_set.add(i);

                    new_set = findInserted(level, initial, new_set);

                    if (new_set.size() > max_set.size())
                        max_set = new_set;
                }

                if (!reversed) {
                    for (int elem : max_set) {
                        int idx = initial.get(elem);

                        Pin src_pin = level.ram.pins[idx];
                        Pin dst_pin = level.cpu.pins[level.ram.connections[idx]];

                        Pair src = new Pair(src_pin.grid_x, src_pin.grid_y);
                        Pair dst = new Pair(dst_pin.grid_x, dst_pin.grid_y);

                        if (getTrack(level, src, dst))
                            success.add(idx);
                    }
                } else {
                    for (int i = max_set.size() - 1; i >= 0; --i) {
                        int idx = initial.get(max_set.get(i));

                        Pin src_pin = level.ram.pins[idx];
                        Pin dst_pin = level.cpu.pins[level.ram.connections[idx]];

                        Pair src = new Pair(src_pin.grid_x, src_pin.grid_y);
                        Pair dst = new Pair(dst_pin.grid_x, dst_pin.grid_y);

                        if (getTrack(level, src, dst))
                            success.add(idx);
                    }
                }

                for (Integer elem: success)
                    initial.remove(elem);
            }
        }

        if (SORT_BY_LENGTH) {
            Collections.sort(initial, new Comparator<Integer>() {
                @Override
                public int compare(Integer pin1, Integer pin2) {
                    return (int) (getDistance(pin1) - getDistance(pin2));
                }
            });
        }

        List<Integer> last = new ArrayList<>();

        for (int elem: initial) {
            Pin src_pin = level.ram.pins[elem];
            Pin dst_pin = level.cpu.pins[level.ram.connections[elem]];

            Pair src = new Pair(src_pin.grid_x, src_pin.grid_y);
            Pair dst = new Pair(dst_pin.grid_x, dst_pin.grid_y);
            if (!getTrack(level, src, dst))
                last.add(elem);
        }

        if (!last.isEmpty()) {
            Level next_level = new Level(grid.height, grid.width);

            for (int i : last) {
                int cpu_pin_num = level.ram.connections[i];
                int ram_pin_num = i;

                Pin cpu_pin = level.cpu.pins[cpu_pin_num];
                Pin ram_pin = level.ram.pins[ram_pin_num];

                next_level.cpu.pins[cpu_pin_num] = new Pin(cpu_pin.x, cpu_pin.y);
                next_level.ram.pins[ram_pin_num] = new Pin(ram_pin.x, ram_pin.y);

                next_level.cpu.connections[cpu_pin_num] = ram_pin_num;
                next_level.ram.connections[ram_pin_num] = cpu_pin_num;

                next_level.cpu.pins[cpu_pin_num].grid_x = cpu_pin.grid_x;
                next_level.cpu.pins[cpu_pin_num].grid_y = cpu_pin.grid_y;

                next_level.ram.pins[ram_pin_num].grid_x = ram_pin.grid_x;
                next_level.ram.pins[ram_pin_num].grid_y = ram_pin.grid_y;

                next_level.set(1, cpu_pin.grid_x, cpu_pin.grid_y);
                next_level.set(1, ram_pin.grid_x, ram_pin.grid_y);

                level.set(8, cpu_pin.grid_x, cpu_pin.grid_y);
                level.set(8, ram_pin.grid_x, ram_pin.grid_y);
            }

            levels.add(next_level);
            drawTracks();
        }
    }

    private int getLength() {
        int count = 0;

        for (Level level : levels) {
            for (int i = 0; i < grid.height; ++i)
                for (int j = 0; j < grid.width; ++j)
                    if (level.get(j, i) > 1)
                        count++;
        }

        return count;
    }

    private int getJumps() {
        int count = 0;

        for (int i = 0; i < grid.height; ++i) {
            for (int j = 0; j < grid.width; ++j)
                if (levels.get(0).get(j, i) == 8)
                    ++count;
        }

        return  count;
    }

    public void printLevels() {
        for (int i = 0; i < levels.size(); ++i)
            levels.get(i).print(i+1);
    }

    public void printInfo() {
        int length = getLength();
        int jumps = getJumps();

        System.out.println("Layers: " + Integer.toString(levels.size()));
        System.out.println("Length: " + Integer.toString(length));
        System.out.println("Jumps " + Integer.toString(jumps));
    }
}
