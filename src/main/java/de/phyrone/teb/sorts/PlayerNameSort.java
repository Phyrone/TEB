/*
 * Copyright © Phyrone 2018
 */

package de.phyrone.teb.sorts;

import de.phyrone.teb.TablistComperator;
import org.bukkit.entity.Player;

public class PlayerNameSort implements TablistComperator {
    @Override
    public int sort(Player player1, Player player2, TablistMeta metaData) {
        return player1.getDisplayName().compareTo(player2.getDisplayName());
    }
}
