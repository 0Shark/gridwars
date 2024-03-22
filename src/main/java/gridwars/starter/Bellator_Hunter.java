package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Arrays;

public class Bellator_Hunter implements PlayerBot {
    private final long RESERVE = 5;
    private Coordinates origin;

    @Override
    public void getNextCommands(UniverseView universeView, List<MovementCommand> list) {
        if (origin == null) {
            origin = universeView.getMyCells().iterator().next();
        }

        int universeSize = universeView.getUniverseSize();
        Coordinates[] edgeTargets = initializeEdgeTargets(universeSize, universeView); // Get targets for all borders

        for (Coordinates myCell : universeView.getMyCells()) {
            long ppl = universeView.getPopulation(myCell);

            Coordinates target = findClosestUnclaimedEdge(universeView, edgeTargets, myCell);

            // Prioritize edges until all edges are yours
            if (target != null || !allEdgesClaimed(universeView, edgeTargets)) {
                spreadAndAttack(list, myCell, ppl, target, universeView, edgeTargets);
            } else {
                // Edges claimed, find closest empty spot for inward spread
                Coordinates emptyNeighbor = findClosestEmptySpot(myCell, universeView);
                if (emptyNeighbor != null) {
                    // spread(list, myCell, dirs, ppl, emptyNeighbor);
                    spreadAndAttack(list, myCell, ppl, emptyNeighbor, universeView, edgeTargets);
                }
            }
        }
    }

    private void spreadAndAttack(List<MovementCommand> list, Coordinates myCell, long ppl, Coordinates target,
            UniverseView universeView, Coordinates[] cornerTargets) {
        MovementCommand.Direction[] dirs = getBestDirections(myCell, target, universeView.getUniverseSize(),
                universeView);

        // Prioritize filling empty spots
        if (allCornersClaimed(universeView, cornerTargets)) {
            Coordinates emptyNeighbor = findClosestEmptySpot(myCell, universeView);
            if (emptyNeighbor != null) {
                spread(list, myCell, dirs, ppl, emptyNeighbor);
                return; // We filled an empty spot, so we're done
            }
        }

        // Otherwise, proceed with corner attack logic
        Coordinates weakNeighbor = weaknessLookup(target, universeView);
        if (weakNeighbor != null) {
            spread(list, myCell, dirs, ppl, weakNeighbor);
        } else {
            spread(list, myCell, dirs, ppl, target);
        }
    }

    private MovementCommand.Direction[] getBestDirections(Coordinates me, Coordinates target, int size,
            UniverseView universeView) {
        int dx = target.getX() - me.getX();
        int dy = target.getY() - me.getY();

        if (Math.abs(dx) > size / 2) {
            dx = dx > 0 ? -size + dx : size + dx;
        }

        if (Math.abs(dy) > size / 2) {
            dy = dy > 0 ? -size + dy : size + dy;
        }

        return new MovementCommand.Direction[] {
                dx > 0 ? MovementCommand.Direction.RIGHT : MovementCommand.Direction.LEFT,
                dy > 0 ? MovementCommand.Direction.DOWN : MovementCommand.Direction.UP
        };
    }

    private void spread(List<MovementCommand> list, Coordinates c, MovementCommand.Direction[] dirs, long ppl,
            Coordinates target) {
        ppl -= RESERVE;

        while (ppl > 0) {
            for (MovementCommand.Direction dir : dirs) {
                if (ppl <= 0)
                    break;

                long unitsToSend = Math.min(ppl, Math.max(1, RESERVE / dirs.length));
                Coordinates nextCell = getNeighbor(c, dir);
                if (nextCell.equals(target)) {
                    unitsToSend = ppl;
                }
                list.add(new MovementCommand(c, dir, (int) unitsToSend));
                ppl -= unitsToSend;
            }
        }
    }

