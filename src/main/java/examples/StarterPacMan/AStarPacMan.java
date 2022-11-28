package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.username).
 */
public class AStarPacMan extends PacmanController {
	private static final Random RANDOM = new Random();
	private Game game;
	private int pacmanCurrentNodeIndex;
	MOVE pacmanLastMoveMade;
	int minGhostDistanceBase = 100; // 80, 100
	private Integer target;
	private TARGET_TYPE targerType;
	private boolean targetFound;

	// Using A* algorithms
	public MOVE getMove(Game game, long timeDue) {
		this.game = game;
		pacmanCurrentNodeIndex = game.getPacmanCurrentNodeIndex();
		pacmanLastMoveMade = game.getPacmanLastMoveMade();

		Path bestPath = getPath();
		bestPath.process();
		bestPath.summary(game);

		MOVE bestPathMove = game.getMoveToMakeToReachDirectNeighbour(pacmanCurrentNodeIndex, bestPath.start);

		// No good paths
		if (!targetFound) {
			bestPathMove = pacmanLastMoveMade.opposite();
		}

		return bestPathMove;
	}

	public Path getPath() {
		target = getTarget(false);
		targetFound = false;
		int currentNode = pacmanCurrentNodeIndex;

		List<Segment> targetSegments = new ArrayList<Segment>();

		while (!targetFound) {

			Segment segment = getOrderedSegments(currentNode).get(0);

			currentNode = segment.start;

			if (isSafe(currentNode, segment)) {
				if (!targetSegments.isEmpty()) {
					Segment lastSegment = targetSegments.get(targetSegments.size() - 1);

					lastSegment.end = segment.start;
					segment.parent = lastSegment;
				}

				if (currentNode == target) {
					targetFound = true;
					segment.end = segment.start;
				}

				targetSegments.add(segment);
			} else {
				target = getTarget(true);
				targetSegments = new ArrayList<Segment>();
				currentNode = pacmanCurrentNodeIndex;
			}

		}

		return new Path(targetSegments);
	}

	private List<Segment> getOrderedSegments(int state) {
		List<Segment> segments = new ArrayList<Segment>();

		for (MOVE move : game.getPossibleMoves(state)) {
			int newState = game.getNeighbour(state, move);

			Segment s = new Segment();
			s.heuristicScore = game.getShortestPathDistance(newState, target);
			s.start = newState;
			s.direction = move;

			segments.add(s);
		}

		Collections.sort(segments, new SegmentHeuristicComparator());

		return segments;
	}

	private MOVE getRandomMove() {
		MOVE[] possibleMoves = game.getPossibleMoves(pacmanCurrentNodeIndex, pacmanLastMoveMade);

		return possibleMoves[RANDOM.nextInt(possibleMoves.length)];
	}

