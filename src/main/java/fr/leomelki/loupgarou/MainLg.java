package fr.leomelki.loupgarou;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityEquipment;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerNamedSoundEffect;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerUpdateHealth;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerUpdateTime;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGStats;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGSkinLoadEvent;
import fr.leomelki.loupgarou.events.LGUpdatePrefixEvent;
import fr.leomelki.loupgarou.listeners.CancelListener;
import fr.leomelki.loupgarou.listeners.ChatListener;
import fr.leomelki.loupgarou.listeners.JoinListener;
import fr.leomelki.loupgarou.listeners.LoupGarouListener;
import fr.leomelki.loupgarou.listeners.PlayerInteractListener;
import fr.leomelki.loupgarou.listeners.VoteListener;
import fr.leomelki.loupgarou.roles.RAnge;
import fr.leomelki.loupgarou.roles.RAssassin;
import fr.leomelki.loupgarou.roles.RBouffon;
import fr.leomelki.loupgarou.roles.RChaperonRouge;
import fr.leomelki.loupgarou.roles.RChasseur;
import fr.leomelki.loupgarou.roles.RChasseurDeVampire;
import fr.leomelki.loupgarou.roles.RChienLoup;
import fr.leomelki.loupgarou.roles.RCorbeau;
import fr.leomelki.loupgarou.roles.RCupidon;
import fr.leomelki.loupgarou.roles.RDetective;
import fr.leomelki.loupgarou.roles.RDictateur;
import fr.leomelki.loupgarou.roles.REnfantSauvage;
import fr.leomelki.loupgarou.roles.RFaucheur;
import fr.leomelki.loupgarou.roles.RGarde;
import fr.leomelki.loupgarou.roles.RGrandMechantLoup;
import fr.leomelki.loupgarou.roles.RLoupGarou;
import fr.leomelki.loupgarou.roles.RLoupGarouBlanc;
import fr.leomelki.loupgarou.roles.RLoupGarouNoir;
import fr.leomelki.loupgarou.roles.RMedium;
import fr.leomelki.loupgarou.roles.RMontreurDOurs;
import fr.leomelki.loupgarou.roles.RPetiteFille;
import fr.leomelki.loupgarou.roles.RPirate;
import fr.leomelki.loupgarou.roles.RPretre;
import fr.leomelki.loupgarou.roles.RPyromane;
import fr.leomelki.loupgarou.roles.RSorciere;
import fr.leomelki.loupgarou.roles.RSurvivant;
import fr.leomelki.loupgarou.roles.RVampire;
import fr.leomelki.loupgarou.roles.RVillageois;
import fr.leomelki.loupgarou.roles.RVoyante;
import fr.leomelki.loupgarou.roles.Role;
import fr.leomelki.loupgarou.utils.ItemBuilder;
import lombok.Getter;
import lombok.Setter;

public class MainLg extends JavaPlugin {
	private static MainLg instance;
	@Getter private final HashMap<String, Constructor<? extends Role>> rolesBuilder = new HashMap<>();
	@Getter private static String prefix = ""/*"§7[§9Loup-Garou§7] "*/;
	@Getter @Setter private LGGame currentGame;//Because for now, only one game will be playable on one server (flemme)

	public static FileConfiguration nicksFile;
	private List<String> startingMemes;
	private LGStats stats;
	private static final String DISTRIBUTION_FIXED_KEY = "distributionFixed.";
	
