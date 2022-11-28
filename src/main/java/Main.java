
import examples.StarterGhostComm.Blinky;
import examples.StarterGhostComm.Inky;
import examples.StarterGhostComm.Pinky;
import examples.StarterGhostComm.Sue;
import examples.StarterPacMan.*;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants.*;
import pacman.game.internal.POType;

import java.util.EnumMap;

/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {

    public static void main(String[] args) {

        MyExecutor executor = new MyExecutor.Builder()
                .setVisual(true)
                .setPacmanPO(false)
                .setTickLimit(10000)
                .setTimeLimit(1)
                .setScaleFactor(2) // Increase game visual size
                .setPOType(POType.RADIUS) // pacman sense objects around it in a radius wide fashion
                .setSightLimit(5000) // The sight radius limit, set to maximum
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        MASController ghosts = new POCommGhosts(50);
        QLearningPacMan player = new QLearningPacMan();

        // executor.runQLearningTraining(player, ghosts, 5000000, false);

        // player.exportQTable();
        // player.reset(0);

        // executor.runGame(player, ghosts, 10); // delay=10; smaller
        // delay for faster gameplay
        executor.runGame(new AStarPacMan(), ghosts, 10);
    }
}
