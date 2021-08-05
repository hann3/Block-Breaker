import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.sound.sampled.*;
import javax.swing.*;

abstract class myPanel extends JPanel implements KeyListener{
	protected Hw5 myFrame;
	protected Dimension size;
	protected Clip clip;
	protected URL bgm_url;
	protected URL img_url;
	protected AudioInputStream audioStream;
	protected ImageIcon icon;
	protected Image img;
	abstract void setSound();
	abstract void setBackground();
}

/*
 * ���� ���� ȭ��. Space ���� ��� ù ȭ������ �̵� 
 */
class EndPanel extends myPanel{
	private static int best_score = 0;
	private int end_score;
	// ���� �гηκ��� ������� ������ �޾ƿ´�. 
	EndPanel(Hw5 in, int score){
		myFrame = in;
		end_score = score;
		if(best_score < end_score) {
			best_score = end_score;
		}
		setSound();
		setBackground();
		setFocusable(true);
		requestFocus();
		addKeyListener(this);
		clip.start();
	}
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		size = getSize();
		g.drawImage(img, 0, 0, size.width, size.height, this);
		g.setFont(new Font("Courier New", Font.PLAIN, size.width/15));
		g.setColor(Color.LIGHT_GRAY);
		g.drawString("Your Score: "+Integer.toString(end_score), size.width/5,size.height/3);
		g.drawString("Best Score: "+Integer.toString(best_score), size.width/5,(int)(size.height/2));
	}

	public void setSound() {
		try {
			clip = AudioSystem.getClip();
			bgm_url = getClass().getResource("Gameover_bgm.wav");;
			audioStream = AudioSystem.getAudioInputStream(bgm_url);
			clip.open(audioStream); 
		}
		catch (LineUnavailableException e) { e.printStackTrace(); }
		catch (UnsupportedAudioFileException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}
	public void setBackground() {
		img_url = getClass().getResource("background.png");
		icon = new ImageIcon(img_url);
		img = icon.getImage(); 
	}

	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_SPACE) {
			clip.close();
			myFrame.add(new StartPanel(myFrame));
			myFrame.setVisible(true);
			setFocusable(false);
		}
	}

	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}

}

class GamePanel extends myPanel implements Runnable{
	private int level;
	private int score;
	private int num_Block;
	private int num_Yellow;
	private int width;
	private int height;
	private int wGap;
	private int hGap;
	private Clip clip2;

	// ��� ��ġ ���� 
	private ArrayList<Integer> ptx;
	private ArrayList<Integer> pty;
	// Ư�� ��� ���� 
	private ArrayList<Integer> idx;
	// ����� ���� ���� 
	private ArrayList<Integer> myX;
	private ArrayList<Integer> myY;
	// �� ���� 
	private ArrayList<MyBall> ball;

	GamePanel(Hw5 in){
		myFrame = in;
		level = 1;
		score = 0;
		num_Block = 0;
		num_Yellow = 0;

		ptx = new ArrayList<Integer>();
		pty = new ArrayList<Integer>();
		myX = new ArrayList<Integer>();
		myY = new ArrayList<Integer>();
		idx = new ArrayList<Integer>();
		ball = new ArrayList<MyBall>();

		setBackground(Color.BLACK);
		setSound();
		setBackground();
		setFocusable(true);
		requestFocus();
		addKeyListener(this);
		clip.loop(Clip.LOOP_CONTINUOUSLY);
		initialize();
		Thread t1 = new Thread(this);
		t1.start();
	}

