import javafx.util.Pair;

import java.awt.*;
import java.awt.geom.Line2D;
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

        double dx, dy;

        double y = grid.y_start;

        for (int i = 0; ; ++i) {
            if (y > 20)
                break;

            double x = grid.x_start;
            for (int j = 0; ; ++j) {
                if (x > 18)
                    break;

                dx = (x < 12 ? 0.25 : 0.2);

                if (i == 0)
                    grid.x_steps.add(dx);

                x += dx;
            }

            dy = (y < 5 ? 0.25 : 0.2);
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

        double x, y;

        y = grid.y_start;
        for (int i = 0; i < grid.height; ++i) {

            x = grid.x_start;
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


    private final int FREE = -2;
    private final int BUSY = -1;

    private LinkedList<Point> getTrack(Level level, Point src, Point dst) {

        int x0 = src.x;
        int y0 = src.y;

        int x1 = dst.x;
        int y1 = dst.y;

        Integer[][] map = level.cloneMap();

        Pin [] cpu_pins = level.cpu.pins;

        int reserv_y = levels.get(0).cpu.pins[0].grid_y - 1;

        for (int i : level.pins) {

            int cpu_idx = level.ram.connections[i];

            int y = cpu_pins[cpu_idx].grid_y;
            int x = cpu_pins[cpu_idx].grid_x;

            if ((y-1 != reserv_y) || ((x == x1) && (y == y1)))
                continue;

            if (map[y-1][x] == 0) {
                map[y-1][x] = 1;
            }
        }

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
                        level.set(5, x, y);
                        return track;
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

    private boolean sphereSegIntersection(dvec2[] seg, dvec2 center) {
        double r = 0.22;
        dvec2 d = seg[1].sub(seg[0]);
        dvec2 f = seg[0].sub(center);

        double a = d.dot( d ) ;
        double b = 2*f.dot( d ) ;
        double c = f.dot( f ) - r*r ;

        double discriminant = b*b-4*a*c;
        if( discriminant < 0 )
        {
            return false;
        }
        else
        {
            discriminant = Math.sqrt( discriminant );
            double t = (-b + discriminant)/(2*a);

            if ((t <= 1) && (t >= 0)) {
                return true;
            }
            else
                return false;
        }
    }

    private void drawParts(int level, Point center, List<Side> sides) {
        Pair<Double, Double> abs_center = gridToCoords(center.x, center.y);

        double dx = 0.25;
        double dy = 0.25;

        double line_width = 0.1;

        Pair<Double, Double> lr_dimens = new Pair<>(dx/2, line_width);
        Pair<Double, Double> tb_dimens = new Pair<>(line_width, dy/2);

        double cx, cy;

        drawRect(level, abs_center, new Pair<>(line_width, line_width));

        for (Side side: sides) {
            switch (side) {
                case LEFT:
                    cx = abs_center.getKey() - dx/4;
                    cy = abs_center.getValue();
                    drawRect(level, new Pair<>(cx, cy), lr_dimens);
                    break;

                case RIGHT:
                    cx = abs_center.getKey() + dx/4;
                    cy = abs_center.getValue();
                    drawRect(level, new Pair<>(cx, cy), lr_dimens);
                    break;

                case TOP:
                    cx = abs_center.getKey();
                    cy = abs_center.getValue() + dy/4;
                    drawRect(level, new Pair<>(cx, cy), tb_dimens);
                    break;

                case BOTTOM:
                    cx = abs_center.getKey();
                    cy = abs_center.getValue() - dy/4;
                    drawRect(level, new Pair<>(cx, cy), tb_dimens);
                    break;
            }
        }
    }

    private void drawRect(int level, Pair<Double, Double> center, Pair<Double, Double> dimens) {
        double x = center.getKey();
        double y = center.getValue();

        double width = dimens.getKey();
        double height = dimens.getValue();

        System.out.format("POLY %d, %d\n%f; %f\n%f; %f\n%f; %f\n%f; %f\n",
                4, level,
                x - width/2, y + height/2,
                x + width/2, y + height/2,
                x + width/2, y - height/2,
                x - width/2, y - height/2);
    }

    private Pair<Double, Double> gridToCoords(int grid_x, int grid_y) {
        double x = grid.x_start;
        double y = grid.y_start;

        for (int i = 0; i < grid_x; ++i)
            x += grid.x_steps.get(i);

        for (int i = 0; i < grid_y; ++i)
            y += grid.y_steps.get(i);

        return new Pair<>(x, y);
    }

    private boolean areCrossing(dvec2[] seg1, dvec2[] seg2) {

        Line2D line1 = new Line2D.Double(seg1[0].x, seg1[0].y, seg1[1].x, seg1[1].y);
        Line2D line2 = new Line2D.Double(seg2[0].x, seg2[0].y, seg2[1].x, seg2[1].y);

        return line1.intersectsLine(line2);
    }

    private boolean inCircle(dvec2 point, dvec2 center) {
        double radius = 0.1;

        double dx = point.x - center.x;
        double dy = point.y - center.y;

        return (Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2))) <= radius;
    }

    private void invalidateTracks(Level level) {

        LinkedList<List<dvec2>> queue = new LinkedList<>();

        for (List<dvec2> track : level.tracks) {
            queue.add(track);
        }

        int iters = queue.size();

        while (iters >= 0) {

            double max_len = -1;
            List<dvec2> max_track = null;
            int max_l = -1;
            int max_r = -1;

            for (int it = 0; it < queue.size(); ++it) {
                List<dvec2> track = queue.pollFirst();

                int max_track_l = -1;
                int max_track_r = -1;
                double max_track_len = -1;

                for (int l = 0; l < track.size(); ++l) {
                    for (int r = track.size()-1; r > l + 1; --r) {
                        dvec2[] line_init = new dvec2[] {track.get(l), track.get(r)};

                        dvec2 dir = line_init[1].sub(line_init[0]);
                        dvec2 normal = dir.normal().normalized();

                        dvec2[][] lines = new dvec2[][] {
                                new dvec2[] { line_init[0].add(normal.mul(0.151)), line_init[1].add(normal.mul(0.151)) },
                                new dvec2[] { line_init[0].sub(normal.mul(0.151)), line_init[1].sub(normal.mul(0.151)) }
                        };

                        dvec2 pin_1 = track.get(0);
                        dvec2 pin_2 = track.get(track.size()-1);

                        //LINE INTERSECTIONS CHECK//
                        boolean able = true;
                        for (List<dvec2> tr: queue) {

                            if (sphereSegIntersection(line_init, pin_1) || sphereSegIntersection(line_init, pin_2)) {
                                able = false;
                                break;
                            }

                            for (int i = 0; i < tr.size()-1; ++i) {

                                dvec2[] ch_line = new dvec2[] { tr.get(i), tr.get(i+1) };

                                if (       areCrossing(line_init, ch_line)
                                        || areCrossing(lines[0], ch_line)
                                        || areCrossing(lines[1], ch_line)) {
                                    able = false;
                                    break;
                                }
                            }

                            if (!able)
                                break;
                        }
                        //---------

                        //JUMPS INTERSECTIONS CHECK//
                        for (Point p: jumps) {
                            Pair<Double, Double> jmp_abs = gridToCoords(p.x, p.y);
                            dvec2 jmp = new dvec2(jmp_abs.getKey(), jmp_abs.getValue());
                            if ((!inCircle(pin_1, jmp)) && (!inCircle(pin_2, jmp))) {
                                if (sphereSegIntersection(line_init, jmp)) {
                                    able = false;
                                    break;
                                }
                            }
                        }
                        //--------------

                        if (able) {
                            double cur_length = 0;
                            for (int i = l; i < r; ++i) {
                                cur_length += track.get(i+1).sub(track.get(i)).norm();
                            }
                            double short_length = track.get(r).sub(track.get(l)).norm();

                            double delta = cur_length-short_length;
                            if (delta > max_track_len) {
                                max_track_len = delta;
                                max_track_l = l;
                                max_track_r = r;
                            }
                        }

                    }
                }

                if (max_track_len > max_len) {
                    max_len = max_track_len;
                    max_l = max_track_l;
                    max_r = max_track_r;
                    max_track = track;
                }

                queue.addLast(track);
            }

            if (max_len > 0) {
                for (int i = max_r - 1; i > max_l; --i) {
                    max_track.remove(i);
                }
            }
            else {
                --iters;
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

    private void outputVertex(int lvl, dvec2 center) {
        double radius = 0.055;

        System.out.format("POLY 360, %d\n", lvl);

        for (int angle = 0; angle < 360; angle++) {
            double x = radius * Math.cos(Math.toRadians(angle));
            double y = radius * Math.sin(Math.toRadians(angle));

            dvec2 delta = new dvec2(x, y);
            delta = delta.add(center);

            System.out.format("%f; %f\n", delta.x, delta.y);
        }
    }

    private void outputLine(int lvl, dvec2[] line) {

        dvec2 dir = line[1].sub(line[0]);
        dvec2 normal = dir.normal().normalized();

        dvec2 v1 = line[0].add(normal.mul(0.0501));
        dvec2 v2 = line[1].add(normal.mul(0.0501));
        dvec2 v3 = line[1].sub(normal.mul(0.0501));
        dvec2 v4 = line[0].sub(normal.mul(0.0501));

        System.out.format("POLY 4, %d\n", lvl);
        System.out.format("%f; %f\n%f; %f\n%f; %f\n%f; %f\n",
                v1.x, v1.y,
                v2.x, v2.y,
                v3.x, v3.y,
                v4.x, v4.y);
    }

    private void outputTracks(Level level, int lvl) {
        for (List<dvec2> track: level.tracks) {
            for (int idx = 0; idx < track.size()-1; ++idx) {

                dvec2 [] seg = new dvec2[] {track.get(idx), track.get(idx+1)};

                outputVertex(lvl, seg[0]);
                outputVertex(lvl, seg[1]);

                outputLine(lvl, seg);
            }

        }
    }

    public void drawTracks() {
        Level level = levels.get(levels.size()-1);

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

            if (track != null) {
                List<dvec2> dtrack = new ArrayList<>();
                for (Point step: track) {
                    Pair<Double, Double> abs = gridToCoords(step.x, step.y);
                    dtrack.add(new dvec2(abs.getKey(), abs.getValue()));
                }
                level.tracks.add(dtrack);
            }
            else
                last.add(elem);
        }

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

                for (Point jump: jumps)
                    next_level.set(8, jump.x, jump.y);
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

                        Pair<Double, Double> jmp_abs = gridToCoords(point.x, point.y);
                        jumps.add(point);
                        Pair<Double, Double> pin_abs = gridToCoords(cpu_pin.grid_x, cpu_pin.grid_y);

                        System.out.format("JUMP %f; %f\n", jmp_abs.getKey(), jmp_abs.getValue());

                        List<dvec2> track = new ArrayList<>();
                        track.add(new dvec2(jmp_abs.getKey(), jmp_abs.getValue()));
                        track.add(new dvec2(pin_abs.getKey(), pin_abs.getValue()));
                        level.tracks.add(track);
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

                        Pair<Double, Double> jmp_abs = gridToCoords(point.x, point.y);
                        jumps.add(point);
                        Pair<Double, Double> pin_abs = gridToCoords(ram_pin.grid_x, ram_pin.grid_y);

                        System.out.format("JUMP %f; %f\n", jmp_abs.getKey(), jmp_abs.getValue());

                        List<dvec2> track = new ArrayList<>();
                        track.add(new dvec2(jmp_abs.getKey(), jmp_abs.getValue()));
                        track.add(new dvec2(pin_abs.getKey(), pin_abs.getValue()));
                        level.tracks.add(track);
                        break;
                    }

                }
            }
        }

        invalidateTracks(level);
        outputTracks(level, levels.size());

        if (last.isEmpty())
            return;

        levels.add(next_level);
        drawTracks();
    }


    private double getDistance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x-b.x,2) + Math.pow(a.y-b.y,2));
    }

    private List<Pair<Character, Point>> getDirection(Point a, Point b) {

        Map<Character, Point> map = new HashMap<>();

        map.put('s', new Point(a.x, a.y + 1));
        map.put('w', new Point(a.x - 1, a.y));
        map.put('n', new Point(a.x, a.y - 1));
        map.put('e', new Point(a.x + 1, a.y));

        List<Pair<Character, Point>> result = new ArrayList<>();

        map.entrySet().stream().sorted(Comparator.comparing(e -> getDistance(e.getValue(), b)))
                .forEachOrdered(e -> result.add(new Pair<Character, Point>(e.getKey(), e.getValue())));


        return result;
    }


    public void printLevels() {
        for (int i = 0; i < levels.size(); ++i)
            levels.get(i).print(i+1);
    }

}
