package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.List;

public class Bellator_Small implements PlayerBot {

    @Override
    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();
        int growthRate = (int) universeView.getGrowthRate();
        int maxPopulation = universeView.getMaximumPopulation();

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);

            // Leave a certain population in the cell for growth
            int populationToLeave = Math.min(currentPopulation, maxPopulation / (growthRate + 1));
            int populationToSpread = currentPopulation - populationToLeave;

            // Spread population evenly to neighboring cells
            MovementCommand.Direction[] directions = MovementCommand.Direction.values();
            int populationPerDirection = populationToSpread / directions.length;
            int remainderPopulation = populationToSpread % directions.length;

            for (MovementCommand.Direction direction : directions) {
                int populationToMove = populationPerDirection;
                if (remainderPopulation > 0) {
                    populationToMove++;
                    remainderPopulation--;
                }

                if (populationToMove > 0) {
                    commandList.add(new MovementCommand(cell, direction, populationToMove));
                }
            }
        }
    }
}