/*
 * Copyright © Phyrone 2018
 */

package de.phyrone.teb;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.ChatColor;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Config {
    private static final GsonBuilder GSON_BUILDER = new GsonBuilder().setPrettyPrinting();
    private static final File FILE = new File("plugins/Teb", "config.json");
    private static Config instance;
    boolean disableJoinLeaveMSG = false;
    private boolean debug = false;
    private int setTablistDelay = -1;
    private List<TablistViewGroup> tablists = new ArrayList<>(Collections.singletonList(new TablistViewGroup("default")));

    private Config() {

    }

    static Config getInstance() {
        if (instance == null) {
            instance = fromDefaults();
        }
        return instance;
    }

    static void load() {
        instance = fromFile();

        // no config file found
        if (instance == null) {
            instance = fromDefaults();
        }
    }

    private static Config fromDefaults() {
        return new Config();
    }

    private static Config fromFile() {
        if (!FILE.getParentFile().mkdirs() && !FILE.getParentFile().exists()) {
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(FILE)));
            return GSON_BUILDER.create().fromJson(reader, Config.class);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    int getSetTablistDelay() {
        return setTablistDelay;
    }

    public void setSetTablistDelay(int setTablistDelay) {
        this.setTablistDelay = setTablistDelay;
    }

    boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    List<TablistViewGroup> getTablists() {
        return tablists == null ? new ArrayList<>(
                Collections.singletonList(new TablistViewGroup())
        ) : tablists;
    }

    void toFile() {
        String jsonConfig = StringEscapeUtils.unescapeJava(GSON_BUILDER.create().toJson(this));
        FileWriter writer;
        try {
            writer = new FileWriter(FILE);
            writer.write(jsonConfig);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return GSON_BUILDER.create().toJson(this);
    }

    static class TablistViewGroup {
        int updateTime = 20;
        int animationDelay = 20 * 5;
        String permission;
        List<TablistGroup> groups = new ArrayList<>(Arrays.asList(
                new TablistGroup("Admin", ChatColor.DARK_RED),
                new TablistGroup("Moderator", ChatColor.BLUE),
                new TablistGroup("Premium", ChatColor.GOLD),
                new TablistGroup()
        ));
        List<String> sortPriority = new ArrayList<>(Arrays.asList(
                "Groups",
                "AFK_Last",
                "YouFirst"
        ));

        TablistViewGroup(String name) {
            permission = ("tablist.view." + name).toLowerCase();
        }

        TablistViewGroup() {
            permission = null;
        }

        int getAnimationDelay() {
            return animationDelay;
        }

        int getUpdateTime() {
            return updateTime;
        }

        List<TablistGroup> getGroups() {
            return groups;
        }

        String getPermission() {
            return permission;
        }
    }

    public static class TablistGroup {

        String permmision = "";
        List<String> prefix = new ArrayList<>(Collections.singletonList(""));
        List<String> suffix = new ArrayList<>(Collections.singletonList(""));

        TablistGroup() {

        }


        TablistGroup(String name, ChatColor color) {

            prefix = new ArrayList<>(Collections.singletonList(color + name + " "));
            permmision = ("tablist." + name).toLowerCase();
        }

        List<String> getPrefix() {
            return prefix == null ? new ArrayList<>() : prefix;
        }

        List<String> getSuffix() {
            return suffix == null ? new ArrayList<>() : suffix;
        }

        public String getPermmision() {
            return permmision == null ? "" : permmision;
        }
    }
}
