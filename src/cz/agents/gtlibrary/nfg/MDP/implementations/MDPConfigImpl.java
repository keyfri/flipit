/*
Copyright 2014 Faculty of Electrical Engineering at CTU in Prague

This file is part of Game Theoretic Library.

Game Theoretic Library is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Game Theoretic Library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Game Theoretic Library.  If not, see <http://www.gnu.org/licenses/>.*/


package cz.agents.gtlibrary.nfg.MDP.implementations;

import cz.agents.gtlibrary.interfaces.Player;
import cz.agents.gtlibrary.nfg.MDP.interfaces.MDPAction;
import cz.agents.gtlibrary.nfg.MDP.interfaces.MDPConfig;
import cz.agents.gtlibrary.nfg.MDP.interfaces.MDPState;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: bosansky
 * Date: 6/27/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MDPConfigImpl implements MDPConfig {

    protected List<Player> allPlayers;
    public abstract double getUtility(MDPStateActionMarginal firstPlayerAction, MDPStateActionMarginal secondPlayerAction);

    public Player getOtherPlayer(Player player) {
        return allPlayers.get(0).equals(player) ? allPlayers.get(1) : allPlayers.get(0);
    }

    public List<Player> getAllPlayers() {
        return allPlayers;
    }

    public static double getEpsilon() {
        return 0.00001;
    }

    public abstract double getBestUtilityValue(Player player);
}
