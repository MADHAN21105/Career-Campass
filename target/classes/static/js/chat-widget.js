class ChatWidget {
    constructor() {
        this.isOpen = false;
        this.init();
    }

    init() {
        this.createStyles();
        this.createDOM();
        this.attachEvents();
    }

    createStyles() {
        const style = document.createElement('style');
        style.textContent = `
            .chat-widget-container {
                position: fixed;
                bottom: 24px;
                right: 24px;
                z-index: 9999;
                font-family: 'Inter', sans-serif;
            }

            .chat-fab {
                width: 60px;
                height: 60px;
                border-radius: 50%;
                background: #6366f1; /* Primary Purple */
                box-shadow: 0 4px 14px rgba(99, 102, 241, 0.4);
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.2s ease;
                color: white;
            }

            .chat-fab:hover {
                transform: scale(1.05);
                box-shadow: 0 6px 20px rgba(99, 102, 241, 0.6);
            }

            .chat-fab:active {
                transform: scale(0.95);
            }

            .chat-window {
                position: absolute;
                bottom: 80px;
                right: 0;
                width: 350px;
                height: 550px;
                background: white;
                border-radius: 16px;
                box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
                display: flex;
                flex-direction: column;
                opacity: 0;
                pointer-events: none;
                transform: translateY(20px) scale(0.95);
                transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
                overflow: hidden;
                border: 1px solid #e2e8f0;
            }

            .chat-window.open {
                opacity: 1;
                pointer-events: auto;
                transform: translateY(0) scale(1);
            }

            .chat-header {
                padding: 16px 20px;
                background: #6366f1; /* Solid Primary Purple */
                color: white;
                display: flex;
                align-items: center;
                justify-content: space-between;
            }

            .chat-header h3 {
                margin: 0;
                font-weight: 700;
                font-size: 16px;
            }
            
            .close-btn {
                background: transparent;
                border: none;
                color: rgba(255,255,255,0.8);
                cursor: pointer;
            }
            .close-btn:hover { color: white; }

            .chat-messages {
                flex: 1;
                padding: 20px;
                overflow-y: auto;
                display: flex;
                flex-direction: column;
                gap: 12px;
                background: #f9fafb;
            }

            .message {
                max-width: 85%;
                padding: 12px 16px;
                border-radius: 12px;
                font-size: 14px;
                line-height: 1.5;
            }

            .message.user {
                align-self: flex-end;
                background: #6366f1;
                color: white;
                border-bottom-right-radius: 4px;
            }

            .message.bot {
                align-self: flex-start;
                background: white;
                color: #1e293b;
                border: 1px solid #e2e8f0;
                border-bottom-left-radius: 4px;
                box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            }

            .message.loading {
                display: flex;
                gap: 4px;
                padding: 14px;
            }

            .dot {
                width: 6px;
                height: 6px;
                background: #94a3b8;
                border-radius: 50%;
                animation: bounce 1.4s infinite ease-in-out;
            }
            
            .dot:nth-child(1) { animation-delay: -0.32s; }
            .dot:nth-child(2) { animation-delay: -0.16s; }

            @keyframes bounce {
                0%, 80%, 100% { transform: scale(0); }
                40% { transform: scale(1); }
            }

            .chat-input-area {
                padding: 16px;
                background: white;
                border-top: 1px solid #e2e8f0;
                display: flex;
                gap: 10px;
                align-items: center;
            }

            .chat-input {
                flex: 1;
                padding: 10px 14px;
                border: 1px solid #e2e8f0;
                border-radius: 8px;
                outline: none;
                font-size: 14px;
                transition: border-color 0.2s;
            }

            .chat-input:focus {
                border-color: #6366f1;
            }

            .send-btn {
                background: #6366f1;
                color: white;
                border: none;
                width: 40px;
                height: 40px;
                border-radius: 8px;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: background 0.2s;
            }

            .send-btn:hover {
                background: #4f46e5;
            }

            .suggested-questions {
                display: flex;
                flex-direction: column;
                gap: 8px;
                margin-top: 4px;
            }

            .suggested-question-btn {
                display: flex;
                gap: 12px;
                padding: 12px;
                background: white;
                border: 1px solid #e2e8f0;
                border-radius: 12px;
                text-align: left;
                font-size: 13px;
                color: #334155;
                cursor: pointer;
                transition: all 0.2s;
                align-items: flex-start;
            }

            .suggested-question-btn:hover {
                background: #fff;
                border-color: #6366f1;
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
            }
            
            .suggested-question-btn i {
                color: #94a3b8;
                margin-top: 2px;
                transition: color 0.2s;
            }
            
            .suggested-question-btn:hover i {
                color: #6366f1;
            }
        `;
        document.head.appendChild(style);
    }

    createDOM() {
        // Container
        this.container = document.createElement('div');
        this.container.className = 'chat-widget-container';

        // Chat Window
        const windowHTML = `
            <div class="chat-window">
                <div class="chat-header">
                    <div class="flex items-center gap-2">
                        <i data-lucide="bot" class="w-6 h-6"></i>
                        <h3>AI Career Coach</h3>
                    </div>
                    <button class="close-btn">
                        <i data-lucide="x" class="w-5 h-5"></i>
                    </button>
                </div>
                
                <div class="chat-messages">
                    <div class="message bot">
                        Hi! I'm your AI Career Coach. Ask me anything about your resume or the job description!
                    </div>
                    
                    <!-- Suggested Questions -->
                    <div class="suggested-questions">
                        <p class="text-xs font-medium text-gray-500 mb-2">Common questions:</p>
                        
                        <button class="suggested-question-btn" data-question="I'm not able to upload my resume. What do I do?">
                            <i data-lucide="help-circle" class="w-4 h-4"></i>
                            <span>I'm not able to upload my resume. What do I do?</span>
                        </button>
                        
                        <button class="suggested-question-btn" data-question="My resume was not read correctly. What do I do?">
                            <i data-lucide="file-warning" class="w-4 h-4"></i>
                            <span>My resume was not read correctly. What do I do?</span>
                        </button>
                        
                        <button class="suggested-question-btn" data-question="I don't have any work experience. What should I do?">
                            <i data-lucide="briefcase" class="w-4 h-4"></i>
                            <span>I don't have any work experience. What should I do?</span>
                        </button>
                    </div>
                </div>
                
                <div class="chat-input-area">
                    <input type="text" class="chat-input" placeholder="Type a message..." />
                    <button class="send-btn">
                        <i data-lucide="send" class="w-5 h-5"></i>
                    </button>
                </div>
            </div>
        `;

        // Floating Action Button
        const fabHTML = `
            <div class="chat-fab">
                <i data-lucide="message-square" class="w-7 h-7"></i>
            </div>
        `;

        this.container.innerHTML = windowHTML + fabHTML;
        document.body.appendChild(this.container);

        // Cache elements
        this.chatWindow = this.container.querySelector('.chat-window');
        this.fab = this.container.querySelector('.chat-fab');
        this.messagesContainer = this.container.querySelector('.chat-messages');
        this.input = this.container.querySelector('.chat-input');
        this.sendBtn = this.container.querySelector('.send-btn');
        this.closeBtn = this.container.querySelector('.close-btn');

        // Re-init icons inside widget
        if (window.lucide) lucide.createIcons();
    }

    attachEvents() {
        // Toggle Open/Close
        this.fab.addEventListener('click', () => this.toggle());
        this.closeBtn.addEventListener('click', () => this.toggle());

        // Send Message
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        this.input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.sendMessage();
        });

        // Suggested Questions
        const suggestedBtns = this.container.querySelectorAll('.suggested-question-btn');
        suggestedBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const question = btn.getAttribute('data-question');
                this.input.value = question;
                this.sendMessage();
                // Hide suggested questions after first use
                const suggestedContainer = this.container.querySelector('.suggested-questions');
                if (suggestedContainer) {
                    suggestedContainer.style.display = 'none';
                }
            });
        });
    }

    toggle() {
        this.isOpen = !this.isOpen;
        this.chatWindow.classList.toggle('open', this.isOpen);
        if (this.isOpen) {
            setTimeout(() => this.input.focus(), 100);
        }
    }

    addMessage(text, isUser = false) {
        const msg = document.createElement('div');
        msg.className = `message ${isUser ? 'user' : 'bot'}`;
        msg.textContent = text;
        this.messagesContainer.appendChild(msg);
        this.scrollToBottom();
    }

    addLoading() {
        const loader = document.createElement('div');
        loader.className = 'message bot loading';
        loader.id = 'chat-loader';
        loader.innerHTML = '<div class="dot"></div><div class="dot"></div><div class="dot"></div>';
        this.messagesContainer.appendChild(loader);
        this.scrollToBottom();
    }

    removeLoading() {
        const loader = document.getElementById('chat-loader');
        if (loader) loader.remove();
    }

    scrollToBottom() {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    getFAQResponse(question) {
        const faqs = {
            "I'm not able to upload my resume. What do I do?":
                "If you're having trouble uploading your resume, try these steps:\n\n1. Make sure your file is in PDF, DOCX, DOC, or TXT format\n2. Ensure the file size is under 5MB\n3. Try using the 'Paste Text' tab instead and copy-paste your resume content\n4. Clear your browser cache and try again\n5. If the issue persists, try a different browser\n\nNeed more help? Feel free to ask!",

            "My resume was not read correctly. What do I do?":
                "If your resume wasn't read correctly, here's what you can do:\n\n1. Use the 'Paste Text' option - this often works better than file upload\n2. Make sure your resume has selectable text (not a scanned image)\n3. Avoid complex formatting, tables, or multi-column layouts\n4. Convert your resume to plain text or a simple PDF format\n5. Remove any special characters or unusual fonts\n\nFor best results, use a clean, ATS-friendly resume format!",

            "I don't have any work experience. What should I do?":
                "No work experience? No problem! Here's what you can do:\n\n1. **Include relevant coursework** and academic projects\n2. **Add internships or volunteer work** - they count as experience\n3. **Highlight transferable skills** like communication, teamwork, or problem-solving\n4. **Include certifications** or online courses you've completed\n5. **Focus on your education** and any academic achievements\n6. **Add personal projects** or portfolio work\n\nRemember: Everyone starts somewhere, and employers understand that!"
        };

        // Check for exact match first
        if (faqs[question]) {
            return faqs[question];
        }

        // Check for partial match
        for (const [key, value] of Object.entries(faqs)) {
            if (question.toLowerCase().includes(key.toLowerCase().substring(0, 20))) {
                return value;
            }
        }

        return null;
    }

    async sendMessage() {
        const text = this.input.value.trim();
        if (!text) return;

        // 1. UI Update
        this.addMessage(text, true);
        this.input.value = '';
        this.addLoading();

        // Check for FAQ questions first
        const faqResponse = this.getFAQResponse(text);
        if (faqResponse) {
            this.removeLoading();
            this.addMessage(faqResponse);
            return;
        }

        // 2. Prepare Context (from localStorage)
        let context = {};
        try {
            // "resumeText" and "jobDescription" are usually sent in /api/analyze body
            // BUT here we only saved the RESULT in 'analysisResults'.
            // The result (AnalysisResponse) contains "jdSkills" and "resumeSkills" but NOT the full text.
            // Oh, wait, the user's `resume-analysis.js` logic did NOT save the raw text to localStorage.
            // It only saved `analysisResults` (the response).

            // However, the resume text IS available if we are on the analysis page (in the textarea).
            // On the result page, we might have lost it if we didn't persist it.
            // Let's check `resume-analysis.js`... ah, it only saved `analysisResults`.
            // The `AnalysisResponse` DTO structure does NOT return the full text back.

            // This is a limitation. For now, I will send what I can, or empty strings.
            // Ideally, we should update `resume-analysis.js` to save the raw inputs to localStorage too.
            // I'll add a quick fix to `resume-analysis.js` later if needed.
            // For now, let's try to pull from localStorage if I modify `resume-analysis.js` to save it.

            // Let's assume we will have `analysisContext` in localStorage.
            const storedContext = localStorage.getItem('analysisContext');
            if (storedContext) {
                context = JSON.parse(storedContext);
            }
        } catch (e) {
            console.error('Error reading context', e);
        }

        try {
            // 3. API Call
            const res = await fetch('http://localhost:8080/api/ask', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    question: text,
                    resumeText: context.resumeText || '',
                    jobDescription: context.jobDescription || ''
                })
            });

            if (!res.ok) throw new Error('API Error');

            const data = await res.json();

            // 4. Show Response
            this.removeLoading();
            this.addMessage(data.answer);

        } catch (err) {
            this.removeLoading();
            this.addMessage("Sorry, I'm having trouble connecting to the server. Please try again.");
            console.error(err);
        }
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    new ChatWidget();
});
