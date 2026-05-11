
const chatContainer = document.getElementById('chat-messages');
const userInput = document.getElementById('user-input');
const sendBtn = document.getElementById('send-btn');
const settingsBtn = document.getElementById('settings-btn');
const setupOverlay = document.getElementById('setup-overlay');

// HARDCODED CONFIG (Auto-updated by Manager)
const config = {
    url: 'https://among-focuses-earned-evaluated.trycloudflare.com',
    key: 'gemini_secret_123'
};

// Instantly hide the setup overlay
setupOverlay.style.display = 'none';

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
        assistantMsg.textContent = `Error: ${error.message}. Ensure your RUN_ALL.bat and Cloudflare Tunnel are active.`;
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

settingsBtn.addEventListener('click', () => {
    setupOverlay.style.display = 'flex';
    setupOverlay.style.opacity = '1';
});

