package tablo.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class Unclosable extends FilterOutputStream {

	public Unclosable(OutputStream out) {
		super(out);
	}

	@Override
	public void close() throws IOException {
		flush(); // flush but do not close
	}

	@Override
	public void write(byte buffer[], int offset, int length) throws IOException {
		out.write(buffer, offset, length);
	}

}
