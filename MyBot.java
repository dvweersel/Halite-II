import hlt.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class MyBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("MyBot");

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                /* If the ship is docked, we leave it */
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }

                /* Get the planets by distance */
                Map<Double, Planet> planetsByDistance = gameMap.nearbyPlanetsByDistance(ship);
                Boolean allPlanetsOwned = Boolean.TRUE;

                /* Fill boolean */
                for (final Planet planet : planetsByDistance.values()) {
                    if (planet.isOwned()) {
                        continue;
                    }
                    DebugLog.addLog("Not all planets are owned. Continuing macro");
                    allPlanetsOwned = Boolean.FALSE;
                    break;
                }

                if(allPlanetsOwned) {
                    for (final Planet planet : planetsByDistance.values()) {
                        if (planet.getOwner() == gameMap.getMyPlayerId()) {
                            continue;
                        }
                    }
                }

                /* Find new resources */
                for (final Planet planet : planetsByDistance.values()) {
                    /* My planet */
                    if (planet.getOwner() == gameMap.getMyPlayerId()) {
                        DebugLog.addLog(String.format("Docked ships is %d", planet.getDockedShips().size()));
                        /* TODO make the production cap dependent on the gamestate */
                        if (planet.getDockedShips().size() < 3){
                            if (ship.canDock(planet)){
                                DebugLog.addLog("Docking owned planet");
                                moveList.add(new DockMove(ship, planet));
                                break;
                            }
                            else{
                                final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);

                                if (newThrustMove != null) {
                                    DebugLog.addLog("Moving to owned planet");
                                    moveList.add(newThrustMove);

                                    break;
                                }
                                DebugLog.addLog("Thrustmove is null");
                            }

                        }
                        continue;
                    }

                    if (planet.isOwned() && !allPlanetsOwned) {
                        continue;
                    }

                    /* If we can dock, we dock */
                    if (ship.canDock(planet)) {
                        DebugLog.addLog("Planet not owned - docking");
                        moveList.add(new DockMove(ship, planet));
                        break;
                    }

                    final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);

                        break;
                    }

                    break;
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}
