package com.nodiumhosting.vaultmapper.mixin;

import com.nodiumhosting.vaultmapper.map.VaultCell;
import com.nodiumhosting.vaultmapper.map.VaultMap;
import com.nodiumhosting.vaultmapper.map.snapshots.MapSnapshot;
import com.nodiumhosting.vaultmapper.proto.CellType;
import iskallia.vault.client.atlas.TextureAtlasRegion;
import iskallia.vault.client.gui.framework.ScreenTextures;
import iskallia.vault.client.gui.screen.options.ui.OptionsCanvas;
import iskallia.vault.client.gui.screen.options.ui.OptionsInput;
import iskallia.vault.client.gui.screen.options.ui.OptionsWidgets;
import iskallia.vault.client.gui.screen.summary.EndScreenTimeline;
import iskallia.vault.client.gui.screen.summary.VaultEndScreen;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.stat.VaultSnapshot;
import iskallia.vault.init.ModTextureAtlases;
import iskallia.vault.VaultMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VaultEndScreen.class, remap = false)
public abstract class VaultEndScreenMixin {

    @Shadow
    private int detailsTab;

    @Shadow
    private int pickerSlot;

    @Shadow
    @Final
    private VaultSnapshot snapshot;

    @Shadow
    @Final
    private boolean isHistory;

    @Shadow
    @Final
    private OptionsCanvas canvas;

    @Shadow
    @Final
    private OptionsInput input;

    @Shadow
    @Final
    private EndScreenTimeline timeline;

    @Unique
    private boolean vaultmapper$recordedExit = false;

    @Unique
    private float vaultmapper$panX = 0.0F;

    @Unique
    private float vaultmapper$panY = 0.0F;

    @Unique
    private float vaultmapper$zoom = 1.0F;

    @Inject(
            method = "<init>(Liskallia/vault/core/vault/stat/VaultSnapshot;Lnet/minecraft/network/chat/Component;Ljava/util/UUID;ZZ)V",
            at = @At("TAIL")
    )
    private void vaultmapper$onConstructed(
            VaultSnapshot snapshot,
            Component title,
            UUID asPlayer,
            boolean isHistory,
            boolean fromLink,
            CallbackInfo ci
    ) {
        if (!this.isHistory && !this.vaultmapper$recordedExit && snapshot != null && snapshot.getEnd() != null) {
            UUID vaultId = snapshot.getEnd().get(Vault.ID);
            if (vaultId != null) {
                MapSnapshot.onVaultExit(vaultId);
                this.vaultmapper$recordedExit = true;
            }
        }
    }

    @Inject(method = "drawTabs", at = @At("TAIL"))
    private void vaultmapper$drawMapTab(float x, float y, CallbackInfo ci) {
        int index = 5;
        boolean selected = this.detailsTab == index;
        float tabX = x + 400.0F - 3.0F;
        float tabY = y + 6.0F + index * 31;

        TextureAtlasRegion mapIcon = TextureAtlasRegion.of(
                ModTextureAtlases.SCREEN,
                VaultMod.id("gui/screen/map")
        );

        this.canvas.sprite(
                selected ? ScreenTextures.TAB_BACKGROUND_RIGHT_SELECTED : ScreenTextures.TAB_BACKGROUND_RIGHT,
                tabX + (selected ? 0 : 3),
                tabY
        );
        this.canvas.sprite(mapIcon, tabX + (selected ? 4 : 6), tabY + 6.0F, 16.0F, 16.0F);

        this.input.hit(this.canvas, "tab:" + index, tabX, tabY, 27.0F, 28.0F)
                .onClick(() -> {
                    OptionsWidgets.playClick();
                    this.detailsTab = index;
                    this.pickerSlot = -1;
                    this.timeline.skip();
                })
                .tooltip(() -> List.of(new TextComponent("Map")));
    }

    @Inject(method = "drawDetails", at = @At("HEAD"), cancellable = true)
    private void vaultmapper$drawMapDetails(float x, float y, CallbackInfo ci) {
        if (this.detailsTab != 5) {
            return;
        }

        float paneX = x + 8.0F;
        float paneY = y + 18.0F;
        float paneWidth = 384.0F;
        float paneHeight = 172.0F;

        this.canvas.nineSlice(ScreenTextures.INSET_DARK_GREY_BACKGROUND, paneX, paneY, paneWidth, paneHeight);
        this.canvas.text("Vault Map", paneX + 6.0F, paneY + 5.0F, 16777215, true);

        Optional<MapSnapshot> mapSnapshot = vaultmapper$resolveSnapshot();
        if (mapSnapshot.isEmpty() || mapSnapshot.get().cells == null || mapSnapshot.get().cells.isEmpty()) {
            this.canvas.textCentered(
                    "No map snapshot recorded for this vault.",
                    paneX + paneWidth / 2.0F,
                    paneY + paneHeight / 2.0F - 4.0F,
                    11184810,
                    true
            );
            ci.cancel();
            return;
        }

        float mapAreaX = paneX + 4.0F;
        float mapAreaY = paneY + 16.0F;
        float mapAreaW = paneWidth - 8.0F;
        float mapAreaH = paneHeight - 20.0F;

        this.canvas.pushClip(mapAreaX, mapAreaY, mapAreaW, mapAreaH);
        vaultmapper$drawCells(mapSnapshot.get().cells, mapAreaX, mapAreaY, mapAreaW, mapAreaH);
        this.canvas.popClip();

        ci.cancel();
    }

