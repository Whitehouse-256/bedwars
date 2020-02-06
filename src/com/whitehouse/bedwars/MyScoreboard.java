package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;

public class MyScoreboard {

    private final BedWars plugin;
    private final Scoreboard globalSidebarScoreboard;
    private final Objective obj;
    private final ArrayList<Team> lines = new ArrayList<Team>();
    private Objective teamColorsObj = null;

    public MyScoreboard(BedWars plugin){
        this.plugin = plugin;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        globalSidebarScoreboard = manager.getNewScoreboard();
        obj = globalSidebarScoreboard.registerNewObjective("myscoreboard", "dummy", plugin.getConfig().getString("main.scoreboardLobbyName"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for(int i=0; i<9; i++){
            Score thisLine = obj.getScore("§"+i);
            thisLine.setScore(9-i);
            Team newTeam = globalSidebarScoreboard.registerNewTeam("line"+(i+1));
            newTeam.addEntry("§"+i);
            this.lines.add(newTeam);
        }
    }

    public Scoreboard getGlobalSidebarScoreboard(){
        return this.globalSidebarScoreboard;
    }

    //Sidebar

    public void setLine(int i, String prefix, String suffix){
        Team t = this.lines.get(i);
        t.setPrefix(prefix);
        t.setSuffix(suffix);
    }

    public void setLineCount(int count){
        for(int i=0; i<9; i++){
            Score thisLine = obj.getScore("§"+i);
            if(i<count) thisLine.setScore(9-i);
            else globalSidebarScoreboard.resetScores("§"+i);
        }
    }

    //Barvy hracu

    public void addPlayerToTeam(int team, Player player){
        if(this.globalSidebarScoreboard.getTeam("team" + team) == null){
            try {
                this.globalSidebarScoreboard.registerNewTeam("team" + team);
            }catch(Exception e){/*divna vec, ale ok*/}
        }
        Team rightTeam = this.globalSidebarScoreboard.getTeam("team"+team);
        rightTeam.setColor(plugin.getPlayerUtilsInstance().getColorOfNthTeam(team));
        //rightTeam.setPrefix(plugin.getMenuInstance().getColorOfNthTeam(team)+" "); //pokud by se chtel davat i prefix
        rightTeam.addEntry(player.getName());
    }

}
