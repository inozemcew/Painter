package NPainter.Convert;

import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.*;

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
        ColorCell cc1 = c1.complement(list, null);
        assertTrue(cc1.equals(c4));
        assertFalse(cc1.equals(c1));
        assertTrue(c1.complement(list2, null).isEmpty());
    }


    @Test
    public void testStat() {
        ColorMatrix mat = new ColorMatrix(4,4);
        mat.put(0,0, Arrays.asList(7));
        mat.put(0,1, Arrays.asList(1,2,3,4));
        mat.put(0,2, Arrays.asList(5,6,7,8));
        mat.put(1,1, Arrays.asList(8));
        mat.put(1,1, Arrays.asList(2));
        Deque<List<Integer>> stat = Combinator.getStat(mat);
        assertEquals(stat.size(),2);
    }

    @Test
    public void testCombinator(){
        {
            Set<ColorCell> ink = new HashSet<>(Arrays.asList(new ColorCell(1, 2), new ColorCell(3, 4)));
            Set<ColorCell> paper = new HashSet<>(Arrays.asList(new ColorCell(4, 5)));
            Integer[] list = {1, 2, 4};
            testRank(ink, paper, list, Combinator.Match.All);
        }
        {
            Set<ColorCell> ink = new HashSet<>(Arrays.asList(new ColorCell(1), new ColorCell(2)));
            Set<ColorCell> paper = new HashSet<>(Arrays.asList(new ColorCell(0, 6)));
            Integer[] list = {0, 4};
            List<ColorCell> cells = testRank(ink, paper, list, Combinator.Match.All);
            final ColorCell cell = cells.get(0);
            Combinator.addTo(ink, cell);
            assertEquals(ink.size(), 2);
            ColorCell complement = cell.complement(Arrays.asList(list), null);
            Combinator.addTo(paper, complement);
            assertEquals(paper.size(),1);
        }

    }

    private List<ColorCell> testRank(Set<ColorCell> ink, Set<ColorCell> paper, Integer[] l, Combinator.Match key ) {
        List<Integer> list = Arrays.asList(l);
        ComplementMaps map = Combinator.createComplementMaps(Arrays.asList(list),null);
        Stream<ColorCell> ps = Combinator.getColorPairsStream(list, null);
        Map<Combinator.Match, List<ColorCell>> m = ps.collect(Collectors.groupingBy(c -> Combinator.rank(c, map.get(list), ink, paper)));
        assertTrue(m.containsKey(key));
        return m.get(key);
    }
}
