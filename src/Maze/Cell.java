package Maze;

import java.awt.Point;
import java.io.Serializable;

public class Cell implements Serializable
{
    public enum Type
    {
        PATH,
        WALL,
        BUSHES,
        UNSEEN
    }

    public enum Ocup
    {
        PLAYER,
        BEAST,
        COIN,
        TREAS,
        BIGT,
        DEAD,
        NOTHING
    }

    Type type;
    Ocup ocup;
    public int x,y;
    int playerNum;
    int coins = 0;
    boolean isCamp = false;
    boolean isMarked = false;

    public Cell(Type type, Ocup ocup, int x, int y)
    {
        this.type = type;
        this.ocup = ocup;
        this.x = x;
        this.y = y;
    }

    public Cell(Type type, Ocup ocup, int x, int y, int playerNum)
    {
        this.type = type;
        this.ocup = ocup;
        this.x = x;
        this.y = y;
        this.playerNum = playerNum;
    }

    public void makeCamp()
    {
        isCamp = true;
    }

    public boolean isCamp()
    {
        return isCamp;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public Ocup getOcup()
    {
        return ocup;
    }

    public void setOcup(Ocup ocup)
    {
        this.ocup = ocup;
    }

    public Point getCord()
    {
        return new Point(this.x, this.y);
    }

    public void setPlayerNum(int x)
    {
        this.playerNum = x;
    }

    public int getPlayerNum()
    {
        if (ocup == Ocup.PLAYER)
            return playerNum;
        else return -1;
    }

    public void setCoins(int coins)
    {
        this.coins = coins;
    }

    public int getCoins()
    {
        return coins;
    }

    public boolean isMarked()
    {
        return isMarked;
    }

    public void makeMarked()
    {
        isMarked = true;
    }
}
