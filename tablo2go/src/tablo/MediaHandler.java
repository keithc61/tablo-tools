package tablo;

import static tablo.Util.selectUnique;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
		public File getTargetFile(Configuration configuration) {
			String directory = configuration.directoryFor(MediaType.Manual);

			if (directory == null) {
				return null;
			}

			String title = getTitle();

			if (title.isEmpty()) {
				return null;
			}

			StringBuilder fileName = new StringBuilder();

			fileName.append(title);

			Calendar time = getTime();

			if (time != null) {
				fileName.append(' ');
				fileName.append(FileTimeFormat.format(time.getTime()));
			}

			fileName.append(".mp4");

			return Paths.get(directory, fixFilename(fileName.toString())).toFile();
		}

		@Override
		public Calendar getTime() {
			return Util.parseAirTime(getAirDate());
		}

		private String getTitle() {
			return getAndTrim("title");
		}

		@Override
		public boolean isSelected(Configuration configuration) {
			return configuration.directoryFor(MediaType.Manual) != null // <br/>
					&& configuration.isSelectedTitle(getTitle());
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
		public File getTargetFile(Configuration configuration) {
			String directory = configuration.directoryFor(MediaType.Movie);

			if (directory == null) {
				return null;
			}

			String title = getTitle();

			if (title.isEmpty()) {
				return null;
			}

			StringBuilder buffer = new StringBuilder(title);
			int year = getYear();

			if (year != 0) {
				buffer.append(" (").append(year).append(')');
			}

			String filename = buffer.append(".mp4").toString();

			return Paths.get(directory, fixFilename(filename)).toFile();
		}

		@Override
		public Calendar getTime() {
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
		public boolean isSelected(Configuration configuration) {
			return configuration.directoryFor(MediaType.Movie) != null // <br/>
					&& configuration.isSelectedTitle(getTitle());
		}

		@Override
		protected void processMetadata(Map<?, ?> meta) {
			moviePath = trim(selectUnique(meta, "movie_path"));
			set("title", selectUnique(meta, "airing_details.show_title"));
			super.processMetadata(meta);
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
		public File getTargetFile(Configuration configuration) {
			String directory = configuration.directoryFor(MediaType.Sports);

			if (directory == null) {
				return null;
			}

			String title = getTitle();

			if (title.isEmpty()) {
				return null;
			}

			StringBuilder fileName = new StringBuilder();

			fileName.append(title);

			Calendar time = getTime();

			if (time != null) {
				fileName.append(' ');
				fileName.append(FileTimeFormat.format(time.getTime()));
			}

			fileName.append(".mp4");

			return Paths.get(directory, fixFilename(fileName.toString())).toFile();
		}

		@Override
		public Calendar getTime() {
			return Util.parseAirTime(getAirDate());
		}

		private String getTitle() {
			return getAndTrim("title");
		}

		@Override
		public boolean isSelected(Configuration configuration) {
			return configuration.directoryFor(MediaType.Sports) != null // <br/>
					&& configuration.isSelectedTitle(getTitle());
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
		public File getTargetFile(Configuration configuration) {
			String directory = configuration.directoryFor(MediaType.TV);

			if (directory == null) {
				return null;
			}

			String series = getSeries();

			if (series.isEmpty()) {
				return null;
			}

			StringBuilder fileName = new StringBuilder();
			String season = getSeason();
			String episode = getEpisode();
			String title = getTitle();

			fileName.append(season).append(padLeft(episode, 2, '0'));

			if (!title.isEmpty()) {
				fileName.append(' ').append(title);
			}

			fileName.append(".mp4");

			return Paths.get(directory, fixFilename(series), fixFilename(fileName.toString())).toFile();
		}

		@Override
		public Calendar getTime() {
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
		public boolean isSelected(Configuration configuration) {
			return configuration.directoryFor(MediaType.TV) != null // <br/>
					&& configuration.isSelectedTitle(getSeries()) // <br/>
					&& configuration.isSelectedSeason(getSeason()) // <br/>
					&& configuration.isSelectedEpisode(getEpisode());
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

	protected static final String fixFilename(String name) {
		StringBuilder buffer = new StringBuilder();

		for (char ch : name.toCharArray()) {
			switch (ch) {
			case '\0':
			case '<':
			case '>':
			case ':':
			case '\"':
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

	public abstract File getTargetFile(Configuration configuration);

	public abstract Calendar getTime();

	public final boolean isFinished() {
		return finished;
	}

	public abstract boolean isSelected(Configuration configuration);

	public final void printMeta(PrintStream out) {
		Map<String, String> meta = new LinkedHashMap<>();

		addMeta(meta);

		meta.values().removeIf(String::isEmpty);

		int keyWidth = meta.keySet().stream().mapToInt(String::length).max().orElse(0) + 2;

		meta.entrySet().forEach(entry -> out.format("  %s%s%n", // <br/>
				padRight(entry.getKey() + ":", keyWidth, ' '), // <br/>
				entry.getValue()));
	}

	protected void processMetadata(Map<?, ?> meta) {
		finished = "finished".equalsIgnoreCase(trim(selectUnique(meta, "video_details.state")));
		trimAndSet("size", selectUnique(meta, "video_details.size"));
	}

	protected final void set(String key, String value) {
		if (value == null || value.isEmpty()) {
			attributes.remove(key);
		} else {
			attributes.put(key, value);
		}
	}

	protected final void trimAndSet(String key, String value) {
		set(key, trim(value));
	}

}
