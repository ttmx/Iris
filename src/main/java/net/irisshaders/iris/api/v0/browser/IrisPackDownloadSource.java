package net.irisshaders.iris.api.v0.browser;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public interface IrisPackDownloadSource extends AutoCloseable {
	String localizationKey();

	int entriesPerPage();

	CompletableFuture<List<PackEntry>> query(String searchQuery, int startIndex, int count, ExecutorService service);

	InputStream queryImageSynchronously(String uri) throws IOException;

	PackEntryAction[] packEntryActions();

	record QueryResult(List<PackEntry> packEntries, @Nullable String error) {
	}

	record PackEntry(String title, String description, String author, int downloads, String iconUrl, String pageUrl,
					 List<String> galleryUrls) {
	}

	record PackEntryAction(String localizationKey, BiConsumer<PackEntry, ExecutorService> action) {}
}
