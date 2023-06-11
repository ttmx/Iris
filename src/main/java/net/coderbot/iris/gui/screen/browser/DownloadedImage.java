package net.coderbot.iris.gui.screen.browser;

import com.mojang.blaze3d.platform.NativeImage;
import net.coderbot.iris.Iris;
import net.irisshaders.iris.api.v0.browser.IrisPackDownloadSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DownloadedImage implements Closeable {
	public final DynamicTexture texture;
	public final ResourceLocation location;
	public final double aspectRatio;
	private final TextureManager textureManager;

	private boolean valid = true;

	private DownloadedImage(TextureManager textureManager, DynamicTexture texture, ResourceLocation location, double aspectRatio) {
		this.textureManager = textureManager;
		this.texture = texture;
		this.location = location;
		this.aspectRatio = aspectRatio;
	}

	public static CompletableFuture<DownloadedImage> download(String uri, Minecraft client, IrisPackDownloadSource source, ExecutorService executor) {
		CompletableFuture<DownloadedImage> future = new CompletableFuture<>();

		executor.submit(() -> {
			try (InputStream data = source.queryImageSynchronously(uri)) {
				NativeImage image = NativeImage.read(data);
				DynamicTexture texture = new DynamicTexture(image);
				ResourceLocation location = new ResourceLocation(Iris.MODID, "download/" + UUID.randomUUID());
				client.getTextureManager().register(location, texture);

				future.complete(
					new DownloadedImage(client.getTextureManager(), texture, location, (double) image.getWidth() / image.getHeight()));
				return;
			} catch (Throwable ex) {
				future.completeExceptionally(ex);
			}

			future.completeExceptionally(new UnsupportedOperationException(
				"Failed to download image from '" + uri + "', unknown error"));
		});

		return future;
	}

	@Override
	public void close() throws IOException {
		this.textureManager.release(this.location);
		this.texture.close();
		this.valid = false;
	}

	public boolean isValid() {
		return valid;
	}
}