	// stage ���� �ʱ�ȭ, ���� 
	void initialize(){
		size = myFrame.getSize();
		num_Block = (int)(Math.pow(3*level, 2));
		num_Yellow = (int)((Math.random()*num_Block)%((num_Block/2))+1);

		ball.clear();
		myX.clear();
		myY.clear();
		
		// 0���� �ʱ�ȭ
		for(int i=0; i<num_Block; i++) {
			idx.add(0);
		}

		// num_Yellow ����ŭ Ư����� ���� ���� 
		int cnt = 0; 
		while(true){
			if(cnt == num_Yellow) break;
			// Ư����� �ε��� 
			int tmp = (int)((Math.random()*num_Block)%num_Block); 
			if(idx.get(tmp) == 0) {
				idx.add(tmp,1);
				cnt++;
			}
		}

		width = (int)(size.getWidth()/(3*level));
		height = (int)((size.getHeight()*0.5)/(3*level));
		for(int i=0; i<3*level; i++) {
			for(int j=0; j<3*level; j++) {
				ptx.add(3*i+j,width*j);
				pty.add(3*i+j,height*i);
			}
		}

		// ����� ���� ��ġ 
		myX.add((int)(size.getWidth()/3));
		myX.add((int)(size.getWidth()/2));
		myY.add((int)(size.getHeight()*0.8));
		myY.add((int)(size.getHeight()*0.83));

		// �� ��ġ 
		ball.add(new MyBall((myX.get(0)+myX.get(1))/2,myY.get(0),5));
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		size = getSize();
		g.drawImage(img, 0, 0, size.width, size.height, this);

		wGap = width/15;
		hGap = height/15;

		for(int i=0; i<num_Block; i++) {
			if(idx.get(i)==1) {img_url = getClass().getResource("special.png");}
			else {img_url = getClass().getResource("block.png");}
			icon = new ImageIcon(img_url);
			g.drawImage(icon.getImage(), ptx.get(i), pty.get(i), width-wGap, height-hGap, this);
		}
		img_url = getClass().getResource("user.png");
		icon = new ImageIcon(img_url);
		g.drawImage(icon.getImage(), myX.get(0), myY.get(0), myX.get(1)-myX.get(0), myY.get(1)-myY.get(0), this);
		for(MyBall b : ball) {
			if(b.isAlive) {
				b.draw(g);
			}
		}
	}
	class MyBall{ 
		boolean isAlive;
		int dx, dy; // �̵����� 
		int ballX, ballY; // ��ġ
		int r;
		MyBall(int in_X, int in_Y, int in_r){
			dx = 3;
			dy = 3;
			ballX = in_X;
			ballY = in_Y;
			r = in_r;
			isAlive = true;
		}
		MyBall(int in_X, int in_Y, int in_dx, int in_dy, int in_r){
			this(in_X, in_Y, in_r);
			dx = in_dx;
			dy = in_dy;
		}
		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillOval(ballX-r, ballY-r, 2*r, 2*r);
		}
		void collisionEdge() {
			int preX = ballX;
			int preY = ballY;
			if(preY + r >= size.getHeight()) {
				// ���� ������ ����� ��� 
				isAlive = false;
				return ;
			}
			else if(preX - r < 0 || preX + r > size.getWidth()){
				// ���� �浹 
				dx = -dx;
			}
			else if(preY - r < 0) {
				// õ��� �浹 
				dy = -dy;
			}

			ballX = preX+dx;
			ballY = preY+dy;
		}

		void collisionRacket() {
			int r_width = (myX.get(1)-myX.get(0))/5;
			if(ballY >= myY.get(0)  && ballY  <= myY.get(1)) {
				if(ballX +r >= myX.get(0)  && ballX -r <= myX.get(1)){
					// ������ ���� �ٸ��� �ֱ� 
					int r = (ballX -myX.get(0))/r_width;
					if(r == 0) {dx = -4;}
					else if(r == 1) {dx = -2;}
					else if(r == 2) {dx = 0;}
					else if(r == 3) {dx = 2;}
					else{dx = 4;}
					dy = -dy;
				}

				// ���� �浹 
				else if(ballX  == myX.get(0) || ballX  == myX.get(1)) {
					dx = -dx;
					dy = -dy;
				}

			}
		}

		void collisionBlock(){
			boolean collision = false; 
			int remove_idx = 0;
			int wTmp =0; 
			int hTmp =0;

			for(int i=0; i<num_Block; i++) {
				// gap ���
				wTmp=(i==0?0:wGap);
				hTmp=(i==0?0:hGap);
				if(ballX >= ptx.get(i)+wTmp && ballX <= ptx.get(i)+width+wTmp) {
					if(ballY <= pty.get(i)+height+hTmp && ballY >= pty.get(i)+hTmp) {
						collision = true;
						remove_idx = i;
						break;
					}
				}
				// ���鿡�� �浹 
				else if(ballY >= pty.get(i)+hTmp && ballY <= pty.get(i)+height+hTmp) {
					if(ballX  >= ptx.get(i)+wTmp && ballX <= ptx.get(i)+height+hTmp) {
						collision = true;
						remove_idx = i;
						break;
					}
				}
			}
			if(collision) {	
				if(idx.get(remove_idx)==1) {
					ball.add(new MyBall(ptx.get(remove_idx), pty.get(remove_idx)+height,-3,3,5));
					ball.add(new MyBall(ptx.get(remove_idx)+width, pty.get(remove_idx)+height,3,3,5));
				}
				dx = -dx;
				dy = -dy;

				num_Block = num_Block-1;
				idx.remove(remove_idx);
				ptx.remove(remove_idx);
				pty.remove(remove_idx);
				score += 10;

				try {
					clip2 = AudioSystem.getClip();
					bgm_url = getClass().getResource("motion_bgm.wav");
					audioStream = AudioSystem.getAudioInputStream(bgm_url);
					clip2.open(audioStream);
					clip2.start();
				} catch (UnsupportedAudioFileException e) {
				}

				catch (LineUnavailableException e) {
				} catch (IOException e) {}

			}
		}


	}

	boolean IsGameContinue() {
		// ���� �ִ� ���� ���ٸ� ���� ����
		for(MyBall b : ball) {
			if(b.isAlive) {
				return true;
			}
		} 
		return false;
	}

	boolean IsStageClear() {
		// ���� �ִ� ����� 0�̶�� �������� Ŭ���� 
		if(num_Block == 0) return true;
		return false;
	}

	@Override
	public void run() {
		while(IsGameContinue()) {
			// stage up 
			if(IsStageClear()) {
				level ++;
				initialize();
			}
			int tmp = ball.size();
		
			for(int i=0; i<tmp; i++) {
				ball.get(i).collisionBlock();	
			}
		
			for(MyBall b : ball) {
				b.collisionRacket();
				b.collisionEdge();
			}
			repaint();
			try {
				Thread.sleep(12);
			} catch (InterruptedException e) {
				return;
			}
		}

		ptx.clear();
		pty.clear();
		myX.clear();
		myY.clear();
		ball.clear();
		clip.close();
		Thread.interrupted();
		myFrame.add(new EndPanel(myFrame, score));
		myFrame.setVisible(true);
		setFocusable(false);
	}

	@Override
	public void setBackground() {
		img_url = getClass().getResource("background.png");
		icon = new ImageIcon(img_url);
		img = icon.getImage(); 
	}
	public void setSound() {
		try {
			clip = AudioSystem.getClip();
			bgm_url = getClass().getResource("Game_bgm.wav");
			audioStream = AudioSystem.getAudioInputStream(bgm_url);
			clip.open(audioStream); 


		}
		catch (LineUnavailableException e) { e.printStackTrace(); }
		catch (UnsupportedAudioFileException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}

	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_LEFT) {
			int x1 = myX.get(0);
			int x2 = myX.get(1);
			if(x1 > 0) {
				myX.add(0,x1-15);
				myX.add(1,x2-15);
			}
			repaint();
		}
		else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
			int x1 = myX.get(0);
			int x2 = myX.get(1);
			if(x2 < size.getWidth()) {
				myX.add(0,x1+15);
				myX.add(1,x2+15);
			}
			repaint();
		}
	}

	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
}



