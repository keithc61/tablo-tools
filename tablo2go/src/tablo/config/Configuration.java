package tablo.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class Configuration {

	private static interface Element {

		String getOption(String name);

		Map<String, String> getOptions();

		void setOption(String name, String value);

	}

	private static final class Handler extends DefaultHandler {

		private static final Element ErrorElement = new Element() {

			@Override
			public String getOption(String name) {
				return null;
			}

			@Override
			public Map<String, String> getOptions() {
				return Collections.emptyMap();
			}

			@Override
			public void setOption(String name, String value) {
				return;
			}

		};

		private final List<Recording> recordings;

		private final LinkedList<Element> stack;

		Handler() {
			super();
			this.recordings = new LinkedList<>();
			this.stack = new LinkedList<>();
		}

		private void addOption(Attributes attributes) throws SAXException {
			String name = null;
			String value = null;

			for (int index = 0, count = attributes.getLength(); index < count; ++index) {
				String attributeName = attributes.getQName(index);
				String attributeValue = attributes.getValue(index);

				switch (attributeName) {
				case "name":
					name = attributeValue;
					break;
				case "value":
					value = attributeValue;
					break;
				default:
					unexpectedAttribute("option", attributeName);
					break;
				}
			}

			if (name != null && value != null) {
				stack.peek().setOption(name, value);
			} else {
				warning(new SAXParseException("option requires both 'name' and 'value' attributes", null));
			}
		}

		private void addRecording(Attributes attributes) throws SAXException {
			String name = null;
			String type = null;

			for (int index = 0, count = attributes.getLength(); index < count; ++index) {
				String attributeName = attributes.getQName(index);
				String attributeValue = attributes.getValue(index);

				switch (attributeName) {
				case "name":
					name = attributeValue;
					break;
				case "type":
					type = attributeValue;
					break;
				default:
					unexpectedAttribute("recording", attributeName);
					break;
				}
			}

			if (name != null && type != null) {
				Recording recording = new Recording(name, type, stack.peek());

				recordings.add(recording);
				stack.push(recording);
			} else {
				warning(new SAXParseException("recording ignored without both 'name' and 'type' attributes", null));
				stack.push(ErrorElement);
			}
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (stack.isEmpty()) {
				error(new SAXParseException("unexpected endElement", null));
			} else {
				stack.pop();
			}
		}

		List<Recording> getRecordings() {
			return recordings;
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			switch (name) {
			case "config":
			case "project":
				if (stack.isEmpty()) {
					stack.push(new Options());
				} else {
					unexpectedElement(name);
				}
				break;

			case "option":
				if (stack.isEmpty()) {
					unexpectedElement(name);
				} else {
					addOption(attributes);
					stack.push(ErrorElement);
				}
				break;

			case "recording":
				if (stack.isEmpty()) {
					unexpectedElement(name);
				} else {
					addRecording(attributes);
				}
				break;

			default:
				unexpectedElement(name);
				break;
			}
		}

		private void unexpectedAttribute(String elementName, String name) throws SAXException {
			String message = String.format("unknown attribute: %s/%s", elementName, name);

			warning(new SAXParseException(message, null));
		}

		private void unexpectedElement(String name) throws SAXException {
			error(new SAXParseException("unexpected element: " + name, null));
			stack.push(ErrorElement);
		}

	}

	private static class Options implements Element {

		private final Map<String, String> options;

		Options() {
			super();
			options = new HashMap<>();
		}

		@Override
		public String getOption(String name) {
			return options.get(name);
		}

		@Override
		public Map<String, String> getOptions() {
			return Collections.unmodifiableMap(options);
		}

		@Override
		public void setOption(String name, String value) {
			options.put(name, value);
		}

	}

	private static final class Recording extends Options {

		private final Element defaultOptions;

		private final String name;

		private final String type;

		Recording(String name, String type, Element defaultOptions) {
			super();
			this.defaultOptions = defaultOptions;
			this.name = name;
			this.type = type;
		}

		@Override
		public String getOption(String optionName) {
			String value = super.getOption(optionName);

			if (value == null) {
				value = defaultOptions.getOption(optionName);
			}

			return value;
		}

		@Override
		public Map<String, String> getOptions() {
			Map<String, String> options = new HashMap<>();

			options.putAll(defaultOptions.getOptions());
			options.putAll(super.getOptions());

			return options;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

	}

	public static void main(String[] args) {
		for (String arg : args) {
			try {
				List<Recording> recordings = parse(arg);

				process(recordings);
			} catch (IOException | ParserConfigurationException | SAXException e) {
				// TODO Auto-generated catch block
			}
		}
	}

	private static List<Recording> parse(String fileName)
			throws IOException, ParserConfigurationException, SAXException {
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		Handler handler = new Handler();

		try (FileInputStream input = new FileInputStream(fileName)) {
			parser.parse(input, handler);
		}

		return handler.getRecordings();
	}

	private static void process(List<Recording> recordings) {
		String format = "" //
				+ "Name:     %s%n" //
				+ "  type:   %s%n";

		for (Recording recording : recordings) {
			System.out.printf(format, recording.getName(), recording.getType());

			recording.getOptions().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
					.forEach(e -> System.out.printf("  option: %s=%s%n", e.getKey(), e.getValue()));
		}
	}

	public Configuration() {
		super();
	}

}
