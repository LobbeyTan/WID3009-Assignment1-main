package examples.StarterPacMan;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import pacman.controllers.PacmanController;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Constants;
import pacman.game.Game;

public class QLearningPacMan extends PacmanController {
    private static final int SEED = 0;
    private static final Random RANDOM = new Random();
    private static final int STATE_SIZE = MOVE.values().length;
    private ArrayList<ArrayList<Double>> qTable = new ArrayList<ArrayList<Double>>();
    private Game game;

    private int pacmanCurrentNodeIndex;
    private MOVE pacmanLastMoveMade;

    // Q-Learning parameters
    private double learningRate = 0.1;
    private double discountRate = 0.9;
    public double explorationRate = 1.0;
    public double totalReward = 0;

    public QLearningPacMan() {
        RANDOM.setSeed(SEED);
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        this.game = game;
        this.pacmanCurrentNodeIndex = game.getPacmanCurrentNodeIndex();
        this.pacmanLastMoveMade = game.getPacmanLastMoveMade();

        this.initializeQTable();

        // Start learning (using epsilon greedy training algorithm)
        double epsilon = RANDOM.nextDouble();

        MOVE action = getAction(pacmanCurrentNodeIndex, epsilon);

        updateLearning(action);

        return action;
    }

    public void updateLearning(MOVE action) {
        double reward = calculateReward(pacmanCurrentNodeIndex, action);

        int newState = game.getNeighbour(pacmanCurrentNodeIndex, action);

        // Update Q-Table
        qTable.get(pacmanCurrentNodeIndex).set(mapMoveToIndex(action),
                getUpdatedValue(pacmanCurrentNodeIndex, action, newState, reward));

        this.totalReward += reward;
    }

    public void reset(double explorationRate) {
        this.explorationRate = explorationRate;
        totalReward = 0;
    }

    public void exportQTable() {
        try {
            FileWriter myWriter = new FileWriter("Q-Table.txt");

            for (ArrayList<Double> row : qTable) {
                for (Double value : row) {
                    myWriter.write(value + " ");
                }
                myWriter.write("\n");
            }
            myWriter.close();
            System.out.println("Successfully export Q-Table.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private double getQValue(int state, MOVE action) {
        return qTable.get(state).get(mapMoveToIndex(action));
    }

    private double computeValueFromQValues(int state) {
        ArrayList<Double> qValues = new ArrayList<Double>();

        for (MOVE action : game.getPossibleMoves(state)) {
            qValues.add(getQValue(state, action));
        }

        return (qValues.isEmpty()) ? 0.0 : Collections.max(qValues);
    }

    private MOVE computeActionFromValues(int state) {
        int bestAction = -1;
        double maxQValue = 0;

        for (MOVE action : game.getPossibleMoves(state)) {
            double qValue = getQValue(state, action);

            if (qValue > maxQValue || bestAction == -1) {
                maxQValue = qValue;
                bestAction = mapMoveToIndex(action);
            }
        }

        return mapIndexToMove(bestAction);
    }

    private MOVE getAction(int state, double epsilon) {
        return (epsilon < explorationRate) ? getRandomMove() : computeActionFromValues(state);
    }

    private double getUpdatedValue(int state, MOVE action, int newState, double reward) {
        double currentValue = getQValue(state, action);

        double updatedValue = reward + (discountRate * computeValueFromQValues(newState));

        return (1 - learningRate) * currentValue + learningRate * updatedValue;
    }

    private double calculateReward(int state, MOVE action) {
        /*
         * Reward Calculation
         * - If eat pill: +1
         * - If eat powerPill: +5
         * - If eat ghost (edible): + 10
         * - If eat ghost (not edible): -10
         */

        double reward = 0;

        int currentNode = game.getNeighbour(state, action);

        int pillIndex = game.getPillIndex(currentNode);
        int powerPillIndex = game.getPowerPillIndex(currentNode);

        if (pillIndex != -1 && game.isPillStillAvailable(pillIndex)) {
            reward += 10;
        }

        if (powerPillIndex != -1 && game.isPowerPillStillAvailable(powerPillIndex)) {
            reward += 50;
        }

        for (GHOST ghost : GHOST.values()) {
            int distance = game.getShortestPathDistance(currentNode,
                    game.getGhostCurrentNodeIndex(ghost));

            if (distance <= Constants.EAT_DISTANCE && distance != -1) {
                if (game.isGhostEdible(ghost)) {
                    reward += 200;
                } else {
                    // reward -= 3;
                    // break;
                }
            }
        }

        return reward - 0.01;
    }

    private void initializeQTable() {
        if (qTable == null || qTable.isEmpty()) {
            for (int i = 0; i < game.getCurrentMaze().graph.length; i++) {
                ArrayList<Double> row = new ArrayList<>();
                for (int j = 0; j < STATE_SIZE; j++) {
                    row.add(0.0);
                }
                qTable.add(row);
            }
        }
    }

    private int mapMoveToIndex(MOVE move) {
        switch (move) {
            case LEFT:
                return 0;
            case DOWN:
                return 1;
            case RIGHT:
                return 2;
            case UP:
                return 3;
            case NEUTRAL:
                return 4;
            default:
                return 4;
        }
    }

    private MOVE mapIndexToMove(int move) {
        switch (move) {
            case 0:
                return MOVE.LEFT;
            case 1:
                return MOVE.DOWN;
            case 2:
                return MOVE.RIGHT;
            case 3:
                return MOVE.UP;
            case 4:
                return MOVE.NEUTRAL;
            default:
                return MOVE.NEUTRAL;
        }
    }

    private MOVE getRandomMove() {
        MOVE[] possibleMoves = game.getPossibleMoves(pacmanCurrentNodeIndex, pacmanLastMoveMade);

        return possibleMoves[RANDOM.nextInt(possibleMoves.length)];
    }
}