	private boolean isSafe(int currentNode, Segment currentSegment) {
		List<Integer> ghostNodeIndices = new ArrayList<>();
		GHOST[] ghosts = GHOST.values();
		for (GHOST ghost : ghosts)
			ghostNodeIndices.add(game.getGhostCurrentNodeIndex(ghost));

		if (ghostNodeIndices.contains(currentNode)) {
			for (GHOST ghost : ghosts) {
				if (game.getGhostCurrentNodeIndex(ghost) == currentNode) {
					if (!game.isGhostEdible(ghost)
							&& game.getGhostLastMoveMade(ghost) == currentSegment.direction.opposite()
							&& game.getEuclideanDistance(pacmanCurrentNodeIndex,
									currentNode) <= minGhostDistanceBase) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private Integer getTarget(boolean random) {
		Integer target = null;

		if (random) {
			target = game.getActivePillsIndices()[RANDOM.nextInt(game.getActivePillsIndices().length)];
			targerType = TARGET_TYPE.PILL;
		}

		// Get all ghosts node index in a list
		int minGhostDistance = Integer.MAX_VALUE;
		List<Integer> ghostNodeIndices = new ArrayList<>();
		GHOST[] ghosts = GHOST.values();
		if (target == null) {
			for (GHOST ghost : ghosts) {
				int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
				int ghostDistance = game.getShortestPathDistance(ghostIndex, pacmanCurrentNodeIndex);

				ghostNodeIndices.add(ghostIndex);

				if (game.isGhostEdible(ghost) && ghostDistance != -1 && ghostDistance < minGhostDistance) {
					target = ghostIndex;
					targerType = TARGET_TYPE.GHOST;
					minGhostDistance = ghostDistance;
				}
			}
		}

		// No edible ghost
		int minPowerPillDistance = Integer.MAX_VALUE;
		if (target == null) {
			for (int powerPillIndex : game.getActivePowerPillsIndices()) {
				int powerPillDistance = game.getShortestPathDistance(powerPillIndex, pacmanCurrentNodeIndex);
				if (powerPillDistance != -1 && powerPillDistance < minPowerPillDistance) {
					target = powerPillIndex;
					targerType = TARGET_TYPE.POWER_PILL;
					minPowerPillDistance = powerPillDistance;
				}
			}
		}

		// No edible ghost and powerpill
		int minPillDistance = Integer.MAX_VALUE;
		if (target == null) {
			for (int pillIndex : game.getActivePillsIndices()) {
				int pillDistance = game.getShortestPathDistance(pillIndex, pacmanCurrentNodeIndex);

				if (pillDistance != -1 && pillDistance < minPillDistance) {
					target = pillDistance;
					targerType = TARGET_TYPE.PILL;
					minPillDistance = pillDistance;
				}
			}
		}

		System.out.println("Current target is " + targerType + " at position: " + target);

		return target;
	}

	public class PathValueComparator implements Comparator<Path> {
		@Override
		public int compare(Path path1, Path path2) {
			return path2.value - path1.value;
		}
	}

	public class SegmentHeuristicComparator implements Comparator<Segment> {
		@Override
		public int compare(Segment seg1, Segment seg2) {
			return seg1.heuristicScore - seg2.heuristicScore;
		}
	}

	public class Path {
		public int start;
		public int end;
		public List<GHOST> ghosts = new ArrayList<GHOST>();
		public int powerPillsCount = 0;
		public int pillsCount = 0;
		public List<Segment> segments = new ArrayList<Segment>();
		public int length;
		public String description = "";
		public boolean safe = true;
		public int value = 0;

		// Important: Segments must be in sequence
		Path(List<Segment> segments) {
			this.segments = segments;
		}

		public void render(Game game) {
			for (Segment segment : segments)
				GameView.addLines(game, segment.color, segment.start, segment.end);
		}

		public void summary(Game game) {
			// String ghostsName = "";
			// for (GHOST ghost : ghosts)
			// ghostsName += ghost.name() + " ";

			// String text = description + "::" + " value:" + value + ", safe:" + (safe ?
			// "safe" : "unsafe") + ", pills:"
			// + pillsCount + ", power pills:" + powerPillsCount + ", ghost:" + ghostsName;

			// if (!safe)
			// System.err.println(text);
			// else
			// System.out.println(text);

			render(game);
		}

		public void process() {
			int segmentsCount = segments.size();

			if (segmentsCount > 0) {
				Segment firstSegment = segments.get(0);
				Segment lastSegment = segments.get(segmentsCount - 1);
				start = firstSegment.start;
				end = lastSegment.end;
				length = lastSegment.lengthSoFar;
				pillsCount = lastSegment.pillsCount;
				value = pillsCount;
				powerPillsCount = lastSegment.powerPillsCount;
				int unsafeSegmentsCount = 0;

				for (Segment segment : segments) {
					if (!segment.ghosts.isEmpty()) {
						ghosts.addAll(segment.ghosts);
						for (GHOST ghost : ghosts)
							if (game.isGhostEdible(ghost)) {
								int distance = game.getShortestPathDistance(pacmanCurrentNodeIndex,
										game.getGhostCurrentNodeIndex(ghost));
								if (distance < 10)
									value += 1;// 15;
								else
									value += 1;// 10;
							}
					}

					if (segment.parent != null && !segment.parent.safe)
						segment.safe = segment.parent.safe;

					if (!segment.safe) {
						unsafeSegmentsCount++;
						value -= 10;
						segment.color = Color.RED;
					}

					value += segment.powerPillsCount * 5;

					description += segment.direction.toString() + " ";
				}

				if (unsafeSegmentsCount > 0)
					safe = false;
			}
		}
	}

	public class Segment {
		public int start;
		public int end;
		public int pillsCount = 0;
		public int powerPillsCount = 0;
		public int heuristicScore = Integer.MAX_VALUE;
		public int lengthSoFar;
		public MOVE direction;
		public Segment parent;
		public List<GHOST> ghosts = new ArrayList<>();
		public Color color = Color.GREEN;
		public boolean safe = true;
	}

	public enum TARGET_TYPE {
		GHOST,
		PILL,
		POWER_PILL,
	}

}