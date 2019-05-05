package tablo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tablo.io.MediaInputStream;

public final class Util {

	private static final DateFormat AirTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX");

	private static final DateTimeFormatter LocalDateTime = new DateTimeFormatterBuilder() // <br/>
			.parseCaseInsensitive() // <br/>
			.appendValue(ChronoField.YEAR, 4, 4, SignStyle.EXCEEDS_PAD) // <br/>
			.appendLiteral('-') // <br/>
			.appendValue(ChronoField.MONTH_OF_YEAR, 2) // <br/>
			.appendLiteral('-') // <br/>
			.appendValue(ChronoField.DAY_OF_MONTH, 2) // <br/>
			.appendLiteral(' ') // <br/>
			.appendValue(ChronoField.HOUR_OF_DAY, 2) // <br/>
			.appendLiteral(':') // <br/>
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2) // <br/>
			.toFormatter();

	public static boolean containsIgnoreCase(Collection<String> values, String search) {
		Pattern pattern = Pattern.compile(search, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
		Predicate<String> finder = value -> pattern.matcher(value.toString()).find();

		return values.stream().filter(Objects::nonNull).anyMatch(finder);
	}

	public static void copyPlaylist(URL in, OutputStream out) throws IOException {
		try (InputStream data = MediaInputStream.open(in)) {
			byte[] buffer = new byte[8192];
			int len;

			while ((len = data.read(buffer)) >= 0) {
				out.write(buffer, 0, len);
			}
		}
	}

	public static String formatAirTime(String time) {
		Objects.requireNonNull(time);

		try {
			return ZonedDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME) // <br/>
					.withZoneSameInstant(ZoneId.systemDefault()) // <br/>
					.format(LocalDateTime);
		} catch (DateTimeParseException e) {
			return time;
		}
	}

	public static Reader openReader(URL url) throws IOException {
		return new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
	}

	public static Calendar parseAirTime(String time) {
		return parseTime(time, AirTimeFormat);
	}

	public static Calendar parseTime(String time, DateFormat format) {
		try {
			if (time != null) {
				return new Calendar.Builder().setInstant(format.parse(time)).build();
			}
		} catch (java.text.ParseException e) {
			// ignore
		}

		return null;
	}

	public static void printHeaderFields(PrintStream out, URLConnection connection) {
		Comparator<String> order = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
		Map<String, List<String>> fields = new TreeMap<>(order);

		fields.putAll(connection.getHeaderFields());

		if (fields.isEmpty()) {
			return;
		}

		out.println("Header fields:");

		for (Map.Entry<String, List<String>> field : fields.entrySet()) {
			String name = field.getKey();
			List<String> values = field.getValue();

			if (values.size() == 1) {
				out.printf("  %s: %s%n", name, values.get(0));
			} else {
				out.printf("  %s:%n", name);

				values.stream().forEach(value -> out.printf("    %s%n", value));
			}
		}
	}

	public static String readFully(URL url) throws IOException {
		StringBuilder buffer = new StringBuilder(128 * 1024);
		char[] segment = new char[8192];
		int length;

		try (Reader reader = openReader(url)) {
			while ((length = reader.read(segment)) >= 0) {
				buffer.append(segment, 0, length);
			}
		}

		return buffer.toString();
	}

	public static String readIndex(URL tablo) throws IOException {
		return readFully(new URL(tablo, "/pvr/"));
	}

	public static Object readJSON(Reader reader) throws IOException {
		try {
			BufferedReader buffered = new BufferedReader(reader);

			buffered.mark(1);

			if (buffered.read() == -1) {
				return Collections.EMPTY_MAP;
			}

			buffered.reset();

			return new JSONParser().parse(buffered);
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	public static Object readJSON(URL url) throws IOException {
		try (Reader reader = openReader(url)) {
			return readJSON(reader);
		}
	}

	public static List<Object> select(Object object, String path) {
		List<Object> result = new ArrayList<>();

		result.add(object);

		for (String segment : path.split("\\.")) {
			List<Object> inputs = result;

			result = new ArrayList<>();

			for (Object input : inputs) {
				if (segment.equals("*")) {
					if (input instanceof List<?>) {
						result.addAll((List<?>) input);
					} else if (input instanceof Map<?, ?>) {
						result.addAll(((Map<?, ?>) input).values());
					} else {
						result.add(input);
					}
				} else {
					if (input instanceof Map<?, ?>) {
						Object value = ((Map<?, ?>) input).get(segment);

						if (value != null) {
							result.add(value);
						}
					}
				}
			}

			if (result.isEmpty()) {
				break;
			}
		}

		return result;
	}

	public static List<String> selectJSON(URL url, String path) throws IOException {
		return toStrings(select(readJSON(url), path));
	}

	public static String selectUnique(Map<?, ?> meta, String paths) {
		List<Object> values = select(meta, paths);

		return values.size() == 1 ? String.valueOf(values.get(0)) : null;
	}

	public static List<String> toStrings(Collection<?> values) {
		return values.stream().map(String::valueOf).collect(Collectors.toList());
	}

}
