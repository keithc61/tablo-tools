package tablo.io;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public final class MediaInputStream extends InputStream {

	private static boolean isPlaylist(String contentType) {
		return "application/x-mpegURL".equalsIgnoreCase(contentType)
				|| "vnd.apple.mpegURL".equalsIgnoreCase(contentType);
	}

	private static boolean isVideo(String contentType) {
		return "video/MP2T".equalsIgnoreCase(contentType);
	}

	public static InputStream open(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		String contentType = connection.getContentType();
		InputStream input = connection.getInputStream();

		if (isPlaylist(contentType)) {
			return new MediaInputStream(url, input);
		} else if (isVideo(contentType)) {
			return input;
		} else {
			input.close();
			throw new IOException("Unsupported content-type: " + contentType);
		}
	}

	/**
	 * Read the next URL from the playlist.
	 *
	 * @param reader
	 * @return the next URL or null if there are no further URLs
	 * @throws IOException
	 */
	private static String readEntry(BufferedReader reader) throws IOException {
		String entry;
		int hash;

		while ((entry = reader.readLine()) != null) {
			if ((hash = entry.indexOf('#')) >= 0) {
				entry = entry.substring(0, hash);
			}

			if (!(entry = entry.trim()).isEmpty()) {
				break;
			}
		}

		return entry;
	}

	private final URL baseURL;

	private InputStream content;

	private BufferedReader playlist;

	private MediaInputStream(URL url, InputStream input) throws IOException {
		super();
		this.baseURL = url;
		this.content = null;
		this.playlist = new BufferedReader(new InputStreamReader(input, "UTF-8"));
		openNext();
	}

	@Override
	public void close() throws IOException {
		if (content != null) {
			content.close();
			content = null;
		}

		if (playlist != null) {
			playlist.close();
			playlist = null;
		}

		super.close();
	}

	private void openNext() throws IOException {
		if (content != null) {
			content.close();
			content = null;
		}

		if (playlist != null) {
			String entry = readEntry(playlist);

			if (entry == null) {
				playlist.close();
				playlist = null;
			} else {
				content = open(new URL(baseURL, entry));
			}
		}
	}

	@Override
	public int read() throws IOException {
		if (content == null) {
			throw new EOFException();
		}

		int byteValue;

		while ((byteValue = content.read()) < 0) {
			openNext();

			if (content == null) {
				// first EOF encounter
				break;
			}
		}

		return byteValue;
	}

	@Override
	public int read(byte buffer[], int offset, int length) throws IOException {
		if (content == null) {
			throw new EOFException();
		}

		int byteCount;

		while ((byteCount = content.read(buffer, offset, length)) < 0) {
			openNext();

			if (content == null) {
				// first EOF encounter
				break;
			}
		}

		return byteCount;
	}

}
