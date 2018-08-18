/*
 * Copyright © Phyrone 2018
 */

package de.phyrone.teb;

import org.bukkit.entity.Player;

public interface Placeholder {
    String onRequest(Player player, String[] args);
}
