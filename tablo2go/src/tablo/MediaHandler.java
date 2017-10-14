package tablo;

import java.io.File;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Map;

public abstract class MediaHandler {

	public static final MediaHandler IGNORE = new MediaHandler(null) {

		@Override
		public File getTargetFile(Map<?, ?> meta) {
			return null;
		}

		@Override
		public Calendar getTime(Map<?, ?> meta) {
			return null;
		}

		@Override
		public String getState(Map<?, ?> meta) {
			return "";
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

	public abstract String getState(Map<?, ?> meta);

	public abstract File getTargetFile(Map<?, ?> meta);

	public abstract Calendar getTime(Map<?, ?> meta);

	public final boolean isFinished(Map<?, ?> meta) {
		return "finished".equalsIgnoreCase(getState(meta));
	}

	public abstract boolean isSelected(MediaSelector selector, Map<?, ?> meta);

	public abstract void printMeta(PrintStream out, Map<?, ?> meta);

}
