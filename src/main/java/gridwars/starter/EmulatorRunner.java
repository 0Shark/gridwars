package gridwars.starter;

import cern.ais.gridwars.Emulator;


/**
 * Instantiates the example bots and starts the game emulator.
 */
public class EmulatorRunner {

    public static void main(String[] args) {
        Bellator_Skanderbot blueBot = new Bellator_Skanderbot();
        Bellator_Alexander redBot = new Bellator_Alexander();

        Emulator.playMatch(blueBot, redBot);
    }
}
