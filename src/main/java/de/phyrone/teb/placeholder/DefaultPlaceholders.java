/*
 * Copyright © Phyrone 2018
 */

package de.phyrone.teb.placeholder;

import de.phyrone.teb.Placeholder;

public enum DefaultPlaceholders {
    NAME((player, args) -> {
        return player.getDisplayName();
    }),
    REALNAME((player, args) -> {
        return player.getName();
    }), LOCALE((player, args) -> {
        return player.getLocale();
    }),
    UUID((player, args) -> {
        return player.getUniqueId().toString();
    }),
    LEVEL((player, args) -> {
        return String.valueOf(player.getLevel());
    });
    final Placeholder placeholder;

    DefaultPlaceholders(Placeholder placeholder) {
        this.placeholder = placeholder;
    }

    public Placeholder getPlaceholder() {
        return placeholder;
    }
}
