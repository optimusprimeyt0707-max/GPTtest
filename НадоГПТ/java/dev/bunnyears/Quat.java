package dev.bunnyears;

/** Минимальный кватернион без внешних зависимостей (чтобы физику можно было тестировать отдельно от сервера). */
public final class Quat {
    public final double w, x, y, z;

    public Quat(double w, double x, double y, double z) {
        this.w = w; this.x = x; this.y = y; this.z = z;
    }

    public static final Quat IDENTITY = new Quat(1, 0, 0, 0);

    public static Quat rotX(double a) { return new Quat(Math.cos(a / 2), Math.sin(a / 2), 0, 0); }
    public static Quat rotY(double a) { return new Quat(Math.cos(a / 2), 0, Math.sin(a / 2), 0); }
    public static Quat rotZ(double a) { return new Quat(Math.cos(a / 2), 0, 0, Math.sin(a / 2)); }

    /** this * o : сначала применяется o, затем this (как у матриц). */
    public Quat mul(Quat o) {
        return new Quat(
            w * o.w - x * o.x - y * o.y - z * o.z,
            w * o.x + x * o.w + y * o.z - z * o.y,
            w * o.y - x * o.z + y * o.w + z * o.x,
            w * o.z + x * o.y - y * o.x + z * o.w);
    }

    public Quat conj() { return new Quat(w, -x, -y, -z); }

    /** Поворот вектора v (массив из 3 double). */
    public double[] rotate(double vx, double vy, double vz) {
        // t = 2 * cross(q.xyz, v); v' = v + w * t + cross(q.xyz, t)
        double tx = 2 * (y * vz - z * vy);
        double ty = 2 * (z * vx - x * vz);
        double tz = 2 * (x * vy - y * vx);
        return new double[] {
            vx + w * tx + (y * tz - z * ty),
            vy + w * ty + (z * tx - x * tz),
            vz + w * tz + (x * ty - y * tx)
        };
    }
}
