package tablo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RangeList {

	private static final class Range {

		private final int max;

		private final int min;

		Range(int min, int max) {
			super();

			if (min > max) {
				throw new IllegalArgumentException();
			}

			this.min = min;
			this.max = max;
		}

		@SuppressWarnings("hiding")
		boolean contains(int min, int max) {
			return this.min <= min && min <= max && max <= this.max;
		}

		@Override
		public String toString() {
			String result = String.valueOf(min);

			if (min != max) {
				result = result.concat("-");

				if (max != Integer.MAX_VALUE) {
					result = result + max;
				}
			}

			return result;
		}
	}

	/**
	 * Add ranges from a comma-separated list.
	 * e.g.
	 *   1-5,10
	 *   20-
	 */
	private static final Pattern PATTERN = Pattern.compile("(\\d+)(-(\\d*))?");

	private final List<Range> ranges;

	public RangeList() {
		super();
		this.ranges = new ArrayList<>();
	}

	public RangeList add(int value) {
		return add(value, value);
	}

	public RangeList add(int min, int max) {
		if (!contains(min, max)) {
			ranges.add(new Range(min, max));
		}

		return this;
	}

	public void addRanges(String string) throws IllegalArgumentException {
		for (String range : string.split(",")) {
			Matcher matcher = PATTERN.matcher(range);

			if (!matcher.matches()) {
				throw new IllegalArgumentException(string);
			}

			int min = Integer.parseInt(matcher.group(1));
			String max = matcher.group(3);

			if (max == null) {
				add(min);
			} else {
				add(min, max.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(max));
			}
		}
	}

	public boolean contains(int value) {
		return contains(value, value);
	}

	private boolean contains(int min, int max) {
		return ranges.stream().anyMatch(range -> range.contains(min, max));
	}

	public boolean isEmpty() {
		return ranges.isEmpty();
	}

	@Override
	public String toString() {
		return ranges.toString();
	}

}
