package cz.agents.gtlibrary.algorithms.cfr.ir.testdomains.alossgame;

import cz.agents.gtlibrary.iinodes.PlayerImpl;
import cz.agents.gtlibrary.interfaces.GameInfo;
import cz.agents.gtlibrary.interfaces.Player;

public class ALossGameInfo implements GameInfo {

    public static final Player FIRST_PLAYER = new PlayerImpl(0);
    public static final Player SECOND_PLAYER = new PlayerImpl(1);
    public static final Player[] ALL_PLAYERS = new Player[]{FIRST_PLAYER, SECOND_PLAYER};

    @Override
    public double getMaxUtility() {
        return 6;
    }

    @Override
    public Player getFirstPlayerToMove() {
        return FIRST_PLAYER;
    }

    @Override
    public Player getOpponent(Player player) {
        return player.equals(FIRST_PLAYER) ? SECOND_PLAYER : FIRST_PLAYER;
    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public int getMaxDepth() {
        return 3;
    }

    @Override
    public Player[] getAllPlayers() {
        return new Player[0];
    }

    @Override
    public double getUtilityStabilizer() {
        return 0;
    }
}
