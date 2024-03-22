package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.List;

public class Bellator_Mini implements PlayerBot {

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);

            // Better split threshold so we don't waste population due to rounding
            if (currentPopulation > (5.0 / (universeView.getGrowthRate() - 1))) {
                // Directions
                MovementCommand.Direction[] directions = {
                    MovementCommand.Direction.UP, MovementCommand.Direction.RIGHT,
                    MovementCommand.Direction.DOWN, MovementCommand.Direction.LEFT
                };
                
                // Calculate the num of directions to spread
                int numDirections = directions.length;
                int populationPerDirection = currentPopulation / numDirections;
                
                // Remainder population after distributing equally
                int remainderPopulation = currentPopulation % numDirections;

                // Distribute population in a checkboard pattern
                for (int i = 0; i < numDirections; i++) {
                    int populationToAdd = populationPerDirection;
                    
                    // Distribute remaining population if any
                    if (remainderPopulation > 0) {
                        populationToAdd++;
                        remainderPopulation--;
                    }
                    
                    // Add movement command to the command list
                    commandList.add(new MovementCommand(cell, directions[i], populationToAdd));
                }
            }
        }
    }
}
