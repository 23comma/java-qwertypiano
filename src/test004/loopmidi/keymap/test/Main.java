package test004.loopmidi.keymap.test;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class Main extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextArea textArea = null;
	private Map<String, Integer> keyMap = null;
	private MidiDevice outDevice = null;
	private Receiver receiver = null; // 리시버를 전역변수로 관리
	private String targetPortName = "loopMIDI Port";
	private Map<Integer, Boolean> keyStateMap = new HashMap<>();
	private boolean isSustainToggled = false;
	private void sendSustainCommand(boolean isOn) {
	    try {
	        if (receiver != null) {
	            ShortMessage msg = new ShortMessage();
	            // 127은 On(밟음), 0은 Off(뗌)
	            int value = isOn ? 127 : 0;
	            // 0xB0(Control Change), 64(Sustain Pedal)
	            msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 64, value);
	            receiver.send(msg, -1);
	            System.out.println("Sustain " + (isOn ? "ON" : "OFF"));
	        }
	    } catch (InvalidMidiDataException e) {
	        e.printStackTrace();
	    }
	}
	private void initMidiPort() {
		try {
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			for (MidiDevice.Info info : infos) {
				if (info.getName().contains(targetPortName)) {
					MidiDevice device = MidiSystem.getMidiDevice(info);
					if (device.getMaxReceivers() != 0) {
						outDevice = device;
						outDevice.open(); // 여기서 딱 한 번만 엽니다.
						receiver = outDevice.getReceiver(); // 리시버 미리 생성
						System.out.println("연결 성공: " + info.getName());
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void noteON(int noteNumber) {
		try {
			if (receiver != null) {
				ShortMessage myMsg = new ShortMessage();
				myMsg.setMessage(ShortMessage.NOTE_ON, 0, noteNumber, 93);
				receiver.send(myMsg, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void noteOFF(int noteNumber) {
		try {
			if (receiver != null) {
				ShortMessage myMsg = new ShortMessage();
				myMsg.setMessage(ShortMessage.NOTE_OFF, 0, noteNumber, 93);
				receiver.send(myMsg, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setKeyMap() {
		keyMap = new HashMap<>();
		String[] keyArray = { "1", "!", "2", "@", "3", "4", "$", "5", "%", "6", "^", "7", "8", "*", "9", "0", "q", "Q",
				"w", "W", "e", "E", "r", "t", "T", "y", "Y", "u", "i", "I", "o", "O", "p", "P", "a", "s", "S", "d", "D",
				"f", "g", "G", "h", "H", "j", "J", "k", "l", "L", "z", "Z", "x", "c", "C", "v", "V", "b", "B", "n", "m",
				"M" };

		for (int i = 0; i < keyArray.length; i++) {
			keyMap.put(keyArray[i], i + 36);
		}
	}

	private void setTextArea() {
	    textArea = new JTextArea();
	    textArea.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyPressed(KeyEvent e) {
	            // 백스페이스 감지 (토글 로직)
	            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
	                isSustainToggled = !isSustainToggled; // 상태 반전
	                sendSustainCommand(isSustainToggled); // MIDI 전송
	                return; // 백스페이스 자체 문자가 입력되지 않도록 리턴
	            }

	            // 기존 건반 연주 로직 (반복키 방지 포함)
	            String charKey = String.valueOf(e.getKeyChar());
	            Integer note = keyMap.get(charKey);
	            if (note != null && !keyStateMap.getOrDefault(note, false)) {
	                keyStateMap.put(note, true);
	                noteON(note);
	            }
	        }

	        @Override
	        public void keyReleased(KeyEvent e) {
	            // 백스페이스 뗄 때는 아무 동작 안 함
	            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) return;

	            String charKey = String.valueOf(e.getKeyChar());
	            Integer note = keyMap.get(charKey);
	            if (note != null) {
	                keyStateMap.put(note, false);
	                noteOFF(note);
	            }
	        }
	    });
	}

	private void setFrame() {
		setLayout(new BorderLayout());
		add(textArea, BorderLayout.CENTER);
		textArea.setFont(new Font("Consolas", Font.PLAIN, 24));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600, 400);
		setVisible(true);
	}

	public Main() {
		initMidiPort();
		setKeyMap();
		setTextArea();
		setFrame();
	}

	public static void main(String[] args) {
		new Main();
	}
}