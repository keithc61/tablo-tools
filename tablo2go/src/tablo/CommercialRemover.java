package tablo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to trim segments (e.g. commercials) from a video with ffmpeg.
 *
 * @author keithc
 */
public final class CommercialRemover {

	/**
	 * The H.264 'constant rate factor'.
	 */
	private static String crf = "25";

	/**
	 * The extension of 'edit list' files.
	 */
	private static final String EDL_EXTENSION = ".edl";

	/**
	 * A pattern to extract the relevant information from edit list files.
	 */
	private static final Pattern EDL_LINE_PATTERN = Pattern.compile(
			"\\s*(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+0\\s*", Pattern.CASE_INSENSITIVE);

	/**
	 * How to locate ffmpeg.
	 */
	private static String ffmpeg = "ffmpeg";

	/**
	 * A pattern to detect and decompose video filenames.
	 */
	private static final Pattern VIDEO = Pattern.compile("(.*)(\\.avi|\\.mkv|\\.mp4)", Pattern.CASE_INSENSITIVE);

	/**
	 * Append a 'pad' to the given buffer. In a complex filter, pads name the
	 * inputs and outputs of individual filters which are to be connected together.
	 * The name of the pad to be writtern here is 'prefix + index'.
	 */
	private static void appendPad(StringBuilder buffer, char prefix, int index) {
		buffer.append('[').append(prefix).append(index).append(']');
	}

