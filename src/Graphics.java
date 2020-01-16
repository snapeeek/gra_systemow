import javax.swing.*;
import java.awt.*;

public class Graphics extends JFrame
{
    JTextArea textArea;
    Board board;
    final int DIM = 800;

    Graphics(String name)
    {
        super(name);
        this.setLayout(new BorderLayout());
        textArea = new JTextArea();
        textArea.setSize(DIM /2,DIM/2);
        textArea.setEditable(false);
        this.add(textArea, BorderLayout.LINE_END);
        board = new Board();
        board.setSize(DIM/2,DIM/2);
        this.add(board, BorderLayout.LINE_START);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(DIM, DIM);
        this.setVisible(true);
    }

    class Board extends JPanel
    {

    }
}
