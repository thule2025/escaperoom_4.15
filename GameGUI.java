/*
 * Project 4.1.5: Escape Room Revisited
 *
 * V1.0
 * Copyright(c) 2024 PLTW to present. All rights reserved
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class GameGUI extends JComponent implements KeyListener
{
  static final long serialVersionUID = 415L;

  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int MOVE = 60;

  private JFrame frame;
  private Image bgImage;
  private Image prizeImage;
  private Image player;
  private Image playerQ;

  private int currX = 15;
  private int currY = 15;
  private boolean atPrize;
  private Point playerLoc;
  private int playerSteps;

  private static final int MAX_LEVEL = 5;

  private int numWalls = 8;
  private int playerLevel = 1;
  private Rectangle[] walls;
  private Rectangle[] prizes;

  private String[][] quiz;
  private int quizIndex = 0;

  private int goodMove = 1;
  private int offGridVal = 5;
  private int hitWallVal = 5;
  private int correctAns = 10;
  private int wrongAns = 7;
  private int score = 0;

  public GameGUI() throws IOException
  {
    newPlayerLevel();
    createQuiz();
    createBoard();
  }

  private void createQuiz() throws IOException
  {
    ArrayList<String[]> quizList = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new FileReader("quiz.csv"));
    String line;

    while ((line = reader.readLine()) != null)
    {
      String[] parts = line.split("\\|");
      if (parts.length >= 2)
      {
        quizList.add(new String[] { parts[0].trim(), parts[1].trim() });
      }
    }
    reader.close();

    if (quizList.size() < playerLevel)
    {
      playerLevel = Math.min(quizList.size(), MAX_LEVEL);
      if (playerLevel < 1)
      {
        playerLevel = 1;
      }
    }

   quiz = new String[quizList.size()][2];
    for (int i = 0; i < quizList.size(); i++)
    {
     quiz[i][0] = quizList.get(i)[0];
     quiz[i][1] = quizList.get(i)[1];
    }
  }

  private void newPlayerLevel()
  {
     try
     {
      BufferedReader reader = new BufferedReader(new FileReader("level.csv"));
      String line = reader.readLine();
      reader.close();

      if (line != null)
      {
        playerLevel = Integer.parseInt(line.trim());
        if (playerLevel < 1) playerLevel = 1;
        if (playerLevel > MAX_LEVEL) playerLevel = MAX_LEVEL;
      }
      else
      {
        playerLevel = 1;
      }
    }
    catch (IOException e)
    {
      System.out.println("Level file not found. Starting at level 1.");
      playerLevel = 1;
    }

    numWalls = 5 + playerLevel * 2;
  }

  @Override
  public void keyPressed(KeyEvent e)
  {
    if (e.getKeyCode() == KeyEvent.VK_P)
    {
      if (atPrize)
      {
        String question = quiz[quizIndex][0];
        String answer = quiz[quizIndex][1];
        String userAnswer = askQuestion(question);

        if (userAnswer != null && userAnswer.equalsIgnoreCase(answer))
        {
          pickupPrize();
          score += correctAns;

          quizIndex++;
          if (quizIndex >= quiz.length)
          {
            quizIndex = quiz.length - 1;
          }

          showMessage("Correct! You picked up a prize.\nCurrent score: " + score +
                      "\nPrizes left: " + countRemainingPrizes());
        }
        else
        {
          score -= wrongAns;
          showMessage("Wrong! The correct answer was: " + answer + "\nCurrent score: " + score);
        }

        repaint();
      }
      else
      {
        showMessage("There is no prize here! Move to a prize and press P.");
      }
    }
    else if (e.getKeyCode() == KeyEvent.VK_Q)
    {
      boolean allCollected = countRemainingPrizes() == 0;

      if (!allCollected)
      {
        showMessage("You must collect all prizes before quitting!");
        return;
      }

      int finalScore = score - playerSteps;
      showMessage("Game over!\nFinal score (after deducting steps): " + finalScore);

      if (finalScore > 0)
      {
        if (playerLevel < MAX_LEVEL)
        {
          playerLevel++;
          showMessage("Congrats! You leveled up to " + playerLevel + "!");
        }
        else
        {
          showMessage("You reached the maximum level!");
        }
      }
      else
      {
        showMessage("Your score was not positive. Level remains the same.");
      }

      endGame();
    }
    else if (e.getKeyCode() == KeyEvent.VK_H)
    {
      String msg = "Move player: arrows or WASD keys\n" +
                   "Pickup prize: p\n" +
                   "Quit: q\n" +
                   "Help: h\n";
      showMessage(msg);
    }
    else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S)
    {
      score += movePlayer(0, MOVE);
    }
    else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
    {
      score += movePlayer(0, -MOVE);
    }
    else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
    {
      score += movePlayer(-MOVE, 0);
    }
    else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
    {
      score += movePlayer(MOVE, 0);
    }
  }

  @Override
  public void keyReleased(KeyEvent e)
  {
    checkForPrize();
  }

  @Override
  public void keyTyped(KeyEvent e) { }

  private void createBoard() throws IOException
  {
    prizes = new Rectangle[playerLevel];
    createPrizes();

    walls = new Rectangle[numWalls];
    createWalls();

    bgImage = ImageIO.read(new File("grid.png"));
    prizeImage = ImageIO.read(new File("coin.png"));
    player = ImageIO.read(new File("player.png"));
    playerQ = ImageIO.read(new File("playerQ.png"));

    playerLoc = new Point(currX, currY);

    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(this);
    frame.setVisible(true);
    frame.setResizable(false);
    frame.addKeyListener(this);

    checkForPrize();
    showMessage("Welcome to the Escape Room. Press h to learn how to play.");
  }

  private int movePlayer(int incrx, int incry)
  {
    int newX = currX + incrx;
    int newY = currY + incry;

    if ((newX < 0 || newX > WIDTH - SPACE_SIZE) || (newY < 0 || newY > HEIGHT - SPACE_SIZE))
    {
      showMessage("You have tried to go off the grid!");
      return -offGridVal;
    }

    for (Rectangle r : walls)
    {
      int startX = (int) r.getX();
      int endX = (int) r.getX() + (int) r.getWidth();
      int startY = (int) r.getY();
      int endY = (int) r.getY() + (int) r.getHeight();

      if ((incrx > 0) && (currX <= startX) && (startX <= newX) && (currY >= startY) && (currY <= endY))
      {
        showMessage("A wall is in the way.");
        return -hitWallVal;
      }
      else if ((incrx < 0) && (currX >= startX) && (startX >= newX) && (currY >= startY) && (currY <= endY))
      {
        showMessage("A wall is in the way.");
        return -hitWallVal;
      }
      else if ((incry > 0) && (currY <= startY) && (startY <= newY) && (currX >= startX) && (currX <= endX))
      {
        showMessage("A wall is in the way.");
        return -hitWallVal;
      }
      else if ((incry < 0) && (currY >= startY) && (startY >= newY) && (currX >= startX) && (currX <= endX))
      {
        showMessage("A wall is in the way.");
        return -hitWallVal;
      }
    }

    playerSteps++;
    currX += incrx;
    currY += incry;
    repaint();
    return goodMove;
  }

  private void showMessage(String str)
  {
    JOptionPane.showMessageDialog(frame, str);
  }

  private String askQuestion(String q)
  {
    return JOptionPane.showInputDialog(frame, q.replace("\\n", "\n"));
  }

  private int countRemainingPrizes()
  {
    int count = 0;
    for (Rectangle p : prizes)
    {
      if (p != null && p.getWidth() > 0)
      {
        count++;
      }
    }
    return count;
  }

  private void checkForPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle r : prizes)
    {
      if (r != null && r.contains(px, py))
      {
        atPrize = true;
        repaint();
        return;
      }
    }
    atPrize = false;
  }

  private void pickupPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (int i = 0; i < prizes.length; i++)
    {
      if (prizes[i] != null && prizes[i].contains(px, py))
      {
        int s = SPACE_SIZE;
        Random rand = new Random();
        int h = rand.nextInt(GRID_H);
        int w = rand.nextInt(GRID_W);
        prizes[i] = new Rectangle((w * s + 15), (h * s + 15), 15, 15);

        atPrize = false;
        repaint();
        return;
      }
    }
  }

  private void endGame()
  {
    try
    {
      FileWriter fw = new FileWriter("level.csv");
      fw.write(playerLevel + "\n");
      fw.close();
    }
    catch (IOException e)
    {
      System.err.println("Could not level up.");
    }

    setVisible(false);
    frame.dispose();
  }

  private void createPrizes()
  {
    int s = SPACE_SIZE;
    Random rand = new Random();

    for (int numPrizes = 0; numPrizes < playerLevel; numPrizes++)
    {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);
      Rectangle r = new Rectangle((w * s + 15), (h * s + 15), 15, 15);

      boolean duplicate = true;
      while (duplicate)
      {
        duplicate = false;
        for (int i = 0; i < numPrizes; i++)
        {
          if (prizes[i] != null && prizes[i].equals(r))
          {
            duplicate = true;
            h = rand.nextInt(GRID_H);
            w = rand.nextInt(GRID_W);
            r = new Rectangle((w * s + 15), (h * s + 15), 15, 15);
            break;
          }
        }
      }

      prizes[numPrizes] = r;
    }
  }

  private void createWalls()
  {
    int s = SPACE_SIZE;
    Random rand = new Random();

    for (int n = 0; n < numWalls; n++)
    {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      if (rand.nextInt(2) == 0)
      {
        r = new Rectangle((w * s + s - 5), h * s, 8, s);
      }
      else
      {
        r = new Rectangle(w * s, (h * s + s - 5), s, 8);
      }

      walls[n] = r;
    }
  }

  @Override
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    g.drawImage(bgImage, 0, 0, null);

    for (Rectangle p : prizes)
    {
      if (p != null && p.getWidth() > 0)
      {
        int px = (int) p.getX();
        int py = (int) p.getY();
        g.drawImage(prizeImage, px, py, null);
      }
    }

    for (Rectangle r : walls)
    {
      g2.setPaint(Color.BLACK);
      g2.fill(r);
    }

    if (atPrize)
    {
      g.drawImage(playerQ, currX, currY, 40, 40, null);
    }
    else
    {
      g.drawImage(player, currX, currY, 40, 40, null);
    }

    playerLoc.setLocation(currX, currY);
  }
}
