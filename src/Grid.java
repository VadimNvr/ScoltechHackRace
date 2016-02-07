import java.util.ArrayList;
import java.util.List;

/**
 * Created by vadim on 04.02.16.
 */
public class Grid {
    int height;
    int width;
    List<Double> x_steps;
    List<Double> y_steps;

    double x_start;
    double y_start;

    Grid() {
        x_start = -4.0;
        y_start = -4.0;

        x_steps = new ArrayList<>();
        y_steps = new ArrayList<>();

        height = 0;
        width  = 0;
    }
}
