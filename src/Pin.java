import java.awt.*;

/**
 * Created by vadim on 03.02.16.
 */
public class Pin {
    public double x;
    public double y;

    public int grid_x;
    public int grid_y;

    Pin(double _x, double _y) {
        x = _x;
        y = _y;
    }

    public Point toPoint() {
        return new Point(grid_x, grid_y);
    }
}
