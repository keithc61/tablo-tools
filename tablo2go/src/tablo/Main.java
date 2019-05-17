package tablo;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public final class Main {

	private static final class Options {

		private static <T> Predicate<T> always(Consumer<T> consumer) {
			return input -> {
				consumer.accept(input);
				return true;
			};
		}

		private static <T> Predicate<T> consumeIf(Predicate<T> test, Consumer<T> consumer) {
			return test.and(always(consumer));
		}

		private static Predicate<String> isFlag(String name) {
			int flagLength = name.length();

			return input -> input.length() == flagLength + 1 // <br/>
					&& input.charAt(0) == '-' // <br/>
					&& input.regionMatches(true, 1, name, 0, flagLength);
		}

		private static Predicate<String> matches(String regex, int flags) {
			Pattern pattern = Pattern.compile(regex, flags);

			return input -> pattern.matcher(input).matches();
		}

		private final List<Predicate<String>> handlers;

		Options() {
			super();
			handlers = new ArrayList<>();
		}

		void flag(String name, Consumer<String> consumer) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(consumer);

			handlers.add(consumeIf(isFlag(name), input -> consumer.accept(name)));
		}

		void handle(String input) {
			if (handlers.stream().noneMatch(handler -> handler.test(input))) {
				System.err.println("Unknown option: " + input);
				throw new IllegalArgumentException(input);
			}
		}

		void value(String name, BiConsumer<String, String> consumer) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(consumer);

			Predicate<String> test = matches("^-\\Q" + name + "\\E=.+", Pattern.CASE_INSENSITIVE);
			int offset = name.length() + 2;

			handlers.add(consumeIf(test, input -> consumer.accept(name, input.substring(offset))));
		}

		void value(String name, Consumer<String> consumer) {
			value(name, (n, value) -> consumer.accept(value));
		}

	}

	private static final int TABLO_API_PORT = 8885;

	private static List<String> getLocalTabloIps() throws IOException {
		URL url = new URL("https://api.tablotv.com/assocserver/getipinfo/");

		return Util.selectJSON(url, "cpes.*.private_ip");
	}

	public static URL getPlaylistURL(String tablo, String airing) throws IOException {
		URL watchUrl = new URL("http", tablo, TABLO_API_PORT, airing + "/watch");
		HttpURLConnection connection = (HttpURLConnection) watchUrl.openConnection();

		connection.setRequestMethod("POST");

		Map<?, ?> watchData = (Map<?, ?>) Util.readJSON(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
		String playlistUrl = Util.selectUnique(watchData, "playlist_url");

		return playlistUrl != null ? new URL(playlistUrl) : null;
	}

	public static void main(String[] args) throws Exception {
		new Main(args).run();
	}

	private static List<String> readAirings(String ip) throws IOException {
		URL airings = new URL("http", ip, TABLO_API_PORT, "/recordings/airings");

		return Util.selectJSON(airings, "*");
	}

	private static void showServerInfo(String ip) throws IOException {
		URL server = new URL("http", ip, TABLO_API_PORT, "/server/info");
		Object info = Util.readJSON(server);

		if (info instanceof Map<?, ?>) {
			System.out.println();
			System.out.println("Server Info:");
			((Map<?, ?>) info) // <br/>
					.forEach((k, v) -> System.out.printf("  %s: %s%n", k, v));
		}
	}

	private final Map<String, String> options;

	private final List<Recording> recordings;

	private final Collection<String> tablos;

	private Main(String[] args) throws IllegalArgumentException, IOException {
		super();
		this.options = new HashMap<>();
		this.recordings = new ArrayList<>();

		// install defaults
		options.put("ffmpeg", "ffmpeg");

		handleOptions(args);

		this.tablos = new LinkedHashSet<>(Arrays.asList(options.getOrDefault("tablos", "").split(",")));

		tablos.removeIf(String::isEmpty);
		tablos.removeIf(ip -> "auto".equalsIgnoreCase(ip));

		// if no tablos were specifically identified, use all local devices
		if (tablos.isEmpty()) {
			tablos.addAll(getLocalTabloIps());
		}
	}

	private void handleOptions(String[] args) {
		Consumer<String> setFlag = name -> options.put(name, "true");
		BiConsumer<String, String> setOption = options::put;

		Options handler = new Options();

		handler.value("cache", setOption);
		handler.value("config", this::readConfig);
		handler.flag("debug", setFlag);
		handler.value("crf", setOption);
		handler.value("ffmpeg", setOption);
		handler.flag("list", setFlag);
		handler.flag("overwrite", setFlag);
		handler.value("tablos", setOption);
		handler.flag("timestamp", setFlag);
		handler.flag("unfinished", setFlag);
		handler.value("videorate", setOption);

		Arrays.stream(args).forEach(handler::handle);
	}

	private void readConfig(String fileName) {
		try {
			Configuration.parse(fileName, recordings, options);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private void run() throws IOException {
		List<Runnable> actions = new ArrayList<>();
		Cache cache = new Cache();
		File cacheFile = null;
		String cacheFilename = options.get("cache");
		boolean debug = Boolean.parseBoolean(options.get("debug"));

		if (cacheFilename != null) {
			cacheFile = new File(cacheFilename);

			if (cacheFile.isFile() && cacheFile.canRead()) {
				cache.load(cacheFile);
			}
		}

		for (String ip : tablos) {
			if (debug) {
				showServerInfo(ip);
			}

			List<String> airings = readAirings(ip);

			if (debug) {
				int count = airings.size();

				System.out.println();
				System.out.printf("Found %d video%s at %s.%n", // <br/>
						Integer.valueOf(count), count == 1 ? "" : "s", ip);
			}

			cache.retainRecordings(ip, airings);

			for (String airing : airings) {
				Map<String, String> attributes = cache.getAttributes(ip, airing);
				MediaHandler handler = MediaHandler.newInstance(airing, attributes);

				if (handler == null) {
					continue;
				}

				if (attributes.isEmpty()) {
					try {
						URL airingUrl = new URL("http", ip, TABLO_API_PORT, airing);

						handler.fetchAttributes(airingUrl);
					} catch (IOException e) {
						System.err.println("Failed to fetch metadata for " // <br/>
								+ airing + ": " + e.getLocalizedMessage());
						continue;
					}

					handler.cacheAttributes(cache, ip, airing);
				}

				if (debug) {
					try {
						getPlaylistURL(ip, airing);
					} catch (IOException e) {
						System.out.println("Failed to get playlist URL for " + airing);
						handler.printMeta(System.out);
						continue;
					}
				}

				Runnable action = handler.getAction(ip, airing, recordings);

				if (action != null) {
					actions.add(action);
				}
			}
		}

		if (cacheFile != null && (cacheFile.canWrite() || !cacheFile.exists())) {
			cache.save(cacheFile);
		}

		actions.stream().sorted().forEach(Runnable::run);
	}

}

//# Below are all of the configurable options, you can edit the defaults here.
//# Run the command with -help to see your settings
//
//# TYPE      OPTION      DEFAULT                 INPUT     DESCRIPTION OF FEATURE
//#                       '*' = REQUIRED!
//#                       EDIT ONLY THIS COLUMN
//# ------    ---------   ---------------------   --------  ---------------------------------------------------------
//OPTIONS  = {'tablo':    ['auto',                'IPADDR', 'Tablo IP address'],
//            'db':       ['ignore',              'PATH',   'Use a cacheing database'],
//            'dbtime':   [604800,                'TIME',   'Time where a cached entry is valid'],
//            'tvcreate': [True,                  '',       'Create Show/Season X directories'],
//            'faildir':  ['./fail',              'PATH',   'Location to save unknown tv videos'],
//            'tempdir':  ['./',                  'PATH',   'Location of temp directory'],
//            'existdir': ['./exists',            'PATH',   'Location to move duplicate files'],
//            'ffmpeg':   ['./ffmpeg.exe',        'PATH',   'Path to ffmpeg'],
//            'handbrake':['./HandBrakeCLI.exe',  'PATH',   'Path to HandBrakeCLI'],
//            'ccextract':['./ccextractorwin.exe','PATH',   'Path to ccextractor'],
//            'cc':       [False,                 '',       'Embed Closed Captioning into file'],
//            'noescape': [True,                  '',       'Forbid the \\ character in paths'],
//            'mce':      [False,                 '',       'Save TV shows in MCEBuddy format'],
//            'mp4tag':   [True,                  '',       'Tag with metadata (requires mutagen)'],
//            'mkv':      [False,                 '',       'Use .mkv (requires HandBrakeCLI)'],
//            'ignore':   [False,                 '',       'Ignore history files'],
//            'complete': [False,                 '',       'Mark matching videos as downloaded'],
//            'a':        [False,                 '',       'Reprocess continually (see -sleep)'],
//            'summary':  [False,                 '',       'Display summary information only'],
//            'list':     [False,                 '',       'List matching videos on Tablo(s)'],
//            'long':     [False,                 '',       'Expand list view with more detail'],
//            'help':     [False,                 '',       'Show this help screen'],
//            'test':     [False,                 '',       'Test System - do not download/mark'],
//            'debug':    [True,                  '',       'Show all debugging output'],
//            'delay':    [0,                     'TIME',   'Shows must be x seconds old'],
//            'not':      [False,                 '',       'Invert the selection'],
//            'custom':   ['',                    'NAME',   'Custom File Naming'],
//            'log':      ['tablo.log',           'PATH',   'Log messages to a file (or use off)']}
//
//# There are a few hidden options as well, this could be added above to make things neater
//# but I didnt want the help screen to be even longer
//#  -reallylong shows all metadata retrieved from the tablo (these fields are valid in custom)
//#  -customtv allows for custom tv naming
//#  -custommovie allows for custom movie naming
//#  -customsports allows for custom sports naming
//#  -skipdelete will not delete .ts file
//#  -skipdownload will not download the files (useful in testing if used with -skipdelete)
