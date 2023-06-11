package net.coderbot.iris.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Class serving as abstraction and
 * centralization for common GUI
 * rendering/other code calls.
 *
 * Helps allow for easier portability
 * to Minecraft 1.17 by abstracting
 * some code that will be changed.
 */
public final class GuiUtil {
	public static final ResourceLocation IRIS_WIDGETS_TEX = new ResourceLocation("iris", "textures/gui/widgets.png");
	private static final Component ELLIPSIS = new TextComponent("...");

	private GuiUtil() {}

	private static Minecraft client() {
		return Minecraft.getInstance();
	}

	/**
	 * Binds Iris's widgets texture to be
	 * used for succeeding draw calls.
	 */
	public static void bindIrisWidgetsTexture() {
		RenderSystem.setShaderTexture(0, IRIS_WIDGETS_TEX);
	}

	/**
	 * Draws a button. Button textures must be mapped with the
	 * same coordinates as those on the vanilla widgets texture.
	 *
	 * @param x X position of the left of the button
	 * @param y Y position of the top of the button
	 * @param width Width of the button, maximum 398
	 * @param height Height of the button, maximum 20
	 * @param hovered Whether the button is being hovered over with the mouse
	 * @param disabled Whether the button should use the "disabled" texture
	 */
	public static void drawButton(PoseStack poseStack, int x, int y, int width, int height, boolean hovered, boolean disabled) {
		// Create variables for half of the width and height.
		// Will not be exact when width and height are odd, but
		// that case is handled within the draw calls.
		int halfWidth = width / 2;
		int halfHeight = height / 2;

		// V offset for which button texture to use
		int vOffset = disabled ? 46 : hovered ? 86 : 66;

		// Sets RenderSystem to use solid white as the tint color for blend mode, and enables blend mode
		RenderSystem.enableBlend();

		// Sets RenderSystem to be able to use textures when drawing
		// This doesn't do anything on 1.17
		RenderSystem.enableTexture();

		// Top left section
		GuiComponent.blit(poseStack, x, y, 0, vOffset, halfWidth, halfHeight, 256, 256);
		// Top right section
		GuiComponent.blit(poseStack, x + halfWidth, y, 200 - (width - halfWidth), vOffset, width - halfWidth, halfHeight, 256, 256);
		// Bottom left section
		GuiComponent.blit(poseStack, x, y + halfHeight, 0, vOffset + (20 - (height - halfHeight)), halfWidth, height - halfHeight, 256, 256);
		// Bottom right section
		GuiComponent.blit(poseStack, x + halfWidth, y + halfHeight, 200 - (width - halfWidth), vOffset + (20 - (height - halfHeight)), width - halfWidth, height - halfHeight, 256, 256);
	}

	/**
	 * Draws a translucent black panel
	 * with a light border.
	 *
	 * @param x The x position of the panel
	 * @param y The y position of the panel
	 * @param width The width of the panel
	 * @param height The height of the panel
	 */
	public static void drawPanel(PoseStack poseStack, int x, int y, int width, int height) {
		drawPanel(poseStack, x, y, width, height, 0xDE);
	}

	/**
	 * Draws a black panel with a light border
	 * and specified translucency.
	 *
	 * @param x The x position of the panel
	 * @param y The y position of the panel
	 * @param width The width of the panel
	 * @param height The height of the panel
	 * @param alpha The translucency of the panel from 0 to 255
	 */
	public static void drawPanel(PoseStack poseStack, int x, int y, int width, int height, int alpha) {
		int borderColor = 0xDEDEDEDE;
		int innerColor = alpha << 24;

		// Top border section
		GuiComponent.fill(poseStack, x, y, x + width, y + 1, borderColor);
		// Bottom border section
		GuiComponent.fill(poseStack, x, (y + height) - 1, x + width, y + height, borderColor);
		// Left border section
		GuiComponent.fill(poseStack, x, y + 1, x + 1, (y + height) - 1, borderColor);
		// Right border section
		GuiComponent.fill(poseStack, (x + width) - 1, y + 1, x + width, (y + height) - 1, borderColor);
		// Inner section
		GuiComponent.fill(poseStack, x + 1, y + 1, (x + width) - 1, (y + height) - 1, innerColor);
	}

