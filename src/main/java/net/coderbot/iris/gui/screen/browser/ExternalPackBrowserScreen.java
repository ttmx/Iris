package net.coderbot.iris.gui.screen.browser;

import com.mojang.blaze3d.vertex.PoseStack;
import net.coderbot.iris.apiimpl.IrisApiV0ModrinthPackDownloadSource;
import net.coderbot.iris.gui.element.ExternalPackBrowserList;
import net.coderbot.iris.gui.screen.HudHideable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Nullable;

public class ExternalPackBrowserScreen extends Screen implements HudHideable {
	private @Nullable ExternalPackBrowserList browserList = null;
	private EditBox searchBox;
	private final ExternalPackBrowser browser = new ExternalPackBrowser();
	private final Screen parent;

	public ExternalPackBrowserScreen(Screen parent) {
		super(new TranslatableComponent("options.iris.shaderPackBrowser.title"));

		this.parent = parent;

		this.browser.setDownloadSource(new IrisApiV0ModrinthPackDownloadSource());
		this.browser.querySubmissionListener = () -> {
			if (this.browserList != null) {
				this.browserList.reset();
				this.browser.clearImageData();
			}
		};
		this.browser.entryListener = () -> {
			if (this.browserList != null) {
				this.browserList.addPackEntries(browser.getDownloadSource(), browser.getEntries());
			}
		};
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
		if (this.minecraft.level == null) {
			this.renderBackground(poseStack);
		} else {
			this.fillGradient(poseStack, 0, 0, width, height, 0x4F232323, 0x4F232323);
		}

		if (browserList != null) {
			this.browserList.render(poseStack, mouseX, mouseY, delta);
		}

		super.render(poseStack, mouseX, mouseY, delta);
		drawCenteredString(poseStack, this.font, this.title, (int) (this.width * 0.5), 8, 0xFFFFFF);
	}

	@Override
	protected void init() {
		super.init();
		int center = this.width / 2 - 76;
		boolean inWorld = this.minecraft.level != null;

		this.clearWidgets();

		this.searchBox = new EditBox(
			this.font, 0, 0, 1, ExternalPackBrowserList.ToolbarEntry.TOOLBAR_HEIGHT - 2, TextComponent.EMPTY);
		this.searchBox.setValue(this.browser.getActiveQuery());
		this.browserList = new ExternalPackBrowserList(this.browser, this.searchBox, this.minecraft,
			this.width, this.height, 32, this.height - 34, 0, this.width);

		this.addWidget(this.searchBox);
		this.addWidget(this.browserList);
		this.addRenderableWidget(new Button(center + 78, this.height - 27, 152, 20,
			CommonComponents.GUI_DONE, button -> onClose()));

		if (inWorld) {
			this.browserList.setRenderBackground(false);
		}

		this.browserList.reset();
		this.browserList.addPackEntries(this.browser.getDownloadSource(), browser.getEntries());
	}

	@Override
	public void onClose() {
		this.browser.clearImageData();
		this.minecraft.setScreen(parent);
	}
}
