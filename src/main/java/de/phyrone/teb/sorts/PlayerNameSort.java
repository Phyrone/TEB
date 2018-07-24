package de.phyrone.teb.sorts;
/*
 *   Copyright © 2018 by Phyrone  *
 *   Creation: 16.07.2018 by Phyrone
 */

import de.phyrone.teb.TablistComperator;
import org.bukkit.entity.Player;

public class PlayerNameSort implements TablistComperator {
    @Override
    public int sort(Player player1, Player player2, TablistMeta metaData) {
        return player1.getDisplayName().compareTo(player2.getDisplayName());
    }
}
