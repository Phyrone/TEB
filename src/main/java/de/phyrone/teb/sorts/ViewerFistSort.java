package de.phyrone.teb.sorts;
/*
 *   Copyright © 2018 by Phyrone  *
 *   Creation: 15.07.2018 by Phyrone
 */

import de.phyrone.teb.TablistComperator;
import org.bukkit.entity.Player;

public class ViewerFistSort implements TablistComperator {
    @Override
    public int sort(Player player1, Player player2, TablistMeta metaData) {
        if (player1.getUniqueId().equals(metaData.getViewer().getUniqueId())) {
            return Result.DOWN;
        } else if (player2.getUniqueId().equals(metaData.getViewer().getUniqueId())) {
            return Result.UP;
        } else {
            return Result.EQUAL;
        }
    }
}
