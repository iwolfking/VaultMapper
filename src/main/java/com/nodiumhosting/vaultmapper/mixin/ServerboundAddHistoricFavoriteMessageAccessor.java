package com.nodiumhosting.vaultmapper.mixin;

import iskallia.vault.network.message.ServerboundAddHistoricFavoriteMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(value = ServerboundAddHistoricFavoriteMessage.class, remap = false)
public interface ServerboundAddHistoricFavoriteMessageAccessor {
    @Accessor("uuid")
    UUID getUUID();
}
