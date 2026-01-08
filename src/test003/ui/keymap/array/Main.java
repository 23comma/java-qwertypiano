package test003.ui.keymap.array;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JTextArea;

public class Main extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextArea textArea = null;
	private Map<String, Integer> keyMap = null;
	private String[] keyArray = null;

	private void setKeyMap() {
		keyMap = new HashMap<String, Integer>();
		keyArray = new String[] { "1", "!", "2", "@", "3", "4", "$", "5", "%", "6", "^", "7", "8", "*", "9", "0", "q",
				"Q", "w", "W", "e", "E", "r", "t", "T", "y", "Y", "u", "i", "I", "o", "O", "p", "P", "a", "s", "S", "d",
				"D", "f", "g", "G", "h", "H", "j", "J", "k", "l", "L", "z", "Z", "x", "c", "C", "v", "V", "b", "B", "n",
				"m", "M" };
		for (int i = 0; i < 61; i++) {
			keyMap.put(keyArray[i], i + 36);
		}
	}

	private void setTextArea() {
		textArea = new JTextArea();
		textArea.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				// 모든 키가 올라올 때 반응
			}

		});
	}

	private void setFrame() {
		setLayout(new BorderLayout());
		add(textArea, BorderLayout.CENTER);
		textArea.setFont(new Font("Consolas", Font.PLAIN, 32));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600, 800);
		setVisible(true);
	}

	public Main() {
		setKeyMap();
		setTextArea();
		setFrame();
	}

	public static void main(String[] args) {
		new Main();
	}
}
