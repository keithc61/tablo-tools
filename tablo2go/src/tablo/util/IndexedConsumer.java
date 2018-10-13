package tablo.util;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.stream.Stream;

public final class IndexedConsumer<T> implements Consumer<T> {

	public static <T> void forEachIndexed(Collection<T> collection, ObjIntConsumer<T> action) {
		forEachIndexed(collection.stream(), action);
	}

	public static <T> void forEachIndexed(Stream<T> stream, ObjIntConsumer<T> action) {
		stream.sequential().forEach(new IndexedConsumer<>(action));
	}

	private final ObjIntConsumer<T> action;

	private int index;

	private IndexedConsumer(ObjIntConsumer<T> action) {
		super();
		this.action = Objects.requireNonNull(action);
		this.index = 0;
	}

	@Override
	public void accept(T value) {
		action.accept(value, index++);
	}

}
