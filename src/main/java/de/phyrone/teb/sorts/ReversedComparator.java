/*
 * Copyright © Phyrone 2018
 */

package de.phyrone.teb.sorts;

import de.phyrone.teb.TablistComperator;
import org.bukkit.entity.Player;

public class ReversedComparator implements TablistComperator {
    private final TablistComperator comparator;

    public ReversedComparator(TablistComperator comparator) {
        this.comparator = comparator;
    }

    @Override
    public int sort(Player player1, Player player2, TablistMeta metaData) {
        return comparator.sort(player1, player2, metaData) * -1;
    }
}
