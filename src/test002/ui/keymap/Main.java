package test002.ui.keymap;

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

	private void setKeyMap() {
		keyMap = new HashMap<String, Integer>();
		keyMap.put("1", 36);
		keyMap.put("!", 37);
		keyMap.put("2", 38);
		keyMap.put("@", 39);
		keyMap.put("3", 40);
		keyMap.put("4", 41);
		keyMap.put("$", 42);
		keyMap.put("5", 43);
		keyMap.put("%", 44);
		keyMap.put("6", 45);
		keyMap.put("^", 46);
		keyMap.put("7", 47);
		keyMap.put("8", 48);
		keyMap.put("*", 50);
		keyMap.put("9", 51);
		keyMap.put("0", 52);
		keyMap.put("q", 53);
		keyMap.put("Q", 54);
		keyMap.put("w", 55);
		keyMap.put("W", 56);
		keyMap.put("e", 57);
		keyMap.put("E", 58);
		keyMap.put("r", 59);
		keyMap.put("t", 60);
		keyMap.put("T", 61);
		keyMap.put("y", 62);
		keyMap.put("Y", 63);
		keyMap.put("u", 64);
		keyMap.put("i", 65);
		keyMap.put("I", 66);
		keyMap.put("o", 67);
		keyMap.put("O", 68);
		keyMap.put("p", 69);
		keyMap.put("P", 70);
		keyMap.put("a", 71);
		keyMap.put("s", 72);
		keyMap.put("S", 73);
		keyMap.put("d", 74);
		keyMap.put("D", 75);
		keyMap.put("f", 76);
		keyMap.put("g", 77);
		keyMap.put("G", 78);
		keyMap.put("h", 79);
		keyMap.put("H", 80);
		keyMap.put("j", 81);
		keyMap.put("J", 82);
		keyMap.put("k", 83);
		keyMap.put("l", 84);
		keyMap.put("L", 85);
		keyMap.put("z",86);
		keyMap.put("Z",87);
		keyMap.put("x",88);
		keyMap.put("c",89);
		keyMap.put("C",90);
		keyMap.put("v",91);
		keyMap.put("V",92);
		keyMap.put("b",93);
		keyMap.put("B",94);
		keyMap.put("n",95);
		keyMap.put("m",96);
		keyMap.put("M",97);
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
