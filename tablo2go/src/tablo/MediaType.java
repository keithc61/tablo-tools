package tablo;

import static tablo.Util.selectUnique;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.function.Consumer;

public enum MediaType {

	Manual {
		@Override
		public MediaHandler handler(String directory) {
			return new ManualHandler(directory);
		}
	},

	Movie {
		@Override
		public MediaHandler handler(String directory) {
			return new MovieHandler(directory);
		}
	},

	Sports {
		@Override
		public MediaHandler handler(String directory) {
			return new SportsHandler(directory);
		}
	},

	TV {
		@Override
		public MediaHandler handler(String directory) {
			return new TVHandler(directory);
		}
	},

	Unknown {
		@Override
		public MediaHandler handler(String directory) {
			// TODO
			return null;
		}
	};

	private static final class ManualHandler extends MediaHandler {

		private static String getAirDate(Map<?, ?> meta) {
			return selectUnique(meta, "recManualProgramAiring.jsonForClient.airDate");
		}

		private static String getSize(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recManualProgramAiring.jsonForClient.video.size"));
		}

		private static String getTitle(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recManualProgram.jsonForClient.title"));
		}

		ManualHandler(String directory) {
			super(directory);
		}

		@Override
		public String getState(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recManualProgramAiring.jsonForClient.video.state"));
		}

		@Override
		public File getTargetFile(Map<?, ?> meta) {
			String title = getTitle(meta);

			if (!title.isEmpty()) {
				StringBuilder fileName = new StringBuilder();

				fileName.append(title);

				Calendar time = getTime(meta);

				if (time != null) {
					fileName.append(' ');
					fileName.append(FileTimeFormat.format(time.getTime()));
				}

				fileName.append(".mp4");

				return Paths.get(directory, fixFilename(fileName.toString())).toFile();
			}

			return null;
		}

		@Override
		public Calendar getTime(Map<?, ?> meta) {
			return Util.parseAirTime(getAirDate(meta));
		}

		@Override
		public boolean isSelected(MediaSelector selector, Map<?, ?> meta) {
			return selector.isSelectedTitle(getTitle(meta));
		}

		@Override
		public void printMeta(PrintStream out, Map<?, ?> meta) {
			out.println("  type:  Manual recording");
			out.println("  title: " + getTitle(meta));
			out.println("  aired: " + Util.formatAirTime(getAirDate(meta)));
			out.println("  size:  " + getSize(meta));
		}

	}

	private static final class MovieHandler extends MediaHandler {

		private static String getSize(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recMovieAiring.jsonForClient.video.size"));
		}

		private static String getTitle(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recMovie.jsonForClient.title"));
		}

