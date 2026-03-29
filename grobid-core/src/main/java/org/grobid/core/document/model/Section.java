package org.grobid.core.document.model;

import java.util.ArrayList;
import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * A document section with a heading and child elements (paragraphs, lists, etc.).
 * Sections can be nested — a section at level 1 may contain sections at level 2.
 *
 * <p>Note: this is a mutable class (not a record) because children are added
 * incrementally as the DocumentStructureBuilder processes token clusters.</p>
 */
public final class Section implements BodyElement {

    private final String heading;
    private final String headingNumber;  // e.g. "1.2.3", null if unnumbered
    private final int level;             // depth derived from heading number (1-based)
    private final List<BoundingBox> headingCoords;
    private final String headingId;      // generated xml:id, may be null

    private final List<BodyElement> children = new ArrayList<>();

    public Section(String heading, String headingNumber, int level,
            List<BoundingBox> headingCoords, String headingId) {
        this.heading = heading;
        this.headingNumber = headingNumber;
        this.level = level;
        this.headingCoords = headingCoords != null ? headingCoords : List.of();
        this.headingId = headingId;
    }

    public String getHeading() {
        return heading;
    }

    public String getHeadingNumber() {
        return headingNumber;
    }

    public int getLevel() {
        return level;
    }

    public List<BoundingBox> getHeadingCoords() {
        return headingCoords;
    }

    public String getHeadingId() {
        return headingId;
    }

    public List<BodyElement> getChildren() {
        return children;
    }

    public void addChild(BodyElement child) {
        children.add(child);
    }
}
