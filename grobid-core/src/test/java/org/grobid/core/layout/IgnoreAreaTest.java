package org.grobid.core.layout;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class IgnoreAreaTest {

    @Test
    public void testConstructor_withAreaType() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, AreaType.FIGURE);

        assertThat(area.getPage(), is(1));
        assertThat(area.getX(), is(100.0));
        assertThat(area.getY(), is(200.0));
        assertThat(area.getWidth(), is(300.0));
        assertThat(area.getHeight(), is(150.0));
        assertThat(area.getType(), is(AreaType.FIGURE));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLegacyConstructor_withStringName() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, "figure");

        assertThat(area.getType(), is(AreaType.FIGURE));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLegacyConstructor_withNullName_defaultsToIgnore() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, (String) null);

        assertThat(area.getType(), is(AreaType.IGNORE));
    }

    @Test
    public void testFromCoordinates_validString() {
        IgnoreArea area = IgnoreArea.fromCoordinates("1,100.5,200.5,300.0,150.0,table");

        assertThat(area.getPage(), is(1));
        assertThat(area.getX(), is(100.5));
        assertThat(area.getY(), is(200.5));
        assertThat(area.getWidth(), is(300.0));
        assertThat(area.getHeight(), is(150.0));
        assertThat(area.getType(), is(AreaType.TABLE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromCoordinates_insufficientParts() {
        IgnoreArea.fromCoordinates("1,100,200,300,150"); // missing type
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromCoordinates_invalidNumber() {
        IgnoreArea.fromCoordinates("1,abc,200,300,150,figure");
    }

    @Test
    public void testContains_tokenInsideArea() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 100.0, 200.0, 200.0, AreaType.FIGURE);

        LayoutToken token = new LayoutToken();
        token.setPage(1);
        token.setX(150.0);
        token.setY(150.0);
        token.setWidth(20.0);
        token.setHeight(10.0);

        assertThat(area.contains(token), is(true));
    }

    @Test
    public void testContains_tokenOutsideArea() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 100.0, 200.0, 200.0, AreaType.FIGURE);

        LayoutToken token = new LayoutToken();
        token.setPage(1);
        token.setX(500.0);
        token.setY(500.0);
        token.setWidth(20.0);
        token.setHeight(10.0);

        assertThat(area.contains(token), is(false));
    }

    @Test
    public void testContains_tokenOnDifferentPage() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 100.0, 200.0, 200.0, AreaType.FIGURE);

        LayoutToken token = new LayoutToken();
        token.setPage(2);
        token.setX(150.0);
        token.setY(150.0);
        token.setWidth(20.0);
        token.setHeight(10.0);

        assertThat(area.contains(token), is(false));
    }

    @Test
    public void testContains_tokenIntersectsArea() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 100.0, 200.0, 200.0, AreaType.FIGURE);

        // Token partially inside the area
        LayoutToken token = new LayoutToken();
        token.setPage(1);
        token.setX(290.0); // Starts at the edge
        token.setY(150.0);
        token.setWidth(20.0);
        token.setHeight(10.0);

        assertThat(area.contains(token), is(true));
    }

    @Test
    public void testContains_tokenBarelyOutsideRight() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 100.0, 200.0, 200.0, AreaType.FIGURE);

        LayoutToken token = new LayoutToken();
        token.setPage(1);
        token.setX(301.0); // Just outside the right edge (100 + 200 = 300)
        token.setY(150.0);
        token.setWidth(20.0);
        token.setHeight(10.0);

        assertThat(area.contains(token), is(false));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetName_legacy() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, AreaType.TABLE);

        assertThat(area.getName(), is("table"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetName_legacy() {
        IgnoreArea area = new IgnoreArea();
        area.setName("ignore");

        assertThat(area.getType(), is(AreaType.IGNORE));
    }

    @Test
    public void testEquals() {
        IgnoreArea area1 = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, AreaType.FIGURE);
        IgnoreArea area2 = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, AreaType.FIGURE);

        assertThat(area1.equals(area2), is(true));
        assertThat(area1.hashCode(), is(area2.hashCode()));
    }

    @Test
    public void testEquals_differentType() {
        IgnoreArea area1 = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, AreaType.FIGURE);
        IgnoreArea area2 = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, AreaType.TABLE);

        assertThat(area1.equals(area2), is(false));
    }

    @Test
    public void testToString() {
        IgnoreArea area = new IgnoreArea(1, 100.0, 200.0, 300.0, 150.0, AreaType.FIGURE);

        String result = area.toString();
        assertThat(result.contains("page=1"), is(true));
        assertThat(result.contains("type='figure'"), is(true));
    }

    @Test
    public void testSetters() {
        IgnoreArea area = new IgnoreArea();
        area.setPage(2);
        area.setX(50.0);
        area.setY(75.0);
        area.setWidth(100.0);
        area.setHeight(80.0);
        area.setType(AreaType.IGNORE);

        assertThat(area.getPage(), is(2));
        assertThat(area.getX(), is(50.0));
        assertThat(area.getY(), is(75.0));
        assertThat(area.getWidth(), is(100.0));
        assertThat(area.getHeight(), is(80.0));
        assertThat(area.getType(), is(AreaType.IGNORE));
    }
}
