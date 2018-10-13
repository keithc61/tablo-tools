package tablo;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
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

	private static final class ErrorElement implements Element {

		static final Element INSTANCE = new ErrorElement();

		private ErrorElement() {
			super();
		}

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

	}

	private static final class Group extends OptionHolder {

		Group(Element context, Attributes attributes) {
			super(context);
			setOptions(attributes);
		}

	}

	private static final class Handler extends DefaultHandler {

		private final Map<String, String> options;

		private final List<Recording> recordings;

		private final LinkedList<Element> stack;

		Handler(List<Recording> recordings, Map<String, String> options) {
			super();
			this.options = options;
			this.recordings = recordings;
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

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (stack.isEmpty()) {
				error(new SAXParseException("unexpected endElement", null));
			} else {
				stack.pop();
			}
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			switch (name) {
			case "config":
			case "project":
				if (stack.isEmpty()) {
					stack.push(new OptionHolder(options));
				} else {
					unexpectedElement(name);
				}
				break;

			case "group":
				if (stack.isEmpty()) {
					unexpectedElement(name);
				} else {
					stack.push(new Group(stack.peek(), attributes));
				}
				break;

			case "option":
				if (stack.isEmpty()) {
					unexpectedElement(name);
				} else {
					addOption(attributes);
					stack.push(ErrorElement.INSTANCE);
				}
				break;

			case "recording":
				if (stack.isEmpty()) {
					unexpectedElement(name);
				} else {
					RecordingElement recording = new RecordingElement(stack.peek(), attributes);

					recordings.add(recording);
					stack.push(recording);
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
			stack.push(ErrorElement.INSTANCE);
		}

	}

	private static class OptionHolder implements Element {

		private final Element context;

		private final Map<String, String> options;

		OptionHolder(Element context) {
			super();
			this.context = context;
			this.options = new HashMap<>();
		}

		OptionHolder(Map<String, String> options) {
			super();
			this.context = null;
			this.options = options;
		}

		@Override
		public final String getOption(String name) {
			String value = options.get(name);

			if (value == null && context != null) {
				value = context.getOption(name);
			}

			return value;
		}

		@Override
		public final Map<String, String> getOptions() {
			Map<String, String> result = new HashMap<>();

			if (context != null) {
				result.putAll(context.getOptions());
			}

			result.putAll(options);

			return Collections.unmodifiableMap(result);
		}

		@Override
		public void setOption(String name, String value) {
			options.put(name, value);
		}

		final void setOptions(Attributes attributes) {
			for (int index = 0, count = attributes.getLength(); index < count; ++index) {
				String name = attributes.getQName(index);
				String value = attributes.getValue(index);

				setOption(name, value);
			}
		}

	}

	private static final class RecordingElement extends OptionHolder implements Recording {

		RecordingElement(Element context, Attributes attributes) {
			super(context);
			setOptions(attributes);
		}

	}

	public static void parse(String fileName, List<Recording> recordings, Map<String, String> options)
			throws IOException, ParserConfigurationException, SAXException {
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

		try (FileInputStream input = new FileInputStream(fileName)) {
			Handler handler = new Handler(recordings, options);

			parser.parse(input, handler);
		}
	}

	private Configuration() {
		super();
	}

}
