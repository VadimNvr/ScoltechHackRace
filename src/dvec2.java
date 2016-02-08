/**
 * Created by vadim on 07.02.16.
 */
public class dvec2 {
    double x;
    double y;

    dvec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    dvec2 add(dvec2 summard) {
        return new dvec2(x + summard.x, y + summard.y);
    }

    dvec2 sub(dvec2 summard) {
        return new dvec2(x - summard.x, y - summard.y);
    }

    dvec2 normal() {
        return new dvec2(y, -x);
    }

    double norm() {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    dvec2 normalized() {
        double n = norm();
        return new dvec2(x / n, y / n);
    }

    dvec2 mul(double k) {
        return new dvec2(k*x, k*y);
    }

    boolean eq(dvec2 vec) {
        return (x == vec.x) && (y == vec.y);
    }

    double dot(dvec2 vec) {
        return vec.x * x + vec.y * y;
    }

}
