/**
 * Created by vadim on 05.02.16.
 */
public class Jump {
    static final int CPU = 0;
    static final int RAM = 1;

    int x;
    int y;
    int belongs;
    int pin;

    Jump(int _x, int _y, int _belongs, int _pin) {
        x = _x;
        y = _y;
        belongs = _belongs;
        pin = _pin;
    }
}
