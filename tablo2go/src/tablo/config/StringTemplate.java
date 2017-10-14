package tablo.config;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTemplate {

	private static final Pattern ReferencePattern = Pattern.compile("\\$\\{([^\\$\\}]+)\\}");

	private static String expand(String pattern, Map<String, String> values) {
		String result = pattern;

		for (;;) {
			Matcher matcher = ReferencePattern.matcher(result);

			if (!matcher.find()) {
				break;
			}

			String name = matcher.group(1);
			String value = values.get(name);

			if (value == null) {
				System.err.printf("No such variable: %s%n", name);
				break;
			}

			int s = matcher.start();
			int e = matcher.end();

			result = result.substring(0, s) + value + result.substring(e);
		}

		return result;
	}

	public static void main(String[] args) {
		Map<String, String> variables = new HashMap<>();

		variables.put("series", "The Good Doctor");
		variables.put("season", "1");
		variables.put("episode", "2");
		variables.put("title", "Pilot");

		// %season$d   -> 1
		// %season$02d -> 01

		System.out.format("%%s -> %s%n", "1");
		System.out.format("%%02s -> %02s%n", "1");

		test("D:/video/${series}/${season}${episode} ${title}.mp4", variables);
		// test("D:/video/${series} - s${seasonz}e${episodez} ${title}.mp4", variables);
	}

	private static void test(String pattern, Map<String, String> variables) {
		String expanded = expand(pattern, variables);

		System.out.format("%s%n  -> %s%n", pattern, expanded);
	}

}
