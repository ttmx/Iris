package net.coderbot.iris.gui.screen.browser;

import net.coderbot.iris.Iris;
import net.irisshaders.iris.api.v0.browser.IrisPackDownloadSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

public class ExternalPackBrowser {
	public static final Component TEXT_NO_RESULTS = new TranslatableComponent("iris.query.status.none")
		.withStyle(ChatFormatting.RED);
	public static final ExecutorService QUERY_EXECUTOR = Executors.newSingleThreadExecutor();

	public Runnable entryListener = () -> {};
	public Runnable querySubmissionListener = () -> {};
	public IntConsumer pageChangedListener = page -> {};

	private @Nullable Component statusMessage = null;
	private IrisPackDownloadSource packDownloadSource;
	private List<IrisPackDownloadSource.PackEntry> entries = List.of();
	private final Map<IrisPackDownloadSource.PackEntry, DownloadedImage> packIcons = new HashMap<>();
	private int page;
	private String pendingSearchQuery = "";
	private String searchQuery = "";

	/**
	 * <b>Call this method with care</b>, do not submit queries too often
	 */
	public void submitActiveQuery() {
		querySubmissionListener.run();

		final int count = this.packDownloadSource.entriesPerPage();
		final CompletableFuture<List<IrisPackDownloadSource.PackEntry>> future = this.packDownloadSource.query(
			this.searchQuery, Math.max(0, this.page * count), Math.max(1, count), QUERY_EXECUTOR);

		future.whenComplete((result, err) -> Minecraft.getInstance().execute(() -> {
			if (result != null) {
				this.entries = result;
				this.updatePage(this.page);
				this.entryListener.run();
				this.statusMessage = null;

				return;
			}

			if (err == null) {
				this.statusMessage = TEXT_NO_RESULTS;
			} else {
				this.statusMessage = new TextComponent(err.getLocalizedMessage()).withStyle(ChatFormatting.RED);
			}
			this.entries = List.of();
			this.updatePage(this.page);
		}));
	}

	public void setDownloadSource(IrisPackDownloadSource source) {
		if (this.packDownloadSource != null) {
			try {
				this.packDownloadSource.close();
			} catch (Exception e) {
				Iris.logger.warn(e);
			}
		}

		this.packDownloadSource = source;
		this.submitActiveQuery();
	}

	public IrisPackDownloadSource getDownloadSource() {
		return this.packDownloadSource;
	}

	public void clearImageData() {
		for (DownloadedImage image : packIcons.values()) {
			try {
				image.close();
			} catch (IOException e) {
				Iris.logger.error("Error closing pack icon", e);
			}
		}
		this.packIcons.clear();
	}

	public void putPackIcon(IrisPackDownloadSource.PackEntry entry) {
		if (!this.packIcons.containsKey(entry)) {
			DownloadedImage.download(entry.iconUrl(), Minecraft.getInstance(),
				this.packDownloadSource, ExternalPackBrowser.QUERY_EXECUTOR)
				.whenComplete((result, err) -> {
					if (err != null) {
						Iris.logger.error("Error downloading pack icon", err);
					} else if (result != null) {
						this.packIcons.put(entry, result);
					}
				});
		}
	}

	public @Nullable DownloadedImage getPackIcon(IrisPackDownloadSource.PackEntry entry) {
		return this.packIcons.get(entry);
	}

	/**
	 * <b>Call this method with care</b>, do not submit queries too often
	 */
	public void search() {
		this.searchQuery = pendingSearchQuery;
		this.updatePage(0);
		this.submitActiveQuery();
	}

	public void pendSearchQuery(String query) {
		this.pendingSearchQuery = query;
	}

	public String getActiveQuery() {
		return this.searchQuery;
	}

	public void updatePage(int page) {
		this.page = page;
		this.pageChangedListener.accept(page);
	}

	public int getPage() {
		return this.page;
	}

	public boolean atMaxPage() {
		return this.entries.size() < this.packDownloadSource.entriesPerPage();
	}

	public List<IrisPackDownloadSource.PackEntry> getEntries() {
		return this.entries;
	}

	public @Nullable Component getStatusMessage() {
		return this.statusMessage;
	}
}
