const chatForm = document.getElementById('chat-form');
const userInput = document.getElementById('user-input');
const chatWindow = document.getElementById('chat-window');
const loadingIndicator = document.getElementById('loading-indicator');

chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const question = userInput.value.trim();
    if (!question) return;

    // 1. 사용자 메시지 추가
    addMessage(question, 'user');
    userInput.value = '';

    // 2. 대기 아이콘 표시
    loadingIndicator.classList.remove('hidden');
    scrollToBottom();

    try {
        const response = await fetch('/', {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
            body: question
        });

        if (!response.ok) throw new Error('서버 응답 오류');

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        let botMessageDiv = addMessage('', 'bot');
        loadingIndicator.classList.add('hidden');

        let accumulatedText = ""; // 전체 텍스트 축적용

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });

            // 3. 'data:' 접두사 제거 및 줄바꿈 처리
            // 여러 줄의 data: 가 한꺼번에 올 수 있으므로 한 줄씩 분리해서 처리합니다.
            const lines = chunk.split('\n');
            lines.forEach(line => {
                if (line.startsWith('data:')) {
                    const content = line.replace('data:', '').trim();
                    if (content) {
                        accumulatedText += content + " "; // 단어 사이에 공백 추가 (한글 가독성)
                    }
                } else if (line.trim() === 'data:') {
                    // 빈 data: 는 줄바꿈으로 처리
                    accumulatedText += "\n";
                }
            });

            // 4. 화면 업데이트 (단순 텍스트 업데이트)
            // 나중에 마크다운 라이브러리(marked.js 등)를 추가하면 더 예쁘게 보입니다.
            botMessageDiv.innerText = accumulatedText;
            scrollToBottom();
        }

    } catch (error) {
        console.error('Error:', error);
        addMessage('오류가 발생했습니다: ' + error.message, 'bot');
        loadingIndicator.classList.add('hidden');
    }
});

function addMessage(text, sender) {
    const div = document.createElement('div');
    div.classList.add('message', sender);
    div.style.whiteSpace = 'pre-wrap'; // 줄바꿈(\n)을 화면에 반영
    div.innerText = text;

    chatWindow.insertBefore(div, loadingIndicator);
    return div;
}

function scrollToBottom() {
    chatWindow.scrollTop = chatWindow.scrollHeight;
}