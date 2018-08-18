/*
 * Copyright © Phyrone 2018
 */

package de.phyrone.teb.sorts;

import de.phyrone.teb.Config;
import de.phyrone.teb.TablistComperator;
import org.bukkit.entity.Player;

public class PlayerGroupSort implements TablistComperator {
    final private boolean debug;

    public PlayerGroupSort(boolean debug) {
        this.debug = debug;
    }

    private boolean isDebug() {
        return debug;
    }

    @Override
    public int sort(Player player1, Player player2, TablistMeta metaData) {
        for (Config.TablistGroup group : metaData.getTablist().getGroups()) {
            if (isDebug())
                System.out.println(">>PERM: " + (group.getPermmision().isEmpty() ? "none" : group.getPermmision()));
            if (group.getPermmision().isEmpty()) {
                if (isDebug())
                    System.out.println(player1.getName() + " VS " + player2.getName() + " -> EQUAL (NOPERMGROUP)");
                return Result.EQUAL;
            } else if (player1.hasPermission(group.getPermmision()) && !player2.hasPermission(group.getPermmision())) {
                if (isDebug())
                    System.out.println(player1.getName() + " VS " + player2.getName() + " -> UP");
                return Result.DOWN;
            } else if (!player1.hasPermission(group.getPermmision()) && player2.hasPermission(group.getPermmision())) {
                if (isDebug())
                    System.out.println(player1.getName() + " VS " + player2.getName() + " -> DOWN");
                return Result.UP;
            }
        }
        if (isDebug())
            System.out.println(player1.getName() + " VS " + player2.getName() + " -> EQUAL");
        return Result.EQUAL;
    }
}
