package examples.StarterPacMan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import pacman.controllers.PacmanController;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class QLearningPacMan extends PacmanController {
    private static final Random RANDOM = new Random();
    private static final int STATE_SIZE = 4;
    private ArrayList<ArrayList<Double>> qTable = new ArrayList<ArrayList<Double>>();
    private Game game;

    private int pacmanCurrentNodeIndex;
    private MOVE pacmanLastMoveMade;

    // Q-Learning parameters
    private double learningRate = 0.1;
    private double discountRate = 0.99;
    public double explorationRate = 1.0;
    public double totalReward = 0;

    @Override
    public MOVE getMove(Game game, long timeDue) {
        this.game = game;
        this.pacmanCurrentNodeIndex = game.getPacmanCurrentNodeIndex();
        this.pacmanLastMoveMade = game.getPacmanLastMoveMade();

        this.initializeQTable();

        // Start learning (using epsilon greedy training algorithm)
        double epsilon = RANDOM.nextDouble();

        MOVE action = getBestAction(epsilon);

        double reward = calculateReward(action);

        // Update Q-Table
        qTable.get(pacmanCurrentNodeIndex).set(mapMoveToIndex(action),
                getUpdatedValue(pacmanCurrentNodeIndex, action, reward));

        this.totalReward += reward;

        return action;
    }

    public void reset(double explorationRate) {
        this.explorationRate = explorationRate;
        totalReward = 0;
    }

    private MOVE getBestAction(double epsilon) {
        MOVE action = (epsilon > explorationRate) ? bestMoveFromState(qTable.get(pacmanCurrentNodeIndex))
                : getRandomMove();

        if (game.getNeighbour(pacmanCurrentNodeIndex, action) == -1) {
            action = getRandomMove();
        }

        return action;
    }

    private double getUpdatedValue(int state, MOVE action, double reward) {
        double currentReward = qTable.get(state).get(mapMoveToIndex(action));
        ArrayList<Double> nextStateRewards = qTable.get(game.getNeighbour(state, action));

        return (1 - learningRate) * currentReward
                + learningRate * (reward + (discountRate * Collections.max(nextStateRewards)));
    }

    private double calculateReward(MOVE action) {
        /*
         * Reward Calculation
         * - If eat pill: +5
         * - If eat powerPill: +5
         * - If eat ghost (edible): + 10
         * - If eat ghost (not edible): -10
         * - Distance between ghost: 1 / D
         */

        double reward = 0;

        int currentNode = game.getNeighbour(pacmanCurrentNodeIndex, action);

        int pillIndex = game.getPillIndex(currentNode);
        int powerPillIndex = game.getPowerPillIndex(currentNode);

        if ((pillIndex != -1 && game.isPillStillAvailable(pillIndex))
                || (powerPillIndex != -1 && game.isPowerPillStillAvailable(powerPillIndex))) {
            reward += 5;
        }

        // Get all ghosts node index in a list
        List<Integer> ghostNodeIndices = new ArrayList<>();
        GHOST[] ghosts = GHOST.values();
        for (GHOST ghost : ghosts) {
            ghostNodeIndices.add(game.getGhostCurrentNodeIndex(ghost));
        }

        for (GHOST ghost : ghosts) {
            if (game.getGhostCurrentNodeIndex(ghost) == currentNode) {
                reward += game.isGhostEdible(ghost) ? 10 : -10;
            } else {
                reward -= 1 / (game.getEuclideanDistance(pacmanCurrentNodeIndex, currentNode) * 4);
            }
        }

        return reward;
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
            default:
                return RANDOM.nextInt(STATE_SIZE);
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
            default:
                return MOVE.NEUTRAL;
        }
    }

    private MOVE bestMoveFromState(ArrayList<Double> scores) {
        int bestMove = 0;
        double maxScore = 0;

        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i) > maxScore) {
                maxScore = scores.get(i);
                bestMove = i;
            }
        }

        return mapIndexToMove(bestMove);
    }

    private MOVE getRandomMove() {
        MOVE[] possibleMoves = game.getPossibleMoves(pacmanCurrentNodeIndex, pacmanLastMoveMade);

        return possibleMoves[RANDOM.nextInt(possibleMoves.length)];
    }
}
