import hlt.*;

import java.util.ArrayList;
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

                /* Find new resources */
                for (Map.Entry<Double, Entity> entityEntry : gameMap.nearbyEntitiesByDistance(ship).entrySet()) {

                    if (entityEntry.getValue() instanceof Ship) {
                        Ship otherShip = (Ship) entityEntry.getValue();
                        if (otherShip.getOwner() != gameMap.getMyPlayerId()) {

                            if (entityEntry.getKey() < 5) {
                                break;
                            } else if(entityEntry.getKey() > Constants.MAX_SPEED) {
                                int thrust = Constants.MAX_SPEED;
                            } else {
                                int thrust = (int) entityEntry.getKey().intValue()/2;
                            }

                            final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, otherShip.getPosition(), Constants.MAX_SPEED, Boolean.TRUE, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0);

                            if (newThrustMove != null) {
                                DebugLog.addLog("==Moving to dock to owned planet");
                                moveList.add(newThrustMove);

                                break;
                            }
                            DebugLog.addLog("==Thrustmove is null");
                        }

                    }

                    if (entityEntry.getValue() instanceof Planet) {
                        DebugLog.addLog("=Planet=");
                        Planet planet = (Planet) entityEntry.getValue();

                        if (!planet.isOwned()) {
                            DebugLog.addLog("==Unowned Planet==");
                            if (ship.canDock(planet)) {
                                DebugLog.addLog("=Planet not owned - docking");
                                moveList.add(new DockMove(ship, planet));
                                break;
                            }

                            final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                            if (newThrustMove != null) {
                                DebugLog.addLog("=Thrusting");
                                moveList.add(newThrustMove);

                                break;
                            }

                        }

                        if (planet.getOwner() == gameMap.getMyPlayerId()) {
                            DebugLog.addLog("==My Planet==");
                            DebugLog.addLog(String.format("==Docked ships: %d Docking spots %d", planet.getDockedShips().size(), planet.getDockingSpots()));
                            if (planet.getDockedShips().size() < planet.getDockingSpots()) {
                                if (ship.canDock(planet)) {
                                    DebugLog.addLog("==Docking owned planet");
                                    moveList.add(new DockMove(ship, planet));
                                    break;
                                } else {
                                    final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);

                                    if (newThrustMove != null) {
                                        DebugLog.addLog("==Moving to dock to owned planet");
                                        moveList.add(newThrustMove);

                                        break;
                                    }
                                    DebugLog.addLog("==Thrustmove is null");
                                }
                            }
                            DebugLog.addLog("==Not docking to owned planet");
                            continue;
                        }

                        continue;
                    }
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}
