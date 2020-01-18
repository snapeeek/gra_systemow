import Maze.Cell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

//TODO wyświetlanie bestii

public class Graphics extends JFrame implements KeyListener
{
    private JTextArea textArea;
    private Board board;
    Cell[][] cells;
    final int DIM = 800;
    public static final int CELL_WIDTH = 10;
    public static final int CELL_HEIGHT = 15;
    public String com = "nothing";

    Graphics(String name, Cell[][] cells)
    {
        super(name);
        this.cells = cells;

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(5, 5));
        textArea = new JTextArea();
        textArea.addKeyListener(this);
        textArea.setSize(DIM / 2, DIM / 2);
        textArea.setPreferredSize(new Dimension(60 * CELL_WIDTH, 30 * CELL_HEIGHT));
        textArea.setEditable(false);
        textArea.setText("Witam serdecznie co sie dzieje\nskad to zwatpienie byku\npamietaj zeby ciagle isc do przodu, bo nikt za Ciebie tego nie zrobi B)");
        board = new Board();
        board.addKeyListener(this);

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
        this.cells = temp.clone();
    }

    void setTextArea(String str)
    {
        textArea.setText(str);
    }

    void repaintBoard()
    {
        board.repaint();
    }

    String getCom()
    {
        return com;
    }

    void resetCom()
    {
        this.com = "nothing";
    }

    @Override
    public void keyTyped(KeyEvent e)
    {

    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case KeyEvent.VK_UP:
                com = "up";
                break;
            case KeyEvent.VK_LEFT:
                com = "left";
                break;
            case KeyEvent.VK_DOWN:
                com = "down";
                break;
            case KeyEvent.VK_RIGHT:
                com = "right";
                break;
            case KeyEvent.VK_Q:
                com = "exit";
                break;
            case KeyEvent.VK_C:
                com = "coin";
                break;
            case KeyEvent.VK_B:
                com = "beast";
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {

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
                    if (cell[j].getType() == Cell.Type.WALL)
                    {
                        g.setColor(Color.BLACK);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                    }
                    else if (cell[j].getType() == Cell.Type.PATH)
                    {
                        g.setColor(Color.WHITE);
                        g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                        if (cell[j].getOcup() == Cell.Ocup.BEAST)
                        {
                            g.setColor(Color.BLACK);
                            g.drawString("*", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].getOcup() == Cell.Ocup.COIN)
                        {
                            g.setColor(Color.YELLOW);
                            g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                            g.setColor(Color.BLACK);
                            g.drawString("c", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].getOcup() == Cell.Ocup.TREAS)
                        {
                            g.setColor(Color.YELLOW);
                            g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                            g.setColor(Color.BLACK);
                            g.drawString("t", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].getOcup() == Cell.Ocup.BIGT)
                        {
                            g.setColor(Color.YELLOW);
                            g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                            g.setColor(Color.BLACK);
                            g.drawString("T", cell[j].x+3, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].isCamp())
                        {
                            if (cell[j].getOcup() == Cell.Ocup.NOTHING)
                            {
                                g.setColor(Color.CYAN);
                                g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                                g.setColor(Color.GRAY);
                                g.drawString("A", cell[j].x + 1, cell[j].y + CELL_HEIGHT - 3);
                            }
                            else if (cell[j].getOcup() == Cell.Ocup.PLAYER)
                            {
                                g.setColor(Color.CYAN);
                                g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                                g.setColor(Color.GRAY);
                                g.drawString(String.valueOf(cell[j].getPlayerNum()), cell[j].x + 1, cell[j].y + CELL_HEIGHT - 3);
                            }
                        }
                        else if (cell[j].getOcup() == Cell.Ocup.PLAYER)
                        {
                            g.setColor(Color.MAGENTA);
                            g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                            g.setColor(Color.yellow);
                            String temp = String.valueOf(cell[j].getPlayerNum());
                            g.drawString(temp, cell[j].x+2, cell[j].y + CELL_HEIGHT - 3);
                        }
                    }
                    else if (cell[j].getType() == Cell.Type.BUSHES)
                    {
                        if (cell[j].getOcup() == Cell.Ocup.NOTHING)
                        {
                            g.setColor(Color.WHITE);
                            g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                            g.setColor(Color.BLACK);
                            g.drawString("#", cell[j].x + 2, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].getOcup() == Cell.Ocup.PLAYER)
                        {
                            g.setColor(Color.MAGENTA);
                            g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                            g.setColor(Color.yellow);
                            g.drawString(String.valueOf(cell[j].getPlayerNum()), cell[j].x+2, cell[j].y + CELL_HEIGHT - 3);
                            g.setColor(Color.BLACK);
                            g.drawString("#", cell[j].x + 2, cell[j].y + CELL_HEIGHT - 3);
                        }
                        else if (cell[j].getOcup() == Cell.Ocup.BIGT)
                        {
                            g.setColor(Color.yellow);
                            g.fillRect(cell[j].x, cell[j].y, CELL_WIDTH, CELL_HEIGHT);
                            g.setColor(Color.black);
                            g.drawString("T", cell[j].x, cell[j].y + CELL_HEIGHT - 3);
                            g.drawString("#", cell[j].x + 2, cell[j].y + CELL_HEIGHT - 3);
                        }
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
