package net.coderbot.iris.apiimpl;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.coderbot.iris.IrisLogging;
import net.irisshaders.iris.api.v0.browser.IrisPackDownloadSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class IrisApiV0ModrinthPackDownloadSource implements IrisPackDownloadSource {
	public static final IrisLogging logger = new IrisLogging("Iris | Modrinth API");

	public static final String LOCALIZATION_KEY = "iris.modrinth";
	public static final String ERR_INVALID_JSON = "Invalid JSON response!";
	public static final PackEntryAction[] ACTIONS = {
		new PackEntryAction("options.iris.modrinthPack.openPage", (entry, exe) -> {
			final Screen parent = Minecraft.getInstance().screen;

			Minecraft.getInstance().setScreen(new ConfirmLinkScreen(
				opened -> Minecraft.getInstance().setScreen(parent),
				entry.pageUrl(),
				true
			));
		})
	};

	public static final String API = "https://api.modrinth.com/v2";
	public static final String SITE = "https://shaders-pre-product.modrinth.com/shader";
	public static final int MAX_INTERVAL_MS = 1200; // Requests sent at shorter intervals than this will be considered "rapid"
	public static final int RAPID_REQUEST_LIMIT = 35; // After this many "rapid" requests, do not send any more

	private static final String requestParams =
		"/search?query=%s&offset=%s&limit=%s&facets=[[%%22categories:%%27iris%%27%%22]]";
	private static final Gson gson = new Gson();

	private final HttpClient http = HttpClients.createDefault();

	// Used as a failsafe to prevent request spam
	private static long lastRequestTimestamp = System.currentTimeMillis();
	private static int panic = 0;

	@Override
	public String localizationKey() {
		return LOCALIZATION_KEY;
	}

	@Override
	public int entriesPerPage() {
		return 12;
	}

	@Override
	public CompletableFuture<List<PackEntry>> query(String searchQuery, int startIndex, int count, ExecutorService service) {
		CompletableFuture<List<PackEntry>> future = new CompletableFuture<>();

		service.submit(() -> {
			long time = System.currentTimeMillis();
			if (time - lastRequestTimestamp < MAX_INTERVAL_MS) {
				panic++;
			} else {
				panic = 0;
			}
			lastRequestTimestamp = time;

			if (panic > RAPID_REQUEST_LIMIT) {
				logger.warn("Requests are being sent too quickly!");
				future.completeExceptionally(new UnsupportedOperationException());

				return;
			}

			final String endpoint = API + String.format(requestParams, searchQuery, startIndex, count);
			HttpGet request = new HttpGet(endpoint);
			request.addHeader("content-type", "application/json");

			try {
				String response = this.http.execute(request, new BasicResponseHandler());
				logger.info("GET " + endpoint);

				JsonObject json = gson.fromJson(response, JsonObject.class);
				JsonArray hits = json.getAsJsonArray("hits");
				if (hits != null) {
					ImmutableList.Builder<PackEntry> packEntries = ImmutableList.builder();
					for (JsonElement el : hits) {
						ModrinthHit hit = gson.fromJson(el, ModrinthHit.class);
						packEntries.add(new PackEntry(
							hit.title, hit.description, hit.author, hit.downloads, hit.icon_url,
							SITE + "/" + hit.slug, List.of(hit.gallery)
						));
					}

					future.complete(packEntries.build());
					return;
				}

				future.completeExceptionally(new IllegalArgumentException(ERR_INVALID_JSON));
			} catch (IOException e) {
				future.completeExceptionally(e);
			}
		});

		return future;
	}

	@Override
	public InputStream queryImageSynchronously(String uri) throws IOException {
		HttpGet request = new HttpGet(uri);
		request.setHeader("content-type", "image/png");
		HttpResponse response = this.http.execute(request);
		logger.info("GET " + uri);

		return response.getEntity().getContent();
	}

	@Override
	public PackEntryAction[] packEntryActions() {
		return ACTIONS;
	}

	@Override
	public void close() throws Exception {
		if (this.http instanceof AutoCloseable closeable) {
			closeable.close();
		}
	}

	static class ModrinthHit {
		String slug;
		String title;
		String description;
		String author;
		int downloads;
		String icon_url;
		String[] gallery;
	}
}
