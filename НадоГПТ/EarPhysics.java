package dev.bunnyears;

/**
 * Пружинная физика двух ушей по три сегмента в каждом.
 * Для каждого сегмента храним два угла относительно предыдущего звена:
 *   pitch - наклон вперёд/назад (поворот вокруг оси X, + = кончик назад),
 *   out   - отклонение в сторону от головы (+ = наружу, одинаково для обоих ушей).
 * Ухо 0 - левое (x &lt; 0), ухо 1 - правое (x &gt; 0).
 * Оси локальные, голова смотрит в -Z, X - вправо от игрока, Y - вверх.
 */
public final class EarPhysics {

    public static final class Settings {
        public double inertia = 1.4;       // рад/с^2 на 1 блок/с^2 ускорения головы
        public double vertGain = 3.5;      // реакция на вертикальное ускорение (прыжок/падение)
        public double gravityGain = 1.0;   // насколько гравитация гнёт уши при наклоне головы
        public double yawLag = 0.25;       // отставание при повороте головы влево-вправо
        public double pitchLag = 0.45;     // отставание при кивке вверх-вниз
        public double flutter = 0.012;     // лёгкое трепыхание при беге
        public double flutterFreq = 14.0;
        public double zeta = 0.28;         // коэффициент затухания (меньше = дольше качаются)
        public double[] stiff = {90, 65, 45};
        public double[] gain = {1.0, 1.3, 1.7};
        // положение покоя: левое ухо чуть свисает (кончик), правое стоит
        public double[][] restOut   = {{0.08, 0.20, 0.75}, {0.08, 0.06, 0.05}};
        public double[][] restPitch = {{0.10, 0.12, 0.30}, {0.08, 0.10, 0.12}};
        public double minOut = -0.35, maxOut = 1.60, minPitch = -0.90, maxPitch = 1.35;
    }

    public final Settings st;
    public final double[][] pitch = new double[2][3];
    public final double[][] pitchV = new double[2][3];
    public final double[][] out = new double[2][3];
    public final double[][] outV = new double[2][3];

    public EarPhysics(Settings st) {
        this.st = st;
        reset();
    }

    public void reset() {
        for (int e = 0; e < 2; e++) {
            for (int s = 0; s < 3; s++) {
                pitch[e][s] = st.restPitch[e][s];
                out[e][s] = st.restOut[e][s];
                pitchV[e][s] = 0;
                outV[e][s] = 0;
            }
        }
    }

    /**
     * Один шаг интегрирования.
     * @param aL ускорение головы в локальных осях (блоки/с^2)
     * @param gL вектор гравитации в локальных осях (обычно (0,-9.8,0) повёрнутый вместе с головой)
     */
    public void step(double dt, double[] aL, double[] gL, double yawAcc, double pitchAcc, double speed, double time) {
        for (int e = 0; e < 2; e++) {
            double side = (e == 0) ? 1.0 : -1.0;
            for (int s = 0; s < 3; s++) {
                double k = st.stiff[s];
                double c = 2.0 * st.zeta * Math.sqrt(k);
                double g = st.gain[s];

                double fPitch = st.inertia * (-aL[2]) + st.gravityGain * gL[2] + st.pitchLag * pitchAcc;
                double fRoll = st.inertia * aL[0] - st.gravityGain * gL[0] + st.yawLag * yawAcc;
                double fOut = side * fRoll + st.vertGain * aL[1];
                double fl = st.flutter * speed * Math.sin(time * st.flutterFreq + e * 1.7 + s * 0.9);

                double accP = k * (st.restPitch[e][s] - pitch[e][s]) - c * pitchV[e][s] + g * fPitch;
                double accO = k * (st.restOut[e][s] - out[e][s]) - c * outV[e][s] + g * (fOut + fl);

                pitchV[e][s] += accP * dt;
                pitch[e][s] += pitchV[e][s] * dt;
                outV[e][s] += accO * dt;
                out[e][s] += outV[e][s] * dt;

                if (pitch[e][s] > st.maxPitch) { pitch[e][s] = st.maxPitch; if (pitchV[e][s] > 0) pitchV[e][s] = 0; }
                if (pitch[e][s] < st.minPitch) { pitch[e][s] = st.minPitch; if (pitchV[e][s] < 0) pitchV[e][s] = 0; }
                if (out[e][s] > st.maxOut) { out[e][s] = st.maxOut; if (outV[e][s] > 0) outV[e][s] = 0; }
                if (out[e][s] < st.minOut) { out[e][s] = st.minOut; if (outV[e][s] < 0) outV[e][s] = 0; }
            }
        }
    }
}
