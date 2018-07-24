package de.phyrone.teb.placeholder;
/*
 *   Copyright © 2018 by Phyrone  *
 *   Creation: 16.07.2018 by Phyrone
 */

import de.phyrone.teb.Placeholder;

public enum DefaultPlaceholders {
    NAME((player, args) -> {
        return player.getDisplayName();
    }),
    REALNAME((player, args) -> {
        return player.getName();
    }),LOCALE((player, args) -> {
        return player.getLocale();
    }),
    UUID((player, args) -> {
        return player.getUniqueId().toString();
    }),
    LEVEL((player, args) -> {
        return String.valueOf(player.getLevel());
    });
    Placeholder placeholder;

    DefaultPlaceholders(Placeholder placeholder) {
        this.placeholder = placeholder;
    }

    public Placeholder getPlaceholder() {
        return placeholder;
    }
}
