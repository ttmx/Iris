package net.coderbot.iris.gui.screen.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.GuiUtil;
import net.coderbot.iris.gui.element.IrisObjectSelectionList;
import net.irisshaders.iris.api.v0.browser.IrisPackDownloadSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import java.util.List;

public class GalleryScreen extends Screen {
	private static final Component LEFT = new TextComponent("<");
	private static final Component RIGHT = new TextComponent(">");
	private final Screen parent;
	private final IrisPackDownloadSource downloadSource;
	private final List<String> urls;
	private final IntSet queried = new IntArraySet();
	private final Int2ObjectMap<DownloadedImage> gallery = new Int2ObjectOpenHashMap<>();
	private int imageIndex = 0;

	public GalleryScreen(Component title, Screen parent, IrisPackDownloadSource downloadSource, List<String> urls) {
		super(title);
		this.parent = parent;
		this.downloadSource = downloadSource;
		this.urls = urls;

		this.tryQueryImage(0);
	}

	private void clearGallery() {
		for (DownloadedImage image : gallery.values()) {
			try {
				image.close();
			} catch (Throwable e) {
				Iris.logger.error("Error closing gallery image", e);
			}
		}
		this.gallery.clear();
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
		if (this.minecraft.level == null) {
			this.renderBackground(poseStack);
		} else {
			this.fillGradient(poseStack, 0, 0, width, height, 0x4F232323, 0x4F232323);
		}

		super.render(poseStack, mouseX, mouseY, delta);
		drawCenteredString(poseStack, this.font, this.title, (int) (this.width * 0.5), 8, 0xFFFFFF);

		GuiUtil.drawPanel(poseStack, 52, 36, this.width - 104, this.height - 74, 0xBF);
		this.renderGalleryImage(poseStack);
	}

	private void renderGalleryImage(PoseStack poseStack) {
		DownloadedImage image = this.gallery.get(this.imageIndex);
		if (image != null && image.isValid()) {
			RenderSystem.enableTexture();
			RenderSystem.enableBlend();
			RenderSystem.setShaderTexture(0, image.location);
			GuiUtil.drawTextureInside(poseStack, 53, 37,
				this.width - 100, this.height - 106, image.aspectRatio);
			return;
		}

		GuiUtil.drawLoadingAnimation(poseStack, (this.width / 2) - 5, (37 + (this.height - 76) / 2) - 5);
	}

	@Override
	protected void init() {
		super.init();
		int center = this.width / 2;
		boolean inWorld = this.minecraft.level != null;

		this.clearWidgets();

		IrisObjectSelectionList<DummyListEntry> background = new IrisObjectSelectionList<>(
			this.minecraft, this.width, this.height, 32, this.height - 34, 0, this.width, 5);
		this.addRenderableWidget(background);
		this.addRenderableWidget(new Button(center - 76, this.height - 27, 152, 20,
			CommonComponents.GUI_DONE, button -> onClose()));

		this.addRenderableWidget(new Button(center - 100, this.height - 27, 20, 20,
			LEFT, button -> this.changeImageIndex(-1)));
		this.addRenderableWidget(new Button(center + 80, this.height - 27, 20, 20,
			RIGHT, button -> this.changeImageIndex(1)));

		if (inWorld) {
			background.setRenderBackground(false);
		}
	}

	private void tryQueryImage(int index) {
		if (!this.queried.contains(index)) {
			this.queried.add(index);

			DownloadedImage.download(
				this.urls.get(index), Minecraft.getInstance(), this.downloadSource, ExternalPackBrowser.QUERY_EXECUTOR)
				.whenComplete((result, err) -> {
					if (err != null) {
						Iris.logger.error("Error downloading gallery image", err);
					} else if (result != null) {
						this.gallery.put(index, result);
					}
				});
		}
	}

	public void changeImageIndex(int by) {
		if (this.urls.size() > 0) {
			this.imageIndex = Math.floorMod(this.imageIndex + by, this.urls.size());
		}
		this.tryQueryImage(this.imageIndex);
	}

	@Override
	public void onClose() {
		this.clearGallery();

		this.minecraft.setScreen(this.parent);
	}

	private static class DummyListEntry extends ObjectSelectionList.Entry<GalleryScreen.DummyListEntry> {
		@Override
		public Component getNarration() {
			return TextComponent.EMPTY;
		}

		@Override
		public void render(PoseStack arg, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {}
	}
}
