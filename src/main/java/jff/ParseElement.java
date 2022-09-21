package jff;

import org.w3c.dom.Element;

/**
 * Interface to parse one document element
 */
public interface ParseElement {
    void parse(Element element);
}
