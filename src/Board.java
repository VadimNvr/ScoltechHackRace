import javafx.util.Pair;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Created by vadim on 03.02.16.
 */
public class Board {

    Grid grid;
    List<Level> levels;
    List<Point> jumps;
    PrintStream result_writer;

    private final boolean SORT_BY_LENGTH = true;
    private final boolean FIRST_OPTIMIZATION = false;

    public Board() {
        grid = new Grid();
        levels = new ArrayList<>();
        jumps = new ArrayList<>();

        try {
            FileOutputStream os = new FileOutputStream("data/result.txt");
            result_writer = new PrintStream(os);
            System.setOut(result_writer);
        } catch (IOException x) {
            System.err.println(x);
        }
    }

    public void initGrid() {
        Double x_start = -4.0;
        Double y_start = -4.05;

        Double dy = 0.2;
        Double dx;

        Double y = y_start;

        for (int i = 0; ; ++i) {
            if (y > 20)
                break;

            Double x = x_start;
            for (int j = 0; ; ++j) {
                if (x > 18)
                    break;

                //dx = (x < 12 ? 0.25 : 0.2);
                dx = 0.2;

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

        for (int i = 0; i < 40; ++i)
            level.pins.add(i);
    }

    private void findPinsOnGrid(Level level) {
        Double x_start = -4.0;
        Double y_start = -4.05;

        Double x, y;

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

    private boolean intersects(Level level, Double x, Double y, int map_i, int map_j) {

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

    private LinkedList<Point> getTrack(Level level, Point src, Point dst) {

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

        LinkedList<Point> track = new LinkedList<>();
        LinkedList<Point> queue = new LinkedList<>();
        queue.add(new Point(x0, y0));

        while (!queue.isEmpty()) {
            Point coord = queue.pollFirst();

            int x = coord.x;
            int y = coord.y;
            int step = map[y][x];

            if ((x == x1) && (y == y1)) {

                level.set(5, x, y);

                while (true) {

                    track.addFirst(new Point(x, y));

                    if ((x == x0) && (y == y0)) {
                        //drawTrack(new Point(prev_x, prev_y), new Point(x, y), new Point(x, y));
                        level.set(5, x, y);
                        return track;
                    }

                    if (map[y+1][x] == map[y][x] - 1) {
                        //drawTrack(new Point(prev_x, prev_y), new Point(x, y), new Point(x, y+1));
                        level.set(3, x, y+1);
                        y += 1;
                    }
                    else if (map[y-1][x] == map[y][x] - 1) {
                        //drawTrack(new Point(prev_x, prev_y), new Point(x, y), new Point(x, y-1));
                        level.set(3, x, y-1);
                        y -= 1;
                    }
                    else if (map[y][x+1] == map[y][x] - 1) {
                        //drawTrack(new Point(prev_x, prev_y), new Point(x, y), new Point(x+1, y));
                        level.set(2, x+1, y);
                        x += 1;
                    }
                    else if (map[y][x-1] == map[y][x] - 1) {
                        //drawTrack(new Point(prev_x, prev_y), new Point(x, y), new Point(x-1, y));
                        level.set(2, x-1, y);
                        x -= 1;
                    }
                }
            }

            if ((x > 0) && (map[y][x-1] == FREE)) {
                queue.add(new Point(x - 1, y));
                map[y][x-1] = step+1;
            }

            if ((y > 0) && (map[y-1][x] == FREE)) {
                queue.add(new Point(x, y-1));
                map[y-1][x] = step+1;
            }

            if ((y < grid.height - 1) && (map[y+1][x] == FREE)) {
                queue.add(new Point(x, y + 1));
                map[y+1][x] = step+1;
            }

            if ((x < grid.width - 1) && (map[y][x+1] == FREE)) {
                queue.add(new Point(x + 1, y));
                map[y][x+1] = step+1;
            }
        }

        return null;
    }

    enum Side {LEFT, RIGHT, TOP, BOTTOM};
    private void drawTrack(LinkedList<Point> track, int level) {

        Point prev, cur, next;

        //System.out.println("New Track");

        for (int i = 0; i < track.size(); ++i) {

            cur = track.get(i);
            prev = (i==0 ? cur : track.get(i-1));
            next = (i==track.size()-1 ? cur : track.get(i+1));

            List<Side> sides = new ArrayList<>();

            if ((prev.x < cur.x) || (next.x < cur.x)) {
                sides.add(Side.LEFT);
            }

            if ((prev.x > cur.x) || (next.x > cur.x)) {
                sides.add(Side.RIGHT);
            }

            if ((prev.y < cur.y) || (next.y < cur.y)) {
                sides.add(Side.BOTTOM);
            }

            if ((prev.y > cur.y) || (next.y > cur.y)) {
                sides.add(Side.TOP);
            }

            drawParts(level, cur, sides);
        }
    }

    private void drawParts(int level, Point center, List<Side> sides) {
        Pair<Double, Double> abs_center = gridToCoords(center.x, center.y);
        Double dx = grid.x_steps.get(center.x);
        Double dy = grid.y_steps.get(center.y);

        Pair<Double, Double> lr_dimens = new Pair<>((dx - 0.1)/2, 0.1);
        Pair<Double, Double> tb_dimens = new Pair<>(0.1, (dy - 0.1)/2);

        //System.out.format("dx: %.4f; dy: %.4f; lr: %.4f; tb: %.4f\n", dx, dy, lr_dimens.getKey(), tb_dimens.getValue());

        drawRect(level, abs_center, new Pair<>(0.1, 0.1));

        for (Side side: sides) {
            switch (side) {
                case LEFT:
                    Double left_center_x = abs_center.getKey() - dx/2 + (lr_dimens.getKey() - 0.002)/2;
                    Double left_center_y = abs_center.getValue();
                    drawRect(level, new Pair<>(left_center_x, left_center_y), lr_dimens);
                    break;

                case RIGHT:
                    Double right_center_x = abs_center.getKey() + dx/2 - (lr_dimens.getKey() - 0.002)/2;
                    Double right_center_y = abs_center.getValue();
                    drawRect(level, new Pair<>(right_center_x, right_center_y), lr_dimens);
                    break;

                case TOP:
                    Double top_center_x = abs_center.getKey();
                    Double top_center_y = abs_center.getValue() + dy/2 - (tb_dimens.getValue() - 0.002)/2;
                    drawRect(level, new Pair<>(top_center_x, top_center_y), tb_dimens);
                    break;

                case BOTTOM:
                    Double bottom_center_x = abs_center.getKey();
                    Double bottom_center_y = abs_center.getValue() - dy/2 + (tb_dimens.getValue() - 0.002)/2;
                    drawRect(level, new Pair<>(bottom_center_x, bottom_center_y), tb_dimens);
                    break;
            }
        }
    }

    private void drawRect(int level, Pair<Double, Double> center, Pair<Double, Double> dimens) {
        double x = center.getKey();
        double y = center.getValue();

        double width = dimens.getKey();
        double height = dimens.getValue();

        System.out.format("POLY %d, %d\n%.3f; %.3f\n%.3f; %.3f\n%.3f; %.3f\n%.3f; %.3f\n",
                4, level,
                x - width/2, y + height/2,
                x + width/2, y + height/2,
                x + width/2, y - height/2,
                x - width/2, y - height/2);
    }

    private Pair<Double, Double> gridToCoords(int grid_x, int grid_y) {
        Double x = -4.0;
        Double y = -4.05;

        for (int i = 0; i < grid_x; ++i)
            x += grid.x_steps.get(i);

        for (int i = 0; i < grid_y; ++i)
            y += grid.y_steps.get(i);

        return new Pair<>(x, y);
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
        /*

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
        */

        /*
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

                        Point src = new Point(src_pin.grid_x, src_pin.grid_y);
                        Point dst = new Point(dst_pin.grid_x, dst_pin.grid_y);

                        LinkedList track = getTrack(level, src, dst);
                        if (track != null) {
                            drawTrack(track);
                            success.add(idx);
                        }
                    }
                } else {
                    for (int i = max_set.size() - 1; i >= 0; --i) {
                        int idx = initial.get(max_set.get(i));

                        Pin src_pin = level.ram.pins[idx];
                        Pin dst_pin = level.cpu.pins[level.ram.connections[idx]];

                        Point src = new Point(src_pin.grid_x, src_pin.grid_y);
                        Point dst = new Point(dst_pin.grid_x, dst_pin.grid_y);

                        LinkedList track = getTrack(level, src, dst);
                        if (track != null) {
                            success.add(idx);
                        }
                    }
                }

                for (Integer elem: success)
                    initial.remove(elem);
            }
        }
        */

        List<Integer> initial = level.pins;

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

            Point src = new Point(src_pin.grid_x, src_pin.grid_y);
            Point dst = new Point(dst_pin.grid_x, dst_pin.grid_y);

            LinkedList<Point> track = getTrack(level, src, dst);

            if (track != null)
                drawTrack(track, levels.size());
            else
                last.add(elem);
        }

        if (last.isEmpty())
            return;

        Level next_level = new Level(grid.height, grid.width);

        for (int i : last) {
            next_level.pins.add(i);
            int cpu_pin_num = level.ram.connections[i];
            int ram_pin_num = i;

            Pin cpu_pin = level.cpu.pins[cpu_pin_num];
            Pin ram_pin = level.ram.pins[ram_pin_num];

            if (levels.size() > 1) {

                next_level.cpu.pins[cpu_pin_num] = new Pin(cpu_pin.grid_x, cpu_pin.grid_y);
                next_level.ram.pins[ram_pin_num] = new Pin(ram_pin.grid_x, ram_pin.grid_y);

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

            else {
                List<Pair<Character, Point>> dir = getDirection(cpu_pin.toPoint(), ram_pin.toPoint());

                for (Pair spot: dir) {
                    Point point = (Point) spot.getValue();

                    if (level.get(point.x, point.y) == 0) {

                        next_level.cpu.pins[cpu_pin_num] = new Pin(point.x, point.y);
                        next_level.cpu.connections[cpu_pin_num] = ram_pin_num;

                        next_level.cpu.pins[cpu_pin_num].grid_x = point.x;
                        next_level.cpu.pins[cpu_pin_num].grid_y = point.y;

                        next_level.set(1, point.x, point.y);
                        level.set(1, cpu_pin.grid_x, cpu_pin.grid_y);
                        level.set(8, point.x, point.y);

                        LinkedList<Point> track = new LinkedList<>();
                        track.add(cpu_pin.toPoint());
                        track.add(point);
                        drawTrack(track, levels.size());

                        Pair<Double, Double> jump_abs = gridToCoords(point.x, point.y);
                        System.out.format("JUMP %f; %f\n", jump_abs.getKey(), jump_abs.getValue());

                        break;
                    }
                }

                dir = getDirection(ram_pin.toPoint(), cpu_pin.toPoint());
                for (Pair spot: dir) {
                    Point point = (Point) spot.getValue();

                    if (level.get(point.x, point.y) == 0) {

                        next_level.ram.pins[ram_pin_num] = new Pin(point.x, point.y);
                        next_level.ram.connections[ram_pin_num] = cpu_pin_num;

                        next_level.ram.pins[ram_pin_num].grid_x = point.x;
                        next_level.ram.pins[ram_pin_num].grid_y = point.y;

                        next_level.set(1, point.x, point.y);
                        level.set(1, ram_pin.grid_x, ram_pin.grid_y);
                        level.set(8, point.x, point.y);

                        LinkedList<Point> track = new LinkedList<>();
                        track.add(ram_pin.toPoint());
                        track.add(point);
                        drawTrack(track, levels.size());

                        Pair<Double, Double> jump_abs = gridToCoords(point.x, point.y);
                        System.out.format("JUMP %f; %f\n", jump_abs.getKey(), jump_abs.getValue());

                        break;
                    }

                }
            }
        }

        //levels.add(next_level);
        //drawTracks();
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

    private double getDistance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x-b.x,2) + Math.pow(a.y-b.y,2));
    }

    private List<Pair<Character, Point>> getDirection(Point a, Point b) {

        Map<Character, Point> map = new HashMap<>();

        map.put('s', new Point(a.x,a.y + 1));
        map.put('w', new Point(a.x - 1,a.y));
        map.put('n', new Point(a.x,a.y - 1));
        map.put('e', new Point(a.x + 1,a.y));

        List<Pair<Character, Point>> result = new ArrayList<>();

        map.entrySet().stream().sorted(Comparator.comparing(e-> getDistance(e.getValue(),b)))
                .forEachOrdered(e->result.add(new Pair<Character, Point>(e.getKey(),e.getValue())));


        return result;
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

        double min_jumps = 30;
        double min_dist  = 3800;

        System.out.println("Layers: " + Integer.toString(levels.size()));
        System.out.println("Length: " + Integer.toString(length));
        System.out.println("Jumps " + Integer.toString(jumps));
        System.out.println("Ratio: " + Double.toString(min_jumps / jumps * 2 + min_dist / length * 3));
    }

    public void test() {
        //Pin test_pin = levels.get(0).cpu.pins[0].toPoint();

        LinkedList<Point> track = getTrack(levels.get(0), levels.get(0).cpu.pins[1].toPoint(), levels.get(0).ram.pins[32].toPoint());
        drawTrack(track, 1);

        //track = getTrack(levels.get(0), levels.get(0).cpu.pins[0].toPoint(), levels.get(0).cpu.pins[3].toPoint());
        //drawTrack(track, 0);
        //Pair<Double, Double> abs = gridToCoords(test_pin.grid_x, test_pin.grid_y);

        //System.out.format("%.3f %.3f\n", abs.getKey(), abs.getValue());
    }
}
