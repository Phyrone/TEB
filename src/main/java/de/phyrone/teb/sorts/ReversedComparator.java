package de.phyrone.teb.sorts;
/*
 *   Copyright © 2018 by Phyrone  *
 *   Creation: 15.07.2018 by Phyrone
 */

import de.phyrone.teb.TablistComperator;
import org.bukkit.entity.Player;

public class ReversedComparator implements TablistComperator {
    private TablistComperator comparator;

    public ReversedComparator(TablistComperator comparator) {
        this.comparator = comparator;
    }

    @Override
    public int sort(Player player1, Player player2, TablistMeta metaData) {
        return comparator.sort(player1, player2, metaData) * -1;
    }
}
