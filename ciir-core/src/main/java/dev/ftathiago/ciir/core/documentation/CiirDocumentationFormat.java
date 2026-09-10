package dev.ftathiago.ciir.core.documentation;

/**
 * The formal documentation-comment format a declaration was documented with. This generator only
 * ever emits {@link #JAVADOC}; the remaining values exist for the CIIR contract's cross-language
 * vocabulary (other generators emit them).
 */
public enum CiirDocumentationFormat {
  UNKNOWN,
  XML_DOC,
  JAVADOC,
  JSDOC,
  TSDOC,
  DOCSTRING,
  MARKDOWN,
  PLAIN
}