    private Coordinates enemyLookup(Coordinates myCell, UniverseView universeView) {
        int size = universeView.getUniverseSize();
        List<Coordinates> enemyCells = new ArrayList<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (!universeView.belongsToMe(x, y) && universeView.getPopulation(x, y) > 0) {
                    enemyCells.add(universeView.getCoordinates(x, y));
                }
            }
        }
        Optional<Coordinates> closest = enemyCells.stream()
                .min((c1, c2) -> Math.abs(c1.getX() - myCell.getX()) + Math.abs(c1.getY() - myCell.getY()) -
                        Math.abs(c2.getX() - myCell.getX()) - Math.abs(c2.getY() - myCell.getY()));

        return closest.orElse(null);
    }

    private Coordinates getNeighbor(Coordinates c, MovementCommand.Direction dir) {
        switch (dir) {
            case UP:
                return c.getUp();
            case DOWN:
                return c.getDown();
            case LEFT:
                return c.getLeft();
            case RIGHT:
                return c.getRight();
            default:
                return c;
        }
    }

    private Coordinates weaknessLookup(Coordinates target, UniverseView universeView) {
        Coordinates weakestCell = null;
        long weakestPopulation = Long.MAX_VALUE;

        for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
            Coordinates neighbor = getNeighbor(target, dir);
            long neighborPop = universeView.getPopulation(neighbor);

            if (!universeView.belongsToMe(neighbor) && neighborPop < weakestPopulation) {
                weakestPopulation = neighborPop;
                weakestCell = neighbor;
            }
        }

        return weakestCell;
    }


    // TODO: FIX THIS
    private Coordinates findClosestUnclaimedCorner(UniverseView universeView, Coordinates[] corners,
            Coordinates myCell) {
        Optional<Coordinates> closestUnclaimed = Arrays.stream(corners)
                .filter(corner -> !universeView.belongsToMe(corner))
                .min((c1, c2) -> Integer.compare(distance(myCell, c1, universeView),
                        distance(myCell, c2, universeView)));

        return closestUnclaimed.orElse(null);
    }

    private int distance(Coordinates c1, Coordinates c2, UniverseView universeView) {
        int universeSize = universeView.getUniverseSize();
        int dx = Math.min(Math.abs(c1.getX() - c2.getX()), universeSize - Math.abs(c1.getX() - c2.getX()));
        int dy = Math.min(Math.abs(c1.getY() - c2.getY()), universeSize - Math.abs(c1.getY() - c2.getY()));
        return dx + dy;
    }

    private Coordinates findClosestEmptySpot(Coordinates myCell, UniverseView universeView) {
        int universeSize = universeView.getUniverseSize();

        for (int distance = 1; distance <= universeSize; distance++) {
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbor = myCell.getRelative(distance, dir);
                if (universeView.isEmpty(neighbor)) {
                    return neighbor;
                }
            }
        }
        return null;
    }

    private boolean allCornersClaimed(UniverseView universeView, Coordinates[] corners) {
        for (Coordinates corner : corners) {
            if (!universeView.belongsToMe(corner)) {
                return false; // At least one corner is not claimed
            }
        }
        return true; // All corners have been claimed
    }

    private Coordinates[] initializeEdgeTargets(int size, UniverseView universeView) {
        List<Coordinates> targets = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            targets.add(universeView.getCoordinates(i, 0)); // Top edge
            targets.add(universeView.getCoordinates(i, size - 1)); // Bottom edge
            targets.add(universeView.getCoordinates(0, i)); // Left edge
            targets.add(universeView.getCoordinates(size - 1, i)); // Right edge
        }
        return targets.toArray(new Coordinates[0]);
    }

    private boolean allEdgesClaimed(UniverseView universeView, Coordinates[] edgeTargets) {
        for (Coordinates edgeCell : edgeTargets) {
            if (!universeView.belongsToMe(edgeCell)) {
                return false; // At least one edge cell is not claimed
            }
        }
        return true; // All edge cells are claimed
    }

    private Coordinates findClosestUnclaimedEdge(UniverseView universeView, Coordinates[] edgeTargets,
            Coordinates myCell) {
        Optional<Coordinates> closestUnclaimed = Arrays.stream(edgeTargets)
                .filter(edgeCell -> !universeView.belongsToMe(edgeCell))
                .min((c1, c2) -> Integer.compare(distance(myCell, c1, universeView),
                        distance(myCell, c2, universeView)));

        return closestUnclaimed.orElse(null);
    }
}