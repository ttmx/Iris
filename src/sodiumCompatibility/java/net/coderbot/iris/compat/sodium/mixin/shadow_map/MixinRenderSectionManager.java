package net.coderbot.iris.compat.sodium.mixin.shadow_map;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkTracker;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.graph.GraphSearch;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import net.coderbot.iris.pipeline.ShadowRenderer;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.coderbot.iris.compat.sodium.impl.shadow_map.SwappableRenderSectionManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Modifies {@link RenderSectionManager} to support maintaining a separate visibility list for the shadow camera, as well
 * as disabling chunk rebuilds when computing visibility for the shadow camera.
 */
@Mixin(RenderSectionManager.class)
public class MixinRenderSectionManager implements SwappableRenderSectionManager {
    @Shadow(remap = false)
    @Final
    @Mutable
    private ChunkRenderList renderList;

    @Shadow(remap = false)
    @Final
    @Mutable
	private GraphSearch.GraphSearchPool graphSearchPool;

	@Shadow(remap = false)
	private boolean needsUpdate;

    @Unique
    private ChunkRenderList chunkRenderListSwap;

    @Unique
    private GraphSearch.GraphSearchPool graphSearchPoolSwap;

    @Unique
    private ObjectList<BlockEntity> visibleBlockEntitiesSwap;

    @Unique
	private boolean needsUpdateSwap;

    @Unique
    private static final ObjectArrayFIFOQueue<?> EMPTY_QUEUE = new ObjectArrayFIFOQueue<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void iris$onInit(SodiumWorldRenderer worldRenderer, ClientLevel world, int renderDistance, ChunkTracker chunkTracker, CommandList commandList, CallbackInfo ci) {
        this.chunkRenderListSwap = null;
        this.graphSearchPoolSwap = new GraphSearch.GraphSearchPool();
        this.visibleBlockEntitiesSwap = new ObjectArrayList<>();
        this.needsUpdateSwap = true;
    }

    @Override
    public void iris$swapVisibilityState() {
        ChunkRenderList chunkRenderListTmp = renderList;
        renderList = chunkRenderListSwap;
        chunkRenderListSwap = chunkRenderListTmp;

        GraphSearch.GraphSearchPool graphSearchPoolTmp = graphSearchPool;
		graphSearchPool = graphSearchPoolSwap;
		graphSearchPoolSwap = graphSearchPoolTmp;

        boolean needsUpdateTmp = needsUpdate;
        needsUpdate = needsUpdateSwap;
        needsUpdateSwap = needsUpdateTmp;
    }

	// TODO: check needsUpdate and needsUpdateSwap patches?
}
