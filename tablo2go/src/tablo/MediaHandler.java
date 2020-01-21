package tablo;

import static tablo.Util.selectUnique;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tablo.util.StringTemplate;

public abstract class MediaHandler {

	private static final class Manual extends MediaHandler {

		Manual(Map<String, String> attributes) {
			super(attributes);
		}

		@Override
		protected void addMeta(Map<String, String> meta) {
			meta.put("type", "Manual recording");
			meta.put("title", getTitle());
			meta.put("aired", Util.formatAirTime(getAirDate()));
			super.addMeta(meta);
		}

		private String getAirDate() {
			return getAndTrim("airDate");
		}

		@Override
		protected Map<String, String> getTemplateMap(Recording recording) {
			String title = getTitle();

			if (title.isEmpty()) {
				return null;
			}

			Map<String, String> values = new HashMap<>();

			values.put("title", title);

			Calendar time = getTime();

			if (time != null) {
				values.put("time", " " + FileTimeFormat.format(time.getTime()));
			}

			return values;
		}

		@Override
		protected Calendar getTime() {
			return Util.parseAirTime(getAirDate());
		}

		private String getTitle() {
			return getAndTrim("title");
		}

		@Override
		public boolean isSelected(Recording recording) {
			return isSelectedType(recording, "Manual") // <br/>
					&& isSelectedName(recording, getTitle());
		}

		@Override
		protected void processMetadata(Map<?, ?> meta) {
			set("airDate", selectUnique(meta, "airing_details.datetime"));
			set("title", selectUnique(meta, "airing_details.show_title"));
			super.processMetadata(meta);
		}

	}

	private static final class Movie extends MediaHandler {

		private String moviePath;

		Movie(Map<String, String> attributes) {
			super(attributes);
			this.moviePath = "";
		}

		@Override
		protected void addMeta(Map<String, String> meta) {
			meta.put("type", "Movie");
			meta.put("title", getTitle());

			int year = getYear();

			if (year != 0) {
				meta.put("released", Integer.toString(year));
			}

			super.addMeta(meta);
		}

		@Override
		public void fetchAttributes(URL airing) throws IOException {
			super.fetchAttributes(airing);

			if (!moviePath.isEmpty()) {
				URL movieUrl = new URL(airing, moviePath);
				Map<?, ?> movieMeta = (Map<?, ?>) Util.readJSON(movieUrl);

				set("year", selectUnique(movieMeta, "movie.release_year"));
			}
		}

		@Override
		public Map<String, String> getPersistentMetadata() {
			Map<String, String> persistent = new HashMap<>();

			int year = getYear();

			if (year != 0) {
				persistent.put("date", Integer.toString(year));
			}

			String title = getTitle();

			if (!title.isEmpty()) {
				persistent.put("title", title);
			}

			return persistent;
		}

		@Override
		protected Map<String, String> getTemplateMap(Recording recording) {
			String title = getTitle();

			if (title.isEmpty()) {
				return null;
			}

			Map<String, String> values = new HashMap<>();

			values.put("title", title);

			int year = getYear();

			if (year != 0) {
				values.put("year", " (" + year + ')');
			}

			return values;
		}

		@Override
		protected Calendar getTime() {
			Calendar time = null;
			int year = getYear();

			if (year != 0) {
				time = Calendar.getInstance();

				time.set(Calendar.YEAR, year);
				time.set(Calendar.MONTH, 6);
				time.set(Calendar.DAY_OF_MONTH, 1);
				time.set(Calendar.HOUR_OF_DAY, 20);
				time.set(Calendar.MINUTE, 0);
				time.set(Calendar.SECOND, 0);
				time.set(Calendar.MILLISECOND, 0);
			}

			return time;
		}

		private String getTitle() {
			return getAndTrim("title");
		}

