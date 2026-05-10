
# My Chat - Gemini Local Interface

A premium, standalone chat interface for your local Gemini server. Designed to be hosted on **Cloudflare Pages**.

## 🚀 Deployment (The Fast Way)
1.  **GitHub**: Create a new repository on GitHub named `my-chat`.
2.  **Upload**: Upload the files in this folder (`index.html`, `style.css`, `script.js`) to that repository.
3.  **Cloudflare Pages**:
    *   Go to your Cloudflare Dashboard.
    *   Navigate to **Workers & Pages** -> **Create application** -> **Pages**.
    *   Connect your GitHub account and select the `my-chat` repository.
    *   Click **Begin setup** -> **Save and Deploy**.

## 🔗 How to Connect
1.  Start your local Gemini server using `RUN_ALL.bat`.
2.  Open your Cloudflare Pages URL (e.g., `https://my-chat.pages.dev`).
3.  Enter your **Cloudflare Tunnel URL** and your **API Key**.
4.  Start chatting!

## 🛡️ Security
This app connects directly from your browser to your local machine via the Cloudflare Tunnel. Your API Key is stored only in your local browser's storage.
