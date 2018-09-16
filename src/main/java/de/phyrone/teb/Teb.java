/*
 * Copyright © Phyrone 2018
 */

package de.phyrone.teb;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.phyrone.teb.placeholder.DefaultPlaceholders;
import de.phyrone.teb.sorts.PlayerGroupSort;
import de.phyrone.teb.sorts.PlayerNameSort;
import de.phyrone.teb.sorts.ReversedComparator;
import de.phyrone.teb.sorts.ViewerFistSort;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Teb extends JavaPlugin implements Listener {
    private static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[{]([^{}]+)[}]");
    private static final Pattern VIEWER_PLACEHOLDER_PATTERN = Pattern.compile("[{](?i)viewer ([^{}]+)[}]");
    private static final Pattern TARGET_PLACEHOLDER_PATTERN = Pattern.compile("[{](?i)target ([^{}]+)[}]");
    private static final String TEAMSUFFIX = "-TEB";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("[%]([^%]+)[%]");
    private final ThreadFactory THREADPOOLFACTORY = new ThreadFactoryBuilder().setNameFormat("UpdateTablistWorker[%d]").build();
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10L, TimeUnit.SECONDS, new SynchronousQueue<>(), THREADPOOLFACTORY);
    private boolean debug = true;
    private List<Tablist> tablists = new LinkedList<>();
    private HashMap<String, TablistComperator> tablistComparators = new HashMap<>();
    private ScoreboardManager scoreboardManager;
    private BukkitScheduler scheduler;
    private HashMap<String, Placeholder> placeholders = new HashMap<>();
    private boolean placeholderapiEnabled;

    private void addComparator(String name, TablistComperator comperator, boolean updateSortlists) {
        tablistComparators.put(name.toLowerCase(), comperator);
        if (updateSortlists) {
            updateSortLists();
        }
    }

    private void addComparator(String name, TablistComperator comperator) {
        addComparator(name, comperator, false);
    }

    private boolean isDebug() {
        return debug;
    }

    private Map<String, Placeholder> getPlaceholders() {
        return ImmutableMap.copyOf(placeholders);
    }

    private String setOwnPlaceholders(Player player, String text, Pattern placeholderPattern) {
        if (text == null) {
            return null;
        } else if (placeholders.isEmpty()) {
            return text;
        } else {
            Matcher m = placeholderPattern.matcher(text);
            Map hooks = getPlaceholders();

            while (m.find()) {
                String format = m.group(1).replace(' ', '_');
                int index = format.indexOf("_");
                if (index > 0 && index < format.length()) {
                    String identifier = format.substring(0, index).toLowerCase();
                    String params = format.substring(index + 1);
                    if (hooks.containsKey(identifier)) {
                        String value = ((Placeholder) hooks.get(identifier)).onRequest(player, params.split(" "));
                        if (value != null) {
                            text = text.replaceAll(Pattern.quote(m.group()), Matcher.quoteReplacement(value));
                        }
                    }
                }
            }

            return text;
        }
    }

    @Override
    public void onLoad() {
        Config.load();
        Config.getInstance().toFile();
        this.debug = Config.getInstance().isDebug();
    }

    private void addPlaceholder(String name, Placeholder placeholder) {
        placeholders.put(name.toLowerCase(), placeholder);
    }

    @Override
    public void onEnable() {
        scoreboardManager = Bukkit.getScoreboardManager();
        scheduler = Bukkit.getScheduler();
        addComparator("Groups", new PlayerGroupSort(isDebug()));
        addComparator("PlayerName", new PlayerNameSort());
        addComparator("YouFirst", new ViewerFistSort(), true);
        for (DefaultPlaceholders placeholder : DefaultPlaceholders.values()) {
            addPlaceholder(placeholder.name(), placeholder.getPlaceholder());
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        placeholderapiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        rebuildTablistsCache();
        try {
            updateTablists();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SimplePie("placeholderapi", () -> placeholderapiEnabled ? ("PlaceholderAPI v." + PlaceholderAPIPlugin.getInstance().getDescription().getVersion()) : "NO"));
        executor.setCorePoolSize(Config.getInstance().getThreadpoolMaxSize() < 1 ? Integer.MAX_VALUE : Config.getInstance().getThreadpoolMaxSize());
        Bukkit.getConsoleSender().sendMessage(
                "\n" + ChatColor.BLUE +
                        "  _____ _____ ____  \n" +
                        " |_   _| ____| __ ) \n" +
                        "   | | |  _| |  _ \\ \n" +
                        "   | | | |___| |_) |\n" +
                        "   |_| |_____|____/ \n" +
                        ChatColor.GOLD +
                        "§6 v" + getDescription().getVersion() + " by Phyrone  \n");
    }

    @Override
    public void onDisable() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {

        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        try {
            if (Config.getInstance().disableJoinLeaveMSG) {
                event.setJoinMessage(null);
            }
            int delay = Config.getInstance().getSetTablistDelay();
            if (delay > 0) {
                scheduler.runTaskLater(this, this::updateTablists, delay);
            } else {
                updateTablists();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateTablist(Player player) {
        Tablist tablist = getTablist(player);
        if (tablist != null) {
            tablist.getSub(player).reListPlayers();
        } else {
            getLogger().info("Player " + player.getName() + " has no Tablist");
        }
    }

    private void updateTablists() {
        Bukkit.getOnlinePlayers().forEach(this::updateTablist);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeave(PlayerQuitEvent event) {
        if (Config.getInstance().disableJoinLeaveMSG) {
            event.setQuitMessage(null);
        }
        unregisterPlayer(event.getPlayer());
    }

    private String setPlaceholders(String text, Player viewer, Player targetplayer) {
        text = setOwnPlaceholders(viewer, text, VIEWER_PLACEHOLDER_PATTERN);
        text = setOwnPlaceholders(targetplayer, text, TARGET_PLACEHOLDER_PATTERN);
        if (placeholderapiEnabled) {
            try {
                text = PlaceholderAPI.setPlaceholders(viewer, text, VIEWER_PLACEHOLDER_PATTERN);
                text = PlaceholderAPI.setPlaceholders(targetplayer, text, TARGET_PLACEHOLDER_PATTERN);
                text = PlaceholderAPI.setRelationalPlaceholders(viewer, targetplayer, text);

            } catch (NoSuchMethodError error) {
                System.err.println("PlaceholderAPI ERROR -> Disabling hook");
                placeholderapiEnabled = false;
                error.printStackTrace();
            } catch (Exception e) {
                System.err.println("Error PlaceholderApi Hook");
                e.printStackTrace();
            }
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String setPlaceholders(String text, Player viewer) {
        text = setOwnPlaceholders(viewer, text, PLACEHOLDER_PATTERN);
        text = setOwnPlaceholders(viewer, text, BRACKET_PLACEHOLDER_PATTERN);
        if (placeholderapiEnabled) {
            try {
                text = PlaceholderAPI.setPlaceholders(viewer, text, PLACEHOLDER_PATTERN);
                text = PlaceholderAPI.setPlaceholders(viewer, text, BRACKET_PLACEHOLDER_PATTERN);
            } catch (NoSuchMethodError error) {
                System.err.println("PlaceholderAPI ERROR -> Disabling hook");
                placeholderapiEnabled = false;
                error.printStackTrace();
            } catch (Exception e) {
                System.err.println("Error PlaceholderApi Hook");
                e.printStackTrace();
            }
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String shortBytes(String text, int leght) {
        while (text.getBytes(StandardCharsets.UTF_8).length > leght) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private Tablist getTablist(Player player) {
        for (Tablist tablist : tablists) {
            if (tablist.getPermission().isEmpty() || player.hasPermission(tablist.getPermission())) {
                return tablist;
            }
        }
        return null;
    }

    public synchronized void reListPlayers() {
        tablists.forEach(Tablist::reListPlayers);
    }

    private void rebuildTablistsCache() {
        tablists.forEach(Tablist::close);
        tablists.clear();
        Config.getInstance().getTablists().forEach(group -> tablists.add(new Tablist(group)));

    }

    private void unregisterPlayer(Player player) {
        tablists.forEach(tablist -> tablist.removeSub(player));
        tablists.forEach(tablist -> tablist.playerTablistHashMap.values().forEach(playerTablist -> {
            playerTablist.removeSub(player);
        }));
    }

    private void updateSortLists() {
        tablists.forEach(Tablist::updateSortlist);
    }

    private interface SubHandler<T> {
        T getSub(Player player);

        void removeSub(Player player);
    }

    public class Tablist implements SubHandler<Tablist.PlayerTablist> {
        String permission;
        List<Config.TablistGroup> groups;
        HashMap<UUID, PlayerTablist> playerTablistHashMap = new HashMap<>();
        List<TablistComperator> sortlist = new ArrayList<>();
        Runnable autoupdateRunnable = () -> playerTablistHashMap.values().forEach(PlayerTablist::updateAll);
        Runnable animationRunnable = () -> playerTablistHashMap.values().forEach(playerTablist -> playerTablist.targetHashMap.values().forEach(PlayerTablist.Target::nextAnimation));
        BukkitTask updateTask;
        BukkitTask animationTask;
        List<String> sortStrings;

        Tablist(Config.TablistViewGroup group) {
            this.permission = group.getPermission() == null ? "" : group.getPermission();
            this.groups = group.getGroups();
            this.sortStrings = group.sortPriority;
            updateTask = scheduler.runTaskTimerAsynchronously(Teb.this, autoupdateRunnable, 0, group.getUpdateTime());
            animationTask = scheduler.runTaskTimerAsynchronously(Teb.this, animationRunnable, 0, group.getAnimationDelay());
            updateSortlist();
        }


        void updateSortlist() {
            if (isDebug())
                System.out.println("Comperators ->");
            sortStrings.forEach(s -> {
                if (s == null || s.isEmpty() || s.equals("!")) {
                    return;
                } else {
                    s = s.toLowerCase();
                }
                sortlist.add(s.startsWith("!")
                        ? new ReversedComparator(getComparator(s.substring(1)))
                        : getComparator(s));
                if (isDebug()) {
                    sortlist.forEach(comperator -> System.out.println("  - " + comperator.getClass().getSimpleName()));
                }
                Collections.reverse(sortlist);
            });
        }

        private TablistComperator getComparator(String name) {
            if (tablistComparators.containsKey(name)) {
                return tablistComparators.get(name);
            } else {
                return (player1, player2, metaData) ->
                        setPlaceholders(name, player1).compareTo(setPlaceholders(name, player2));
            }
        }

        public List<Config.TablistGroup> getGroups() {
            return groups;
        }


        String getPermission() {
            return permission;
        }

        void close() {
            clear();
            if (updateTask != null) {
                updateTask.cancel();
            }
            if (animationTask != null) {
                animationTask.cancel();
            }
        }

        synchronized void reListPlayers() {
            playerTablistHashMap.values().forEach(PlayerTablist::reListPlayers);
        }

        void clear() {
            playerTablistHashMap.values().forEach(PlayerTablist::clear);
        }

        private List<String> getPrefix(Player player) {
            for (Config.TablistGroup group : groups) {
                if (group.getPermmision().isEmpty() || player.hasPermission(group.getPermmision()))
                    return group.getPrefix();
            }
            return new ArrayList<>();
        }

        private List<String> getSuffix(Player player) {
            for (Config.TablistGroup group : groups) {
                if (group.getPermmision().isEmpty() || player.hasPermission(group.getPermmision()))
                    return group.getSuffix();
            }
            return new ArrayList<>();
        }


        @Override
        public PlayerTablist getSub(Player player) {
            if (!playerTablistHashMap.containsKey(player.getUniqueId())) {
                playerTablistHashMap.put(player.getUniqueId(), new PlayerTablist(player));
            }
            return playerTablistHashMap.get(player.getUniqueId());
        }

        @Override
        public void removeSub(Player player) {
            if (playerTablistHashMap.containsKey(player.getUniqueId())) {
                playerTablistHashMap.get(player.getUniqueId()).clear();
            }
            playerTablistHashMap.remove(player.getUniqueId());
        }

        public class PlayerTablist implements SubHandler<PlayerTablist.Target> {
            final Object relistSyncer = new Object();
            Player viewer;
            TablistComperator.TablistMeta tablistMeta = new TablistComperator.TablistMeta() {
                @Override
                public Player getViewer() {
                    return viewer;
                }

                @Override
                public Tablist getTablist() {
                    return Tablist.this;
                }

                @Override
                public PlayerTablist getPlayerTablist() {
                    return PlayerTablist.this;
                }
            };
            Scoreboard scoreboard;
            LinkedHashMap<UUID, Target> targetHashMap = new LinkedHashMap<>();

            PlayerTablist(Player player) {

                this.viewer = player;
                assert player != null;
                if (isDebug()) {
                    System.out.println(player);
                    System.out.println(player.getScoreboard());
                }

                if (player.getScoreboard() == null || player.getScoreboard().equals(scoreboardManager.getMainScoreboard())) {
                    scoreboard = scoreboardManager.getNewScoreboard();
                    player.setScoreboard(scoreboard);
                } else {
                    scoreboard = player.getScoreboard();
                    scoreboard.getTeams().forEach(team -> {
                        if (team.getName().endsWith(TEAMSUFFIX)) {
                            team.unregister();
                        }
                    });
                }
                reListPlayers();
            }

            private List<Player> getPlayersSorted() {
                List<Player> players = new ArrayList<>(Collections2.filter(Bukkit.getOnlinePlayers(), input -> viewer.canSee(input)));
                players.sort(Comparator.comparing(Player::getDisplayName));
                for (TablistComperator comperator : Tablist.this.sortlist) {
                    players.sort((player1, player2) -> comperator.sort(player1, player2, tablistMeta));
                }
                return players;
            }

            Player getViewer() {
                return viewer;
            }

            void reListPlayers() {
                executor.submit(() -> {
                    synchronized (relistSyncer) {
                        clear();
                        int i = 0;
                        if (isDebug())
                            System.out.println(">>>>>Sorter Tablist<<<<<");
                        for (Player player : getPlayersSorted()) {
                            if (isDebug()) {
                                System.out.println(" -> " + player.getName());
                            }
                            targetHashMap.put(player.getUniqueId(), new Target(player, i));
                            i++;
                        }
                    }
                });

            }

            synchronized void clear() {
                targetHashMap.values().forEach(Target::remove);
                targetHashMap.clear();
            }

            void updateAll() {
                if (isDebug()) {
                    System.out.println(">>>>>Tablist<<<<<");
                }
                targetHashMap.forEach((uuid, target) -> target.update());
                if (isDebug()) {
                    System.out.println("-----------------");
                }
            }

            @Override
            public Target getSub(Player player) {
                if (!targetHashMap.containsKey(player.getUniqueId())) {
                    targetHashMap.put(player.getUniqueId(), new Target(player, 0));
                }
                return targetHashMap.get(player.getUniqueId());
            }

            @Override
            public void removeSub(Player player) {
                removeSub(player.getUniqueId());
            }

            void removeSub(UUID uuid) {
                if (targetHashMap.containsKey(uuid))
                    targetHashMap.get(uuid).remove();
                targetHashMap.remove(uuid);
            }

            private int getZeros() {
                return String.valueOf(playerTablistHashMap.size()).length();
            }

            class Target {
                Team team;
                Player player;
                String prefix;
                String suffix;
                List<String> prefixes;
                List<String> suffixes;
                Iterator<String> prefixIterator;
                Iterator<String> suffixiterator;

                Target(Player player, int sort) {
                    int sortZeros = String.valueOf(sort).length() - 1;
                    this.player = player;
                    team = scoreboard.registerNewTeam(shortBytes(
                            Strings.repeat("0", getZeros() - sortZeros >= 0 ? getZeros() - sortZeros : 0)
                                    + String.valueOf(sort)
                                    + TEAMSUFFIX, 32));
                    team.addEntry(player.getName());
                    prefixes = getPrefix(player);
                    prefixes = prefixes.isEmpty() ? new ArrayList<>(Collections.singletonList("")) : prefixes;
                    prefixIterator = prefixes.iterator();
                    suffixes = getSuffix(player);
                    suffixes = suffixes.isEmpty() ? new ArrayList<>(Collections.singletonList("")) : suffixes;
                    suffixiterator = prefixes.iterator();
                    nextAnimation();
                    update();

                }

                void nextAnimation() {
                    /* Prefix */
                    if (!prefixIterator.hasNext()) {
                        prefixIterator = prefixes.iterator();
                    }
                    prefix = prefixIterator.next();
                    /* Suffix */
                    if (!suffixiterator.hasNext()) {
                        suffixiterator = suffixes.iterator();
                    }
                    suffix = suffixiterator.next();
                }


                void update() {
                    team.setPrefix(shortBytes(setPlaceholders(prefix, PlayerTablist.this.viewer, Target.this.player), 16));
                    team.setSuffix(shortBytes(setPlaceholders(suffix, PlayerTablist.this.viewer, Target.this.player), 16));
                    if (isDebug())
                        System.out.println("Viewer: " + PlayerTablist.this.viewer.getName() + " -> " +
                                shortBytes(setPlaceholders(prefix, PlayerTablist.this.viewer, Target.this.player), 16) +
                                player.getDisplayName() +
                                shortBytes(setPlaceholders(suffix, PlayerTablist.this.viewer, Target.this.player), 16));
                }

                void remove() {
                    team.unregister();
                }
            }
        }
    }
}
