package net.coderbot.iris.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.coderbot.iris.gui.GuiUtil;
import net.coderbot.iris.gui.screen.browser.DownloadedImage;
import net.coderbot.iris.gui.screen.browser.ExternalPackBrowser;
import net.coderbot.iris.gui.screen.browser.GalleryScreen;
import net.irisshaders.iris.api.v0.browser.IrisPackDownloadSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.List;

public class ExternalPackBrowserList extends IrisObjectSelectionList<ExternalPackBrowserList.BaseEntry> {
	public final ExternalPackBrowser browser;
	private final ToolbarEntry toolbar;

	public ExternalPackBrowserList(ExternalPackBrowser browser, EditBox searchBox, Minecraft client,
								   int width, int height, int top, int bottom, int left, int right) {
		super(client, width, height, top, bottom, left, right, 38);

		this.browser = browser;
		this.toolbar = new ToolbarEntry(browser, searchBox);
	}

	public void reset() {
		this.clearEntries();

		this.addEntry(toolbar);
	}

	public void addPackEntries(IrisPackDownloadSource source, List<IrisPackDownloadSource.PackEntry> entries) {
		for (IrisPackDownloadSource.PackEntry entry : entries) {
			this.addEntry(new PackResultEntry(this, source, entry));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(358, width - 50);
	}

	public static abstract class BaseEntry extends ObjectSelectionList.Entry<ExternalPackBrowserList.BaseEntry> {
		protected BaseEntry() {}
	}

	public static class PackResultEntry extends BaseEntry {
		public static final Component GALLERY_TOOLTIP = new TranslatableComponent("options.iris.gallery");
		private static final int ACTION_BAR_HEIGHT = 15;
		private static final String[] DOWNLOAD_COUNT_SUFFIXES = {"K", "M", "B", "T", "P", "E"};
		private final Font font = Minecraft.getInstance().font;
		private final IrisPackDownloadSource.PackEntry entry;
		private final ExternalPackBrowserList list;
		private final Component label;
		private final Component downloads;
		private final IrisElementRow actions = new IrisElementRow();

		public PackResultEntry(ExternalPackBrowserList list, IrisPackDownloadSource source, IrisPackDownloadSource.PackEntry entry) {
			this.list = list;
			this.entry = entry;
			this.label = new TextComponent(entry.title())
				.append(new TranslatableComponent("pack.iris.author", entry.author())
					.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			this.downloads = new TextComponent(GuiUtil.truncateNumber(entry.downloads(), DOWNLOAD_COUNT_SUFFIXES))
				.withStyle(style -> style.withColor(0x2bccff));
			list.browser.putPackIcon(this.entry);

			if (this.entry.galleryUrls().size() > 0) {
				actions.add(new IrisElementRow.IconButtonElement(
					GuiUtil.Icon.CAMERA, GuiUtil.Icon.CAMERA_COLORED,
					button -> {
						GuiUtil.playButtonClickSound();
						Minecraft.getInstance().setScreen(
							new GalleryScreen(this.label, Minecraft.getInstance().screen, source, entry.galleryUrls()));

						return true;
					}
				), 16);
			}

			for (IrisPackDownloadSource.PackEntryAction action : source.packEntryActions()) {
				var text = new TranslatableComponent(action.localizationKey());
				actions.add(new IrisElementRow.TextButtonElement(text, button -> {
					action.action().accept(entry, ExternalPackBrowser.QUERY_EXECUTOR);
					GuiUtil.playButtonClickSound();

					return true;
				}), font.width(text) + 8);
			}
		}

		@Override
		public Component getNarration() {
			return this.label;
		}

		@Override
		public void render(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			int width = entryWidth - 1;
			int iconSize = entryHeight - 6;

			GuiUtil.drawPanel(poseStack, x, y, width, entryHeight, 0x5A);
			GuiUtil.drawPanel(poseStack, x + 2, y + 2, iconSize + 2, iconSize + 2);

			DownloadedImage icon = this.list.browser.getPackIcon(this.entry);
			if (icon != null && icon.isValid()) {
				RenderSystem.enableBlend();
				RenderSystem.enableTexture();
				RenderSystem.setShaderTexture(0, icon.location);
				GuiUtil.drawTextureInside(poseStack, x + 3, y + 3, iconSize, iconSize, icon.aspectRatio);
			} else {
				GuiUtil.drawLoadingAnimation(poseStack, x + (iconSize / 2) - 2, y + (iconSize / 2) - 2);
			}

			font.drawShadow(poseStack, this.label, x + 8 + iconSize, y + 6, 0xFFFFFFFF);
			this.actions.render(poseStack, (x + width) - (this.actions.getRowWidth() + 2),
				(y + entryHeight) - (ACTION_BAR_HEIGHT + 3), ACTION_BAR_HEIGHT, mouseX, mouseY, tickDelta, hovered);

			int downloadLabelWidth = font.width(this.downloads);
			font.drawShadow(poseStack, this.downloads, (x + width) - (downloadLabelWidth + 5), y + 5, 0xFFFFFFFF);
			GuiUtil.bindIrisWidgetsTexture();
			GuiUtil.Icon.IMPORT_COLORED.draw(poseStack, (x + width) - (downloadLabelWidth + 15), y + 5);

			String desc = this.entry.description();
			int maxDescWidth = width - (this.actions.getRowWidth() + iconSize + 20);
			if (font.width(desc) > maxDescWidth) {
				desc = font.plainSubstrByWidth(desc, maxDescWidth) + "...";
			}
			font.drawShadow(poseStack, desc, x + 8 + iconSize, (y + entryHeight) - 14, 0xFFBCBCBC);
		}

		@Override
		public boolean mouseClicked(double mx, double my, int button) {
			return this.actions.mouseClicked(mx, my, button);
		}
	}

	public static class ToolbarEntry extends BaseEntry {
		public static final int TOOLBAR_HEIGHT = 17;
		private static final int NAV_BUTTON_WIDTH = 16;
		private static final int QUERY_BUTTON_WIDTH = 17;
		private static final int NUMBER_DISPLAY_WIDTH = 23;

		private final IrisElementRow navigation = new IrisElementRow();
		private final IrisElementRow query = new IrisElementRow();
		private final IrisElementRow.Element pagePrevious;
		private final IrisElementRow.Element pageNext;
		private final IrisElementRow.TextButtonElement pageNumber;
		private final EditBox searchBox;
		private final ExternalPackBrowser browser;

		public ToolbarEntry(ExternalPackBrowser browser, EditBox searchBox) {
			this.browser = browser;
			this.searchBox = searchBox;
			searchBox.setResponder(browser::pendSearchQuery);

			this.pageNumber = new IrisElementRow.TextButtonElement(TextComponent.EMPTY, el -> false);
			pageNumber.disabled = true;
			browser.pageChangedListener = this::updatePage;
			this.pagePrevious = new IrisElementRow.IconButtonElement(
				GuiUtil.Icon.LEFT,
				button -> {
					browser.updatePage(browser.getPage() - 1);
					browser.submitActiveQuery();
					GuiUtil.playButtonClickSound();

					return true;
				}
			);
			this.pageNext = new IrisElementRow.IconButtonElement(
				GuiUtil.Icon.RIGHT,
				button -> {
					browser.updatePage(browser.getPage() + 1);
					browser.submitActiveQuery();
					GuiUtil.playButtonClickSound();

					return true;
				}
			);
			this.updatePage(browser.getPage());

			navigation.add(this.pagePrevious, NAV_BUTTON_WIDTH);
			navigation.add(this.pageNumber, NUMBER_DISPLAY_WIDTH);
			navigation.add(this.pageNext, NAV_BUTTON_WIDTH);
			navigation.add(new IrisElementRow.IconButtonElement(
				GuiUtil.Icon.REFRESH_SMALL, GuiUtil.Icon.REFRESH_SMALL_COLORED,
				button -> {
					browser.submitActiveQuery();
					GuiUtil.playButtonClickSound();

					return true;
				}
			), QUERY_BUTTON_WIDTH);

			query.add(new IrisElementRow.IconButtonElement(
				GuiUtil.Icon.SEARCH, GuiUtil.Icon.SEARCH_COLORED,
				button -> {
					browser.search();
					GuiUtil.playButtonClickSound();

					return true;
				}
			), QUERY_BUTTON_WIDTH);
			query.add(new IrisElementRow.IconButtonElement(
				GuiUtil.Icon.CLOSE, GuiUtil.Icon.CLOSE_COLORED,
				button -> {
					this.searchBox.setValue("");
					if (!this.browser.getActiveQuery().isEmpty()) {
						this.browser.search();
					}
					GuiUtil.playButtonClickSound();

					return true;
				}
			), QUERY_BUTTON_WIDTH);
		}

		private void updatePage(int page) {
			this.pageNumber.text = new TextComponent(Integer.toString(page));
			this.pagePrevious.disabled = page <= 0;
			this.pageNext.disabled = this.browser.atMaxPage();
		}

		@Override
		public Component getNarration() {
			// FIXME
			return searchBox.getMessage();
		}

		@Override
		public void render(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			this.searchBox.x = x + 1 + this.navigation.getRowWidth();
			this.searchBox.y = y + 1;
			this.searchBox.setWidth(entryWidth - (this.navigation.getRowWidth() + this.query.getRowWidth() + 3));

			this.navigation.render(poseStack, x, y, TOOLBAR_HEIGHT, mouseX, mouseY, tickDelta, hovered);
			this.searchBox.render(poseStack, mouseX, mouseY, tickDelta);
			this.query.render(poseStack,
				x + (entryWidth - this.query.getRowWidth()), y, TOOLBAR_HEIGHT, mouseX, mouseY, tickDelta, hovered);

			fill(poseStack, x - 3, (y + entryHeight) - 7, x + entryWidth, (y + entryHeight) - 6, 0x66BEBEBE);
		}

		@Override
		public boolean mouseClicked(double mx, double my, int button) {
			return this.navigation.mouseClicked(mx, my, button) || this.query.mouseClicked(mx, my, button);
		}
	}
}
