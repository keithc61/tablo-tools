package tablo;

import static tablo.Util.selectUnique;

import java.io.File;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

public abstract class MediaHandler {

	protected static final String trim(String string) {
		return string != null ? string.trim() : "";
	}

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

	@SuppressWarnings("static-method")
	public final String getState(Map<?, ?> meta) {
		return trim(selectUnique(meta, "video_details.state"));
	}

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
