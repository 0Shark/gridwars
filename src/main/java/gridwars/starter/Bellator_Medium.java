package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.List;

public class Bellator_Medium implements PlayerBot {

  private static final int MIN_POP_SPLIT = 4;
  private static final int MIN_POP_DIRECTIONAL = 7;
  private static final int SPLIT_DIV = 4;
  private static final int DIR_DIV = 2;
  private static final int TURN_THRESH = 300;

  @Override
  public void getNextCommands(UniverseView universe, List<MovementCommand> moves) {
    int turn = universe.getCurrentTurn();
    for (Coordinates cell : universe.getMyCells()) {
      int pop = universe.getPopulation(cell);
      int share = pop - MIN_POP_SPLIT;
      if (turn < TURN_THRESH && pop > MIN_POP_SPLIT) {
        fastSpawn(cell, moves, share);
      } else if (pop > MIN_POP_DIRECTIONAL) {
        fastSpread(cell, moves, share, universe);
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
}
