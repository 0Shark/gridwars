/*
 *  Bellator_Hunter
 *  We are going straight for the kill with this one.
 */

package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class Bellator_Alexander implements PlayerBot {
  private final long RESERVE = 5; // Reserve population on each cell
  private Coordinates origin; // Store initial cell as reference
  private static final int MIN_POP_SPLIT = 3;
  private static final int MIN_POP_DIRECTIONAL = 5;
  private static final int SPLIT_DIV = 4;
  private static final int DIR_DIV = 2;
  private static final int TURN_THRESH = 150;

  @Override
  public void getNextCommands(UniverseView universeView, List<MovementCommand> list) {
    if (origin == null) {
      origin = universeView.getMyCells().iterator().next(); // Get first cell as origin
    }

    if (universeView.getCurrentTurn() > TURN_THRESH) {
      int turn = universeView.getCurrentTurn();

      for (Coordinates cell : universeView.getMyCells()) {
        int pop = universeView.getPopulation(cell);
        int share = pop - MIN_POP_SPLIT;
        if (turn < TURN_THRESH && pop > MIN_POP_SPLIT) {
          fastSpawn(cell, list, share);
        } else if (pop > MIN_POP_DIRECTIONAL) {
          fastSpread(cell, list, share, universeView);
        }
      }
    } else {
      for (Coordinates myCell : universeView.getMyCells()) {
        long ppl = universeView.getPopulation(myCell);
        Coordinates target = enemyLookup(myCell, universeView);

        if (target != null) { // Attack phase
          Coordinates weakNeighbor = weaknessLookup(target, universeView);
          if (weakNeighbor != null) {
            MovementCommand.Direction[] dirs = getBestDirections(myCell, weakNeighbor, universeView.getUniverseSize(),
                universeView);
            spread(list, myCell, dirs, ppl, weakNeighbor);
          } else {
            MovementCommand.Direction[] dirs = getBestDirections(myCell, target, universeView.getUniverseSize(),
                universeView);
            spread(list, myCell, dirs, ppl, target);
          }
        } else { // Expansion phase
          adaptiveSwarm(myCell, list, DIR_DIV, universeView);
        }
      }
    }
  }

  private void fastSpawn(Coordinates cell, List<MovementCommand> moves, int share) {
    for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
      moves.add(new MovementCommand(cell, dir, share / SPLIT_DIV));
    }
  }

  private void fastSpread(Coordinates cell, List<MovementCommand> moves, int share, UniverseView universe) {
    int x = cell.getX();
    int y = cell.getY();
    int size = universe.getUniverseSize();

    // Trying to expand away from the center to reach edges faster
    // Spiral pattern test
    if (Math.abs(size / 2 - x) > Math.abs(size / 2 - y)) { // Fast horizontal movement
      if (x > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.DOWN, share / DIR_DIV));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.UP, share / DIR_DIV));
      }
      if (y > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.LEFT, share / DIR_DIV));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.RIGHT, share / DIR_DIV));
      }
    } else { // Fast vertical movement
      if (y > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.LEFT, share / DIR_DIV));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.RIGHT, share / DIR_DIV));
      }
      if (x > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.DOWN, share / DIR_DIV));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.UP, share / DIR_DIV));
      }
    }
  }

  private void adaptiveSwarm(Coordinates cell, List<MovementCommand> moves, int share, UniverseView universe) {
    int x = cell.getX();
    int y = cell.getY();
    int size = universe.getUniverseSize();

    // 1. Enemy Proximity Check
    // Optional<Coordinates> nearestEnemy = enemyLookup(cell, universe)
    //     .stream()
    //     .min((c1, c2) -> Integer.compare(getDistance(cell, c1), getDistance(cell, c2)));
    Optional<Coordinates> nearestEnemy = Optional.ofNullable(enemyLookup(cell, universe));

    if (nearestEnemy.isPresent() && getDistance(cell, nearestEnemy.get(), universe) < 5) {
      // Prioritize aggression over standard spread
      MovementCommand.Direction[] attackDirs = getBestDirections(cell, nearestEnemy.get(), size, universe);
      spread(moves, cell, attackDirs, share, nearestEnemy.get());

    } else {
      // 2. Prioritized Spreading (similar to torusRain)
      if (Math.abs(size - x) > Math.abs(size - y)) { // Prioritize horizontal rain
        moves.add(new MovementCommand(cell, MovementCommand.Direction.RIGHT, share / 3)); // Adjusted split for possible
                                                                                          // counter-attack
        moves.add(new MovementCommand(cell, MovementCommand.Direction.LEFT, share / 3));

      } else { // Prioritize vertical rain
        moves.add(new MovementCommand(cell, MovementCommand.Direction.DOWN, share / 3));
        moves.add(new MovementCommand(cell, MovementCommand.Direction.UP, share / 3));
      }

      // 3. Secondary Opportunistic Movement
      if (share % 3 != 0) { // Remaining population
        MovementCommand.Direction secondaryDir;
        if (Math.abs(size - x) > Math.abs(size - y)) {
          secondaryDir = y > size / 2 ? MovementCommand.Direction.LEFT : MovementCommand.Direction.RIGHT;
        } else {
          secondaryDir = x > size / 2 ? MovementCommand.Direction.DOWN : MovementCommand.Direction.UP;
        }
        moves.add(new MovementCommand(cell, secondaryDir, share % 3));
      }
    }
  }

  // Helper for distance calculation (considering torus)
  private int getDistance(Coordinates c1, Coordinates c2, UniverseView universeView) {
    int dx = Math.abs(c1.getX() - c2.getX());
    int dy = Math.abs(c1.getY() - c2.getY());

    int size = universeView.getUniverseSize(); // Adjust for torus wrap-around

    if (dx > size / 2)
      dx = size - dx;
    if (dy > size / 2)
      dy = size - dy;

    return dx + dy;
  }

  private void torusRain(Coordinates cell, List<MovementCommand> moves, int share, UniverseView universe) {
    int x = cell.getX();
    int y = cell.getY();
    int size = universe.getUniverseSize();

    // Prioritize spreading in the direction with furthest wrap-around distance
    if (Math.abs(size - x) > Math.abs(size - y)) { // Prioritize horizontal rain
      // Split horizontally, accounting for wrap-around
      moves.add(new MovementCommand(cell, MovementCommand.Direction.RIGHT, share / 2));
      moves.add(new MovementCommand(cell, MovementCommand.Direction.LEFT, share / 2));
    } else { // Prioritize vertical rain
      // Split vertically, accounting for wrap-around
      moves.add(new MovementCommand(cell, MovementCommand.Direction.DOWN, share / 2));
      moves.add(new MovementCommand(cell, MovementCommand.Direction.UP, share / 2));
    }
  }

  private MovementCommand.Direction[] getBestDirections(Coordinates me, Coordinates target, int size,
      UniverseView universeView) {
    int dx = target.getX() - me.getX();
    int dy = target.getY() - me.getY();

    // Adjust for torus wrap-around (X-axis)
    if (Math.abs(dx) > size / 2) {
      dx = dx > 0 ? -size + dx : size + dx;
    }

    // Adjust for torus wrap-around (Y-axis)
    if (Math.abs(dy) > size / 2) {
      dy = dy > 0 ? -size + dy : size + dy;
    }

    // Find the best directions to move in
    if (Math.abs(dx) >= Math.abs(dy)) {
      return new MovementCommand.Direction[] {
          dx > 0 ? MovementCommand.Direction.RIGHT : MovementCommand.Direction.LEFT,
          dy > 0 ? MovementCommand.Direction.DOWN : MovementCommand.Direction.UP
      };
    } else {
      return new MovementCommand.Direction[] {
          dy > 0 ? MovementCommand.Direction.DOWN : MovementCommand.Direction.UP,
          dx > 0 ? MovementCommand.Direction.RIGHT : MovementCommand.Direction.LEFT
      };
    }
  }

  // Spread population in the specified directions
  private void spread(List<MovementCommand> list, Coordinates c, MovementCommand.Direction[] dirs,
      long ppl,
      Coordinates target) {
    // Keep a reserve equivalent to a single batch
    ppl -= RESERVE;

    while (ppl > 0) {
      for (MovementCommand.Direction dir : dirs) {
        if (ppl <= 0)
          break;

        long unitsToSend = Math.min(ppl, Math.max(1, RESERVE / dirs.length));
        Coordinates nextCell = getNeighbor(c, dir);
        if (nextCell.equals(target)) {
          unitsToSend = ppl; // Send all remaining ppl towards the target
        }
        list.add(new MovementCommand(c, dir, (int) unitsToSend));
        ppl -= unitsToSend;
      }
    }
  }

  // Looking for the closest enemy cell
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

  // Get the neighbor cell in the specified direction
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

  // Looks for the weakest cell surrounding the target enemy cell
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

  private void spiralSpread(Coordinates cell, List<MovementCommand> moves, int share, UniverseView universe) {
    int x = cell.getX();
    int y = cell.getY();
    int size = universe.getUniverseSize();

    // Trying to expand away from the center to reach edges faster
    // Spiral pattern test
    if (Math.abs(size / 2 - x) > Math.abs(size / 2 - y)) { // Fast horizontal movement
      if (x > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.DOWN, share / 2));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.UP, share / 2));
      }
      if (y > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.LEFT, share / 2));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.RIGHT, share / 2));
      }
    } else { // Fast vertical movement
      if (y > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.LEFT, share / 2));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.RIGHT, share / 2));
      }
      if (x > size / 2) {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.DOWN, share / 2));
      } else {
        moves.add(new MovementCommand(cell, MovementCommand.Direction.UP, share / 2));
      }
    }
  }
}
