package test009.function_key;

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

    // 실제 눌린 상태 추적
    private Map<Integer, Boolean> keyStateMap = new HashMap<>();
    private Map<Integer, Boolean> physicalKeyMap = new HashMap<>();   // 손가락으로 실제 누르고 있는 중
    private Map<Integer, Boolean> sustainedNoteMap = new HashMap<>(); // 페달로 유지 중

    private boolean isSustainToggled = false; // Shift+Backspace 토글 sustain
    private boolean isSpacePressed = false;   // Space hold sustain

    private int octaveOffset = 0;
    private int transposeOffset = 0;

    // 자동 sustain 토글 관련
    private Thread autoPedalThread = null;
    private int pedalInterval = 100;

    private int releaseTime = 100;
    private int reverbLevel = 50;
    private int chorusLevel = 50;

    private boolean isPedalDown() {
        return isSustainToggled || isSpacePressed;
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

    private void sendSustainCommand(boolean isOn) {
        try {
            if (receiver != null) {
                ShortMessage msg = new ShortMessage();
                int value = isOn ? 127 : 0;
                msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 64, value);
                receiver.send(msg, -1);
            }
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void releasePedaledNotes() {
        for (Map.Entry<Integer, Boolean> entry : sustainedNoteMap.entrySet()) {
            int note = entry.getKey();
            boolean sustained = entry.getValue();
            boolean physicallyHeld = physicalKeyMap.getOrDefault(note, false);

            // 페달로만 유지되고 있고 손은 이미 뗀 상태면 여기서 끊음
            if (sustained && !physicallyHeld) {
                noteOFF(note);
                entry.setValue(false);
                keyStateMap.put(note, false);
            }
        }
    }

    private void applySustainState() {
        boolean sustainOn = isPedalDown();
        sendSustainCommand(sustainOn);

        // 페달을 떼는 순간, 손에서 이미 뗀 음들만 정리
        if (!sustainOn) {
            releasePedaledNotes();
        }
    }

    private void manageAutoPedal(boolean start) {
        if (start) {
            if (autoPedalThread != null && autoPedalThread.isAlive()) {
                return;
            }

            isSustainToggled = true;
            applySustainState();

            autoPedalThread = new Thread(() -> {
                try {
                    while (isSustainToggled) {
                        Thread.sleep(pedalInterval);
                    }
                } catch (InterruptedException e) {
                    // ignore
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

                        sendSustainCommand(false);
                        setReverb(reverbLevel);
                        setChorus(chorusLevel);
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
                Thread.sleep(releaseTime);
            } catch (InterruptedException e) {
                // ignore
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
        String[] keyArray = {
            "1", "!", "2", "@", "3", "4", "$", "5", "%", "6", "^", "7", "8", "*", "9", "(", "0",
            "q", "Q", "w", "W", "e", "E", "r", "t", "T", "y", "Y", "u", "i", "I", "o", "O", "p", "P",
            "a", "s", "S", "d", "D", "f", "g", "G", "h", "H", "j", "J", "k", "l", "L",
            "z", "Z", "x", "c", "C", "v", "V", "b", "B", "n", "m", "M"
        };

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

                // Space hold sustain
                if (keyCode == KeyEvent.VK_SPACE) {
                    if (!isSpacePressed) {
                        isSpacePressed = true;
                        applySustainState();
                        updateStatus();
                    }
                    e.consume();
                    return;
                }

                // Reverb [ ]
                if (keyCode == KeyEvent.VK_OPEN_BRACKET) {
                    reverbLevel = Math.max(0, reverbLevel - 1);
                    setReverb(reverbLevel);
                    updateStatus();
                    return;
                }

                if (keyCode == KeyEvent.VK_CLOSE_BRACKET) {
                    reverbLevel = Math.min(127, reverbLevel + 1);
                    setReverb(reverbLevel);
                    updateStatus();
                    return;
                }

                // Chorus ; '
                if (keyCode == KeyEvent.VK_SEMICOLON) {
                    chorusLevel = Math.max(0, chorusLevel - 1);
                    setChorus(chorusLevel);
                    updateStatus();
                    return;
                }

                if (keyCode == KeyEvent.VK_QUOTE) {
                    chorusLevel = Math.min(127, chorusLevel + 1);
                    setChorus(chorusLevel);
                    updateStatus();
                    return;
                }

                // Release - / =
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

                // Shift + Backspace → sustain toggle
                if (keyCode == KeyEvent.VK_BACK_SPACE && e.isShiftDown()) {
                    manageAutoPedal(!isSustainToggled);
                    updateStatus();
                    e.consume();
                    return;
                }

                // Shift + 방향키
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
                    default:
                        break;
                    }
                }

                // 일반 노트 입력
                String charKey = String.valueOf(e.getKeyChar());
                Integer baseNote = keyMap.get(charKey);

                if (baseNote != null) {
                    int finalNote = baseNote + octaveOffset + transposeOffset;

                    if (finalNote >= 0 && finalNote <= 127 && !keyStateMap.getOrDefault(finalNote, false)) {
                        keyStateMap.put(finalNote, true);
                        physicalKeyMap.put(finalNote, true);
                        sustainedNoteMap.put(finalNote, true);
                        noteON(finalNote);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_SPACE) {
                    isSpacePressed = false;
                    applySustainState();
                    updateStatus();
                    e.consume();
                    return;
                }

                if (keyCode == KeyEvent.VK_BACK_SPACE) {
                    return;
                }

                String charKey = String.valueOf(e.getKeyChar());
                Integer baseNote = keyMap.get(charKey);

                if (baseNote != null) {
                    int finalNote = baseNote + octaveOffset + transposeOffset;
                    physicalKeyMap.put(finalNote, false);

                    if (isPedalDown()) {
                        // 페달 눌려 있으면 note off 즉시 보내지 않고 유지
                        sustainedNoteMap.put(finalNote, true);
                    } else {
                        // 페달 안 눌려 있으면 바로 끊음
                        keyStateMap.put(finalNote, false);
                        sustainedNoteMap.put(finalNote, false);
                        noteOFF(finalNote);
                    }
                }
            }
        });
    }

    private void updateStatus() {
        String status = String.format(
            "Octave: %+d | Transpose: %+d | Reverb: %d | Chorus: %d | SustainToggle: %s | SpacePedal: %s | Release: %dms",
            octaveOffset / 12,
            transposeOffset,
            reverbLevel,
            chorusLevel,
            isSustainToggled ? "ON" : "OFF",
            isSpacePressed ? "DOWN" : "UP",
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
        textArea.requestFocusInWindow();
    }

    public Main() {
        initMidiPort();
        setKeyMap();
        setTextArea();
        setFrame();
        manageAutoPedal(false);
        updateStatus();
    }

    public static void main(String[] args) {
        new Main();
    }
}