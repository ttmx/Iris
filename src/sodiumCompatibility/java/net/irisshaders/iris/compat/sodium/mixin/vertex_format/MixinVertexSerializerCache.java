package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.MemoryTransfer;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializer;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializerCache;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.EntityToGlintVertexSerializer;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.EntityToModelVertexSerializer;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.EntityToTerrainVertexSerializer;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = VertexSerializerCache.class, remap = false)
public abstract class MixinVertexSerializerCache {
	@Shadow
	@Final
	private static Long2ReferenceMap<VertexSerializer> CACHE;

	@Shadow
	private static List<MemoryTransfer> mergeAdjacentMemoryTransfers(ArrayList<MemoryTransfer> src) {
		return null;
	}

	@Shadow
	protected static long getSerializerKey(VertexFormatDescription a, VertexFormatDescription b) {
		return 0;
	}

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void putSerializerIris(CallbackInfo ci) {
		CACHE.put(getSerializerKey(VertexFormatRegistry.get(IrisVertexFormats.ENTITY), VertexFormatRegistry.get(IrisVertexFormats.TERRAIN)), new EntityToTerrainVertexSerializer());
		CACHE.put(getSerializerKey(VertexFormatRegistry.get(IrisVertexFormats.ENTITY), VertexFormatRegistry.get(DefaultVertexFormat.NEW_ENTITY)), new EntityToModelVertexSerializer());
		CACHE.put(getSerializerKey(VertexFormatRegistry.get(IrisVertexFormats.ENTITY), VertexFormatRegistry.get(DefaultVertexFormat.POSITION_TEX)), new EntityToGlintVertexSerializer());
	}

	@Inject(method = "createSerializer", at = @At("HEAD"))
	private static void wtfAreYouEvenMaking(VertexFormatDescription srcVertexFormat, VertexFormatDescription dstVertexFormat, CallbackInfoReturnable<VertexSerializer> cir) {
		int i = 0;
		for (VertexFormatElement element : dstVertexFormat.getElements()) {
			System.out.println(element.getType() + " " + element.getUsage() + " " + element.getCount() + " " + i);
			i += element.getByteSize();
		}
	}
}
