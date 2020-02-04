package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;

public class MyScoreboard {

    private final BedWars plugin;
    private Scoreboard globalScoreboard;
    private Objective obj;
    private ArrayList<Team> lines = new ArrayList<Team>();

    public MyScoreboard(BedWars plugin){
        this.plugin = plugin;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        globalScoreboard = manager.getNewScoreboard();
        obj = globalScoreboard.registerNewObjective("dummy", "myscoreboard", plugin.getConfig().getString("main.scoreboardLobbyName"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for(int i=0; i<9; i++){
            Score thisLine = obj.getScore("§"+i);
            thisLine.setScore(9-i);
            Team newTeam = globalScoreboard.registerNewTeam("line"+(i+1));
            newTeam.addEntry("§"+i);
            this.lines.add(newTeam);
        }
    }

    public Scoreboard getGlobalScoreboard(){
        return this.globalScoreboard;
    }

    public void setLine(int i, String prefix, String suffix){
        Team t = this.lines.get(i);
        t.setPrefix(prefix);
        t.setSuffix(suffix);
    }

}