	@Override
	public void onEnable() {
		instance = this;
		loadRolesBuilder();
		FileConfiguration config = getConfig();
		if(!new File(getDataFolder(), "config.yml").exists()) {
			config.set("showScoreboard", true);
			config.set("roleDistribution", "fixed");
			config.set("distributionRandom.villageRoles", 5);
			config.set("distributionRandom.evilRoles", 3);
			config.set("distributionRandom.neutralRoles", 1);

			// Nombre de participant pour chaque rôle
			for(String role : rolesBuilder.keySet()) {
				config.set(DISTRIBUTION_FIXED_KEY + role, 1);
			}
			
			config.set("startingMemes", new ArrayList<String>(Arrays.asList(
				"Appuyez sur §bALT+F4§f pour débloquer un skin unique. Cette offre expirera dans 20 minutes.",
				"Appuyez sur §bF§f pour présenter vos condoléances",
				"Brossez-vous les dents après chaque repas. Surtout si vous êtes un §bloup-garou§f.",
				"Connaissez-vous le jeu gratuit §bPath Of Exile§f ?",
				"Contrairement aux idées reçues, même vos §bmeilleurs amis§f n'auront aucun scrupule à vous immoler ou vous jetter sous un bus à la première occasion",
				"J'ai vu, je sais qui c'est, mais je ne dirai rien. Surtout pas à vous.",
				"La §bsorcière§f ne vous sauvera pas : elle ne vous aime pas et ne vous a jamais aimé.",
				"La sauce barbecue est la meilleure pour vos grillades. Ce message est sponsorisé par votre §bpyromane§f local.",
				"Les loup-garous tue toujours les mecs §ben face d'eux§f. Sauf quand ils ne le font pas.",
				"Mangez 5 fruits et légumes par jour. Si vous êtes loup-garou, ajoutez un villagois.",
				"Ne dévoilez pas votre innocence trop vite, vous risqueriez de vos faire §bdévorer très fort§f",
				"Pour déconner, le serveur tuera automatiquement la §b1ère personne qui votera§f.",
				"Quelle différence y a t il entre le §bbon et le mauvais chasseur§f ? Bon y faut expliquer tu vois y'a le §bmauvais chasseur§f, y voit un truc qui bouge y tire, y tire. Le §bbon chasseur§f y voit un truc y tire mais c'est un bon chasseur. Voilà c'est ça on ne peut pas les confondre.",
				"Si vous mourrez en tant que §bchasseur§f, un §b360 no-scope§f est la plus belle façon d'éliminer quelqu'un qui vous ne pouvez pas piffer",
				"Un bon §bfaucheur§f est un faucheur mort. Lui, et la moitié de votre village en un coup",
				"Visitez §bpathofexile.com§f, vous me remercierez plus tard.",
				"Vous aussi pouvez avoir une vie aussi trépidente que §bl'Inspecteur Derrick§f en endossant le rôle du §bdétective§f",
				"Vous risquez de finir en §bsandwich§f pour loup-garou. Pas très vegan tout ça.."
			)));
			
			
			config.set("spawns", new ArrayList<List<Double>>());
			saveConfig();
		}

		loadConfig();

		try {
			this.stats = new LGStats(config, getDataFolder(), rolesBuilder.keySet());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		final File f = new File(getDataFolder(), "nicks.yml");
		nicksFile = YamlConfiguration.loadConfiguration(f);
		try {
			nicksFile.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Bukkit.getConsoleSender().sendMessage("/");
		Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(rolesBuilder), this);
		Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new CancelListener(), this);
		Bukkit.getPluginManager().registerEvents(new VoteListener(), this);
		Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
		Bukkit.getPluginManager().registerEvents(new LoupGarouListener(), this);
		
		for(Player player : Bukkit.getOnlinePlayers())
			Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(player, "is connected"));
		
	    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.UPDATE_TIME) {
				@Override
				public void onPacketSending(PacketEvent event) {
					WrapperPlayServerUpdateTime time = new WrapperPlayServerUpdateTime(event.getPacket());
					LGPlayer lgp = LGPlayer.thePlayer(event.getPlayer());
					if(lgp.getGame() != null && lgp.getGame().getTime() != time.getTimeOfDay())
						event.setCancelled(true);
				}
			}
		);
		//Éviter que les gens s'entendent quand ils se sélectionnent et qu'ils sont trop proche
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
			@Override
			public void onPacketSending(PacketEvent event) {
					WrapperPlayServerNamedSoundEffect sound = new WrapperPlayServerNamedSoundEffect(event.getPacket());
					if(sound.getSoundEffect() == Sound.ENTITY_PLAYER_ATTACK_NODAMAGE)
						event.setCancelled(true);
			}
		}
	);
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
			@Override
			public void onPacketSending(PacketEvent event) {
				LGPlayer player = LGPlayer.thePlayer(event.getPlayer());
				WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(event.getPacket());
				ArrayList<PlayerInfoData> datas = new ArrayList<>();
				for(PlayerInfoData data : info.getData()) {
					LGPlayer lgp = LGPlayer.thePlayer(Bukkit.getPlayer(data.getProfile().getUUID()));
					if(player.getGame() != null && player.getGame() == lgp.getGame()) {
						LGUpdatePrefixEvent evt2 = new LGUpdatePrefixEvent(player.getGame(), lgp, player, "");
						WrappedChatComponent displayName = data.getDisplayName();
						Bukkit.getPluginManager().callEvent(evt2);
						if(evt2.getPrefix().length() > 0) {
							try {
								if(displayName != null) {
									JSONObject obj = (JSONObject) new JSONParser().parse(displayName.getJson());
									displayName = WrappedChatComponent.fromText(evt2.getPrefix()+obj.get("text"));
								} else {
									displayName = WrappedChatComponent.fromText(evt2.getPrefix()+data.getProfile().getName());
								}
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}

                                                //try{
                                                //        if (lgp.getNick()!=null){
                                                            //JSONObject obj = (JSONObject) new JSONParser().parse(displayName.getJson());
                                                            //displayName = WrappedChatComponent.fromText( obj.get("text") + " §l" + lgp.getName());
                                                //        }
                                                //} catch (ParseException e) {
						//	e.printStackTrace();
						//}

						LGSkinLoadEvent evt = new LGSkinLoadEvent(lgp.getGame(), lgp, player, data.getProfile());
						Bukkit.getPluginManager().callEvent(evt);
						datas.add(new PlayerInfoData(evt.getProfile(), data.getLatency(), data.getGameMode(), displayName));
					} else {
						datas.add(data);
					}
				}
				info.setData(datas);
			}
		});
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.UPDATE_HEALTH) {
			@Override
			public void onPacketSending(PacketEvent event) {
				LGPlayer player = LGPlayer.thePlayer(event.getPlayer());
				if(player.getGame() != null && player.getGame().isStarted()) {
					WrapperPlayServerUpdateHealth health = new WrapperPlayServerUpdateHealth(event.getPacket());
					health.setFood(6);
				}
			}
		});
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.SCOREBOARD_TEAM) {
			@Override
			public void onPacketSending(PacketEvent event) {
				LGPlayer player = LGPlayer.thePlayer(event.getPlayer());
				WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam(event.getPacket());
				team.setColor(ChatColor.WHITE);
				Player other = Bukkit.getPlayer(team.getName());
				if(other == null)return;
				LGPlayer lgp = LGPlayer.thePlayer(other);
				if(player.getGame() != null && player.getGame() == lgp.getGame()) {
					LGUpdatePrefixEvent evt2 = new LGUpdatePrefixEvent(player.getGame(), lgp, player, "");
					Bukkit.getPluginManager().callEvent(evt2);
					if(evt2.getPrefix().length() > 0)
						team.setPrefix(WrappedChatComponent.fromText(evt2.getPrefix()));
					else {
						team.setPrefix(WrappedChatComponent.fromText("§f"));
						if (lgp.getNick() != null) {
							team.setSuffix(WrappedChatComponent.fromText("§8 => §b" + lgp.getName()));
						}
					}
				}
			}
		});
		protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				LGPlayer player = LGPlayer.thePlayer(event.getPlayer());
				if(player.getGame() != null) {
					WrapperPlayServerEntityEquipment equip = new WrapperPlayServerEntityEquipment(event.getPacket());
					if(equip.getSlot() == ItemSlot.OFFHAND && equip.getEntityID() != player.getPlayer().getEntityId())
						equip.setItem(new ItemStack(Material.AIR));
				}
			}
		});
	
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("lg")) {
			if(args[0].equalsIgnoreCase("roles") && args.length == 1){
				sender.sendMessage(prefix + "§6Voici la liste des rôles:");
				for (String role : rolesBuilder.keySet())
					if (MainLg.getInstance().getConfig().getInt(DISTRIBUTION_FIXED_KEY + role) > 0) {
						sender.sendMessage(prefix + "  §e- §6" + role + " §e: " + MainLg.getInstance().getConfig().getInt(DISTRIBUTION_FIXED_KEY + role));
					}
			} else if (args[0].equalsIgnoreCase("roles") && args.length == 2){
				if(args[1].equalsIgnoreCase("list")){
					sender.sendMessage(prefix + "§6Voici la liste des rôles:");
					int index = 0;
					for (String role : rolesBuilder.keySet())
						sender.sendMessage(prefix + "  §e- " + index++ + " - §6" + role + " §e> " + MainLg.getInstance().getConfig().getInt(DISTRIBUTION_FIXED_KEY + role));
					sender.sendMessage("\n" + prefix + " §7Écrivez §8§o/lg roles set <role_id/role_name> <nombre>§7 pour définir le nombre de joueurs qui devrons avoir ce rôle.");
				}
			}
			if(!sender.hasPermission("loupgarou.admin")) {
				sender.sendMessage(prefix+"§4Erreur: Vous n'avez pas la permission...");
				return true;
			}
			if(args.length >= 1) {
				if(args[0].equalsIgnoreCase("addspawn")) {
					Player player = (Player)sender;
					Location loc = player.getLocation();
					List<Object> list = (List<Object>) getConfig().getList("spawns");
					list.add(Arrays.asList((double)loc.getBlockX(), loc.getY(), (double)loc.getBlockZ(), (double)loc.getYaw(), (double)loc.getPitch()));
					saveConfig();
					loadConfig();
					sender.sendMessage(prefix+"§aLa position a bien été ajoutée !");
					return true;
				}else if(args[0].equalsIgnoreCase("end")) {
					Player selected = (args.length == 2)
						? Bukkit.getPlayer(args[1])
						: (Player)sender;

					if(selected == null) {
						sender.sendMessage("§4Erreur : §cLe joueur §4"+args[1]+"§c n'est pas connecté.");
						return true;
					}
					LGGame game = LGPlayer.thePlayer(selected).getGame();
					if(game == null) {
						sender.sendMessage("§4Erreur : §cLe joueur §4"+selected.getName()+"§c n'est pas dans une partie.");
						return true;
					}
					game.cancelWait();
					game.endGame(LGWinType.EQUAL);
					game.broadcastMessage("§cLa partie a été arrêtée de force !");
					return true;
				}else if(args[0].equalsIgnoreCase("start")) {
					Player player = (args.length == 2)
						? Bukkit.getPlayer(args[1])
						: (Player)sender;

					if(player == null) {
						sender.sendMessage("§4Erreur : §cLe joueur §4"+args[1]+"§c n'existe pas !");
						return true;
					}
					LGPlayer lgp = LGPlayer.thePlayer(player);
					if(lgp.getGame() == null) {
						sender.sendMessage("§4Erreur : §cLe joueur §4"+lgp.getName()+"§c n'est pas dans une partie.");
						return true;
					}
					if(MainLg.getInstance().getConfig().getList("spawns").size() < lgp.getGame().getMaxPlayers()) {
						sender.sendMessage("§4Erreur : §cIl n'y a pas assez de points de spawn !");
						sender.sendMessage("§8§oPour les définir, merci de faire §7/lg addSpawn");
						return true;
					}
					sender.sendMessage("§aVous avez bien démarré une nouvelle partie !");
					

					lgp.getGame().updateStart();
					return true;
				}else if(args[0].equalsIgnoreCase("reloadconfig")) {
					sender.sendMessage("§aVous avez bien reload la config !");
					sender.sendMessage("§7§oSi vous avez changé les rôles, écriver §8§o/lg joinall§7§o !");
					loadConfig();
					return true;
				}else if(args[0].equalsIgnoreCase("joinall")) {
					for(Player p : Bukkit.getOnlinePlayers())
						Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(p, "joinall"));
					for(Player p : Bukkit.getOnlinePlayers()){
						Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(p, "joinall"));
						if(p.getPlayer().hasPermission("loupgarou.admin")){
							p.getPlayer().getInventory().setItem(1,new ItemBuilder(Material.ENDER_EYE).setName("Choisir les rôles").build());
							p.getPlayer().getInventory().setItem(3,new ItemBuilder(Material.EMERALD).setName("Lancer la partie").build());
						}
					}
					return true;
				}else if(args[0].equalsIgnoreCase("reloadPacks")) {
					for(Player p : Bukkit.getOnlinePlayers())
						Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(p, "reloadPacks"));
					for(Player p : Bukkit.getOnlinePlayers())
						Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(p, "reloadPacks"));
					return true;
				}else if(args[0].equalsIgnoreCase("nextNight")) {
					sender.sendMessage("§aVous êtes passé à la prochaine nuit");
					if(getCurrentGame() != null) {
						getCurrentGame().broadcastMessage("§2§lLe passage à la prochaine nuit a été forcé !");
						for(LGPlayer lgp : getCurrentGame().getInGame())
							lgp.stopChoosing();
						getCurrentGame().cancelWait();
						getCurrentGame().nextNight();
					}
					return true;
				}else if(args[0].equalsIgnoreCase("nextDay")) {
					sender.sendMessage("§aVous êtes passé à la prochaine journée");
					if(getCurrentGame() != null) {
						getCurrentGame().broadcastMessage("§2§lLe passage à la prochaine journée a été forcé !");
						getCurrentGame().cancelWait();
						for(LGPlayer lgp : getCurrentGame().getInGame())
							lgp.stopChoosing();
						getCurrentGame().endNight();
					}
					return true;
				}else if(args[0].equalsIgnoreCase("roles")) {
					if(args.length != 1 && !args[1].equalsIgnoreCase("list")) {
						if(args[1].equalsIgnoreCase("set") && args.length == 4) {
							String role = null;
							if(args[2].length() <= 2)
								try {
									Integer i = Integer.valueOf(args[2]);
									Object[] array = rolesBuilder.keySet().toArray();
									if(array.length <= i) {
										sender.sendMessage(prefix+"§4Erreur: §cCe rôle n'existe pas.");
										return true;
									}else
										role = (String)array[i];
								}catch(Exception err) {sender.sendMessage(prefix+"§4Erreur: §cCeci n'est pas un nombre");}
							else
								role = args[2];
							
							if(role != null) {
								String real_role = null;
								for(String real : rolesBuilder.keySet())
									if(real.equalsIgnoreCase(role)) {
										real_role = real;
										break;
									}
								
								if(real_role != null) {
									try {
										MainLg.getInstance().getConfig().set(DISTRIBUTION_FIXED_KEY + real_role, Integer.valueOf(args[3]));
										sender.sendMessage(prefix+"§6Il y aura §e"+args[3]+" §6"+real_role);
										saveConfig();
										loadConfig();
										sender.sendMessage("§7§oSi vous avez fini de changer les rôles, écriver §8§o/lg joinall§7§o !");
									}catch(Exception err) {
										sender.sendMessage(prefix+"§4Erreur: §c"+args[3]+" n'est pas un nombre");
									}
									return true;
								}
							}
							sender.sendMessage(prefix+"§4Erreur: §cLe rôle que vous avez entré est incorrect");
							
						} else {
							sender.sendMessage(prefix+"§4Erreur: §cCommande incorrecte.");
							sender.sendMessage(prefix+"§4Essayez §c/lg roles set <role_id/role_name> <nombre>§4 ou §c/lg roles list");
						}
					}
					return true;
				}else if(args[0].equalsIgnoreCase("nick")) {
					if (args.length < 3) {
						sender.sendMessage(prefix+"§4Erreur: §cCommande incorrecte.");
						sender.sendMessage(prefix+"§4Essayez §c/lg nick <pseudo_minecraft> <surnom>");
						
						return true;
					}

					Player player = Bukkit.getPlayer(args[1]);
					if(player == null) {
									sender.sendMessage("§4Erreur : §cLe joueur §4"+args[1]+"§c n'existe pas !");
									return true;
					}
					LGPlayer lgp = LGPlayer.thePlayer(player);
					if(lgp.getGame() == null) {
									sender.sendMessage("§4Erreur : §cLe joueur §4"+lgp.getName()+"§c n'est pas dans une partie.");
									return true;
					}
					
					final ArrayList<String> nicknameTokens = new ArrayList<>();
					for (int index = 2; index < args.length; index++) nicknameTokens.add(args[index]);
					final String nickname = String.join(" ", nicknameTokens);

					Player detect = Bukkit.getPlayerExact(nickname);
					if(detect != null) {    
						sender.sendMessage("§4Erreur : §cCe surnom est déjà le pseudo d'un joueur !");
						return true;
					}
					for(LGPlayer other : getCurrentGame().getInGame()) {
									if(nickname.equalsIgnoreCase(other.getNick())){
											sender.sendMessage("§4Erreur : §cCe surnom est déjà le surnom d'un joueur !");
											return true;
									}
					}

					lgp.setNick(nickname);
					for(LGPlayer other : getCurrentGame().getInGame()) {
									if(lgp != other) {
													other.getPlayer().hidePlayer(MainLg.getInstance(), lgp.getPlayer());
													other.getPlayer().showPlayer(MainLg.getInstance(), lgp.getPlayer());
									}
					}
					lgp.updatePrefix();
					sender.sendMessage("§7§o"+lgp.getName(true)+" s'appellera désormais §8§o"+nickname+"§7§o !");
					nicksFile.set(lgp.getPlayer().getUniqueId().toString(), nickname);
					File f = new File(getDataFolder(), "nicks.yml");
					try{
									nicksFile.save(f);
					} catch (IOException e) {
									sender.sendMessage("§4Erreur : §cImpossible de sauvegarder le surnom");
					}

					return true;


					
        }else if(args[0].equalsIgnoreCase("unnick")) {
					if(args.length == 2) {
                                            Player player = Bukkit.getPlayer(args[1]);
                                            if(player == null) {
                                                    sender.sendMessage("§4Erreur : §cLe joueur §4"+args[1]+"§c n'existe pas !");
                                                    return true;
                                            }
                                            LGPlayer lgp = LGPlayer.thePlayer(player);
                                            if(lgp.getGame() == null) {
                                                    sender.sendMessage("§4Erreur : §cLe joueur §4"+lgp.getName()+"§c n'est pas dans une partie.");
                                                    return true;
                                            }
                                            lgp.setNick(null);
                                            for(LGPlayer other : getCurrentGame().getInGame()) {
                                                    if(lgp != other) {
                                                            other.getPlayer().hidePlayer(MainLg.getInstance(), lgp.getPlayer());
                                                            other.getPlayer().showPlayer(MainLg.getInstance(), lgp.getPlayer());
                                                    }
                                            }
                                            lgp.updatePrefix();
                                            sender.sendMessage("§7§o"+lgp.getName(true)+" s'appellera désormais §8§o"+lgp.getName(false)+"§7§o !");
                                            nicksFile.set(lgp.getPlayer().getUniqueId().toString(), null);
                                            File f = new File(getDataFolder(), "nicks.yml");
                                            try{
                                                    nicksFile.save(f);
                                            } catch (IOException e) {
                                                    sender.sendMessage("§4Erreur : §cImpossible de sauvegarder le surnom");
                                            }
                                        }else{
                                            sender.sendMessage(prefix+"§4Erreur: §cCommande incorrecte.");
                                            sender.sendMessage(prefix+"§4Essayez §c/lg unnick <pseudo_minecraft>");
                                        }
                                        return true;
                                }
			}
			sender.sendMessage(prefix+"§4Erreur: §cCommande incorrecte.");
			sender.sendMessage(prefix+"§4Essayez /lg §caddSpawn/end/start/nextNight/nextDay/reloadConfig/roles/reloadPacks/joinAll/nick/unnick");
			return true;
		}
		return false;
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(!sender.hasPermission("loupgarou.admin"))
			return new ArrayList<>(0);
		
		if(args.length > 1) {
			if(args[0].equalsIgnoreCase("roles"))
				if(args.length == 2)
					return getStartingList(args[1], "list", "set");
				else if(args.length == 3 && args[1].equalsIgnoreCase("set"))
					return getStartingList(args[2], rolesBuilder.keySet().toArray(new String[rolesBuilder.size()]));
				else if(args.length == 4)
					return Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
		} else if(args.length == 1) {
			return getStartingList(args[0], "addSpawn", "end", "start", "nextNight", "nextDay", "reloadConfig", "roles", "joinAll", "reloadPacks", "nick", "unnick");
		}
		return new ArrayList<>(0);
	}
	private List<String> getStartingList(String startsWith, String... list){
		startsWith = startsWith.toLowerCase();
		ArrayList<String> returnlist = new ArrayList<>();
		if(startsWith.length() == 0)
			return Arrays.asList(list);
		for(String s : list)
			if(s.toLowerCase().startsWith(startsWith))
				returnlist.add(s);
		return returnlist;
	}
	public void loadConfig() {
		final FileConfiguration config = getConfig();
		final String roleDistribution = config.getString("roleDistribution");
		int players = 0;

		if (roleDistribution.equals("fixed")) {
			for(String role : rolesBuilder.keySet()) {
				players += config.getInt(DISTRIBUTION_FIXED_KEY + role);
			}
		}

		if (roleDistribution.equals("random")) {
			players = config.getInt("distributionRandom.amountOfPlayers");
		}

		currentGame = new LGGame(players);
		startingMemes = config.getStringList("startingMemes");
	}

	public String getRandomStartingMeme() {
		return (!startingMemes.isEmpty())
			? "§6N'oubliez pas: §f" + startingMemes.get(ThreadLocalRandom.current().nextInt(startingMemes.size()))
			: null;
	}

	@Override
	public void onDisable() {
		ProtocolLibrary.getProtocolManager().removePacketListeners(this);
	}
	public static MainLg getInstance() {
		return instance;
	}
	private void loadRolesBuilder() {
		try {
			rolesBuilder.put("LoupGarou", RLoupGarou.class.getConstructor(LGGame.class));
			rolesBuilder.put("LoupGarouNoir", RLoupGarouNoir.class.getConstructor(LGGame.class));
			rolesBuilder.put("Garde", RGarde.class.getConstructor(LGGame.class));
			rolesBuilder.put("Sorciere", RSorciere.class.getConstructor(LGGame.class));
			rolesBuilder.put("Voyante", RVoyante.class.getConstructor(LGGame.class));
			rolesBuilder.put("Chasseur", RChasseur.class.getConstructor(LGGame.class));
			rolesBuilder.put("Villageois", RVillageois.class.getConstructor(LGGame.class));
			rolesBuilder.put("Medium", RMedium.class.getConstructor(LGGame.class));
			rolesBuilder.put("Dictateur", RDictateur.class.getConstructor(LGGame.class));
			rolesBuilder.put("Cupidon", RCupidon.class.getConstructor(LGGame.class));
			rolesBuilder.put("PetiteFille", RPetiteFille.class.getConstructor(LGGame.class));
			rolesBuilder.put("ChaperonRouge", RChaperonRouge.class.getConstructor(LGGame.class));
			rolesBuilder.put("LoupGarouBlanc", RLoupGarouBlanc.class.getConstructor(LGGame.class));
			rolesBuilder.put("Bouffon", RBouffon.class.getConstructor(LGGame.class));
			rolesBuilder.put("Ange", RAnge.class.getConstructor(LGGame.class));
			rolesBuilder.put("Survivant", RSurvivant.class.getConstructor(LGGame.class));
			rolesBuilder.put("Assassin", RAssassin.class.getConstructor(LGGame.class));
			rolesBuilder.put("GrandMechantLoup", RGrandMechantLoup.class.getConstructor(LGGame.class));
			rolesBuilder.put("Corbeau", RCorbeau.class.getConstructor(LGGame.class));
			rolesBuilder.put("Detective", RDetective.class.getConstructor(LGGame.class));
			rolesBuilder.put("ChienLoup", RChienLoup.class.getConstructor(LGGame.class));
			rolesBuilder.put("Pirate", RPirate.class.getConstructor(LGGame.class));
			rolesBuilder.put("Pyromane", RPyromane.class.getConstructor(LGGame.class));
			rolesBuilder.put("Pretre", RPretre.class.getConstructor(LGGame.class));
			rolesBuilder.put("Faucheur", RFaucheur.class.getConstructor(LGGame.class));
			rolesBuilder.put("EnfantSauvage", REnfantSauvage.class.getConstructor(LGGame.class));
			rolesBuilder.put("MontreurDOurs", RMontreurDOurs.class.getConstructor(LGGame.class));
			rolesBuilder.put("Vampire", RVampire.class.getConstructor(LGGame.class));
			rolesBuilder.put("ChasseurDeVampire", RChasseurDeVampire.class.getConstructor(LGGame.class));
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

	public void saveStats(LGWinType winType) {
		try {
			this.stats.saveRound(winType);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
