package com.nodiumhosting.vaultmapper.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nodiumhosting.vaultmapper.map.snapshots.MapSnapshot;
import iskallia.vault.client.gui.screen.summary.VaultHistoryScreen;
import iskallia.vault.network.message.ServerboundAddHistoricFavoriteMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VaultHistoryScreen.class, remap = false)
public abstract class VaultHistoryScreenMixin {

    @WrapOperation(
        method = "lambda$drawStar$21",
        at = @At(
            value = "INVOKE",
            target = "Liskallia/vault/init/ModNetwork;sendToServer(Ljava/lang/Object;)V"
        )
    )
    private static void vaultmapper$toggleFavoriteOnStarClick(Object message, Operation<Void> original) {
        if (message instanceof ServerboundAddHistoricFavoriteMessage msg) {
            MapSnapshot.toggleFavorite(((ServerboundAddHistoricFavoriteMessageAccessor)msg).getUUID());
        }

        original.call(message);
    }
}