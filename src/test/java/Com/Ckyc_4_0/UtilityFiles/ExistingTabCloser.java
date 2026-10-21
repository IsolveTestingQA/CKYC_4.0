/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The iFlow application refuses to run when it is already open in another browser tab on this machine.
 * Before the automation browser is launched this closes every Chrome / Edge TAB whose title contains one of the
 * keywords (default "iFlowCKYC"). Only the matching tabs are closed - never a whole window, never another tab.
 * Windows only (UI Automation); other systems and any failure just log a warning and the run continues.
 *
 * config.properties: closeExistingAppTabs=true (default), appTabTitleKeywords=iFlowCKYC (comma separated).
 */
public final class ExistingTabCloser {

	private static final Logger logger = LoggerFactory.getLogger(ExistingTabCloser.class);

	private static final String SCRIPT = """
			param([string]$Keywords, [string]$Mode)
			$ErrorActionPreference = 'SilentlyContinue'
			Add-Type -AssemblyName UIAutomationClient
			Add-Type -AssemblyName UIAutomationTypes
			$AE = [System.Windows.Automation.AutomationElement]
			$CT = [System.Windows.Automation.ControlType]
			$PC = [System.Windows.Automation.PropertyCondition]
			$TS = [System.Windows.Automation.TreeScope]
			$keys = @($Keywords.Split(',') | ForEach-Object { $_.Trim().ToLower() } | Where-Object { $_ })
			$found = 0; $closed = 0
			$wins = $AE::RootElement.FindAll($TS::Children, (New-Object $PC($AE::ClassNameProperty, 'Chrome_WidgetWin_1')))
			foreach ($w in $wins) {
			  $proc = (Get-Process -Id $w.Current.ProcessId).ProcessName.ToLower()
			  if ($proc -ne 'chrome' -and $proc -ne 'msedge') { continue }
			  $tabs = $w.FindAll($TS::Descendants, (New-Object $PC($AE::ControlTypeProperty, $CT::TabItem)))
			  foreach ($t in $tabs) {
			    $n = $t.Current.Name
			    if (-not $n) { continue }
			    $low = $n.ToLower(); $hit = $false
			    # the automation's own report pages (Extent / Advanced) carry the product name too - keep them open
			    if ($low.Contains('report') -and ($low.Contains('automation') -or $low.Contains('tested by'))) { continue }
			    foreach ($k in $keys) { if ($low.Contains($k)) { $hit = $true } }
			    if (-not $hit) { continue }
			    $found++
			    Write-Output ('TAB: ' + $proc + ' | ' + $n)
			    if ($Mode -eq 'close') {
			      $btn = $t.FindFirst($TS::Descendants, (New-Object $PC($AE::NameProperty, 'Close')))
			      if ($btn) { $btn.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke(); $closed++ }
			      else { Write-Output ('NOCLOSE: ' + $n) }
			    }
			  }
			}
			Write-Output ('FOUND=' + $found + ' CLOSED=' + $closed)
			""";

	private ExistingTabCloser() {
	}

	public static boolean enabled() {
		return Boolean.parseBoolean(BaseClass.getProperty("closeExistingAppTabs", "true"));
	}

	/** Closes (or, with listOnly, only reports) the tabs of this machine that already show the application. */
	public static int closeAppTabs(boolean listOnly) {
		if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
			logger.info("Existing-tab check skipped (not Windows)");
			return 0;
		}
		String keywords = BaseClass.getProperty("appTabTitleKeywords", "iFlowCKYC");
		Path script = null;
		try {
			script = Files.createTempFile("ckyc-close-tabs-", ".ps1");
			Files.writeString(script, SCRIPT, StandardCharsets.UTF_8);
			List<String> cmd = new ArrayList<>(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy",
					"Bypass", "-File", script.toString(), "-Keywords", keywords, "-Mode", listOnly ? "list" : "close"));
			int found = 0;
			int closed = 0;
			// Chrome publishes its accessibility tree lazily - a second pass catches tabs the first one missed
			for (int pass = 1; pass <= 2; pass++) {
				String out = run(cmd);
				for (String line : out.split("\\R")) {
					if (line.startsWith("TAB: ") || line.startsWith("NOCLOSE: ")) {
						logger.info("Existing app tab (pass {}): {}", pass, line);
					}
					if (line.startsWith("FOUND=")) {
						found = Math.max(found, parse(line, "FOUND="));
						closed += parse(line, "CLOSED=");
					}
				}
				if (listOnly || closed > 0) {
					break;
				}
				sleep(1500);
			}
			if (listOnly) {
				logger.info("Existing app tabs found on this machine: {}", found);
			} else {
				logger.info("Existing app tabs found: {} | closed: {}", found, closed);
				if (found > closed) {
					logger.warn("{} tab(s) could not be closed automatically - close them by hand if the app says it is already open",
							found - closed);
				}
				if (closed > 0) {
					sleep(1500); // let Chrome finish closing before the automation browser starts
				}
			}
			return closed;
		} catch (Exception e) {
			logger.warn("Existing-tab check skipped: {}", e.getMessage());
			return 0;
		} finally {
			if (script != null) {
				try {
					Files.deleteIfExists(script);
				} catch (Exception ignored) {
					// temp file - ignore
				}
			}
		}
	}

	private static String run(List<String> cmd) throws Exception {
		Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
		StringBuilder sb = new StringBuilder();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = r.readLine()) != null) {
				sb.append(line).append('\n');
			}
		}
		if (!p.waitFor(30, TimeUnit.SECONDS)) {
			p.destroyForcibly();
		}
		return sb.toString();
	}

	private static int parse(String line, String key) {
		int i = line.indexOf(key);
		if (i < 0) {
			return 0;
		}
		int j = i + key.length();
		int k = j;
		while (k < line.length() && Character.isDigit(line.charAt(k))) {
			k++;
		}
		return k > j ? Integer.parseInt(line.substring(j, k)) : 0;
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
