package com.whitehouse.bedwars;

public enum GameState {
    LOBBY,
    STARTING,
    FULL_STARTING,
    INGAME,
    RESTARTING;

    public boolean isJoinable(){
        switch(this){
            case LOBBY:
            case STARTING:
                return true;
        }
        return false;
    }

}
