package NPainter.Convert;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Created by ainozemtsev on 09.11.16.
 */
public class ConvertTest {
    List<ColorCell> colorCells = new ArrayList<>();
    {
        colorCells.add(new ColorCell(0,0));
        colorCells.add(new ColorCell(1,1));
        colorCells.add(new ColorCell(1,1));
        colorCells.add(new ColorCell(2,2));
        colorCells.add(new ColorCell(2,2));
        colorCells.add(new ColorCell(2,2));
    }

    @Test
    public void testPairCount() throws Exception {
/*
        ColorCell p = new ColorCell(colorCells,0,0);
        Assert.assertEquals(p.getCount(),0);
        p = new ColorCell(colorCells,1,1);
        Assert.assertEquals(p.getCount(),1);
        p = new ColorCell(colorCells,2,2);
        Assert.assertEquals(p.getCount(),2);
        p = new ColorCell(colorCells,3,3);
        Assert.assertEquals(p.getCount(),0);
*/
        ColorCell c1 = new ColorCell(1,0);
        ColorCell c2 = new ColorCell(0,1);
        ColorCell c3 = new ColorCell(0);
        ColorCell c4 = new ColorCell(2,3);
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertTrue(c1.contains(c3));
        assertFalse(c3.contains(c1));
        final ArrayList<Integer> list = new ArrayList<>();
        list.add(0); list.add(1); list.add(2); list.add(3);
        final ArrayList<Integer> list2 = new ArrayList<>();
        list.add(0); list.add(1);
        ColorCell cc1 = c1.complement(list);
        assertTrue(cc1.equals(c4));
        assertFalse(cc1.equals(c1));
        assertTrue(c1.complement(list2).isEmpty());
    }

    @Test
    public void testCombinator(){
        final HashSet<ColorCell> ink = new HashSet<>();
        ink.add(new ColorCell(1,2));
        final HashSet<ColorCell> paper = new HashSet<>();
        paper.add(new ColorCell(3,4));
        List<Integer> list = IntStream.of(2,3,4).mapToObj(Integer::new).collect(Collectors.toList());
        Combinator combinator = new Combinator(ink, paper);
        Stream<ColorCell> ps = Combinator.getColorPairsStream(list, null);
        Map<Integer, List<ColorCell>> m = ps.collect(Collectors.groupingBy(p -> p.rank(list, ink, paper)));
        assertTrue(m.containsKey(0));
    }
}
