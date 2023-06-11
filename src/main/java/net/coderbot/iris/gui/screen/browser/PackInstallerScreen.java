package net.coderbot.iris.gui.screen.browser;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.coderbot.iris.Iris;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class PackInstallerScreen extends Screen {
	private final String packName;
	private final boolean wasFullscreen;
	private Screen parent;
	private final URL packUrl;
	private int notificationDialogTimer;
	private MutableComponent notificationDialog;

	public PackInstallerScreen(Screen parent, String packName, URL packUrl, boolean wasFullscreen) {
		super(new TextComponent("Iris Shaderpack Installer"));
		this.parent = parent;
		this.packName = packName;
		this.packUrl = packUrl;
		this.wasFullscreen = wasFullscreen;
	}

	@Override
	protected void init() {
		super.init();
		int center = this.width / 2 - 76;

		this.clearWidgets();

		this.addRenderableWidget(new Button(10, 70, 152, 20, new TextComponent("Download pack"), (button) -> {
			Util.getPlatform().openUrl(packUrl);
		}));

		this.addRenderableWidget(new Button(center, this.height - 27, 152, 20,
			CommonComponents.GUI_CANCEL, button -> onClose()));
	}

	@Override
	public void render(PoseStack poseStack, int pInt1, int pInt2, float pFloat3) {
		this.renderBackground(poseStack);

		super.render(poseStack, pInt1, pInt2, pFloat3);
		drawCenteredString(poseStack, this.font, "Iris Shaderpack Installer", (int) (this.width * 0.5), 8, 0xFFFFFF);
		if (notificationDialogTimer > 0) {
			drawCenteredString(poseStack, this.font, notificationDialog, (int) (this.width * 0.5), 20, 0xFFFFFF);
		}

		font.drawWordWrap(new TextComponent("Download the shaderpack from the link. Use the top featured download if unsure."), 10, 40, (int) (width / 1.5f), 0xFFFFFF);
		font.drawWordWrap(new TextComponent("Then, simply drag the zip file into this window."), 10, 130, (int) (width / 1.5f), 0xFFFFFF);

	}

	@Override
	public void onFilesDrop(List<Path> packs) {

		if (packs.size() > 1) {
			Iris.logger.warn("Trying to copy multiple shader packs during install");

			this.notificationDialog = new TranslatableComponent(
				"options.iris.shaderPackSelection.tooManyFiles"
			).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED);

			this.notificationDialogTimer = 100;

			return;
		}

		for (Path pack : packs) {
			String fileName = pack.getFileName().toString();

			if (Iris.isValidShaderpack(pack)) {
				try {
					Iris.getShaderpacksDirectoryManager().copyPackIntoDirectory(fileName, pack);
					onClose();
				} catch (FileAlreadyExistsException e) {
					this.notificationDialog = new TranslatableComponent(
						"options.iris.shaderPackSelection.copyErrorAlreadyExists",
						fileName
					).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED);

					this.notificationDialogTimer = 100;

					return;
				} catch (IOException e) {
					Iris.logger.warn("Error copying dragged shader pack", e);

					this.notificationDialog = new TranslatableComponent(
						"options.iris.shaderPackSelection.copyError",
						fileName
					).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED);

					this.notificationDialogTimer = 100;

					return;
				}
			} else {
				Iris.logger.warn("Tried to add an invalid shaderpack");

				this.notificationDialog = new TranslatableComponent(
					"options.iris.shaderPackSelection.failedAddSingle",
					fileName
				).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED);

				this.notificationDialogTimer = 100;

				return;
			}
		}
	}

	@Override
	public void onClose() {
		if (parent instanceof ExternalPackBrowserScreen screen) {
			parent = screen.parent;
		}

		if (wasFullscreen && !Minecraft.getInstance().getWindow().isFullscreen()) {
			Minecraft.getInstance().getWindow().toggleFullScreen();
		}

		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public void tick() {
		super.tick();

		if (this.notificationDialogTimer > 0) {
			this.notificationDialogTimer--;
		}
	}
}
