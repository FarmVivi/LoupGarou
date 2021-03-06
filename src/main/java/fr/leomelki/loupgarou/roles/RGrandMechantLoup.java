package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import org.bukkit.event.EventHandler;

public class RGrandMechantLoup extends Role {
    boolean lgDied;
    Runnable callback;

    public RGrandMechantLoup(LGGame game) {
        super(game);
    }

    @Override
    public String getRawName() {
        return "GrandMechantLoup";
    }

    @Override
    public String getName(int amount) {
        return (amount > 1) ? "§c§lGrands Méchants Loups" : this.getName();
    }

    @Override
    public String getName() {
        return "§c§lGrand Méchant Loup";
    }

    @Override
    public String getFriendlyName() {
        return "du " + getName();
    }

    @Override
    public String getShortDescription() {
        return "Tu gagnes avec les §c§lLoups-Garous";
    }

    @Override
    public String getDescription() {
        return "Tu gagnes avec les §c§lLoups-Garous§f. Chaque nuit, tu te réunis avec tes compères pour décider d'une victime à éliminer... Tant qu'aucun autre §c§lLoup§f n'est mort, tu peux, chaque nuit, dévorer une victime supplémentaire.";
    }

    @Override
    public String getTask() {
        return "Choisis un joueur à dévorer.";
    }

    @Override
    public String getBroadcastedTask() {
        return "Le §c§lGrand Méchant Loup§9 n'en a pas terminé...";
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
        return 15;
    }

    @Override
    public boolean hasPlayersLeft() {
        return super.hasPlayersLeft() && !lgDied;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        this.callback = callback;

        player.showView();
        player.choose(choosen -> {
            if (choosen != null && choosen != player) {
                player.sendActionBarMessage("§e§l" + choosen.getFullName() + "§6 va mourir cette nuit");
                player.sendMessage("§6Tu as choisi de manger §7§l" + choosen.getFullName() + "§6.");
                getGame().kill(choosen,
                        getGame().getDeaths().containsKey(Reason.LOUP_GAROU) ? Reason.GM_LOUP_GAROU : Reason.LOUP_GAROU);
                player.stopChoosing();
                player.hideView();
                callback.run();
            }
        });
    }

    @EventHandler
    public void onPlayerDie(LGPlayerKilledEvent e) {// Quand un Loup-Garou meurt, les grands méchants loups ne peuvent
        // plus jouer.
        if (e.getGame() == getGame() && e.getKilled().getRoleType() == RoleType.LOUP_GAROU)
            lgDied = true;
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.stopChoosing();
        player.hideView();
        player.sendMessage("§6Tu n'as tué personne.");
    }

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        for (Role role : getGame().getRoles())
            if (role instanceof RLoupGarou)
                role.join(player, false);
    }
}
