package tablo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Cache {

	//  <cache>
	//    <tablo ip="10.1.9.90">
	//      <recording id="/recordings/series/episodes/298492">
	//        <attribute name="airDate" value="2018-02-28 21:00" />
	//        <attribute name="episode" value="5" />
	//        <attribute name="originalAirDate" value="2018-02-28 21:00" />
	//        <attribute name="season" value="1" />
	//        <attribute name="series" value="The Detectives" />
	//        <attribute name="size" value="1291988992" />
	//        <attribute name="title" value="Stranger Calling" />
	//      </recording>
	//    </tablo>
	//  </cache>

	private static final String TagAttribute = "attribute";
	private static final String TagCache = "cache";
	private static final String TagRecording = "recording";
	private static final String TagTablo = "tablo";

	private static void addRecording(Map<String, String> attributes, Node recording) {
		NodeList children = recording.getChildNodes();

		for (int i = 0, n = children.getLength(); i < n; ++i) {
			Node node = children.item(i);

			if (!(node instanceof Element)) {
				continue;
			}

			Element attribute = (Element) node;

			if (!TagAttribute.equals(attribute.getTagName())) {
				continue;
			}

			Attr name = attribute.getAttributeNode("name");
			Attr value = attribute.getAttributeNode("value");

			if (name != null && value != null) {
				attributes.put(name.getValue(), value.getValue());
			}
		}
	}

	private static void addRecordings(Map<String, Map<String, String>> tablo, Node root) {
		NodeList children = root.getChildNodes();

		for (int i = 0, n = children.getLength(); i < n; ++i) {
			Node node = children.item(i);

			if (!(node instanceof Element)) {
				continue;
			}

			Element element = (Element) node;

			if (!TagRecording.equals(element.getTagName())) {
				continue;
			}

			Attr id = element.getAttributeNode("id");

			if (id != null) {
				addRecording(tablo.computeIfAbsent(id.getValue(), attributeMapSupplier()), node);
			}
		}
	}

	private static Function<String, Map<String, String>> attributeMapSupplier() {
		return addr -> newAttributeMap();
	}

	private static Map<String, String> newAttributeMap() {
		return new TreeMap<>();
	}

	private static Element newChildElement(Node parent, String name) {
		Document document = parent instanceof Document ? (Document) parent : parent.getOwnerDocument();
		Element child = document.createElement(name);

		parent.appendChild(child);

		return child;
	}

	private static DocumentBuilder newDocumentBuilder() throws IOException {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	private static Function<String, Map<String, Map<String, String>>> recordingMapSupplier() {
		return addr -> new TreeMap<>();
	}

	private static void xmlAddAttribute(Element recording, String name, String value) {
		Element attribute = newChildElement(recording, TagAttribute);

		attribute.setAttribute("name", name);
		attribute.setAttribute("value", value);
	}

	private static void xmlAddRecording(Element tablo, String id, Map<String, String> attributes) {
		Element recording = newChildElement(tablo, TagRecording);

		recording.setAttribute("id", id);
		attributes.forEach((name, value) -> xmlAddAttribute(recording, name, value));
	}

	private static void xmlAddRecordings(Element root, String ip, Map<String, Map<String, String>> recordings) {
		Element tablo = newChildElement(root, TagTablo);

		tablo.setAttribute("ip", ip);
		recordings.forEach((id, attributes) -> xmlAddRecording(tablo, id, attributes));
	}

	// ip -> (recording -> (attr -> value))
	private final Map<String, Map<String, Map<String, String>>> content;

	public Cache() {
		super();
		this.content = new TreeMap<>();
	}

	public Map<String, String> getAttributes(String address, String recording) {
		Map<String, Map<String, String>> recordings = content.getOrDefault(address, Collections.emptyMap());
		Map<String, String> attributes = recordings.getOrDefault(recording, Collections.emptyMap());

		return Collections.unmodifiableMap(attributes);
	}

	public void load(File file) throws IOException {
		Document document = null;

		try (InputStream in = new FileInputStream(file)) {
			document = newDocumentBuilder().parse(in);
		} catch (SAXException e) {
			throw new IOException(e);
		}

		Element root = document.getDocumentElement();

		if (!TagCache.equals(root.getTagName())) {
			return;
		}

		NodeList children = root.getChildNodes();

		for (int i = 0, n = children.getLength(); i < n; ++i) {
			Node node = children.item(i);

			if (!(node instanceof Element)) {
				continue;
			}

			Element element = (Element) node;

			if (!TagTablo.equals(element.getTagName())) {
				continue;
			}

			Attr address = element.getAttributeNode("ip");

			if (address != null) {
				addRecordings(content.computeIfAbsent(address.getValue(), recordingMapSupplier()), node);
			}
		}
	}

	public void putAttributes(String address, String recording, Map<String, String> attributes) {
		Map<String, String> copy = newAttributeMap();

		copy.putAll(attributes);

		content.computeIfAbsent(address, recordingMapSupplier()) // <br/>
				.put(recording, copy);
	}

	public void retainRecordings(String address, Collection<String> recordings) {
		content.getOrDefault(address, Collections.emptyMap()).keySet().retainAll(recordings);
	}

	public void save(File file) throws IOException {
		Document document = newDocumentBuilder().newDocument();
		Element cache = newChildElement(document, TagCache);

		content.forEach((ip, recordings) -> xmlAddRecordings(cache, ip, recordings));

		try (OutputStream out = new FileOutputStream(file)) {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();

			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

			DOMSource source = new DOMSource(cache);
			StreamResult target = new StreamResult(out);

			transformer.transform(source, target);
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}

}