/*
 * ���۽� �������� ù ȭ��
 * 1. ���� ���� 2. ���� 
 * Space�� Up, Down ����Ű�� ���� �� �޴� ����
 */
class StartPanel extends myPanel{
	private int[] ptx;	// Ŀ��(�ﰢ��)�� �׸��� ���� ��ǥ  
	private int[] pty;
	private int flag;	// ����ڰ� ������ �޴� 
	StartPanel(Hw5 in) {
		myFrame = in;
		flag = 0;
		ptx = new int[3];
		pty = new int[3];
		setSound();
		setBackground();
		setFocusable(true);
		requestFocus();
		addKeyListener(this);
		clip.start();
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		size = getSize();
		g.drawImage(img, 0, 0, size.width, size.height, this);

		g.setFont(new Font("Courier New", Font.BOLD, size.width/10));
		g.setColor(Color.LIGHT_GRAY);
		g.drawString("Java #5", size.width/6,size.height/5);
		g.drawString("Block Breaker", size.width/6,(int)(size.height/2.5));

		g.setFont(new Font("Courier New", Font.BOLD, size.width/20));
		setLocation(flag);
		g.fillPolygon(ptx, pty, 3);
		g.drawString("Press Spacebar to start", size.width/5,(int)(size.height/1.5));
		g.drawString("Exit", size.width/5,(int)(size.height/1.3));
	}

	void setLocation(int flag){
		size = getSize();
		if(flag == 0) { // Play
			ptx[0] = (int)(size.width/6.5);
			ptx[1] = (int)(size.width/5.5);
			ptx[2] = (int)(size.width/6.5);

			pty[0] = (int)(size.height/1.56);
			pty[1] = (int)(size.height/1.53);
			pty[2] = (int)(size.height/1.5);
		}
		else {	// Exit
			ptx[0] = (int)(size.width/6.5);
			ptx[1] = (int)(size.width/5.5);
			ptx[2] = (int)(size.width/6.5);

			pty[0] = (int)(size.height/1.35);
			pty[1] = (int)(size.height/1.33);
			pty[2] = (int)(size.height/1.3);
		}
	}


	public void setSound() {
		try {
			clip = AudioSystem.getClip();
			bgm_url = getClass().getResource("Start_bgm.wav");
			audioStream = AudioSystem.getAudioInputStream(bgm_url);
			clip.open(audioStream); 

		}
		catch (LineUnavailableException e) { e.printStackTrace(); }
		catch (UnsupportedAudioFileException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}

	@Override
	public void setBackground() {
		img_url = getClass().getResource("background.png");
		icon = new ImageIcon(img_url);
		img = icon.getImage(); 
	}
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_SPACE) {
			if(flag == 0) {
				clip.stop();
				myFrame.add(new GamePanel(myFrame));
				myFrame.setVisible(true);
				setFocusable(false);
			}
			else {
				// Exit ���ý� ���α׷� ���� 
				System.exit(0);
			}
		}
		else if(e.getKeyCode() == KeyEvent.VK_UP) {
			flag = 0;
			repaint();
		}
		else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
			flag = 1;
			repaint();
		}
	}
	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
}



public class Hw5 extends JFrame {
	public static void main(String[] args) {
		new Hw5();

	}
	Hw5(){
		setTitle("Block Breaker");
		setSize(800,800);
		add(new StartPanel(this));
		setVisible(true);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

}
