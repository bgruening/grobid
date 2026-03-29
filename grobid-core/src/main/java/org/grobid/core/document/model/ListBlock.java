package org.grobid.core.document.model;

import java.util.List;

/**
 * A list (ordered or unordered) containing list items.
 * Produced when consecutive ITEM-labeled token clusters are encountered.
 *
 * @param items  the list items in order
 */
public record ListBlock(List<ListItem> items) implements BodyElement {
}
