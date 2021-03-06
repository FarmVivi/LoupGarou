package fr.leomelki.loupgarou.classes;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityLook;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import fr.leomelki.loupgarou.classes.LGGame.TextGenerator;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGVoteLeaderChange;
import fr.leomelki.loupgarou.utils.VariousUtils;
import lombok.Getter;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public class LGVote {
    private static final EntityArmorStand eas = new EntityArmorStand(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(),
            0, 0, 0);
    private static DataWatcherObject<String> aB;
    private static DataWatcherObject<Boolean> aC;
    private static DataWatcherObject<Byte> Z;

    static {
        try {
            Field f = Entity.class.getDeclaredField("aB");
            f.setAccessible(true);
            aB = (DataWatcherObject<String>) f.get(null);
            f = Entity.class.getDeclaredField("aC");
            f.setAccessible(true);
            aC = (DataWatcherObject<Boolean>) f.get(null);
            f = Entity.class.getDeclaredField("Z");
            f.setAccessible(true);
            Z = (DataWatcherObject<Byte>) f.get(null);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private final int initialTimeout;
    private final int littleTimeout;
    private final LGGame game;
    private final TextGenerator generator;
    @Getter
    private final HashMap<LGPlayer, List<LGPlayer>> votes = new HashMap<>();
    private final boolean randomIfEqual;
    @Getter
    LGPlayer choosen;
    WrappedDataWatcherObject invisible = new WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
    WrappedDataWatcherObject noGravity = new WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class));
    private int timeout;
    private Runnable callback;
    @Getter
    private List<LGPlayer> participants;
    @Getter
    private List<LGPlayer> viewers;
    private int votesSize = 0;
    private LGPlayer mayor;
    private List<LGPlayer> latestTop = new ArrayList<>();
    private List<LGPlayer> blacklisted = new ArrayList<>();
    @Getter
    private boolean mayorVote;
    private boolean ended;

    public LGVote(int timeout, int littleTimeout, LGGame game, boolean randomIfEqual, TextGenerator generator) {
        this.littleTimeout = littleTimeout;
        this.initialTimeout = timeout;
        this.timeout = timeout;
        this.game = game;
        this.generator = generator;
        this.randomIfEqual = randomIfEqual;
    }

    public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback) {
        this.callback = callback;
        this.participants = participants;
        this.viewers = viewers;
        game.wait(timeout, this::end, generator);
        for (LGPlayer player : participants)
            player.choose(getChooseCallback(player));
    }

    public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback,
                      List<LGPlayer> blacklisted) {
        this.callback = callback;
        this.participants = participants;
        this.viewers = viewers;
        game.wait(timeout, this::end, generator);
        for (LGPlayer player : participants)
            player.choose(getChooseCallback(player));
        this.blacklisted = blacklisted;
    }

    public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback, LGPlayer mayor) {
        this.callback = callback;
        this.participants = participants;
        this.viewers = viewers;
        this.mayor = mayor;
        game.wait(timeout, this::end, generator);
        for (LGPlayer player : participants)
            player.choose(getChooseCallback(player));
    }

    private void end() {
        ended = true;
        for (LGPlayer lgp : viewers)
            showVoting(lgp, null);
        for (LGPlayer lgp : votes.keySet())
            updateVotes(lgp, true);
        int max = 0;
        boolean equal = false;
        for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
            if (entry.getValue().size() > max) {
                equal = false;
                max = entry.getValue().size();
                choosen = entry.getKey();
            } else if (entry.getValue().size() == max) {
                equal = true;
            }
        for (LGPlayer player : participants) {
            player.getCache().remove("vote");
            player.stopChoosing();
        }
        if (equal)
            choosen = null;
        if (equal && mayor == null && randomIfEqual) {
            ArrayList<LGPlayer> choosable = new ArrayList<>();
            for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if (entry.getValue().size() == max)
                    choosable.add(entry.getKey());
            choosen = choosable.get(game.getRandom().nextInt(choosable.size()));
        }

        if (equal && mayor != null && max != 0) {
            for (LGPlayer player : viewers)
                player.sendMessage("§9Égalité, le §5§lCapitaine§9 va départager les votes.");
            mayor.sendMessage("§6Tu dois choisir qui va mourir.");

            ArrayList<LGPlayer> choosable = new ArrayList<>();
            for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if (entry.getValue().size() == max)
                    choosable.add(entry.getKey());

            for (int i = 0; i < choosable.size(); i++) {
                LGPlayer lgp = choosable.get(i);
                showArrow(mayor, lgp, -mayor.getPlayer().getEntityId() - i);
            }

            StringJoiner sj = new StringJoiner(", ");
            for (int i = 0; i < choosable.size() - 1; i++)
                sj.add(choosable.get(0).getName());
            ArrayList<LGPlayer> blackListed = new ArrayList<>();
            for (LGPlayer player : participants)
                if (!choosable.contains(player))
                    blackListed.add(player);
                else {
                    VariousUtils.setWarning(player.getPlayer(), true);
                }
            mayorVote = true;
            game.wait(30, () -> {
                for (LGPlayer player : participants)
                    if (choosable.contains(player))
                        VariousUtils.setWarning(player.getPlayer(), false);

                for (int i = 0; i < choosable.size(); i++) {
                    showArrow(mayor, null, -mayor.getPlayer().getEntityId() - i);
                }
                // Choix au hasard d'un joueur si personne n'a été désigné
                choosen = choosable.get(game.getRandom().nextInt(choosable.size()));
                callback.run();
            }, (player, secondsLeft) -> {
                timeout = secondsLeft;
                return mayor == player
                        ? "§6Il te reste §e" + secondsLeft + " seconde" + (secondsLeft > 1 ? "s" : "") + "§6 pour délibérer"
                        : "§6Le §5§lCapitaine§6 délibère (§e" + secondsLeft + " s§6)";
            });
            mayor.choose(choosen -> {
                if (choosen != null) {
                    if (blackListed.contains(choosen))
                        mayor.sendMessage("§4§oCe joueur n'est pas concerné par le choix.");
                    else {
                        for (LGPlayer player : participants)
                            if (choosable.contains(player))
                                VariousUtils.setWarning(player.getPlayer(), false);

                        for (int i = 0; i < choosable.size(); i++) {
                            showArrow(mayor, null, -mayor.getPlayer().getEntityId() - i);
                        }
                        game.cancelWait();
                        LGVote.this.choosen = choosen;
                        callback.run();
                    }
                }
            });
        } else {
            game.cancelWait();
            callback.run();
        }

    }

    public LGChooseCallback getChooseCallback(LGPlayer who) {
        return choosen -> {
            if (choosen != null)
                vote(who, choosen);
        };
    }

    public void vote(LGPlayer voter, LGPlayer voted) {
        if (blacklisted.contains(voted)) {
            voter.sendMessage("§cVous ne pouvez pas voter pour §7§l" + voted.getFullName() + "§c.");
            return;
        }
        if (voted == voter.getCache().get("vote"))
            voted = null;

        if (voted != null && voter.getPlayer() != null)
            votesSize++;
        if (voter.getCache().has("vote"))
            votesSize--;

        if (votesSize == participants.size() && game.getWaitTicks() > littleTimeout * 20) {
            votesSize = 999;
            game.wait(littleTimeout, initialTimeout, this::end, generator);
        }
        boolean changeVote = false;
        if (voter.getCache().has("vote")) {// On enlève l'ancien vote
            LGPlayer devoted = voter.getCache().get("vote");
            if (votes.containsKey(devoted)) {
                List<LGPlayer> voters = votes.get(devoted);
                if (voters != null) {
                    voters.remove(voter);
                    if (voters.isEmpty())
                        votes.remove(devoted);
                }
            }
            voter.getCache().remove("vote");
            updateVotes(devoted);
            changeVote = true;
        }

        if (voted != null) {// Si il vient de voter, on ajoute le nouveau vote
            if (votes.containsKey(voted))
                votes.get(voted).add(voter);
            else
                votes.put(voted, new ArrayList<>(Collections.singletonList(voter)));
            voter.getCache().set("vote", voted);
            updateVotes(voted);
        }

        if (voter.getPlayer() != null) {
            showVoting(voter, voted);

            String message;
            final String voterName = voter.getFullName();

            if (voted != null) {
                final String targetName = voted.getFullName();

                if (changeVote) {
                    message = "§7§l" + voterName + "§6 a changé son vote pour §7§l" + targetName + "§6.";
                    voter.sendMessage("§6Tu as changé de vote pour §7§l" + targetName + "§6.");
                } else {
                    message = "§7§l" + voterName + "§6 a voté pour §7§l" + targetName + "§6.";
                    voter.sendMessage("§6Tu as voté pour §7§l" + targetName + "§6.");
                }
            } else {
                message = "§7§l" + voterName + "§6 a annulé son vote.";
                voter.sendMessage("§6Tu as annulé ton vote.");
            }

            //TODO Nouveau système pour afficher qui vote qui
            for (LGPlayer player : viewers)
                if (player != voter)
                    player.sendMessage(message);
        }
    }

    public List<LGPlayer> getVotes(LGPlayer voted) {
        return votes.containsKey(voted) ? votes.get(voted) : new ArrayList<>(0);
    }

    private void updateVotes(LGPlayer voted) {
        updateVotes(voted, false);
    }

    private void updateVotes(LGPlayer voted, boolean kill) {
        int entityId = Integer.MIN_VALUE + voted.getPlayer().getEntityId();
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[]{entityId});
        for (LGPlayer lgp : viewers)
            destroy.sendPacket(lgp.getPlayer());

        if (!kill) {
            int max = 0;
            for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if (entry.getValue().size() > max)
                    max = entry.getValue().size();
            List<LGPlayer> last = latestTop;
            latestTop = new ArrayList<>();
            for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if (entry.getValue().size() == max)
                    latestTop.add(entry.getKey());
            Bukkit.getPluginManager().callEvent(new LGVoteLeaderChange(game, this, last, latestTop));
        }

        if (votes.containsKey(voted) && !kill) {
            Location loc = voted.getPlayer().getLocation();

            WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
            spawn.setEntityID(entityId);
            spawn.setType(EntityType.ARMOR_STAND);
            spawn.setX(loc.getX());
            spawn.setY(loc.getY() + 0.3);
            spawn.setZ(loc.getZ());

            int votesNbr = votes.get(voted).size();
            final int numberOfParticipants = participants.size();
            final double votePercentage = ((double) votesNbr / numberOfParticipants) * 100;
            final String votePercentageFormated = String.format("%.0f%%", votePercentage);
            final String voteContent = "§6§l" + votesNbr + "§e vote" + (votesNbr > 1 ? "s" : "") + " (§6§l" + votePercentageFormated + "§e)";

            DataWatcher datawatcher = new DataWatcher(eas);
            datawatcher.register(Z, (byte) 0x20);
            datawatcher.register(aB, voteContent);
            datawatcher.register(aC, true);
            PacketPlayOutEntityMetadata meta = new PacketPlayOutEntityMetadata(entityId, datawatcher, true);

            for (LGPlayer lgp : viewers) {
                spawn.sendPacket(lgp.getPlayer());
                ((CraftPlayer) lgp.getPlayer()).getHandle().playerConnection.sendPacket(meta);
            }
        }
    }

    private void showVoting(LGPlayer to, LGPlayer ofWho) {
        int entityId = -to.getPlayer().getEntityId();
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[]{entityId});
        destroy.sendPacket(to.getPlayer());
        if (ofWho != null) {
            WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
            spawn.setEntityID(entityId);
            spawn.setType(EntityType.ARMOR_STAND);
            Location loc = ofWho.getPlayer().getLocation();
            spawn.setX(loc.getX());
            spawn.setY(loc.getY() + 1.3);
            spawn.setZ(loc.getZ());
            spawn.setHeadPitch(0);
            Location toLoc = to.getPlayer().getLocation();
            double diffX = loc.getX() - toLoc.getX();
            double diffZ = loc.getZ() - toLoc.getZ();
            float yaw = 180 - ((float) Math.toDegrees(Math.atan2(diffX, diffZ)));

            spawn.setYaw(yaw);
            spawn.sendPacket(to.getPlayer());

            WrapperPlayServerEntityMetadata meta = new WrapperPlayServerEntityMetadata();
            meta.setEntityID(entityId);
            meta.setMetadata(Arrays.asList(new WrappedWatchableObject(invisible, (byte) 0x20),
                    new WrappedWatchableObject(noGravity, true)));
            meta.sendPacket(to.getPlayer());

            WrapperPlayServerEntityLook look = new WrapperPlayServerEntityLook();
            look.setEntityID(entityId);
            look.setPitch(0);
            look.setYaw(yaw);
            look.sendPacket(to.getPlayer());
        }
    }

    private void showArrow(LGPlayer to, LGPlayer ofWho, int entityId) {
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[]{entityId});
        destroy.sendPacket(to.getPlayer());
        if (ofWho != null) {
            WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
            spawn.setEntityID(entityId);
            spawn.setType(EntityType.ARMOR_STAND);
            Location loc = ofWho.getPlayer().getLocation();
            spawn.setX(loc.getX());
            spawn.setY(loc.getY() + 1.3);
            spawn.setZ(loc.getZ());
            spawn.setHeadPitch(0);
            Location toLoc = to.getPlayer().getLocation();
            double diffX = loc.getX() - toLoc.getX();
            double diffZ = loc.getZ() - toLoc.getZ();
            float yaw = 180 - ((float) Math.toDegrees(Math.atan2(diffX, diffZ)));

            spawn.setYaw(yaw);
            spawn.sendPacket(to.getPlayer());

            WrapperPlayServerEntityMetadata meta = new WrapperPlayServerEntityMetadata();
            meta.setEntityID(entityId);
            meta.setMetadata(Arrays.asList(new WrappedWatchableObject(invisible, (byte) 0x20),
                    new WrappedWatchableObject(noGravity, true)));
            meta.sendPacket(to.getPlayer());

            WrapperPlayServerEntityLook look = new WrapperPlayServerEntityLook();
            look.setEntityID(entityId);
            look.setPitch(0);
            look.setYaw(yaw);
            look.sendPacket(to.getPlayer());
        }
    }

    public void remove(LGPlayer killed) {
        participants.remove(killed);
        if (!ended) {
            votes.remove(killed);
            latestTop.remove(killed);
        }
    }
}
