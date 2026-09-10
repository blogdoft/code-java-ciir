package dev.ftathiago.ciir.application.input;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Minimal, read-only {@code pom.xml} inspection — just enough to tell an aggregator POM ({@code
 * <modules>}) apart from a leaf module POM, and to list its declared module paths. Deliberately
 * does not depend on the full Maven model builder (YAGNI); no build/dependency resolution happens
 * here.
 */
public final class PomInspector {

  private PomInspector() {}

  /** {@code true} when {@code pomPath} declares at least one {@code <modules><module>}. */
  public static boolean isAggregator(Path pomPath) throws IOException {
    return !readModules(pomPath).isEmpty();
  }

  /**
   * The module's {@code <artifactId>} — the Maven analogue of a C# {@code .csproj}'s filename, used
   * as this module's logical CIIR project name. Falls back to the parent directory name if the POM
   * has no direct {@code <artifactId>} child (should not happen for a valid POM).
   */
  public static String readArtifactId(Path pomPath) throws IOException {
    var project = parse(pomPath);
    var children = project.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      var node = children.item(i);
      if (node instanceof Element element && "artifactId".equals(element.getTagName())) {
        var text = element.getTextContent();
        if (text != null && !text.isBlank()) {
          return text.trim();
        }
      }
    }
    var parent = pomPath.getParent();
    return parent != null ? parent.getFileName().toString() : pomPath.toString();
  }

  /** The raw (relative) module directory paths declared by {@code <modules><module>...}. */
  public static List<String> readModules(Path pomPath) throws IOException {
    var project = parse(pomPath);
    var modules = new ArrayList<String>();

    var modulesNodes = project.getElementsByTagName("modules");
    for (int i = 0; i < modulesNodes.getLength(); i++) {
      var modulesElement = (Element) modulesNodes.item(i);
      if (modulesElement.getParentNode() != project) {
        continue;
      }
      NodeList moduleNodes = modulesElement.getElementsByTagName("module");
      for (int j = 0; j < moduleNodes.getLength(); j++) {
        var text = moduleNodes.item(j).getTextContent();
        if (text != null && !text.isBlank()) {
          modules.add(text.trim());
        }
      }
    }
    return modules;
  }

  private static Element parse(Path pomPath) throws IOException {
    try {
      var factory = DocumentBuilderFactory.newInstance();
      // Disable external entity resolution (XXE hardening) — this is a read-only project-
      // file reader, not a general-purpose XML parser.
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

      var builder = factory.newDocumentBuilder();
      var document = builder.parse(pomPath.toFile());
      return document.getDocumentElement();
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException("Failed to parse " + pomPath + ": " + e.getMessage(), e);
    }
  }
}
