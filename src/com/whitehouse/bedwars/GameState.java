package com.whitehouse.bedwars;

public enum GameState {
    LOBBY,
    STARTING,
    FULL_STARTING,
    INGAME,
    RESTARTING,
    SETUP;

    public boolean isJoinable(){
        switch(this){
            case LOBBY:
            case STARTING:
                return true;
        }
        return false;
    }

    public boolean isInvincible(){
        switch(this){
            case LOBBY:
            case STARTING:
            case FULL_STARTING:
            case RESTARTING:
                return true;
        }
        return false;
    }

    public boolean isTeamSelectable(){
        switch(this){
            case LOBBY:
            case STARTING:
            case FULL_STARTING:
                return true;
        }
        return false;
    }

}
