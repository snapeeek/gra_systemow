import Maze.Cell;

import javax.swing.*;
import java.awt.*;

//TODO wyświetlanie graczy, obozowiska i bestii

public class Graphics extends JFrame
{
    private JTextArea textArea;
    private Board board;
    Cell[][] cells;
    final int DIM = 800;
    public static final int CELL_WIDTH = 10;
    public static final int CELL_HEIGHT = 15;

    Graphics(String name, Cell[][] cells)
    {
        super(name);
        this.cells = cells;
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(5,5));
        textArea = new JTextArea();
        textArea.setSize(DIM /2,DIM/2);
        textArea.setPreferredSize(new Dimension(60*CELL_WIDTH, 30 * CELL_HEIGHT));
        textArea.setEditable(false);
        textArea.setText("Witam serdecznie co sie dzieje\nskad to zwatpienie byku\npamietaj zeby ciagle isc do przodu, bo nikt za Ciebie tego nie zrobi B)");
        board = new Board();

        contentPane.add(board, BorderLayout.LINE_START);
        contentPane.add(textArea, BorderLayout.CENTER);
        this.setContentPane(contentPane);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(DIM, DIM);
        this.pack();
        this.setResizable(false);
        this.setVisible(true);
    }

    void setArray(Cell[][] temp)
    {
        this.cells = temp;
    }

    void repaintBoard()
    {
        board.repaint();
    }

    class Board extends JPanel
    {
        private static final int GAP = 5;
        Board()
        {
            //setBorder(BorderFactory.createLineBorder(Color.RED, GAP, true));
            setSize(DIM/2, DIM/2);
            Dimension dim = new Dimension();
            dim.setSize(DIM/2,DIM/2);
        }

        @Override
        public Dimension getPreferredSize()
        {
            return (new Dimension(60*CELL_WIDTH, 30*CELL_HEIGHT));
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
                    }
                    else if (cell[j].type == Cell.Type.PATH)
                    {
                        g.setColor(Color.WHITE);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                        if (cell[j].ocup == Cell.Ocup.BEAST)
                        {
                            g.setColor(Color.BLACK);
                            g.drawString("*", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].ocup == Cell.Ocup.COIN)
                        {
                            g.setColor(Color.BLACK);
                            g.drawString("c", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].ocup == Cell.Ocup.TREAS)
                        {
                            g.setColor(Color.BLACK);
                            g.drawString("t", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].ocup == Cell.Ocup.BIGT)
                        {
                            g.setColor(Color.BLACK);
                            g.drawString("T", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else
                        {

                        }
                    }
                    else if (cell[j].type == Cell.Type.BUSHES)
                    {
                        g.setColor(Color.WHITE);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                        g.setColor(Color.BLACK);
                        g.drawString("#", cell[j].x+2, cell[j].y + CELL_HEIGHT - 3);
                    }
                    else
                    {
                        g.setColor(Color.GRAY);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                    }
                }
            }

        }
    }
}
