
const setupOverlay = document.getElementById('setup-overlay');
const apiUrlInput = document.getElementById('api-url');
const apiKeyInput = document.getElementById('api-key');
const startBtn = document.getElementById('start-btn');
const chatContainer = document.getElementById('chat-messages');
const userInput = document.getElementById('user-input');
const sendBtn = document.getElementById('send-btn');
const settingsBtn = document.getElementById('settings-btn');

let config = {
    url: '',
    key: ''
};

// Load saved config
const saved = localStorage.getItem('gemini_chat_config');
if (saved) {
    const parsed = JSON.parse(saved);
    apiUrlInput.value = parsed.url;
    apiKeyInput.value = parsed.key;
}

startBtn.addEventListener('click', () => {
    config.url = apiUrlInput.value.trim().replace(/\/$/, "");
    config.key = apiKeyInput.value.trim();

    if (!config.url) {
        alert("Please enter your Tunnel URL");
        return;
    }

    localStorage.setItem('gemini_chat_config', JSON.stringify(config));
    setupOverlay.style.opacity = '0';
    setTimeout(() => setupOverlay.style.display = 'none', 300);
});

settingsBtn.addEventListener('click', () => {
    setupOverlay.style.display = 'flex';
    setTimeout(() => setupOverlay.style.opacity = '1', 10);
});

function appendMessage(role, text) {
    const div = document.createElement('div');
    div.className = `message ${role}`;
    div.textContent = text;
    chatContainer.appendChild(div);
    chatContainer.scrollTop = chatContainer.scrollHeight;
    return div;
}

async function sendMessage() {
    const text = userInput.value.trim();
    if (!text) return;

    userInput.value = '';
    userInput.style.height = 'auto';
    appendMessage('user', text);

    const assistantMsg = appendMessage('assistant', '...');
    
    try {
        const response = await fetch(`${config.url}/v1/chat/completions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${config.key}`
            },
            body: JSON.stringify({
                messages: [{ role: "user", content: text }],
                model: "gemini-pro-browser"
            })
        });

        if (!response.ok) {
            throw new Error(`Server returned ${response.status}`);
        }

        const data = await response.json();
        assistantMsg.textContent = data.choices[0].message.content;
    } catch (error) {
        assistantMsg.textContent = `Error: ${error.message}. Make sure your local server is running and the Tunnel URL is correct.`;
        assistantMsg.style.color = '#ef4444';
    }
}

sendBtn.addEventListener('click', sendMessage);

userInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

userInput.addEventListener('input', () => {
    userInput.style.height = 'auto';
    userInput.style.height = userInput.scrollHeight + 'px';
});
