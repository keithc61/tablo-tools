package tablo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Main {

	private static final class ConfigImpl implements Configuration {

		private static boolean isSelectedIn(String value, RangeList list) {
			if (value != null) {
				try {
					return list.isEmpty() || list.contains(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			return false;
		}

		private final RangeList episodes;

		private final Map<MediaType, String> mediaDirectory;

		private final RangeList seasons;

		private final List<Pattern> titles;

		ConfigImpl(Map<MediaType, String> mediaDirectory) {
			super();
			this.episodes = new RangeList();
			this.mediaDirectory = mediaDirectory;
			this.seasons = new RangeList();
			this.titles = new ArrayList<>();
		}

		void addEpisodes(String list) {
			episodes.addRanges(list);
		}

		void addSeasons(String list) {
			seasons.addRanges(list);
		}

		void addTitle(String title) throws IllegalArgumentException {
			if (title.isEmpty()) {
				throw new IllegalArgumentException(title);
			}

			titles.add(Pattern.compile(title, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
		}

		@Override
		public String directoryFor(MediaType mediaType) {
			return mediaDirectory.get(mediaType);
		}

		@Override
		public boolean isSelectedEpisode(String episode) {
			return isSelectedIn(episode, episodes);
		}

		@Override
		public boolean isSelectedSeason(String season) {
			return isSelectedIn(season, seasons);
		}

		@Override
		public boolean isSelectedTitle(String title) {
			if (title == null) {
				return false;
			} else if (titles.isEmpty()) {
				return true;
			} else {
				return titles.stream() // <br/>
						.anyMatch(pattern -> pattern.matcher(title).find());
			}
		}

	}

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

		void flag(String name, Runnable action) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(action);

			handlers.add(consumeIf(isFlag(name), input -> action.run()));
		}

		void handle(String input) {
			if (handlers.stream().noneMatch(handler -> handler.test(input))) {
				System.err.println("Unknown option: " + input);
				throw new IllegalArgumentException(input);
			}
		}

		void regex(String regex, Consumer<String> consumer) {
			Objects.requireNonNull(regex);
			Objects.requireNonNull(consumer);

			handlers.add(consumeIf(matches(regex, Pattern.CASE_INSENSITIVE), consumer));
		}

		void value(String name, Consumer<String> consumer) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(consumer);

			Predicate<String> test = matches("^-\\Q" + name + "\\E=.+", Pattern.CASE_INSENSITIVE);
			int offset = name.length() + 2;

			handlers.add(consumeIf(test, input -> consumer.accept(input.substring(offset))));
		}

	}

	private static final int TABLO_API_PORT = 8885;

	// private static final int TABLO_PORT = 18080;

	private static void applyConfig(Map<String, String> config, String name, Consumer<? super String> action) {
		Optional.ofNullable(config.get(name)).ifPresent(action);
	}

	private static List<String> getLocalTabloIps() throws IOException {
		URL url = new URL("https://api.tablotv.com/assocserver/getipinfo/");

		return Util.selectJSON(url, "cpes.*.private_ip");
	}

	private static URL getPlaylistURL(String tablo, String airing) throws IOException {
		URL watchUrl = new URL("http", tablo, TABLO_API_PORT, airing + "/watch");
		HttpURLConnection connection = (HttpURLConnection) watchUrl.openConnection();

		connection.setRequestMethod("POST");

		Map<?, ?> watchData = (Map<?, ?>) Util.readJSON(new InputStreamReader(connection.getInputStream(), Util.UTF_8));
		String playlistUrl = Util.selectUnique(watchData, "playlist_url");

		return playlistUrl != null ? new URL(playlistUrl) : null;
	}

	private static Comparator<String> idComparator() {
		return Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder());
	}

	private static Map<String, String> loadPropertyMap(String fileName) throws IOException {
		Properties properties = new Properties();

		try (InputStream in = new FileInputStream(fileName)) {
			properties.load(in);
		}

		Map<String, String> config = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Consumer<String> action = key -> config.put(key, properties.getProperty(key));

		properties.stringPropertyNames().stream().forEach(action);

		return config;
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

	private File cacheFile;

	private String crf;

	private boolean debug;

	private String ffmpeg;

	private boolean includeUnfinished;

	private boolean listOnly;

	private final Map<MediaType, String> mediaDirectory;

	private boolean overwrite;

	private final ConfigImpl selector;

	private final List<String> tablos;

	private boolean timestamp;

	private String videoRate;

	private Main(String[] args) throws IllegalArgumentException, IOException {
		super();
		this.cacheFile = null;
		this.crf = null;
		this.debug = false;
		this.ffmpeg = "ffmpeg";
		this.includeUnfinished = false;
		this.listOnly = false;
		this.mediaDirectory = new HashMap<>();
		this.overwrite = false;
		this.selector = new ConfigImpl(mediaDirectory);
		this.tablos = new ArrayList<>();
		this.timestamp = false;
		this.videoRate = null;

		handleOptions(args);
	}

	private void handleOptions(String[] args) throws IOException {
		Options options = new Options();

		options.value("cache", this::setCacheFile);
		options.value("config", this::readConfig);
		options.flag("debug", this::setDebug);
		options.value("crf", this::setCrf);
		options.value("episode", selector::addEpisodes);
		options.value("ffmpeg", this::setFfmpeg);
		options.flag("list", this::setListOnly);
		options.flag("overwrite", this::setOverwrite);
		options.value("season", selector::addSeasons);
		options.value("tablo", tablos::add);
		options.flag("timestamp", this::setTimestamp);
		options.value("unfinished", this::setUnfinished);
		options.value("videorate", this::setVideoRate);

		MediaType.forEach( // <br/>
				type -> options.value(type.name(), // <br/>
						input -> mediaDirectory.put(type, input)));

		options.regex("^[^-].*", input -> selector.addTitle(input.trim()));

		Arrays.stream(args).forEach(options::handle);

		// if no tablos were specifically identified, use all local devices
		if (tablos.isEmpty()) {
			tablos.addAll(getLocalTabloIps());
		}
	}

	private void readConfig(String fileName) {
		Map<String, String> config;

		try {
			config = loadPropertyMap(fileName);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		applyConfig(config, "cache", this::setCacheFile);
		applyConfig(config, "crf", this::setCrf);
		applyConfig(config, "debug", this::setDebug);
		applyConfig(config, "ffmpeg", this::setFfmpeg);
		applyConfig(config, "overwrite", this::setOverwrite);
		applyConfig(config, "timestamp", this::setTimestamp);
		applyConfig(config, "unfinished", this::setUnfinished);
		applyConfig(config, "videoRate", this::setVideoRate);

		applyConfig(config, "tablo", value -> {
			Arrays.stream(value.split(",")) // <br/>
					.map(String::trim) // <br/>
					.filter(ip -> !ip.isEmpty()) // <br/>
					.forEach(tablos::add);
		});

		MediaType.forEach(type -> applyConfig(config, type.name(), // <br/>
				value -> mediaDirectory.put(type, value)));
	}

	private void run() throws IOException {
		List<Runnable> actions = new ArrayList<>();
		Cache cache = new Cache();

		if (cacheFile != null && cacheFile.isFile() && cacheFile.canRead()) {
			cache.load(cacheFile);
		}

		for (String ip : tablos) {
			if (debug) {
				showServerInfo(ip);
			}

			List<String> airings = readAirings(ip);

			if (debug) {
				int count = airings.size();

				System.out.println();
				System.out.printf("Found %d video%s at %s.\n", // <br/>
						Integer.valueOf(count), count == 1 ? "" : "s", ip);
			}

			cache.retainRecordings(ip, airings);

			Map<String, Runnable> pendingActions = new TreeMap<>(idComparator());

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

				if (!handler.isSelected(selector)) {
					continue;
				}

				if (listOnly) {
					System.out.printf("Video: %s\n", airing);
					handler.printMeta(System.out);
				} else if (includeUnfinished || handler.isFinished()) {
					URL playlist = getPlaylistURL(ip, airing);

					if (playlist == null) {
						System.err.println("Failed to get playlist URL for " + airing);
					} else {
						String videoId = airing.substring(airing.lastIndexOf('/') + 1);

						pendingActions.put(videoId, () -> save(playlist, handler));
					}
				}
			}

			actions.addAll(pendingActions.values());
		}

		if (cacheFile != null && (cacheFile.canWrite() || !cacheFile.exists())) {
			cache.save(cacheFile);
		}

		actions.stream().forEach(Runnable::run);
	}

	private void save(URL video, MediaHandler handler) {
		try {
			File dest = handler.getTargetFile(selector);

			if (dest == null) {
				System.out.println("Skipping " + video);
				return;
			}

			File folder = dest.getParentFile();

			folder.mkdirs();

			if (!folder.isDirectory()) {
				System.out.println("Skipping " + video + "; " + folder + " is not a directory");
				return;
			}

			boolean fetch = overwrite;

			if (dest.createNewFile()) {
				System.out.println("Saving " + dest.getAbsolutePath());
				fetch = true;
			} else if (overwrite) {
				System.out.println("Overwriting " + dest.getAbsolutePath());
			} else if (!timestamp) {
				System.out.println("Skipping existing file " + dest.getAbsolutePath());
			}

			if (fetch) {
				// ffmpeg doesn't like non-ASCII filenames
				File temp = File.createTempFile("tablo-", ".tmp", folder);

				try {
					Process process = startFilter(video, temp, handler.getPersistentMetadata());

					try {
						process.waitFor();
					} catch (InterruptedException e) {
						// ignore
					}

					if (!(dest.delete() && temp.renameTo(dest))) {
						System.err.format("Failed to rename %s to '%s'%n", temp.getName(), dest.getName());
					}
				} finally {
					// remove temporary files on failure
					// (this does nothing if the file was successfully renamed)
					temp.delete();
				}
			}

			if (timestamp) {
				Calendar time = handler.getTime();

				if (time != null) {
					if (!fetch) {
						System.out.println("Updating timestamp for " + dest.getAbsolutePath());
					}

					if (time.get(Calendar.YEAR) < 1970) {
						System.out.println("Clamping timestamp to 1970 for " + dest.getAbsolutePath());
						time.set(Calendar.YEAR, 1970);
					}

					dest.setLastModified(time.getTimeInMillis());
				} else if (debug) {
					System.out.println("No timestamp provided for " + dest.getAbsolutePath());
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to save " + video + ": " + e.getLocalizedMessage());
		}
	}

	private void setCacheFile(String fileName) {
		cacheFile = new File(fileName);
	}

	private void setCrf(String value) {
		crf = value;
	}

	private void setDebug() {
		debug = true;
	}

	private void setDebug(String value) {
		debug = Boolean.parseBoolean(value);
	}

	private void setFfmpeg(String value) {
		ffmpeg = value;
	}

	private void setListOnly() {
		listOnly = true;
	}

	private void setOverwrite() {
		overwrite = true;
	}

	private void setOverwrite(String value) {
		overwrite = Boolean.parseBoolean(value);
	}

	private void setTimestamp() {
		timestamp = true;
	}

	private void setTimestamp(String value) {
		timestamp = Boolean.parseBoolean(value);
	}

	private void setUnfinished(String value) {
		includeUnfinished = Boolean.parseBoolean(value);
	}

	private void setVideoRate(String value) {
		videoRate = value;
	}

	private Process startFilter(URL input, File output, Map<String, String> metadata) throws IOException {
		List<String> command = new ArrayList<>(20);

		command.add("nice");

		command.add(ffmpeg);

		command.add("-y");

		command.add("-loglevel");
		command.add("error");

		command.add("-nostdin");

		command.add("-nostats");

		command.add("-i");
		command.add(input.toExternalForm());

		command.add("-bsf:a");
		command.add("aac_adtstoasc");

		if (crf != null) {
			command.add("-c:a");
			command.add("copy");

			command.add("-crf");
			command.add(crf);
		} else if (videoRate != null) {
			command.add("-c:a");
			command.add("copy");

			command.add("-b:v");
			command.add(videoRate);
		} else {
			command.add("-c");
			command.add("copy");
		}

		metadata.forEach((key, value) -> {
			command.add("-metadata");
			command.add(key + "=" + value);
		});

		command.add("-f");
		command.add("mp4");

		command.add(output.getAbsolutePath());

		return new ProcessBuilder(command) // <br/>
				.redirectError(ProcessBuilder.Redirect.INHERIT) // <br/>
				.redirectOutput(ProcessBuilder.Redirect.INHERIT) // <br/>
				.start();
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
