package test001.midi_loop_connect;
import javax.sound.midi.*;

public class MidiLoopConnect {
    public static void main(String[] args) {
        // 1. 시스템의 모든 MIDI 장치 정보 가져오기
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice outDevice = null;
        
        String targetPortName = "loopMIDI Port"; // LoopMIDI에서 설정한 이름

        try {
            for (MidiDevice.Info info : infos) {
                // 이름이 일치하는 장치 찾기
                if (info.getName().contains(targetPortName)) {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    
                    // 출력이 가능한 장치인지 확인 (MaxReceivers가 0이 아니어야 함)
                    if (device.getMaxReceivers() != 0) {
                        outDevice = device;
                        System.out.println("연결 성공: " + info.getName());
                        break;
                    }
                }
            }

            if (outDevice != null) {
                // 2. 장치 열기
                outDevice.open();
                Receiver receiver = outDevice.getReceiver();

                // 3. MIDI 메시지 전송 (예: Note On - 60번 건반)
                ShortMessage myMsg = new ShortMessage(); 
                myMsg.setMessage(ShortMessage.NOTE_ON, 0, 36, 93);
                receiver.send(myMsg, -1);
                
                System.out.println("메시지 전송 완료");

                // 종료 전 대기 및 닫기
                Thread.sleep(2000);
                outDevice.close();
            } else {
                System.out.println("해당 MIDI 포트를 찾을 수 없습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}