	/**
	 * Draws a text with a panel behind it.
	 *
	 * @param text The text component to draw
	 * @param x The x position of the panel
	 * @param y The y position of the panel
	 */
	public static void drawTextPanel(Font font, PoseStack poseStack, Component text, int x, int y) {
		drawPanel(poseStack, x, y, font.width(text) + 8, 16);
		font.drawShadow(poseStack, text, x + 4, y + 4, 0xFFFFFF);
	}

	/**
	 * Draws a texture such that it will fit inside a box of defined
	 * width and height, while preserving the texture's aspect ratio.
	 *
	 * @param x The x position of the left edge of the quad
	 * @param y The y position of the top edge of the quad
	 * @param width The width of the bounding area to draw in
	 * @param height The height of the bounding area to draw in
	 * @param aspectRatio The aspect ratio of the texture being drawn - width / height
	 */
	public static void drawTextureInside(PoseStack poseStack, float x, float y, float width, float height, double aspectRatio) {
		float areaRatio = width / height;
		float quadWidth = aspectRatio > areaRatio ? width : (float) (height * aspectRatio);
		float quadHeight = aspectRatio < areaRatio ? height : (float) (width / aspectRatio);

		drawTexture(poseStack,
			x + (width / 2) - (quadWidth / 2), y + (height / 2) - (quadHeight / 2),
			quadWidth, quadHeight, 0, 0, 1, 1);
	}