		private static int getYear(Map<?, ?> meta) {
			String year = selectUnique(meta, "recMovie.jsonForClient.releaseYear");

			try {
				return Integer.parseInt(year);
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		MovieHandler(String directory) {
			super(directory);
		}

		@Override
		public String getState(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recMovieAiring.jsonForClient.video.state"));
		}

		@Override
		public File getTargetFile(Map<?, ?> meta) {
			String title = getTitle(meta);

			if (!title.isEmpty()) {
				StringBuilder buffer = new StringBuilder(title);
				int year = getYear(meta);

				if (year != 0) {
					buffer.append(" (").append(year).append(')');
				}

				String filename = buffer.append(".mp4").toString();

				return Paths.get(directory, fixFilename(filename)).toFile();
			}

			return null;
		}

		@Override
		public Calendar getTime(Map<?, ?> meta) {
			Calendar time = null;
			int year = getYear(meta);

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

		@Override
		public boolean isSelected(MediaSelector selector, Map<?, ?> meta) {
			return selector.isSelectedTitle(getTitle(meta));
		}

		@Override
		public void printMeta(PrintStream out, Map<?, ?> meta) {
			int year = getYear(meta);

			out.println("  type:  Movie");
			out.println("  title: " + getTitle(meta));

			if (year != 0) {
				out.println("  released: " + year);
			}

			out.println("  size:     " + getSize(meta));
		}

	}

	private static final class SportsHandler extends MediaHandler {

		private static String getAirDate(Map<?, ?> meta) {
			return selectUnique(meta, "recSportEvent.jsonForClient.airDate");
		}

		private static String getSize(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recSportEvent.jsonForClient.video.size"));
		}

		private static String getTitle(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recSportEvent.jsonForClient.eventTitle"));
		}

		SportsHandler(String directory) {
			super(directory);
		}

		@Override
		public String getState(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recSportEvent.jsonForClient.video.state"));
		}

		@Override
		public File getTargetFile(Map<?, ?> meta) {
			String title = getTitle(meta);

			if (!title.isEmpty()) {
				StringBuilder fileName = new StringBuilder();

				fileName.append(title);

				Calendar time = getTime(meta);

				if (time != null) {
					fileName.append(' ');
					fileName.append(FileTimeFormat.format(time.getTime()));
				}

				fileName.append(".mp4");

				return Paths.get(directory, fixFilename(fileName.toString())).toFile();
			}

			return null;
		}

		@Override
		public Calendar getTime(Map<?, ?> meta) {
			return Util.parseAirTime(getAirDate(meta));
		}

		@Override
		public boolean isSelected(MediaSelector selector, Map<?, ?> meta) {
			return selector.isSelectedTitle(getTitle(meta));
		}

		@Override
		public void printMeta(PrintStream out, Map<?, ?> meta) {
			out.println("  type:  Sports event");
			out.println("  title: " + getTitle(meta));
			out.println("  aired: " + Util.formatAirTime(getAirDate(meta)));
			out.println("  size:  " + getSize(meta));
		}

	}

	private static final class TVHandler extends MediaHandler {

		private static final DateFormat AirDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		private static String getAirDate(Map<?, ?> meta) {
			return selectUnique(meta, "recEpisode.jsonForClient.airDate");
		}

		private static String getEpisode(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recEpisode.jsonForClient.episodeNumber"));
		}

		private static String getOriginalAirDate(Map<?, ?> meta) {
			return selectUnique(meta, "recEpisode.jsonForClient.originalAirDate");
		}

		private static String getSeason(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recEpisode.jsonForClient.seasonNumber"));
		}

		private static String getSeries(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recSeries.jsonForClient.title"));
		}

		private static String getSize(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recEpisode.jsonForClient.video.size"));
		}

		private static String getTitle(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recEpisode.jsonForClient.title"));
		}

		TVHandler(String directory) {
			super(directory);
		}

		@Override
		public String getState(Map<?, ?> meta) {
			return trim(selectUnique(meta, "recEpisode.jsonForClient.video.state"));
		}

		@Override
		public File getTargetFile(Map<?, ?> meta) {
			String series = getSeries(meta);
			String season = getSeason(meta);
			String episode = getEpisode(meta);
			String title = getTitle(meta);

			if (!series.isEmpty()) {
				StringBuilder fileName = new StringBuilder();

				fileName.append(season).append(padLeft(episode, 2, '0'));
				if (!title.isEmpty()) {
					fileName.append(' ');
				}
				fileName.append(title).append(".mp4");

				return Paths.get(directory, fixFilename(series), fixFilename(fileName.toString()))
						.toFile();
			}

			return null;
		}

		@Override
		public Calendar getTime(Map<?, ?> meta) {
			Calendar time = Util.parseAirTime(getAirDate(meta));

			if (time != null) {
				Calendar origDate = Util.parseTime(getOriginalAirDate(meta), AirDateFormat);

				if (origDate != null) {
					// keep time but use original date
					time.set(Calendar.YEAR, origDate.get(Calendar.YEAR));
					time.set(Calendar.MONTH, origDate.get(Calendar.MONTH));
					time.set(Calendar.DAY_OF_MONTH, origDate.get(Calendar.DAY_OF_MONTH));
				}
			}

			return time;
		}

		@Override
		public boolean isSelected(MediaSelector selector, Map<?, ?> meta) {
			return selector.isSelectedTitle(getSeries(meta))
				&& selector.isSelectedSeason(getSeason(meta))
				&& selector.isSelectedEpisode(getEpisode(meta));
		}

		@Override
		public void printMeta(PrintStream out, Map<?, ?> meta) {
			out.println("  type:    Television Show");
			out.println("  series:  " + getSeries(meta));
			out.println("  season:  " + getSeason(meta));
			out.println("  episode: " + getEpisode(meta));
			out.println("  title:   " + getTitle(meta));
			out.println("  aired:   " + Util.formatAirTime(getAirDate(meta)));
			out.println("  size:    " + getSize(meta));
		}

	}

	static final DateFormat FileTimeFormat = new SimpleDateFormat("yyyy-MM-dd HHmm");

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	final static String fixFilename(String name) {
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

	public static void forEach(Consumer<? super MediaType> action) {
		Arrays.stream(values()).forEach(action);
	}

	public static final MediaType fromMeta(Map<?, ?> meta) {
		if (meta.containsKey("recManualProgramAiring")) {
			return Manual;
		} else if (meta.containsKey("recMovieAiring")) {
			return Movie;
		} else if (meta.containsKey("recSportEvent")) {
			return Sports;
		} else if (meta.containsKey("recEpisode")) {
			return TV;
		} else {
			return Unknown;
		}
	}

	final static String padLeft(String string, int length, char padChar) {
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

	final static String trim(String string) {
		return string != null ? string.trim() : "";
	}

	public abstract MediaHandler handler(String directory);

}
