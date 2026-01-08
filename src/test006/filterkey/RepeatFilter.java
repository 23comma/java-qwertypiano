package test006.filterkey;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

class RepeatFilter extends DocumentFilter {
    private String lastText = "";

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) 
            throws BadLocationException {
        
        long currentTime = System.currentTimeMillis();
        
        // 이전 입력과 동일한 문자이면서 설정한 시간(THRESHOLD)보다 빨리 입력된 경우 차단
     // 1. 입력하려는 문자(text)가 바로 직전 문자(lastText)와 동일한지 확인
        if (text != null && text.equals(lastText)) {
            return; // 동일하면 아무 작업도 하지 않고 리턴 (차단)
        }

        // 2. 새로운 문자를 lastText에 저장
        lastText = text;

        // 3. 실제 Document에 반영
        super.replace(fb, offset, length, text, attrs);
    }
    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        lastText = ""; // 지우면 마지막 입력 기록 초기화
        super.remove(fb, offset, length);
    }
}