	public static void main(String[] args) {
		for (String arg : args) {
			try {
				if (arg.startsWith("-crf=")) {
					crf = arg.substring(5);
				} else if (arg.startsWith("-fmpeg=")) {
					ffmpeg = arg.substring(7);
				} else {
					process(arg);
				}
			} catch (IOException e) {
				printError("Error processing", arg);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Given a trim list containing:
	 *     49.92:814.15
	 *   1043.14:1664.36
	 *   1922.89:2405.00
	 *   2697.63:3002.97
	 *   3276.74:3532.00
	 *   3825.12
	 *
	 * Produce the complex filter:
	 *	 [0:v] split=6 [s0][s1][s2][s3][s4][s5];
	 *	 [s0] trim=49.92:814.15,    setpts=PTS-STARTPTS [v0];
	 *	 [s1] trim=1043.14:1664.36, setpts=PTS-STARTPTS [v1];
	 *	 [s2] trim=1922.89:2405.00, setpts=PTS-STARTPTS [v2];
	 *	 [s3] trim=2697.63:3002.97, setpts=PTS-STARTPTS [v3];
	 *	 [s4] trim=3276.74:3532.00, setpts=PTS-STARTPTS [v4];
	 *	 [s5] trim=3825.12,         setpts=PTS-STARTPTS [v5];
	 *
	 *	 [0:a] asplit=6 [t0][t1][t2][t3][t4][t5];
	 *	 [t1] atrim=49.92:814.15,    asetpts=PTS-STARTPTS [a0];
	 *	 [t1] atrim=1043.14:1664.36, asetpts=PTS-STARTPTS [a1];
	 *	 [t2] atrim=1922.89:2405.00, asetpts=PTS-STARTPTS [a2];
	 *	 [t3] atrim=2697.63:3002.97, asetpts=PTS-STARTPTS [a3];
	 *	 [t4] atrim=3276.74:3532.00, asetpts=PTS-STARTPTS [a4];
	 *	 [t5] atrim=3825.12,         asetpts=PTS-STARTPTS [a5];
	 *
	 *	 [v0][a0][v1][a1][v2][a2][v3][a3][v4][a4][v5][a5] concat=n=6:v=1:a=1 [v] [a]
	 */
	private static String makeFilter(List<String> trimList) {
		final char videoIn = 's';
		final char audioIn = 't';
		final char videoOut = 'v';
		final char audioOut = 'a';

		final StringBuilder filter = new StringBuilder();
		final int trimCount = trimList.size();

		// [0:v] split=6 [s0][s1][s2][s3][s4][s5]
		filter.append("[0:v] split=").append(trimCount).append(' ');
		for (int i = 0; i < trimCount; ++i) {
			appendPad(filter, videoIn, i);
		}

		// ; [s0] trim=49.92:814.15, setpts=PTS-STARTPTS [v0] ...
		for (int i = 0; i < trimCount; ++i) {
			filter.append("; ");
			appendPad(filter, videoIn, i);
			filter.append(" trim=").append(trimList.get(i)).append(", setpts=PTS-STARTPTS ");
			appendPad(filter, videoOut, i);
		}

		// ; [0:a] asplit=5 [t0][t1][t2][t3][t4][t5] ...
		filter.append("; [0:a] asplit=").append(trimCount).append(' ');
		for (int i = 0; i < trimCount; ++i) {
			appendPad(filter, audioIn, i);
		}

		// ; [t0] atrim=49.92:814.15, asetpts=PTS-STARTPTS [a0] ...
		for (int i = 0; i < trimCount; ++i) {
			filter.append("; ");
			appendPad(filter, audioIn, i);
			filter.append(" atrim=").append(trimList.get(i)).append(", asetpts=PTS-STARTPTS ");
			appendPad(filter, audioOut, i);
		}

		// ; [v0][a0][v1][a1][v2][a2][v3][a3][v4][a4][v5][a5] concat=n=6:v=1:a=1 [v][a]
		filter.append("; ");
		for (int i = 0; i < trimCount; ++i) {
			appendPad(filter, videoOut, i);
			appendPad(filter, audioOut, i);
		}
		filter.append(" concat=n=").append(trimCount).append(":v=1:a=1 [v][a]");

		return filter.toString();
	}

	private static void printError(String label, String argument) {
		System.err.printf("%s: %s%n", label, argument);
	}

	/**
	 * Process the video in the given fileName.
	 *
	 * @param fileName
	 * @throws IOException
	 */
	private static void process(String fileName) throws IOException {
		Matcher matcher = VIDEO.matcher(fileName);

		if (!matcher.matches()) {
			printError("Unsupported file type", fileName);
			return;
		}

		File videoFile = new File(fileName);

		if (!videoFile.exists()) {
			printError("File not found", fileName);
			return;
		}

		String baseName = matcher.group(1);
		File edlFile = new File(baseName + EDL_EXTENSION);

		if (!edlFile.exists()) {
			printError("File not found", edlFile.getAbsolutePath());
			return;
		}

		File outFile = new File(baseName + "-nc.mp4");

		if (outFile.exists()) {
			printError("Output already exists", outFile.getAbsolutePath());
			return;
		}

		// ffmpeg ... -i in.mp4 -filter_complex "%filter%" -map "[v]" -map "[a]" {codecs} out.mp4
		List<String> command = new ArrayList<>();

		command.add("nice");

		command.add(ffmpeg);

		command.add("-accurate_seek");

		command.add("-nostdin");

		command.add("-loglevel");
		command.add("error");

		command.add("-nostats");

		command.add("-i");
		command.add(fileName);

		command.add("-filter_complex");
		command.add(makeFilter(readEditList(edlFile)));

		command.add("-map");
		command.add("[v]");

		command.add("-map");
		command.add("[a]");

		command.add("-codec:v");
		command.add("h264");

		command.add("-crf");
		command.add(crf);

		command.add("-codec:a");
		command.add("aac");

		command.add("-b:a");
		command.add("160k");

		command.add("-shortest");

		command.add(outFile.getAbsolutePath());

		System.out.println("Processing: " + fileName);

		Process process = new ProcessBuilder(command) // <br/>
				.redirectError(ProcessBuilder.Redirect.INHERIT) // <br/>
				.redirectOutput(ProcessBuilder.Redirect.INHERIT) // <br/>
				.start();

		try {
			process.getOutputStream().close();
			process.waitFor();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Given an edit list file containing:
	 *	0.00	49.92	0
	 *	814.15	1043.14	0
	 *	1664.36	1922.89	0
	 *	2405.00	2697.63	0
	 *	3002.97	3276.74	0
	 *	3532.00	3825.12	0
	 *
	 * produce a 'trim list' containing:
	 *     49.92:814.15
	 *   1043.14:1664.36
	 *   1922.89:2405.00
	 *   2697.63:3002.97
	 *   3276.74:3532.00
	 *   3825.12
	 */
	private static List<String> readEditList(File edlFile) throws IOException {
		try (BufferedReader r = new BufferedReader(new FileReader(edlFile))) {
			List<String> trimList = new ArrayList<>();
			String lastStop = "0.00";
			String line;

			while ((line = r.readLine()) != null) {
				Matcher matcher = EDL_LINE_PATTERN.matcher(line);

				if (matcher.matches()) {
					String start = matcher.group(1);

					if (!start.equals(lastStop)) {
						trimList.add(lastStop + ":" + start);
					}

					lastStop = matcher.group(2);
				}
			}

			trimList.add(lastStop);

			return trimList;
		}
	}

}
