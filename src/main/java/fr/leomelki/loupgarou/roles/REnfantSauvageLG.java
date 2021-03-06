package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;

public class REnfantSauvageLG extends Role {
    public REnfantSauvageLG(LGGame game) {
        super(game);
    }

    @Override
    public String getRawName() {
        return "EnfantSauvageLg";
    }

    @Override
    public String getName(int amount) {
        final String baseline = this.getName();

        return (amount > 1) ? baseline.replace("nfant-", "nfants-") : baseline;
    }

    @Override
    public String getName() {
        for (LGPlayer lgp : getPlayers())
            if (lgp.getPlayer() != null && lgp.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY))
                return "§c§lEnfant-Sauvage";
        return (!getPlayers().isEmpty() ? "§a" : "§c") + "§lEnfant-Sauvage";
    }

    @Override
    public String getFriendlyName() {
        return "de l'" + getName();
    }

    @Override
    public String getShortDescription() {
        return "Tu gagnes avec le §a§lVillage";
    }

    @Override
    public String getDescription() {
        return "Tu gagnes avec le §a§lVillage§f. Au début de la première nuit, tu dois choisir un joueur comme modèle. S'il meurt au cours de la partie, tu deviendras un §c§lLoup-Garou§f.";
    }

    @Override
    public String getTask() {
        return "Qui veux-tu prendre comme modèle ?";
    }

    @Override
    public String getBroadcastedTask() {
        return "L'" + getName() + "§9 cherche ses marques...";
    }

    @Override
    public RoleType getType() {
        return RoleType.LOUP_GAROU;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.LOUP_GAROU;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        player.setRole(this);
        LGCustomItems.updateItem(player);
        RLoupGarou lgRole = null;
        for (Role role : getGame().getRoles())
            if (role instanceof RLoupGarou)
                lgRole = (RLoupGarou) role;

        if (lgRole == null) {
            lgRole = new RLoupGarou(getGame());
            getGame().getRoles().add(lgRole);

            getGame().getRoles().sort(Comparator.comparingInt(Role::getTurnOrder));
        }

        lgRole.join(player, false);
        for (LGPlayer lgp : lgRole.getPlayers())
            if (lgp != player)
                lgp.sendMessage("§7§l" + player.getFullName() + "§6 a rejoint les §c§lLoups-Garous§6.");
    }
}
