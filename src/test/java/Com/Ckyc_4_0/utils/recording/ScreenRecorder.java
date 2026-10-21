/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.recording;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records the automation browser to an AVI file. The frames come from the browser itself (Chrome DevTools "screencast" on
 * a separate connection), NOT from the screen: another window, tab or application opened on top of the automation
 * browser does not appear in the video and does not stop it. Every frame carries a REC timer and the date/time in the
 * top-right corner.
 *
 * Nothing here may ever fail a test run: every problem is logged and the recording simply ends.
 */
public final class ScreenRecorder {

	private static final Logger logger = LoggerFactory.getLogger(ScreenRecorder.class);

	/** Where the browser's DevTools endpoint is: "localhost:12345" plus the id of the page (window) to record. */
	public record Target(String address, String targetId) {
	}

	private final File file;
	private final Supplier<Target> targetSupplier;
	private final int fps;
	private final int maxWidth;
	private final float quality;
	private final String label;

	private volatile boolean running;
	private volatile byte[] latestJpeg;
	private volatile WebSocket ws;
	private volatile CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);
	private final StringBuilder partial = new StringBuilder();
	private Thread writerThread;
	private long lastConnectTry;
	private int framesWritten;
	private long firstFrameMs;
	private int msgId = 10;

	public ScreenRecorder(File file, Supplier<Target> targetSupplier, int fps, int maxWidth, float quality, String label) {
		this.file = file;
		this.targetSupplier = targetSupplier;
		this.fps = Math.max(1, Math.min(fps, 15));
		this.maxWidth = Math.max(480, maxWidth);
		this.quality = Math.max(0.2f, Math.min(quality, 0.95f));
		this.label = label == null ? "" : label;
	}

	public File file() {
		return file;
	}

	public int framesWritten() {
		return framesWritten;
	}

	public void start() {
		running = true;
		file.getParentFile().mkdirs();
		writerThread = new Thread(this::loop, "ckyc-screen-recorder");
		writerThread.setDaemon(true);
		writerThread.start();
		logger.info("Screen recording started: {} ({} fps, max width {})", file.getAbsolutePath(), fps, maxWidth);
	}

	/** Stops, finishes the file and returns it; null (file deleted) when no frame was ever received. */
	public File stop() {
		running = false;
		try {
			if (writerThread != null) {
				writerThread.join(15000);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		closeSocket();
		if (framesWritten == 0) {
			try {
				file.delete();
			} catch (RuntimeException ignored) {
				// nothing to keep
			}
			logger.warn("Screen recording produced no frames - nothing saved");
			return null;
		}
		logger.info("Screen recording saved: {} ({} frames)", file.getAbsolutePath(), framesWritten);
		return file;
	}

	// ------------------------------------------------------------------ writer loop

	private void loop() {
		AviMjpegWriter avi = null;
		try {
			ImageWriter jpegWriter = jpegWriter();
			int w = 0;
			int h = 0;
			long periodMs = 1000L / fps;
			long next = System.currentTimeMillis();
			byte[] lastEncoded = null;
			byte[] decodedFrom = null;
			BufferedImage src = null;
			while (running) {
				long now = System.currentTimeMillis();
				if (now < next) {
					sleep(next - now);
					continue;
				}
				next += periodMs;
				ensureConnected();
				byte[] jpeg = latestJpeg;
				if (jpeg == null) {
					next = Math.max(next, System.currentTimeMillis()); // nothing to draw yet - do not build a backlog
					continue;
				}
				if (jpeg != decodedFrom) {
					BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpeg));
					if (img != null) {
						src = img;
						decodedFrom = jpeg;
					}
				}
				if (src == null) {
					continue;
				}
				if (avi == null) {
					w = even(src.getWidth());
					h = even(src.getHeight());
					avi = new AviMjpegWriter(file, w, h, fps);
					firstFrameMs = System.currentTimeMillis();
					next = firstFrameMs + periodMs;
				}
				if (avi.isFull()) {
					logger.warn("Screen recording reached the AVI size limit - recording stopped");
					running = false;
					break;
				}
				lastEncoded = encode(jpegWriter, compose(src, w, h));
				avi.addFrame(lastEncoded);
				framesWritten++;
				// the screenshot took long: repeat the last picture so the video keeps real time
				long lateBy = System.currentTimeMillis() - next;
				while (lateBy >= periodMs && running) {
					avi.addFrame(lastEncoded);
					framesWritten++;
					next += periodMs;
					lateBy -= periodMs;
				}
			}
		} catch (Exception | Error e) {
			logger.warn("Screen recording stopped by an error: {}", e.toString());
		} finally {
			if (avi != null) {
				try {
					avi.close();
				} catch (Exception e) {
					logger.warn("Could not finish the recording file: {}", e.getMessage());
				}
			}
		}
	}

	private BufferedImage compose(BufferedImage src, int w, int h) {
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.drawImage(src, 0, 0, w, h, null);
			long elapsed = Math.max(0, System.currentTimeMillis() - firstFrameMs) / 1000;
			String timer = String.format(Locale.ROOT, "%02d:%02d:%02d", elapsed / 3600, (elapsed % 3600) / 60, elapsed % 60);
			String stamp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH).format(new Date())
					+ (label.isEmpty() ? "" : "  |  " + label);
			int big = Math.max(15, w / 75);
			int small = Math.max(11, big - 4);
			Font fBig = new Font("SansSerif", Font.BOLD, big);
			Font fSmall = new Font("SansSerif", Font.PLAIN, small);
			g.setFont(fBig);
			int w1 = g.getFontMetrics().stringWidth("REC  " + timer);
			g.setFont(fSmall);
			int w2 = g.getFontMetrics().stringWidth(stamp);
			int boxW = Math.max(w1 + big + 26, w2 + 20);
			int boxH = big + small + 24;
			int x = w - boxW - 10;
			int y = 10;
			g.setColor(new Color(0, 0, 0, 175));
			g.fillRoundRect(x, y, boxW, boxH, 12, 12);
			boolean blink = (elapsed % 2) == 0;
			g.setColor(blink ? new Color(255, 59, 48) : new Color(120, 30, 30));
			g.fillOval(x + 10, y + 10 + (big - big / 2) / 2, big / 2 + 2, big / 2 + 2);
			g.setColor(Color.WHITE);
			g.setFont(fBig);
			g.drawString("REC  " + timer, x + 12 + big / 2 + 6, y + 10 + big - 2);
			g.setFont(fSmall);
			g.setColor(new Color(210, 210, 210));
			g.drawString(stamp, x + 10, y + 12 + big + small);
		} finally {
			g.dispose();
		}
		return out;
	}

	private byte[] encode(ImageWriter writer, BufferedImage img) throws Exception {
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(quality);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64 * 1024);
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(img, null, null), param);
			writer.reset();
		}
		return bos.toByteArray();
	}

	private static ImageWriter jpegWriter() {
		Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
		if (!it.hasNext()) {
			throw new IllegalStateException("No JPEG writer available");
		}
		return it.next();
	}

	private static int even(int v) {
		return Math.max(2, v - (v % 2));
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(Math.max(1, Math.min(ms, 1000)));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// ------------------------------------------------------------------ DevTools connection

	private void ensureConnected() {
		if (ws != null || !running) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastConnectTry < 2000) {
			return;
		}
		lastConnectTry = now;
		try {
			Target t = targetSupplier.get();
			if (t == null || t.address() == null || t.address().isBlank()) {
				return;
			}
			String wsUrl = pageSocketUrl(t);
			if (wsUrl == null) {
				return;
			}
			HttpClient client = HttpClient.newHttpClient();
			WebSocket socket = client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5))
					.buildAsync(URI.create(wsUrl), new Listener()).get(8, TimeUnit.SECONDS);
			ws = socket;
			sendChain = CompletableFuture.completedFuture(null);
			send(cmd("Page.enable", null));
			send(cmd("Page.startScreencast", new JSONObject().put("format", "jpeg").put("quality", 70)
					.put("maxWidth", maxWidth).put("maxHeight", 4000).put("everyNthFrame", 1)));
			send(cmd("Page.captureScreenshot", new JSONObject().put("format", "jpeg").put("quality", 70)));
			logger.info("Screen recording connected to the browser page");
		} catch (Exception e) {
			logger.debug("Screen recording could not connect yet: {}", e.toString());
		}
	}

	private String pageSocketUrl(Target t) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create("http://" + t.address() + "/json/list"))
				.timeout(Duration.ofSeconds(4)).GET().build();
		HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
		JSONArray pages = new JSONArray(resp.body());
		String fallback = null;
		for (int i = 0; i < pages.length(); i++) {
			JSONObject p = pages.getJSONObject(i);
			if (!"page".equals(p.optString("type"))) {
				continue;
			}
			String url = p.optString("url");
			if (url.startsWith("devtools://") || url.startsWith("chrome-extension://")) {
				continue;
			}
			String socket = p.optString("webSocketDebuggerUrl", null);
			if (socket == null) {
				continue;
			}
			if (t.targetId() != null && t.targetId().equals(p.optString("id"))) {
				return socket;
			}
			if (fallback == null) {
				fallback = socket;
			}
		}
		return fallback;
	}

	private String cmd(String method, JSONObject params) {
		JSONObject o = new JSONObject().put("id", ++msgId).put("method", method);
		if (params != null) {
			o.put("params", params);
		}
		return o.toString();
	}

	private synchronized void send(String text) {
		WebSocket socket = ws;
		if (socket == null) {
			return;
		}
		sendChain = sendChain.handle((v, e) -> null).thenCompose(x -> socket.sendText(text, true));
	}

	private void closeSocket() {
		WebSocket socket = ws;
		ws = null;
		if (socket == null) {
			return;
		}
		try {
			socket.sendText(cmd("Page.stopScreencast", null), true).get(2, TimeUnit.SECONDS);
		} catch (Exception ignored) {
			// closing anyway
		}
		try {
			socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(2, TimeUnit.SECONDS);
		} catch (Exception ignored) {
			socket.abort();
		}
	}

	private final class Listener implements WebSocket.Listener {
		@Override
		public void onOpen(WebSocket webSocket) {
			webSocket.request(1);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			partial.append(data);
			if (last) {
				String message = partial.toString();
				partial.setLength(0);
				try {
					handle(message);
				} catch (RuntimeException e) {
					logger.debug("Screen recording ignored a message: {}", e.toString());
				}
			}
			webSocket.request(1);
			return null;
		}

		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			if (ws == webSocket) {
				ws = null;
			}
			return null;
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			if (ws == webSocket) {
				ws = null;
			}
		}
	}

	private void handle(String message) {
		if (!message.startsWith("{")) {
			return;
		}
		JSONObject o = new JSONObject(message);
		String method = o.optString("method", "");
		if ("Page.screencastFrame".equals(method)) {
			JSONObject p = o.getJSONObject("params");
			latestJpeg = Base64.getDecoder().decode(p.getString("data"));
			send(cmd("Page.screencastFrameAck", new JSONObject().put("sessionId", p.getInt("sessionId"))));
		} else if (o.has("result") && o.getJSONObject("result").has("data") && latestJpeg == null) {
			latestJpeg = Base64.getDecoder().decode(o.getJSONObject("result").getString("data"));
		}
	}
}