		private int getYear() {
			String year = getAndTrim("year");

			try {
				return Integer.parseInt(year);
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		@Override
		public boolean isSelected(Recording recording) {
			return isSelectedType(recording, "Movie") // <br/>
					&& isSelectedName(recording, getTitle());
		}

		@Override
		protected void processMetadata(Map<?, ?> meta) {
			moviePath = trim(selectUnique(meta, "movie_path"));
			set("title", selectUnique(meta, "airing_details.show_title"));
			super.processMetadata(meta);
		}

	}

	private static final class OrderedAction implements Comparable<OrderedAction>, Runnable {

		private final Runnable action;

		private final String airing;

		private final int index;

		OrderedAction(int index, String airing, Runnable action) {
			super();
			this.action = action;
			this.airing = airing;
			this.index = index;
		}

		@Override
		public int compareTo(OrderedAction that) {
			int result = Integer.compare(this.index, that.index);

			if (result == 0) {
				result = Integer.compare(this.airing.length(), that.airing.length());
			}

			if (result == 0) {
				result = this.airing.compareTo(that.airing);
			}

			return result;
		}

		@Override
		public void run() {
			action.run();
		}

	}

	private static final class Sports extends MediaHandler {

		Sports(Map<String, String> attributes) {
			super(attributes);
		}

		@Override
		protected void addMeta(Map<String, String> meta) {
			meta.put("type", "Sports event");
			meta.put("title", getTitle());
			meta.put("aired", Util.formatAirTime(getAirDate()));
			super.addMeta(meta);
		}

		private String getAirDate() {
			return getAndTrim("airDate");
		}

		@Override
		protected Map<String, String> getTemplateMap(Recording recording) {
			String title = getTitle();

			if (title.isEmpty()) {
				return null;
			}

			Map<String, String> values = new HashMap<>();

			values.put("title", title);

			Calendar time = getTime();

			if (time != null) {
				values.put("time", " " + FileTimeFormat.format(time.getTime()));
			}

			return values;
		}

		@Override
		protected Calendar getTime() {
			return Util.parseAirTime(getAirDate());
		}

		private String getTitle() {
			return getAndTrim("title");
		}

		@Override
		public boolean isSelected(Recording recording) {
			return isSelectedType(recording, "Sports") // <br/>
					&& isSelectedName(recording, getTitle());
		}

		@Override
		protected void processMetadata(Map<?, ?> meta) {
			set("airDate", selectUnique(meta, "airing_details.datetime"));
			trimAndSet("title", selectUnique(meta, "airing_details.show_title"));
			super.processMetadata(meta);
		}

	}

	private static final class TV extends MediaHandler {

		private static final DateFormat AirDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		private static final boolean isSelectedEpisode(Recording recording, String episode) {
			if (episode == null) {
				return false;
			}

			String episodes = recording.getOption("episodes");

			if (episodes == null) {
				return true;
			}

			RangeList list = new RangeList();

			list.addRanges(episodes);

			return isSelectedIn(episode, list);
		}

		private static final boolean isSelectedSeason(Recording recording, String season) {
			if (season == null) {
				return false;
			}

			String seasons = recording.getOption("seasons");

			if (seasons == null) {
				return true;
			}

			RangeList list = new RangeList();

			list.addRanges(seasons);

			return isSelectedIn(season, list);
		}

		TV(Map<String, String> attributes) {
			super(attributes);
		}

		@Override
		protected void addMeta(Map<String, String> meta) {
			meta.put("type", "Television Show");
			meta.put("series", getSeries());
			meta.put("season", getSeason());
			meta.put("episode", getEpisode());
			meta.put("title", getTitle());
			meta.put("aired", Util.formatAirTime(getAirDate()));
			super.addMeta(meta);
		}

		private String getAirDate() {
			return getAndTrim("airDate");
		}

		private String getEpisode() {
			return getAndTrim("episode");
		}

		private String getOriginalAirDate() {
			return getAndTrim("originalAirDate");
		}

		@Override
		public Map<String, String> getPersistentMetadata() {
			Map<String, String> persistent = new HashMap<>();

			persistent.put("show", getSeries());
			persistent.put("title", getTitle());
			persistent.put("season_number", getSeason());
			persistent.put("episode_sort", getEpisode());

			persistent.values().removeIf(String::isEmpty);

			return persistent;
		}

		private String getSeason() {
			return getAndTrim("season");
		}

		private String getSeries() {
			return getAndTrim("series");
		}

		@Override
		protected Map<String, String> getTemplateMap(Recording recording) {
			String series = getSeries();

			if (series.isEmpty()) {
				return null;
			}

			Map<String, String> values = new HashMap<>();

			values.put("series", series);
			values.put("season", getSeason());
			values.put("episode", getEpisode());
			values.put("title", getTitle());

			return values;
		}

		@Override
		protected Calendar getTime() {
			Calendar time = Util.parseAirTime(getAirDate());

			if (time != null) {
				Calendar origDate = Util.parseTime(getOriginalAirDate(), AirDateFormat);

				if (origDate != null) {
					// keep time but use original date
					time.set(Calendar.YEAR, origDate.get(Calendar.YEAR));
					time.set(Calendar.MONTH, origDate.get(Calendar.MONTH));
					time.set(Calendar.DAY_OF_MONTH, origDate.get(Calendar.DAY_OF_MONTH));
				}
			}

			return time;
		}

		private String getTitle() {
			return getAndTrim("title");
		}

		@Override
		public boolean isSelected(Recording recording) {
			return isSelectedType(recording, "TV") // <br/>
					&& isSelectedName(recording, getSeries()) // <br/>
					&& isSelectedSeason(recording, getSeason()) // <br/>
					&& isSelectedEpisode(recording, getEpisode());
		}

		@Override
		protected void processMetadata(Map<?, ?> meta) {
			set("airDate", selectUnique(meta, "airing_details.datetime"));
			trimAndSet("originalAirDate", selectUnique(meta, "episode.orig_air_date"));
			trimAndSet("episode", selectUnique(meta, "episode.number"));
			trimAndSet("season", selectUnique(meta, "episode.season_number"));
			trimAndSet("series", selectUnique(meta, "airing_details.show_title"));
			trimAndSet("title", selectUnique(meta, "episode.title"));
			super.processMetadata(meta);
		}

	}

	protected static final DateFormat FileTimeFormat = new SimpleDateFormat("yyyy-MM-dd HHmm");

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	private static boolean booleanOption(Recording recording, String name) {
		return Boolean.parseBoolean(recording.getOption(name));
	}

	protected static final String fixPathSegment(String segment) {
		if (segment == null) {
			return null;
		}

		StringBuilder buffer = new StringBuilder();

		for (char ch : segment.toCharArray()) {
			switch (ch) {
			case '\0':
			case '<':
			case '>':
			case ':':
			case '"':
			case '/':
			case '\\':
			case '|':
			case '?':
			case '*':
			case '%': // a legal character, but used as an escape here
				buffer.append('%');
				buffer.append(HEX[(ch >> 4) & 0xF]);
				buffer.append(HEX[(ch >> 0) & 0xF]);
				break;

			default:
				buffer.append(ch);
				break;
			}
		}

		return buffer.toString();
	}

	protected static final boolean isSelectedIn(String value, RangeList list) {
		if (value != null) {
			try {
				return list.isEmpty() || list.contains(Integer.parseInt(value));
			} catch (NumberFormatException e) {
				// ignore
			}
		}

		return false;
	}

	protected static final boolean isSelectedName(Recording recording, String title) {
		if (title == null) {
			return false;
		}

		String name = recording.getOption("name");

		if (name == null) {
			return true;
		}

		return title.equalsIgnoreCase(name);
	}

	protected static final boolean isSelectedType(Recording recording, String type) {
		return Objects.requireNonNull(type).equalsIgnoreCase(recording.getOption("type"));
	}

	public static MediaHandler newInstance(String airing, Map<String, String> attributes) {
		if (airing.startsWith("/recordings/programs/")) {
			return new Manual(attributes);
		} else if (airing.startsWith("/recordings/movies/")) {
			return new Movie(attributes);
		} else if (airing.startsWith("/recordings/sports/")) {
			return new Sports(attributes);
		} else if (airing.startsWith("/recordings/series/")) {
			return new TV(attributes);
		} else {
			return null;
		}
	}

	protected static final String padLeft(String string, int length, char padChar) {
		int pad = length - string.length();

		if (pad > 0) {
			StringBuilder buffer = new StringBuilder();

			do {
				buffer.append(padChar);
			} while (--pad > 0);

			string = buffer.append(string).toString();
		}

		return string;
	}

	protected static final String padRight(String string, int length, char padChar) {
		int pad = length - string.length();

		if (pad > 0) {
			StringBuilder buffer = new StringBuilder(string);

			do {
				buffer.append(padChar);
			} while (--pad > 0);

			string = buffer.toString();
		}

		return string;
	}

	private static Process startFilter(Recording recording, URL input, File output, Map<String, String> metadata)
			throws IOException {
		List<String> command = new ArrayList<>(20);
		String option;

		command.add("nice");

		command.add(recording.getOption("ffmpeg"));

		command.add("-y");

		command.add("-loglevel");
		command.add("error");

		command.add("-nostdin");

		command.add("-nostats");

		command.add("-i");
		command.add(input.toExternalForm());

		command.add("-bsf:a");
		command.add("aac_adtstoasc");

		if ((option = recording.getOption("crf")) != null) {
			command.add("-codec:a");
			command.add("copy");

			command.add("-crf");
			command.add(option);
		} else if ((option = recording.getOption("videoRate")) != null) {
			command.add("-codec:a");
			command.add("copy");

			command.add("-b:v");
			command.add(option);
		} else {
			command.add("-c");
			command.add("copy");
		}

		metadata.forEach((key, value) -> {
			command.add("-metadata");
			command.add(key + "=" + value.replaceAll("'", "\\'"));
		});

		command.add("-f");
		command.add("mp4");

		command.add(output.getAbsolutePath());

		return new ProcessBuilder(command) // <br/>
				.redirectError(ProcessBuilder.Redirect.INHERIT) // <br/>
				.redirectOutput(ProcessBuilder.Redirect.INHERIT) // <br/>
				.start();
	}

	protected static final String trim(String string) {
		return string != null ? string.trim() : "";
	}

	private final Map<String, String> attributes;

	private boolean finished;

	protected MediaHandler(Map<String, String> attributes) {
		super();
		this.attributes = new HashMap<>(attributes);
		this.finished = true;
	}

	protected void addMeta(Map<String, String> meta) {
		String size = getSize();

		try {
			size = String.format("%,d bytes", Long.valueOf(size));
		} catch (NumberFormatException e) {
			// leave unformated
		}

		meta.put("size", size);

		if (!finished) {
			meta.put("unfinished", "true");
		} else if ("false".equalsIgnoreCase(getAndTrim("clean"))) {
			meta.put("clean", "false");
		}
	}

	public final void cacheAttributes(Cache cache, String ip, String airing) {
		if (finished) {
			cache.putAttributes(ip, airing, attributes);
		}
	}

	public void fetchAttributes(URL airing) throws IOException {
		Map<?, ?> meta = (Map<?, ?>) Util.readJSON(airing);

		processMetadata(meta);
	}

	/**
	 * @param ip
	 * @param airing
	 * @param recordings
	 * @return
	 * @throws IOException
	 */
	public final Runnable getAction(String ip, String airing, List<Recording> recordings) throws IOException {
		for (int index = 0, count = recordings.size(); index < count; ++index) {
			Recording recording = recordings.get(index);

			if (isSelected(recording) && !skipExisting(recording)) {
				Runnable action = null;

				if (booleanOption(recording, "list")) {
					action = () -> {
						System.out.printf("Video: %s%n", airing);
						printMeta(System.out);
					};
				} else if (isFinished() || booleanOption(recording, "includeUnfinished")) {
					URL playlist = Main.getPlaylistURL(ip, airing);

					if (playlist != null) {
						action = () -> save(recording, playlist);
					} else {
						System.err.println("Failed to get playlist URL for " + airing);
					}
				}

				if (action != null) {
					return new OrderedAction(index, airing, action);
				}

				break;
			}
		}

		return null;
	}

	protected final String getAndTrim(String key) {
		return attributes.getOrDefault(key, "").trim();
	}

	@SuppressWarnings("static-method")
	public Map<String, String> getPersistentMetadata() {
		return Collections.emptyMap();
	}

	protected final String getSize() {
		return getAndTrim("size");
	}

	protected final File getTargetFile(Recording recording) {
		String output = recording.getOption("output");

		if (output == null || output.isEmpty()) {
			return null;
		}

		Map<String, String> values = getTemplateMap(recording);

		if (values == null) {
			return null;
		}

		String path = StringTemplate.expand(output, key -> fixPathSegment(values.get(key)));

		return new File(path);
	}

	protected abstract Map<String, String> getTemplateMap(Recording recording);

	protected abstract Calendar getTime();

	protected final boolean isFinished() {
		return finished;
	}

	public abstract boolean isSelected(Recording recording);

	protected final void printMeta(PrintStream out) {
		Map<String, String> meta = new LinkedHashMap<>();

		addMeta(meta);

		meta.values().removeIf(String::isEmpty);

		int keyWidth = meta.keySet().stream().mapToInt(String::length).max().orElse(0) + 2;

		meta.forEach((name, value) -> out.format("  %s%s%n", // <br/>
				padRight(name + ":", keyWidth, ' '), // <br/>
				value));
	}

	protected void processMetadata(Map<?, ?> meta) {
		trimAndSet("clean", selectUnique(meta, "video_details.clean"));
		finished = "finished".equalsIgnoreCase(trim(selectUnique(meta, "video_details.state")));
		trimAndSet("size", selectUnique(meta, "video_details.size"));
	}

	private void save(Recording recording, URL video) {
		try {
			File dest = getTargetFile(recording);

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

			boolean timestamp = booleanOption(recording, "timestamp");
			boolean fetch = false;

			if (dest.createNewFile()) {
				System.out.println("Saving " + dest.getAbsolutePath());
				fetch = true;
			} else if (booleanOption(recording, "overwrite")) {
				System.out.println("Overwriting " + dest.getAbsolutePath());
				fetch = true;
			} else if (!timestamp) {
				System.out.println("Skipping existing file " + dest.getAbsolutePath());
			}

			if (fetch) {
				// ffmpeg doesn't like non-ASCII filenames
				File temp = File.createTempFile("tablo-", ".tmp", folder);

				try {
					Process process = startFilter(recording, video, temp, getPersistentMetadata());

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
				Calendar time = getTime();

				if (time != null) {
					int min = time.get(Calendar.MINUTE);

					// truncate to a multiple of 5 minutes
					min -= min % 5;

					time.set(Calendar.MINUTE, min);

					if (!fetch) {
						System.out.println("Updating timestamp for " + dest.getAbsolutePath());
					}

					if (time.get(Calendar.YEAR) < 1970) {
						System.out.println("Clamping timestamp to 1970 for " + dest.getAbsolutePath());
						time.set(Calendar.YEAR, 1970);
					}

					dest.setLastModified(time.getTimeInMillis());
				} else if (booleanOption(recording, "debug")) {
					System.out.println("No timestamp provided for " + dest.getAbsolutePath());
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to save " + video + ": " + e.getLocalizedMessage());
		}
	}

	protected final void set(String key, String value) {
		if (value == null || value.isEmpty()) {
			attributes.remove(key);
		} else {
			attributes.put(key, value);
		}
	}

	private boolean skipExisting(Recording recording) {
		if ("ignore".equalsIgnoreCase(recording.getOption("existing"))) {
			File target = getTargetFile(recording);

			if (target != null) {
				return target.exists();
			}
		}

		return false;
	}

	protected final void trimAndSet(String key, String value) {
		set(key, trim(value));
	}

}
