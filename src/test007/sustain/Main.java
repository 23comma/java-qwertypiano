package test007.sustain;

import java.awt.BorderLayout;
import java.awt.Color;
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
	private Receiver receiver = null;
	private String targetPortName = "loopMIDI Port";
	private Map<Integer, Boolean> keyStateMap = new HashMap<>();

	private boolean isSustainToggled = false;
	private int octaveOffset = 0;
	private int transposeOffset = 0;

	// 자동 페달 관련 변수 (서스테인 유지 및 주기적 리셋)
	private Thread autoPedalThread = null;
	private int pedalInterval = 1000; // 페달 유지 간격 (ms)

	private void sendSustainCommand(boolean isOn) {
		try {
			if (receiver != null) {
				ShortMessage msg = new ShortMessage();
				int value = isOn ? 127 : 0;
				// Control Change #64 는 서스테인(Damper Pedal)을 의미합니다.
				msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 64, value);
				receiver.send(msg, -1);
			}
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	// 서스테인이 켜져있을 때 효과를 유지하거나 주기적으로 갱신하는 로직
	private void manageAutoPedal(boolean start) {
		if (start) {
			// 이미 실행 중이면 중복 실행 방지
			if (autoPedalThread != null && autoPedalThread.isAlive())
				return;

			isSustainToggled = true;
			sendSustainCommand(true); // 우선 페달을 밟음

			// 필요 시 주기적으로 페달을 뗐다 밟아서 잔향을 정리하거나 상태를 유지하는 스레드
			autoPedalThread = new Thread(() -> {
				try {
					while (isSustainToggled) {
						// 현재는 단순 유지 모드이므로 대기 (필요 시 여기서 페달 리셋 로직 추가 가능)
						Thread.sleep(pedalInterval);
						
						// 만약 "자동 페달 갈기" 기능이 필요하다면 아래 주석 해제
						/*
						sendSustainCommand(false);
						Thread.sleep(50);
						sendSustainCommand(true);
						*/
					}
				} catch (InterruptedException e) {
					// 스레드 종료 시 catch됨
				}
			});
			autoPedalThread.setDaemon(true);
			autoPedalThread.start();
		} else {
			isSustainToggled = false;
			if (autoPedalThread != null) {
				autoPedalThread.interrupt();
				autoPedalThread = null;
			}
			sendSustainCommand(false); // 페달 떼기
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
						outDevice.open();
						receiver = outDevice.getReceiver();
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
		String[] keyArray = { "1", "!", "2", "@", "3", "4", "$", "5", "%", "6", "^", "7", "8", "*", "9", "(", "0", "q",
				"Q", "w", "W", "e", "E", "r", "t", "T", "y", "Y", "u", "i", "I", "o", "O", "p", "P", "a", "s", "S", "d",
				"D", "f", "g", "G", "h", "H", "j", "J", "k", "l", "L", "z", "Z", "x", "c", "C", "v", "V", "b", "B", "n",
				"m", "M" };

		for (int i = 0; i < keyArray.length; i++) {
			keyMap.put(keyArray[i], i + 36);
		}
	}

	private void setTextArea() {
		textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();

				// Shift + Backspace: 서스테인 토글 로직 구현
				if (keyCode == KeyEvent.VK_BACK_SPACE && e.isShiftDown()) {
					manageAutoPedal(!isSustainToggled);
					updateStatus();
					e.consume(); // 이벤트 전파 방지 (글자 지워짐 방지)
					return;
				}

				// 방향키 제어
				switch (keyCode) {
				case KeyEvent.VK_UP:
					octaveOffset += 12;
					updateStatus();
					return;
				case KeyEvent.VK_DOWN:
					octaveOffset -= 12;
					updateStatus();
					return;
				case KeyEvent.VK_RIGHT:
					transposeOffset += 1;
					updateStatus();
					return;
				case KeyEvent.VK_LEFT:
					transposeOffset -= 1;
					updateStatus();
					return;
				}

				String charKey = String.valueOf(e.getKeyChar());
				Integer baseNote = keyMap.get(charKey);

				if (baseNote != null) {
					int finalNote = baseNote + octaveOffset + transposeOffset;
					if (finalNote >= 0 && finalNote <= 127 && !keyStateMap.getOrDefault(finalNote, false)) {
						keyStateMap.put(finalNote, true);
						noteON(finalNote);
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// Backspace 자체는 노트 오프와 무관하므로 무시
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
					return;
					
				String charKey = String.valueOf(e.getKeyChar());
				Integer baseNote = keyMap.get(charKey);

				if (baseNote != null) {
					int finalNote = baseNote + octaveOffset + transposeOffset;
					keyStateMap.put(finalNote, false);
					noteOFF(finalNote);
				}
			}
		});
	}

	private void updateStatus() {
		String status = String.format("Octave: %+d | Transpose: %+d | Sustain: %s", octaveOffset / 12,
				transposeOffset, isSustainToggled ? "ON (Hold)" : "OFF");
		setTitle("MIDI Controller - " + status);
	}

	private void setFrame() {
		setLayout(new BorderLayout());
		add(textArea, BorderLayout.CENTER);
		textArea.setFont(new Font("Consolas", Font.PLAIN, 24));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 500);
		textArea.setBackground(Color.black);
		textArea.setForeground(Color.white);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public Main() {
		initMidiPort();
		setKeyMap();
		setTextArea();
		setFrame();
		updateStatus();
	}

	public static void main(String[] args) {
		new Main();
	}
}