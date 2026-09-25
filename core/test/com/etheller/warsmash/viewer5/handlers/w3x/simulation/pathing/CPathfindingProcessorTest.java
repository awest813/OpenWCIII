package com.etheller.warsmash.viewer5.handlers.w3x.simulation.pathing;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.parsers.w3x.wpm.War3MapWpm;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWorldCollision;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMove;

class CPathfindingProcessorTest {
    @Test
    void interleavedPlayersProduceTheSamePathsAsIndependentSearches() throws Exception {
        final War3MapWpm wpm = new War3MapWpm(null);
        wpm.getSize()[0] = 96;
        wpm.getSize()[1] = 96;
        final short[] cells = new short[96 * 96];
        for (int y = 0; y < 88; y++) cells[y * 96 + 48] = 2; // Unwalkable wall with a distant opening.
        wpm.setPathing(cells);
        final PathingGrid grid = new PathingGrid(wpm, new float[] { 0, 0 });
        final CWorldCollision collision = new CWorldCollision(new Rectangle(0, 0, 3072, 3072), 64);
        final CPathfindingProcessor.Node[][] nodes = CPathfindingProcessor.createNodes(grid);
        final CPathfindingProcessor.Node[][] corners = CPathfindingProcessor.createCornerNodes(grid);
        final AtomicInteger ids = new AtomicInteger();
        final CPathfindingProcessor first = new CPathfindingProcessor(grid, collision, nodes, corners, ids);
        final CPathfindingProcessor second = new CPathfindingProcessor(grid, collision, nodes, corners, ids);
        final Result a = enqueue(first, grid, false);
        final Result b = enqueue(second, grid, true);
        first.update(null);
        assertNull(a.path, "Fixture must yield so another player can search before it resumes");
        for (int i = 0; i < 40 && (a.path == null || b.path == null); i++) {
            second.update(null);
            first.update(null);
        }
        assertNotNull(a.path);
        assertNotNull(b.path);
        assertFalse(a.path.isEmpty());
        assertFalse(b.path.isEmpty());
        assertEquals(independent(grid, collision, false), a.path);
        assertEquals(independent(grid, collision, true), b.path);
    }

    private static List<Point2D.Float> independent(PathingGrid grid, CWorldCollision collision, boolean reverse) {
        final CPathfindingProcessor processor = new CPathfindingProcessor(grid, collision);
        final Result result = enqueue(processor, grid, reverse);
        for (int i = 0; i < 40 && result.path == null; i++) processor.update(null);
        assertNotNull(result.path);
        return result.path;
    }

    private static Result enqueue(CPathfindingProcessor processor, PathingGrid grid, boolean reverse) {
        final Result result = new Result();
        processor.findNaiveSlowPath(null, null, grid.getWorldX(reverse ? 85 : 10), grid.getWorldY(10),
                new Point2D.Float(grid.getWorldX(reverse ? 10 : 85), grid.getWorldY(10)),
                MovementType.FOOT, reverse ? 8 : 0, false, result);
        return result;
    }

    private static final class Result extends CBehaviorMove {
        List<Point2D.Float> path;
        Result() { super(null); }
        @Override public void pathFound(List<Point2D.Float> waypoints, CSimulation simulation) {
            this.path = waypoints;
        }
    }
}
