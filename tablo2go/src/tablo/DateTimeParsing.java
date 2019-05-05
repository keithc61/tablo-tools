package tablo;

public final class DateTimeParsing {

	public static void main(String[] args) {
		print("2013-04-19T22:00Z");

		for (String arg : args) {
			print(arg);
		}
	}

	private static void print(String text) {
		String readable = Util.formatAirTime(text);

		System.out.printf("%s -> %s%n", text, readable);
	}

}
