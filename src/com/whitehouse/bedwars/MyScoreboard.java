package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class MyScoreboard {

    private final BedWars plugin;
    private final Scoreboard globalSidebarScoreboard;
    private final Objective obj;
    private final ArrayList<Team> lines = new ArrayList<>();

    public MyScoreboard(BedWars plugin){
        this.plugin = plugin;
        ScoreboardManager manager = Objects.requireNonNull(Bukkit.getScoreboardManager());
        globalSidebarScoreboard = manager.getNewScoreboard();
        obj = globalSidebarScoreboard.registerNewObjective("myscoreboard", "dummy", Objects.requireNonNull(plugin.getConfig().getString("main.scoreboardLobbyName")));
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
        Team rightTeam = Objects.requireNonNull(this.globalSidebarScoreboard.getTeam("team"+team));
        rightTeam.setColor(plugin.getPlayerUtilsInstance().getColorOfNthTeam(team));
        rightTeam.setAllowFriendlyFire(false);
        rightTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OWN_TEAM);
        //rightTeam.setPrefix(plugin.getMenuInstance().getColorOfNthTeam(team)+" "); //pokud by se chtel davat i prefix
        rightTeam.addEntry(player.getName());
    }

    public void removePlayerFromAllTeams(Player player){
        Set<Team> teams = this.globalSidebarScoreboard.getTeams();
        for(Team team : teams){
            if(team.hasEntry(player.getName())){
                team.removeEntry(player.getName());
            }
        }
    }

    public void addPlayerToSpectatorTeam(Player player){
        if(this.globalSidebarScoreboard.getTeam("teamSpec") == null){
            try {
                this.globalSidebarScoreboard.registerNewTeam("teamSpec");
            }catch(Exception e){/*divna vec, ale ok*/}
        }
        Team rightTeam = this.globalSidebarScoreboard.getTeam("teamSpec");
        Objects.requireNonNull(rightTeam).setColor(ChatColor.GRAY);
        rightTeam.setPrefix("§8§lSPEC §o");
        rightTeam.addEntry(player.getName());
    }

}