    @Unique
    private void vaultmapper$drawCells(
            List<VaultCell> cells,
            float areaX,
            float areaY,
            float areaW,
            float areaH
    ) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (VaultCell cell : cells) {
            if (cell.x < minX) minX = cell.x;
            if (cell.x > maxX) maxX = cell.x;
            if (cell.z < minZ) minZ = cell.z;
            if (cell.z > maxZ) maxZ = cell.z;
        }

        int spanX = Math.max(1, maxX - minX + 1);
        int spanZ = Math.max(1, maxZ - minZ + 1);

        float pad = 8.0F;
        float baseCellSize = Math.max(4.0F, Math.min((areaW - pad * 2.0F) / spanX, (areaH - pad * 2.0F) / spanZ));
        float cellSize = baseCellSize * this.vaultmapper$zoom;
        float gap = Math.max(1.0F, cellSize * 0.12F);
        float step = cellSize + gap;

        float totalW = spanX * step - gap;
        float totalH = spanZ * step - gap;
        float originX = areaX + (areaW - totalW) / 2.0F + this.vaultmapper$panX;
        float originY = areaY + (areaH - totalH) / 2.0F + this.vaultmapper$panY;

        this.input.hit(this.canvas, "map:viewport", areaX, areaY, areaW, areaH)
                .scrollsList(dir -> {
                    float factor = dir > 0 ? 1.15F : 1.0F / 1.15F;
                    this.vaultmapper$zoom = Math.max(0.5F, Math.min(4.0F, this.vaultmapper$zoom * factor));
                });

        for (int i = 0; i < cells.size(); i++) {
            VaultCell cell = cells.get(i);
            float drawX = originX + (cell.x - minX) * step;
            float drawY = originY + (cell.z - minZ) * step;

            if (drawX + cellSize < areaX || drawX > areaX + areaW || drawY + cellSize < areaY || drawY > areaY + areaH) {
                continue;
            }

            int fillColor = vaultmapper$cellColor(cell);
            this.canvas.fill(drawX, drawY, cellSize, cellSize, fillColor, 0.9F);

            if (cellSize >= 7.0F) {
                this.canvas.fill(drawX, drawY, cellSize, 1.0F, 16777215, 0.18F);
                this.canvas.fill(drawX, drawY + cellSize - 1.0F, cellSize, 1.0F, 0, 0.35F);
                this.canvas.fill(drawX, drawY, 1.0F, cellSize, 16777215, 0.18F);
                this.canvas.fill(drawX + cellSize - 1.0F, drawY, 1.0F, cellSize, 0, 0.35F);
            }

            this.input.hit(this.canvas, "map:cell:" + i, drawX, drawY, cellSize, cellSize)
                    .tooltip(() -> vaultmapper$cellTooltip(cell));
        }
    }

    @Unique
    private int vaultmapper$cellColor(VaultCell cell) {
        String hex = VaultMap.getCellColor(cell);
        if (hex != null && !hex.isEmpty()) {
            String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
            try {
                return (int) (Long.parseLong(cleaned, 16) & 0xFFFFFFL);
            } catch (NumberFormatException ignored) {
                // Fall back below if config string isn't a valid hex color
            }
        }

        if (cell.cellType == CellType.CELLTYPE_TUNNEL_X || cell.cellType == CellType.CELLTYPE_TUNNEL_Z) {
            return 0x555555;
        }
        if (cell.cellType == CellType.CELLTYPE_UNKNOWN) {
            return 0x2B2B2B;
        }
        return 0x727272;
    }

    @Unique
    private List<Component> vaultmapper$cellTooltip(VaultCell cell) {
        List<Component> lines = new ArrayList<>();

        String title = (cell.roomName != null && !cell.roomName.isEmpty())
                ? cell.roomName
                : (cell.roomType != null ? cell.roomType.name() : String.valueOf(cell.cellType));
        lines.add(new TextComponent(title));
        lines.add(new TextComponent("Coords: " + cell.x + ", " + cell.z).withStyle(ChatFormatting.GRAY));

        if (cell.marked) {
            lines.add(new TextComponent("Marked").withStyle(ChatFormatting.YELLOW));
        }
        if (cell.inscripted) {
            lines.add(new TextComponent("Inscription Room").withStyle(ChatFormatting.AQUA));
        }
        if (cell.explored) {
            lines.add(new TextComponent("Explored").withStyle(ChatFormatting.GREEN));
        }

        return lines;
    }

    @Unique
    private Optional<MapSnapshot> vaultmapper$resolveSnapshot() {
        if (this.snapshot != null && this.snapshot.getEnd() != null) {
            UUID vaultId = this.snapshot.getEnd().get(Vault.ID);
            if (vaultId != null) {
                Optional<MapSnapshot> saved = MapSnapshot.from(vaultId);
                if (saved.isPresent()) {
                    return saved;
                }
            }
        }
        return Optional.ofNullable(MapSnapshot.lastSnapshotCache);
    }
}