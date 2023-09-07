package tablo.util;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTemplate {

	private static final Pattern ReferencePattern = Pattern.compile("\\$\\{([a-z]+)(?:,(\\d+))?\\}");

	/**
	 * Expand references like '${title}' or '${episode,2}' in the given template.
	 * When present, the suffix ',2' requests the value be padded on the left with
	 * zeroes until it is at least 2 characters in length.
	 *
	 * @param template the template to be expanded
	 * @param valueProvider the mapping from references to values
	 * @return the expanded template
	 * @throws IllegalArgumentException if the template makes references unknown to valueProvider
	 * or if the padding expression is excessive
	 */
	public static String expand(String template, Function<String, String> valueProvider) {
		Matcher matcher = ReferencePattern.matcher(template);

		if (!matcher.find()) {
			return template;
		}

		StringBuffer expansion = new StringBuffer();

		do {
			String name = matcher.group(1);
			String value = valueProvider.apply(name);

			if (value == null) {
				throw new IllegalArgumentException("No such variable: " + name);
			}

			String width = matcher.group(2);

			if (width != null) {
				try {
					value = padLeft(value, Integer.parseInt(width), '0');
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Width out of bounds: " + width);
				}
			}

			matcher.appendReplacement(expansion, value);
		} while (matcher.find());

		return matcher.appendTail(expansion).toString();
	}

	private static final String padLeft(String string, int length, char padChar) {
		int pad = length - string.length();

		if (pad > 0) {
			StringBuilder buffer = new StringBuilder(length);

			do {
				buffer.append(padChar);
			} while (--pad > 0);

			string = buffer.append(string).toString();
		}

		return string;
	}

}
