package dev.bunnyears;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Кроличьи уши с физикой. Каждое ухо - три ItemDisplay (сегменты), плюс один ItemDisplay для повязки.
 * Положение считает EarRigMath (чистая Java), здесь только работа с сервером.
 */
public final class BunnyEarsPlugin extends JavaPlugin implements Listener {

    /** Игрок в модели вытянут в 0.9375 от размера блока: 1 px модели = 0.9375/16 блока. */
    private static final float SCALE = 0.9375f;
    private static final double PX = SCALE / 16.0;
    private static final double DT = 0.05;

    private final Map<UUID, Rig> rigs = new HashMap<>();
    private EarPhysics.Settings settings = new EarPhysics.Settings();
    private boolean autoEquip;
    private boolean flipItemDisplay;
    private int interpolation;
    private int teleportDuration;
    private double neckBelowEye;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        if (autoEquip) {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.hasPermission("bunnyears.wear")) equip(p);
            }
        }
    }

    @Override
    public void onDisable() {
        for (Rig r : rigs.values()) r.remove();
        rigs.clear();
    }

    // ------------------------------------------------------------------ настройки

    private static double[] arr(List<Double> list, double[] def) {
        if (list == null || list.size() != def.length) return def;
        double[] r = new double[def.length];
        for (int i = 0; i < r.length; i++) r[i] = list.get(i);
        return r;
    }

    private void loadSettings() {
        FileConfiguration c = getConfig();
        EarPhysics.Settings s = new EarPhysics.Settings();
        s.inertia = c.getDouble("physics.inertia", s.inertia);
        s.vertGain = c.getDouble("physics.vertical-gain", s.vertGain);
        s.gravityGain = c.getDouble("physics.gravity-gain", s.gravityGain);
        s.yawLag = c.getDouble("physics.yaw-lag", s.yawLag);
        s.pitchLag = c.getDouble("physics.pitch-lag", s.pitchLag);
        s.flutter = c.getDouble("physics.flutter", s.flutter);
        s.zeta = c.getDouble("physics.damping", s.zeta);
        s.stiff = arr(c.getDoubleList("physics.stiffness"), s.stiff);
        s.gain = arr(c.getDoubleList("physics.segment-gain"), s.gain);
        s.restOut[0] = arr(c.getDoubleList("rest.left.outward"), s.restOut[0]);
        s.restOut[1] = arr(c.getDoubleList("rest.right.outward"), s.restOut[1]);
        s.restPitch[0] = arr(c.getDoubleList("rest.left.back"), s.restPitch[0]);
        s.restPitch[1] = arr(c.getDoubleList("rest.right.back"), s.restPitch[1]);
        settings = s;
        autoEquip = c.getBoolean("auto-equip", true);
        flipItemDisplay = c.getBoolean("compensate-item-display-flip", true);
        interpolation = c.getInt("smoothing.interpolation-ticks", 2);
        teleportDuration = c.getInt("smoothing.teleport-ticks", 3);
        neckBelowEye = c.getDouble("neck-below-eye", 0.21);
    }

    // ------------------------------------------------------------------ события и команды

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (autoEquip && e.getPlayer().hasPermission("bunnyears.wear")) equip(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        unequip(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "toggle";
        if (sub.equals("reload")) {
            if (!sender.hasPermission("bunnyears.admin")) {
                sender.sendMessage("Нет прав.");
                return true;
            }
            reloadConfig();
            loadSettings();
            List<UUID> ids = new ArrayList<>(rigs.keySet());
            for (UUID id : ids) {
                Player p = getServer().getPlayer(id);
                unequip(id);
                if (p != null) equip(p);
            }
            sender.sendMessage("BunnyEars: настройки перезагружены.");
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Команда только для игроков.");
            return true;
        }
        if (!p.hasPermission("bunnyears.wear")) {
            p.sendMessage("Нет прав.");
            return true;
        }
        boolean has = rigs.containsKey(p.getUniqueId());
        boolean want = sub.equals("on") || (sub.equals("toggle") && !has);
        if (want && !has) {
            equip(p);
            p.sendMessage("Ушки надеты.");
        } else if (!want && has) {
            unequip(p);
            p.sendMessage("Ушки сняты.");
        }
        return true;
    }

    // ------------------------------------------------------------------ управление

    private void equip(Player p) {
        if (rigs.containsKey(p.getUniqueId())) return;
        rigs.put(p.getUniqueId(), new Rig(p));
    }

    private void unequip(Player p) {
        unequip(p.getUniqueId());
    }

    private void unequip(UUID id) {
        Rig r = rigs.remove(id);
        if (r != null) r.remove();
    }

    private void tick() {
        Iterator<Map.Entry<UUID, Rig>> it = rigs.entrySet().iterator();
        while (it.hasNext()) {
            Rig r = it.next().getValue();
            if (!r.player.isOnline()) {
                r.remove();
                it.remove();
                continue;
            }
            try {
                r.update();
            } catch (RuntimeException ex) {
                getLogger().warning("Ошибка обновления ушей " + r.player.getName() + ": " + ex);
            }
        }
    }

    // ------------------------------------------------------------------ один комплект ушей

    private final class Rig {
        final Player player;
        final EarRigMath math;
        final ItemDisplay band;
        final ItemDisplay[][] seg = new ItemDisplay[2][3];
        boolean visible = true;

        Rig(Player p) {
            this.player = p;
            this.math = new EarRigMath(settings);
            Location base = p.getLocation();
            base.setYaw(0f);
            base.setPitch(0f);
            band = spawn(base, "headband");
            for (int e = 0; e < 2; e++) {
                for (int s = 0; s < 3; s++) seg[e][s] = spawn(base, "ear_seg" + (s + 1));
            }
        }

        private ItemDisplay spawn(Location base, String model) {
            return base.getWorld().spawn(base, ItemDisplay.class, d -> {
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setItemModel(new NamespacedKey("bunnyears", model));
                item.setItemMeta(meta);
                d.setItemStack(item);
                d.setPersistent(false);
                d.setGravity(false);
                d.setInvulnerable(true);
                d.setTeleportDuration(teleportDuration);
                d.setInterpolationDuration(interpolation);
                d.setViewRange(1.0f);
            });
        }

        private boolean shouldShow() {
            return !player.isDead()
                && player.getGameMode() != GameMode.SPECTATOR
                && !player.hasPotionEffect(PotionEffectType.INVISIBILITY);
        }

        void update() {
            boolean vis = shouldShow();
            if (vis != visible) {
                visible = vis;
                float range = vis ? 1.0f : 0.0f;
                band.setViewRange(range);
                for (ItemDisplay[] row : seg) for (ItemDisplay d : row) d.setViewRange(range);
            }
            Location loc = player.getLocation();
            Location eye = player.getEyeLocation();
            EarRigMath.Pose p = math.update(DT, loc.getX(), loc.getY(), loc.getZ(), eye.getYaw(), eye.getPitch());

            Location base = loc.clone();
            base.setYaw(0f);
            base.setPitch(0f);
            // шея: чуть ниже глаз; считаем относительно положения дисплеев (= ноги игрока)
            Vector3f neck = new Vector3f(
                (float) (eye.getX() - loc.getX()),
                (float) (eye.getY() - neckBelowEye - loc.getY()),
                (float) (eye.getZ() - loc.getZ()));

            apply(band, base, neck, p.bandT, p.bandQ);
            for (int e = 0; e < 2; e++) {
                for (int s = 0; s < 3; s++) apply(seg[e][s], base, neck, p.segT[e][s], p.segQ[e][s]);
            }
        }

        private void apply(ItemDisplay d, Location base, Vector3f neck, double[] t, double[] q) {
            d.teleport(base);
            Vector3f tr = new Vector3f(
                neck.x + (float) (t[0] * PX),
                neck.y + (float) (t[1] * PX),
                neck.z + (float) (t[2] * PX));
            Quaternionf left = new Quaternionf((float) q[0], (float) q[1], (float) q[2], (float) q[3]);
            if (flipItemDisplay) left.rotateY((float) Math.PI);
            d.setTransformation(new Transformation(tr, left, new Vector3f(SCALE, SCALE, SCALE), new Quaternionf()));
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(interpolation);
        }

        void remove() {
            if (band != null) band.remove();
            for (ItemDisplay[] row : seg) for (ItemDisplay d : row) if (d != null) d.remove();
        }
    }
}
