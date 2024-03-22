package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.List;

public class ExpandBot implements PlayerBot {

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();
        var turn = universeView.getCurrentTurn();
        boolean isEvenTurn = turn % 2 == 0;

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);

            if (isEvenTurn) {
                // Spread during even turns
                int split = 1;

                // Check left, right, up, down for cells that don't belong to me
                for (MovementCommand.Direction direction : MovementCommand.Direction.values()) {
                    if (!universeView.isEmpty(cell.getNeighbour(direction))) {
                        split++;
                    }
                }

                // Expand to all neighbors
                for (MovementCommand.Direction direction : MovementCommand.Direction.values()) {
                    if (!universeView.isEmpty(cell.getNeighbour(direction))) {
                        commandList.add(new MovementCommand(cell, direction, currentPopulation / split));
                    }
                }
            } else {
                // Spread only to outer layers during odd turns
                if (cell.getX() == 0 || cell.getX() == universeView.getUniverseSize() - 1 ||
                        cell.getY() == 0 || cell.getY() == universeView.getUniverseSize() - 1) {
                    // Expand to neighbors if on outer layers
                    for (MovementCommand.Direction direction : MovementCommand.Direction.values()) {
                        if (!universeView.isEmpty(cell.getNeighbour(direction))) {
                            commandList.add(new MovementCommand(cell, direction, currentPopulation));
                        }
                    }
                }
            }
        }
    }
}
