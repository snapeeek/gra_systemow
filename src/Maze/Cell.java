package Maze;

import java.awt.Point;

public class Cell
{
    public enum Type
    {
        PATH,
        WALL,
        BUSHES
    }

    public enum Ocup
    {
        PLAYER,
        BEAST,
        COIN,
        TREAS,
        BIGT,
        NOTHING
    }

    public Type type;
    public Ocup ocup;
    public int x,y;

    public Cell(Type type, Ocup ocup, int x, int y)
    {
        this.type = type;
        this.ocup = ocup;
        this.x = x;
        this.y = y;
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
}
