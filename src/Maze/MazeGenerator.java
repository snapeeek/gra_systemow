package Maze;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class MazeGenerator
{
    int length, width;
    Cell[][] cells;
    public static final int CELL_WIDTH = 10;
    public static final int CELL_HEIGTH = 15;
    private static final int HORIZONTAL = 1;
    private static final int VERTICAL = 2;
    private Random rand = new Random();

    public MazeGenerator(int x, int y)
    {
        this.width = x;
        this.length = y;

        cells = new Cell[x][y];
        for (int i = 0; i < x; i++)
        {
            for (int j = 0; j < y; j++)
            {
                cells[i][j] = new Cell(Cell.Type.WALL, Cell.Ocup.NOTHING, i*CELL_WIDTH, j*CELL_HEIGTH);
            }
        }
        //Random random = new Random();
        divide(cells, 0, 0, length, width, chooseOrientation(length, width));

        int bushCount = 0;
        while (bushCount < 50)
        {
            int i = rand.nextInt(60);
            int j = rand.nextInt(30);

            if (cells[i][j].getType() == Cell.Type.PATH && cells[i][j].getOcup() == Cell.Ocup.NOTHING)
            {
                cells[i][j].setType(Cell.Type.BUSHES);
                bushCount++;
            }
        }

        int i, j;
        do
        {
            i = rand.nextInt(60);
            j = rand.nextInt(30);
        } while (cells[i][j].getType() != Cell.Type.PATH || cells[i][j].getOcup() != Cell.Ocup.NOTHING);
        cells[i][j].setOcup(Cell.Ocup.CAMP);


    }

    public Cell[][] getCells()
    {
        return cells;
    }




    private void divide(Cell[][] grid, int x, int y, int width, int height, int orientation)
    {
        if (width < 3 || height < 3)
        {
            return;
        }
        boolean horizontal = (orientation == HORIZONTAL);

        int wx, wy;
        if (horizontal)
        {
            wx = x;
            if (height > 2)
                wy = y + rand.nextInt(height - 2);
            else
                wy = y;
        }
        else
        {
            if (width > 2)
                wx = x + rand.nextInt(width - 2);
            else
                wx = x;
            wy = y;
        }
        /*int wx = x + (horizontal ? 0 : rand.nextInt(width - 2));
        int wy = y + (horizontal ? rand.nextInt(height - 2) : 0);*/

        int px = wx + (horizontal ? rand.nextInt(width) : 0);
        int py = wy + (horizontal ? 0 : rand.nextInt(height));

        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;

        int length = horizontal ? width : height;

        //int dir = horizontal ? S : E;

        for (int i = 0; i < length; i++)
        {
            if (wx != px || wy != py)
            {
                grid[wy][wx].setType(Cell.Type.PATH);
            }
            wx += dx;
            wy += dy;
        }

        int nx = x;
        int ny = y;
        int w = horizontal ? width : wx - x + 1;
        int h = horizontal ? wy - y + 1 : height;
        divide(grid, nx, ny, w, h, chooseOrientation(w, h));

        nx = horizontal ? x : wx + 1;
        ny = horizontal ? wy + 1 : y;
        w = horizontal ? width : x + width - wx - 1;
        h = horizontal ? y + height - wy - 1 : height;
        divide(grid, nx, ny, w, h, chooseOrientation(w, h));
    }

    private int chooseOrientation(int w, int h)
    {
        if (w < h)
        {
            return HORIZONTAL;
        }
        else if (h < w)
        {
            return VERTICAL;
        }
        else
        {
            return rand.nextInt(2) + 1;
        }
    }
}
