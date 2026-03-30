package test008.sustain_fix;

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

	private boolean isSustainToggled = false; // 시작 시 false (OFF)
	private int octaveOffset = 0;
	private int transposeOffset = 0;
	private boolean isSpacePressed = false;
	// 자동 페달 관련 변수 (서스테인 유지 및 주기적 리셋)
	private Thread autoPedalThread = null;
	private int pedalInterval = 100; // 페달 유지 간격 (ms)
	private int releaseTime = 100; // 
	private int reverbLevel = 50; // 기본값
	private int chorusLevel = 50;
	private void applySustainState() {
	    boolean sustainOn = isSustainToggled || isSpacePressed;
	    sendSustainCommand(sustainOn);
	}
	
	private void setReverb(int value) {
		try {
			if (receiver != null) {
				ShortMessage msg = new ShortMessage();
				msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 91, value);
				receiver.send(msg, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
	        if (autoPedalThread != null && autoPedalThread.isAlive())
	            return;

	        isSustainToggled = true;
	        applySustainState();

	        autoPedalThread = new Thread(() -> {
	            try {
	                while (isSustainToggled) {
	                    Thread.sleep(pedalInterval);
	                }
	            } catch (InterruptedException e) {
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
	        applySustainState();
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
						// 초기 상태에서 MIDI 장치에도 Sustain OFF 명령을 명시적으로 보냄
						sendSustainCommand(false);
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
		new Thread(() -> {
			try {
				Thread.sleep(releaseTime); // 🔥 릴리즈 시간
			} catch (InterruptedException e) {
			}

			try {
				if (receiver != null) {
					ShortMessage myMsg = new ShortMessage();
					myMsg.setMessage(ShortMessage.NOTE_OFF, 0, noteNumber, 93);
					receiver.send(myMsg, -1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
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

	private void setChorus(int value) {
		try {
			if (receiver != null) {
				ShortMessage msg = new ShortMessage();
				msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 93, value);
				receiver.send(msg, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
				// 🔥 Reverb ([ / ])
				if (keyCode == KeyEvent.VK_SPACE) {
				    if (!isSpacePressed) {
				        isSpacePressed = true;
				        applySustainState();
				        updateStatus();
				    }
				    e.consume();
				    return;
				}
				if (keyCode == KeyEvent.VK_OPEN_BRACKET) { // [
					reverbLevel = Math.max(0, reverbLevel - 1);
					setReverb(reverbLevel);
					updateStatus();
					return;
				}

				if (keyCode == KeyEvent.VK_CLOSE_BRACKET) { // ]
					reverbLevel = Math.min(127, reverbLevel + 1);
					setReverb(reverbLevel);
					updateStatus();
					return;
				}

				// 🔥 Chorus (; / ')
				if (keyCode == KeyEvent.VK_SEMICOLON) { // ;
					chorusLevel = Math.max(0, chorusLevel - 1);
					setChorus(chorusLevel);
					updateStatus();
					return;
				}

				if (keyCode == KeyEvent.VK_QUOTE) { // '
					chorusLevel = Math.min(127, chorusLevel + 1);
					setChorus(chorusLevel);
					updateStatus();
					return;
				}
				// Shift + Backspace → Sustain 토글
				if (keyCode == KeyEvent.VK_BACK_SPACE && e.isShiftDown()) {
					manageAutoPedal(!isSustainToggled);
					updateStatus();
					e.consume();
					return;
				}
				// 🔥 Release 조절 (Shift + , / Shift + -)
				if (keyCode == KeyEvent.VK_MINUS) {
					releaseTime = Math.max(0, releaseTime - 10);
					updateStatus();
					return;
				}

				if (keyCode == KeyEvent.VK_EQUALS) {
					releaseTime = Math.min(1000, releaseTime + 10);
					updateStatus();
					return;
				}
				// Shift 조합 컨트롤
				if (e.isShiftDown()) {

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

				}

				// 일반 노트 입력
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
	    String status = String.format(
	        "Octave: %+d | Transpose: %+d | Reverb: %d | Chorus: %d | Sustain: %s | Release: %dms",
	        octaveOffset / 12,
	        transposeOffset,
	        reverbLevel,
	        chorusLevel,
	        isSustainToggled ? "ON" : "OFF",
	        releaseTime
	    );
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
		// 생성자에서 manageAutoPedal(false)를 명시적으로 호출하여 OFF 상태로 시작
		manageAutoPedal(false);
		updateStatus();
	}

	public static void main(String[] args) {
		new Main();
	}
}