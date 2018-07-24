package de.phyrone.teb;
/*
 *   Copyright © 2018 by Phyrone  *
 *   Creation: 15.07.2018 by Phyrone
 */

import de.phyrone.teb.Teb;
import org.bukkit.entity.Player;

public interface TablistComperator {
    int sort(Player player1, Player player2, TablistMeta metaData);

    interface TablistMeta {
        Player getViewer();

        Teb.Tablist getTablist();

        Teb.Tablist.PlayerTablist getPlayerTablist();
    }

    class Result {
        public static final int UP = 1;
        public static final int DOWN = -1;
        public static final int EQUAL = 0;
    }
}
