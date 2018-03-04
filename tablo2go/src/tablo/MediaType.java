package tablo;

import java.util.Arrays;
import java.util.function.Consumer;

public enum MediaType {

	Manual, Movie, Sports, TV;

	public static void forEach(Consumer<? super MediaType> action) {
		Arrays.stream(values()).forEach(action);
	}

}
