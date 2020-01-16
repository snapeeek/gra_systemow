import javax.swing.*;
import java.awt.*;

//TODO okienko obok panelu rysującego z danymi graczy
//TODO wyświetlanie graczy, obozowiska i bestii
//FIXME sprawdzenie czy klasa będzie działać dla gracza

public class Graphics extends JFrame
{
    JTextArea textArea;
    Board board;
    Cell[][] cells;
    final int DIM = 800;
    public static final int CELL_WIDTH = 10;
    public static final int CELL_HEIGHT = 15;

    Graphics(String name, Cell[][] cells)
    {
        super(name);
        this.cells = cells;
        //this.setLayout(new BorderLayout());
        /*textArea = new JTextArea();
        textArea.setSize(DIM /2,DIM/2);
        textArea.setEditable(false);
        textArea.setText("Witam serdecznie co sie dzieje\nskad to zwatpienie byku\npamietaj zeby ciagle isc do przodu, bo nikt za Ciebie tego nie zrobi B)");
        this.add(textArea, BorderLayout.LINE_END);*/
        board = new Board();
        //board.setSize(DIM/2,DIM/2);
        this.add(board/*, BorderLayout.LINE_START*/);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(DIM, DIM);
        //this.pack();
        this.setResizable(false);
        this.setVisible(true);
    }

    void setArray(Cell[][] temp)
    {
        this.cells = temp;
    }

    class Board extends JPanel
    {
        Board()
        {
            setBorder(BorderFactory.createLineBorder(Color.RED));
            setSize(DIM/2, DIM/2);
            Dimension dim = new Dimension();
            dim.setSize(DIM/2,DIM/2);
            setMinimumSize(dim);
            setMaximumSize(dim);

        }

        public void paintComponent(java.awt.Graphics g)
        {
            super.paintComponent(g);
            for (Cell[] cell : cells)
            {
                for (int j = 0; j < cell.length; j++)
                {
                    if (cell[j].type == Cell.Type.WALL)
                    {
                        g.setColor(Color.BLACK);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                    } else if (cell[j].type == Cell.Type.PATH)
                    {
                        g.setColor(Color.WHITE);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                    } else if (cell[j].type == Cell.Type.BUSHES)
                    {
                        g.setColor(Color.WHITE);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                        g.setColor(Color.BLACK);
                        g.drawString("#", cell[j].x, cell[j].y + CELL_HEIGHT - 2);
                    }
                }
            }

        }
    }
}
