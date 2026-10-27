import dev.bunnyears.*;
import java.util.*;

/** Прогон сценариев без сервера: печатает JSON с позами ушей (для эскизов) и ключевые углы. */
public class PhysicsDemo {
    static StringBuilder json = new StringBuilder("{");
    static boolean first = true;
    static void dump(String name, EarRigMath.Pose p, EarPhysics ph) {
        if (!first) json.append(","); first = false;
        json.append("\"").append(name).append("\":{");
        json.append("\"bandQ\":").append(Arrays.toString(p.bandQ)).append(",\"bandT\":").append(Arrays.toString(p.bandT)).append(",\"segT\":[");
        for (int e = 0; e < 2; e++) { if (e > 0) json.append(","); json.append("["); for (int s = 0; s < 3; s++) { if (s > 0) json.append(","); json.append(Arrays.toString(p.segT[e][s])); } json.append("]"); }
        json.append("],\"segQ\":[");
        for (int e = 0; e < 2; e++) { if (e > 0) json.append(","); json.append("["); for (int s = 0; s < 3; s++) { if (s > 0) json.append(","); json.append(Arrays.toString(p.segQ[e][s])); } json.append("]"); }
        json.append("],\"pitch\":").append(Arrays.deepToString(ph.pitch)).append(",\"out\":").append(Arrays.deepToString(ph.out)).append("}");
        System.out.printf("%-14s L pitch %s out %s | R pitch %s out %s%n", name,
            fmt(ph.pitch[0]), fmt(ph.out[0]), fmt(ph.pitch[1]), fmt(ph.out[1]));
    }
    static String fmt(double[] a) { return String.format("[%.2f %.2f %.2f]", a[0], a[1], a[2]); }

    static final double DT = 0.05;

    public static void main(String[] args) {
        EarPhysics.Settings st = new EarPhysics.Settings();
        EarRigMath rig = new EarRigMath(st);
        double x = 0, y = 64, z = 0, vx = 0, vy = 0, vz = 0, yaw = 0, pitch = 0;
        EarRigMath.Pose p = null;
        // 1. покой
        for (int t = 0; t < 80; t++) p = rig.update(DT, x, y, z, yaw, pitch);
        dump("rest", p, rig.physics());
        // 2. разбег вперёд (yaw 0 = лицом в +Z)
        for (int t = 0; t < 8; t++) { vz += 12 * DT; z += vz * DT; p = rig.update(DT, x, y, z, yaw, pitch); if (t == 5) dump("run_start", p, rig.physics()); }
        for (int t = 0; t < 40; t++) { z += vz * DT; p = rig.update(DT, x, y, z, yaw, pitch); }
        dump("run_steady", p, rig.physics());
        // 3. резкая остановка
        for (int t = 0; t < 6; t++) { vz = Math.max(0, vz - 20 * DT); z += vz * DT; p = rig.update(DT, x, y, z, yaw, pitch); if (t == 4) dump("stop", p, rig.physics()); }
        for (int t = 0; t < 120; t++) p = rig.update(DT, x, y, z, yaw, pitch);
        dump("settled", p, rig.physics());
        // 4. прыжок
        double vyj = 0.42;
        for (int t = 0; t < 10; t++) { y += vyj; vyj = (vyj - 0.08) * 0.98; p = rig.update(DT, x, y, z, yaw, pitch); if (t == 2) dump("jump", p, rig.physics()); }
        for (int t = 0; t < 100; t++) { p = rig.update(DT, x, y, z, yaw, pitch); }
        // 5. поворот головы вправо (yaw растёт)
        for (int t = 0; t < 5; t++) { yaw += 14; p = rig.update(DT, x, y, z, yaw, pitch); if (t == 3) dump("turn", p, rig.physics()); }
        for (int t = 0; t < 100; t++) { p = rig.update(DT, x, y, z, yaw, pitch); }
        // 6. кивок вниз
        for (int t = 0; t < 4; t++) { pitch += 12; p = rig.update(DT, x, y, z, yaw, pitch); if (t == 3) dump("nod_down", p, rig.physics()); }
        for (int t = 0; t < 20; t++) p = rig.update(DT, x, y, z, yaw, pitch);
        dump("look_down", p, rig.physics());
        json.append("}");
        try { java.nio.file.Files.writeString(java.nio.file.Path.of(args.length > 0 ? args[0] : "poses.json"), json.toString()); } catch (Exception ex) { throw new RuntimeException(ex); }
    }
}
