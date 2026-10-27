package dev.bunnyears;

/**
 * Превращает движение игрока (позиция, поворот головы) в положение повязки и сегментов ушей.
 * Все длины здесь в «пикселях модели» (1 px = 1/16 блока до масштаба игрока 0.9375),
 * начало координат - шея игрока. Плагин переводит это в блоки и применяет к ItemDisplay.
 */
public final class EarRigMath {

    /** Положение ушей на голове (пиксели, относительно центра головы). */
    public static final double EAR_X = 2.4;
    public static final double EAR_JOINT_Y = 6.0;
    /** Длины сегментов (пиксели) - должны совпадать с моделями ear_seg1..3. */
    public static final double[] SEG_LEN = {4.6, 4.8, 4.6};

    public static final class Pose {
        public final double[] bandT = new double[3];
        public final double[] bandQ = new double[4];            // x, y, z, w
        public final double[][][] segT = new double[2][3][3];
        public final double[][][] segQ = new double[2][3][4];   // x, y, z, w
    }

    private final EarPhysics phys;
    private final EarPhysics.Settings st;
    private boolean init = false;
    private double lx, ly, lz, lvx, lvy, lvz, lyaw, lpitch, lyawRate, lpitchRate;
    private double ax, ay, az, yawAcc, pitchAcc, time;
    private double maxAccel = 60.0;

    public EarRigMath(EarPhysics.Settings st) {
        this.st = st;
        this.phys = new EarPhysics(st);
    }

    public EarPhysics physics() { return phys; }

    private static double wrapDeg(double d) {
        d = d % 360.0;
        if (d > 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    public Pose update(double dt, double x, double y, double z, double yawDeg, double pitchDeg) {
        if (!init) {
            lx = x; ly = y; lz = z; lyaw = yawDeg; lpitch = pitchDeg;
            lvx = lvy = lvz = 0; lyawRate = lpitchRate = 0;
            init = true;
        }
        double vx = (x - lx) / dt, vy = (y - ly) / dt, vz = (z - lz) / dt;
        double sp = Math.sqrt(vx * vx + vy * vy + vz * vz);
        boolean jump = sp > 40.0;                 // телепорт: не считаем это ускорением
        if (jump) { vx = vy = vz = 0; lvx = lvy = lvz = 0; }
        double rax = (vx - lvx) / dt, ray = (vy - lvy) / dt, raz = (vz - lvz) / dt;
        double am = Math.sqrt(rax * rax + ray * ray + raz * raz);
        if (am > maxAccel) { double f = maxAccel / am; rax *= f; ray *= f; raz *= f; }
        ax += (rax - ax) * 0.5; ay += (ray - ay) * 0.5; az += (raz - az) * 0.5;

        double yawRate = Math.toRadians(wrapDeg(yawDeg - lyaw)) / dt;
        double pitchRate = Math.toRadians(pitchDeg - lpitch) / dt;
        double ryAcc = clamp((yawRate - lyawRate) / dt, 120.0);
        double rpAcc = clamp((pitchRate - lpitchRate) / dt, 120.0);
        yawAcc += (ryAcc - yawAcc) * 0.5;
        pitchAcc += (rpAcc - pitchAcc) * 0.5;

        lx = x; ly = y; lz = z; lvx = vx; lvy = vy; lvz = vz;
        lyaw = yawDeg; lpitch = pitchDeg; lyawRate = yawRate; lpitchRate = pitchRate;

        // поворот головы: перед модели (-Z) должен смотреть туда же, куда смотрит игрок
        Quat qHead = Quat.rotY(Math.PI - Math.toRadians(yawDeg)).mul(Quat.rotX(-Math.toRadians(pitchDeg)));
        Quat inv = qHead.conj();
        double[] aL = inv.rotate(ax, ay, az);
        double[] gL = inv.rotate(0, -9.8, 0);
        double speed = Math.sqrt(vx * vx + vz * vz);

        double h = dt / 2.0;
        for (int i = 0; i < 2; i++) {
            time += h;
            phys.step(h, aL, gL, yawAcc, pitchAcc, speed, time);
        }

        Pose p = new Pose();
        double[] bc = qHead.rotate(0, 4.0, 0);
        System.arraycopy(bc, 0, p.bandT, 0, 3);
        p.bandQ[0] = qHead.x; p.bandQ[1] = qHead.y; p.bandQ[2] = qHead.z; p.bandQ[3] = qHead.w;

        for (int e = 0; e < 2; e++) {
            double side = (e == 0) ? 1.0 : -1.0;
            double sx = (e == 0) ? -EAR_X : EAR_X;
            double[] joint = qHead.rotate(sx, 4.0 + EAR_JOINT_Y, 0);
            Quat q = qHead;
            for (int s = 0; s < 3; s++) {
                double beta = side * phys.out[e][s];
                q = q.mul(Quat.rotZ(beta)).mul(Quat.rotX(phys.pitch[e][s]));
                p.segT[e][s][0] = joint[0]; p.segT[e][s][1] = joint[1]; p.segT[e][s][2] = joint[2];
                p.segQ[e][s][0] = q.x; p.segQ[e][s][1] = q.y; p.segQ[e][s][2] = q.z; p.segQ[e][s][3] = q.w;
                double[] d = q.rotate(0, SEG_LEN[s], 0);
                joint[0] += d[0]; joint[1] += d[1]; joint[2] += d[2];
            }
        }
        return p;
    }

    private static double clamp(double v, double m) { return Math.max(-m, Math.min(m, v)); }
}
