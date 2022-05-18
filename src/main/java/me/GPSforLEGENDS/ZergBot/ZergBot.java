package me.GPSforLEGENDS.ZergBot;

import SC2APIProtocol.Raw;
import SC2APIProtocol.Sc2Api;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.game.raw.StartRaw;
import com.github.ocraft.s2client.protocol.response.ResponseGameInfo;
import com.github.ocraft.s2client.protocol.spatial.Point;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;

import java.util.*;
import java.util.function.Predicate;

public class ZergBot extends S2Agent {

    private int overlordsBeeingTrained = 0;

    long start = 0;
    boolean stop = false;
    int counter = 0;

    List<Point> expansionPoints;

    @Override
    public void onGameStart(){

        expansionPoints = query().calculateExpansionLocations(observation());

        expansionPoints.sort(Comparator.comparing(point -> {
            Point2d startLoc = observation().getStartLocation().toPoint2d();
            return query().pathingDistance(startLoc, point.toPoint2d());
        }));

        System.out.println("Hello World");
    }

    @Override
    public void onStep(){
        trainDrone();
        System.out.println(observation().getScore());
    }

    @Override
    public void onUnitCreated(UnitInPool unitInPool){
        if(unitInPool.unit().getType() == Units.ZERG_OVERLORD){
            overlordsBeeingTrained--;
        }
    }

    @Override
    public void onUnitIdle(UnitInPool unitInPool){
        /*Unit unit = unitInPool.unit();

        if(observation().getMinerals() >= 50) {
            switch ((Units) unit.getType()) {
                case ZERG_LARVA: {
                    if (observation().getFoodUsed() > observation().getFoodCap() - 2) {
                        tryTrainOverlord(unit);
                    }
                    actions().unitCommand(unit, Abilities.TRAIN_DRONE, false);
                    break;
                }
            }
        }*/
    }

    private boolean tryTrainOverlord(Unit unit) {

        if(overlordsBeeingTrained > 0){
            return false;
        }
        overlordsBeeingTrained++;
        actions().unitCommand(unit, Abilities.TRAIN_OVERLORD, false);
        return true;
    }


    private void trainDrone(){
        List<UnitInPool> larvas = observation().getUnits(Alliance.SELF, unitInPool ->
                unitInPool.unit().getType() == Units.ZERG_LARVA
        );

        if(!larvas.isEmpty()){
            Unit larva = larvas.get(0).unit();

            actions().unitCommand(larva, Abilities.TRAIN_DRONE, false);
        }
    }

    private void buildArmy(){
        List<UnitInPool> larvas = observation().getUnits(Alliance.SELF, unitInPool ->
                unitInPool.unit().getType() == Units.ZERG_LARVA
        );

        if(!larvas.isEmpty()){
            Unit larva = larvas.get(0).unit();

            actions().unitCommand(larva, Abilities.TRAIN_ZERGLING, false);
        }
    }

    private void attackEnemy(){
        List<Unit> army = getAllArmy();
        Optional<Point2d> possibleEnemyPos = findEnemyPosition();

        if(possibleEnemyPos.isPresent()) {
            actions().unitCommand(army, Abilities.ATTACK_ATTACK, possibleEnemyPos.get(), false);
        }
    }

    private void retreatArmy(){
        List<Unit> army = getAllArmy();
        Point2d basePos = observation().getStartLocation().toPoint2d();

        actions().unitCommand(army, Abilities.MOVE, basePos, false);
    }

    private void doNothing(){

    }

    private void buildExpansion(){
        //TODO
    }

    private Unit getRandomMiningDrone(){
        List<UnitInPool> miningDrones = observation().getUnits(Alliance.SELF, unitInPool -> {
            //TODO figure out how to get a mining drone
            return unitInPool.unit().getType() == Units.ZERG_DRONE;
        });

        if(!miningDrones.isEmpty()){
            return miningDrones.get(0).unit();
        }

        return null;
    }

    private int startCounter = -1;
    private Optional<Point2d> findEnemyPosition(){
        ResponseGameInfo gameInfo = observation().getGameInfo();

        Optional<StartRaw> startRaw = gameInfo.getStartRaw();
        if(startRaw.isPresent()){
            Set<Point2d> startLocations = new HashSet<>(startRaw.get().getStartLocations());
            startLocations.remove(observation().getStartLocation().toPoint2d());
            if(startLocations.isEmpty()) return Optional.empty();
            startCounter++;
            if(startCounter < startLocations.size()) {
                return Optional.of((new ArrayList<>(startLocations).get(startCounter)));
            }
        }
        return Optional.empty();
    }

    private List<Unit> getAllArmy(){
        List<UnitInPool> armyUnitsInPool = observation().getUnits(Alliance.SELF, unitInPool -> {
            UnitType type = unitInPool.unit().getType();
            return type == Units.ZERG_ZERGLING;
        });

        List<Unit> armyUnits = new ArrayList<>();

        for(UnitInPool uip : armyUnitsInPool){
            armyUnits.add(uip.unit());
        }

        return armyUnits;
    }


}
