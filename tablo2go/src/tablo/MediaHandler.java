package tablo;

import java.io.File;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

public abstract class MediaHandler {

	public static final MediaHandler IGNORE = new MediaHandler(null) {

		@Override
		public String getState(Map<?, ?> meta) {
			return "";
		}

		@Override
		public File getTargetFile(Map<?, ?> meta) {
			return null;
		}

		@Override
		public Calendar getTime(Map<?, ?> meta) {
			return null;
		}

		@Override
		public boolean isSelected(MediaSelector selector, Map<?, ?> meta) {
			return false;
		}

		@Override
		public void printMeta(PrintStream out, Map<?, ?> meta) {
			return;
		}

	};

	protected final String directory;

	public MediaHandler(String directory) {
		super();
		this.directory = directory;
	}

	/**
	 * @param meta raw metadata retrieved from tablo
	 */
	@SuppressWarnings("static-method")
	public Map<String, String> getPersistentMetadata(Map<?, ?> meta) {
		return Collections.emptyMap();
	}

	public abstract String getState(Map<?, ?> meta);

	public abstract File getTargetFile(Map<?, ?> meta);

	public abstract Calendar getTime(Map<?, ?> meta);

	public final boolean isFinished(Map<?, ?> meta) {
		return "finished".equalsIgnoreCase(getState(meta));
	}

	public abstract boolean isSelected(MediaSelector selector, Map<?, ?> meta);

	public void printMeta(PrintStream out, Map<?, ?> meta) {
		if (false == isFinished(meta)) {
			out.println("  unfinished");
		}
	}

}
