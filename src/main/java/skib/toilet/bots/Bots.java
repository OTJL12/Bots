package skib.toilet.bots;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Bots extends JavaPlugin {

    private final List<NPC> npcs = new ArrayList<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int count = getConfig().getInt("bot-count");
        List<String> names = getConfig().getStringList("bot-names");

        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().severe("Citizens not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        World world = getServer().getWorlds().get(0);
        Location spawn = world.getSpawnLocation();

        for (int i = 0; i < count; i++) {
            String name = names.get(random.nextInt(names.size()));
            NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
            npc.spawn(spawn);
            npcs.add(npc);

            // Delay 1 tick to ensure entity is fully spawned, then set tab list name
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (npc.isSpawned() && npc.getEntity() instanceof Player playerEntity) {
                    playerEntity.setCustomNameVisible(true);
                    playerEntity.setPlayerListName(npc.getName());
                    // Optionally, also show nameplate above head
                    playerEntity.setCustomName(npc.getName());
                }
            }, 1L);

            // Schedule random movement as before
            scheduleRandomMovement(npc);
        }
    }

    private void scheduleRandomMovement(NPC npc) {
        long delayTicks = 10L + (random.nextInt(3) * 20L); // 2-6 seconds

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (npc.isSpawned()) {
                if (Math.random() < 0.7) { // 70% move
                    Location loc = npc.getEntity().getLocation();
                    double dx = (Math.random() - 0.5) * 10;
                    double dz = (Math.random() - 0.5) * 10;
                    Location target = loc.clone().add(dx, 0, dz);
                    target.setY(target.getWorld().getHighestBlockYAt(target) + 1);

                    npc.getNavigator().getDefaultParameters()
                            .range(16)
                            .speedModifier(1.0F)
                            .baseSpeed(1.0F)
                            .stuckAction((npc1, path) -> {
                                Location stuckLoc = npc1.getEntity().getLocation();
                                Location newTarget = stuckLoc.clone().add(
                                        (Math.random() - 0.5) * 8,
                                        0,
                                        (Math.random() - 0.5) * 8
                                );
                                npc1.getNavigator().setTarget(newTarget);
                                return true;
                            });

                    npc.getNavigator().setTarget(target);
                } else {
                    npc.faceLocation(npc.getEntity().getLocation().add(
                            (Math.random() - 0.5) * 5, 0, (Math.random() - 0.5) * 5
                    ));
                }
                // Schedule next movement
                scheduleRandomMovement(npc);
            }
        }, delayTicks);
    }

    @Override
    public void onDisable() {
        npcs.forEach(NPC::despawn);
        CitizensAPI.getNPCRegistry().deregisterAll();
    }
}