	/**
	 * Draws a texture using floating-point specified coordinates.
	 *
	 * @param x The x position of the left edge of the quad
	 * @param y The y position of the top edge of the quad
	 * @param width The width of the quad
	 * @param height The height of the quad
	 * @param u0 The left edge of the texture section (0-1)
	 * @param v0 The top edge of the texture section (0-1)
	 * @param u1 The right edge of the texture section (0-1)
	 * @param v1 The bottom edge of the texture section (0-1)
	 */
	public static void drawTexture(PoseStack poseStack, float x, float y, float width, float height,
								   float u0, float v0, float u1, float v1) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		Matrix4f pose = poseStack.last().pose();

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(pose, x, y + height, 0).uv(u0, v1).endVertex();
		buffer.vertex(pose, x + width, y + height, 0).uv(u1, v1).endVertex();
		buffer.vertex(pose, x + width, y, 0).uv(u1, v0).endVertex();
		buffer.vertex(pose, x, y, 0).uv(u0, v0).endVertex();
		buffer.end();
		BufferUploader.end(buffer);
	}

	/**
	 * Draws a 10px by 11px loading animation at the specified coordinates.
	 *
	 * @param x The x position of the left edge of the animation
	 * @param y The y position of the top edge of the animation
	 */
	public static void drawLoadingAnimation(PoseStack poseStack, int x, int y) {
		// The number being used to mask the system time has 9 bits, meaning it cycles every 512 ms
		// By shifting the masked system time right 6 bits, it will cycle through numbers 0-7 every half second or so
		// Fast and easy way to make an animation timer with 8 frames
		int frame = (int) (System.currentTimeMillis() & 0b111000000) >> 6;

		RenderSystem.enableBlend();
		RenderSystem.enableTexture();
		bindIrisWidgetsTexture();
		GuiComponent.blit(poseStack, x, y, 10 * frame, 245, 10, 11, 256, 256);
	}

	/**
	 * Shorten a text to a specific length, adding an ellipsis (...)
	 * to the end if shortened.
	 *
	 * Text may lose formatting.
	 *
	 * @param font Font to use for determining the width of text
	 * @param text Text to shorten
	 * @param width Width to shorten text to
	 * @return a shortened text
	 */
	public static MutableComponent shortenText(Font font, MutableComponent text, int width) {
		if (font.width(text) > width) {
			return new TextComponent(font.plainSubstrByWidth(text.getString(), width - font.width(ELLIPSIS))).append(ELLIPSIS).setStyle(text.getStyle());
		}
		return text;
	}

	/**
	 * Creates a new translated text, if a translation
	 * is present. If not, will return the default text
	 * component passed.
	 *
	 * @param defaultText Default text to use if no translation is found
	 * @param translationDesc Translation key to try and use
	 * @param format Formatting arguments for the translated text, if created
	 * @return the translated text if found, otherwise the default provided
	 */
	public static MutableComponent translateOrDefault(MutableComponent defaultText, String translationDesc, Object ... format) {
		if (I18n.exists(translationDesc)) {
			return new TranslatableComponent(translationDesc, format);
		}
		return defaultText;
	}

	/**
	 * Truncates a number if greater than 1000 and adds a suffix.
	 * Each suffix corresponds to 1000 to the power of the suffix's index.
	 *
	 * <p>Example: {@code number = 5249, suffixes = ["KB", "MB", "GB"]; returns "5.2KB"}
	 *
	 * @param number The number to truncate
	 * @param suffixes A list of suffixes corresponding to powers of 1000; may be empty
	 * @return a string representing the truncated number
	 */
	public static String truncateNumber(int number, String[] suffixes) {
		if (number < 1000) {
			return Integer.toString(number);
		}

		int powerOf1K = (int) (Math.log(Math.abs(number)) / Math.log(1000));
		String significand = new DecimalFormat("0.#")
			.format((double) number / Math.pow(1000, powerOf1K));

		return significand + (powerOf1K <= suffixes.length ? suffixes[powerOf1K - 1] : "");
	}

	/**
	 * Plays the {@code UI_BUTTON_CLICK} sound event as a
	 * master sound effect.
	 *
	 * Used in non-{@code ButtonWidget} UI elements upon click
	 * or other action.
	 */
	public static void playButtonClickSound() {
		client().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
	}

	/**
	 * A class representing a section of a
	 * texture, to be easily drawn in GUIs.
	 */
	public static class Icon {
		public static final Icon SEARCH = new Icon(0, 0, 7, 8);
		public static final Icon SEARCH_COLORED = new Icon(0, 8, 7, 8);
		public static final Icon CLOSE = new Icon(7, 0, 5, 6);
		public static final Icon CLOSE_COLORED = new Icon(7, 6, 5, 6);
		public static final Icon REFRESH = new Icon(12, 0, 10, 10);
		public static final Icon EXPORT = new Icon(22, 0, 7, 8);
		public static final Icon EXPORT_COLORED = new Icon(29, 0, 7, 8);
		public static final Icon IMPORT = new Icon(22, 8, 7, 8);
		public static final Icon IMPORT_COLORED = new Icon(29, 8, 7, 8);
		public static final Icon EXPLORE = new Icon(36, 0, 10, 10);
		public static final Icon EXPLORE_COLORED = new Icon(46, 0, 10, 10);
		public static final Icon REFRESH_SMALL = new Icon(36, 10, 7, 8);
		public static final Icon REFRESH_SMALL_COLORED = new Icon(43, 10, 7, 8);
		public static final Icon LEFT = new Icon(12, 10, 4, 8);
		public static final Icon RIGHT = new Icon(16, 10, 4, 8);
		public static final Icon CAMERA = new Icon(0, 16, 8, 7);
		public static final Icon CAMERA_COLORED = new Icon(0, 23, 8, 7);

		private final int u;
		private final int v;
		private final int width;
		private final int height;

		public Icon(int u, int v, int width, int height) {
			this.u = u;
			this.v = v;
			this.width = width;
			this.height = height;
		}

		/**
		 * Draws this icon to the screen at the specified coordinates.
		 *
		 * @param x The x position to draw the icon at (left)
		 * @param y The y position to draw the icon at (top)
		 */
		public void draw(PoseStack poseStack, int x, int y) {
			// Sets RenderSystem to use solid white as the tint color for blend mode (1.16), and enables blend mode
			RenderSystem.enableBlend();

			// Sets RenderSystem to be able to use textures when drawing
			RenderSystem.enableTexture();

			// Draw the texture to the screen
			GuiComponent.blit(poseStack, x, y, u, v, width, height, 256, 256);
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}
	}
}
