/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.capture;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The locators the automation already has (every static {@code By} constant of the {@code pages} package) and a way to ask a
 * live page how many elements each of them matches right now - so a crawl can tell which existing locators still work,
 * and which elements on the page are new (not in the code at all).
 */
public final class LocatorHealth {

	private static final Logger logger = LoggerFactory.getLogger(LocatorHealth.class);

	/** id = Class.FIELD, kind = xpath | css, expr = the locator text. */
	public record Existing(String id, String kind, String expr) {
	}

	private LocatorHealth() {
	}

	public static List<Existing> load() {
		List<Existing> out = new ArrayList<>();
		Path root = Paths.get(System.getProperty("user.dir"), "src", "test", "java");
		Path pages = root.resolve("Com").resolve("Ckyc_4_0").resolve("pages");
		if (!Files.exists(pages)) {
			return out;
		}
		try (Stream<Path> walk = Files.walk(pages)) {
			for (Path p : (Iterable<Path>) walk.filter(f -> f.toString().endsWith(".java"))::iterator) {
				String rel = root.relativize(p).toString().replace('\\', '.').replace('/', '.');
				String cls = rel.substring(0, rel.length() - 5);
				try {
					Class<?> c = Class.forName(cls, true, LocatorHealth.class.getClassLoader());
					for (Field f : c.getDeclaredFields()) {
						if (!Modifier.isStatic(f.getModifiers()) || !By.class.isAssignableFrom(f.getType())) {
							continue;
						}
						f.setAccessible(true);
						Object v = f.get(null);
						if (v == null) {
							continue;
						}
						String s = v.toString();
						if (s.startsWith("By.xpath: ")) {
							out.add(new Existing(c.getSimpleName() + "." + f.getName(), "xpath", s.substring(10)));
						} else if (s.startsWith("By.cssSelector: ")) {
							out.add(new Existing(c.getSimpleName() + "." + f.getName(), "css", s.substring(16)));
						} else if (s.startsWith("By.id: ")) {
							out.add(new Existing(c.getSimpleName() + "." + f.getName(), "css", "#" + s.substring(7)));
						}
					}
				} catch (ReflectiveOperationException | LinkageError e) {
					logger.debug("Locators of {} not read: {}", cls, e.toString());
				}
			}
		} catch (IOException e) {
			logger.warn("Existing locators could not be listed: {}", e.getMessage());
		}
		return out;
	}

	private static final String COUNT_JS = "var list=JSON.parse(arguments[0]);return JSON.stringify(list.map(function(x){try{"
			+ "if(x[1]==='xpath'){return document.evaluate('count('+x[2]+')',document,null,XPathResult.NUMBER_TYPE,null).numberValue;}"
			+ "return document.querySelectorAll(x[2]).length;}catch(e){return -1;}}));";

	/** Matches of every existing locator on the page that is open now (-1 = the expression is not valid here). */
	public static int[] count(WebDriver driver, List<Existing> all) {
		JSONArray in = new JSONArray();
		for (int i = 0; i < all.size(); i++) {
			in.put(new JSONArray().put(i).put(all.get(i).kind()).put(all.get(i).expr()));
		}
		Object o = ((JavascriptExecutor) driver).executeScript(COUNT_JS, in.toString());
		JSONArray r = new JSONArray(String.valueOf(o));
		int[] out = new int[r.length()];
		for (int i = 0; i < out.length; i++) {
			out[i] = r.getInt(i);
		}
		return out;
	}

	static String norm(String s) {
		return s == null ? "" : s.replaceAll("\\s+", "").replace("\"", "'");
	}

	static JSONObject describe(Existing e) {
		return new JSONObject().put("id", e.id()).put("kind", e.kind()).put("expr", e.expr());
	}
}
