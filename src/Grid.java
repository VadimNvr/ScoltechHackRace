import java.util.ArrayList;
import java.util.List;

/**
 * Created by vadim on 04.02.16.
 */
public class Grid {
    int height;
    int width;
    List<Float> x_steps;
    List<Float> y_steps;

    Grid() {
        x_steps = new ArrayList<>();
        y_steps = new ArrayList<>();

        height = 0;
        width  = 0;
    }
}
