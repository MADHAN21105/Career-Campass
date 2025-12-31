document.addEventListener('DOMContentLoaded', () => {
    // ==========================================
    // COVER LETTER GENERATOR PAGE LOGIC
    // ==========================================
    const generateBtn = document.getElementById('generate-btn');

    if (generateBtn) {
        generateBtn.addEventListener('click', async () => {
            // 1. Gather Data
            const fullName = document.getElementById('fullName').value.trim();
            const email = document.getElementById('email').value.trim();
            const phone = document.getElementById('phone').value.trim();
            const companyName = document.getElementById('companyName').value.trim();
            const jobTitle = document.getElementById('jobTitle').value.trim();
            const hiringManager = document.getElementById('hiringManager').value.trim();
            const jobDescription = document.getElementById('jobDescription').value.trim();

            // 2. Simple Validation
            if (!fullName || !email || !companyName || !jobTitle || !jobDescription) {
                alert('Please fill in all required fields (Name, Email, Company, Job Title, Job Description).');
                return;
            }

            // 3. UI Loading State
            const originalText = generateBtn.innerHTML;
            generateBtn.disabled = true;
            generateBtn.innerHTML = `<i data-lucide="loader-2" class="w-4 h-4 mr-2 animate-spin"></i> Generating...`;
            lucide.createIcons();

            try {
                // 4. API Call
                const response = await fetch(`${CONFIG.API_BASE_URL}/api/cover-letter/generate`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        fullName,
                        email,
                        phone,
                        companyName,
                        jobTitle,
                        hiringManager,
                        jobDescription
                    })
                });

                if (!response.ok) {
                    throw new Error(`Server returned ${response.status}: ${response.statusText}`);
                }

                const data = await response.json();

                // 5. Success Handling
                if (data.coverLetter) {
                    localStorage.setItem('generatedCoverLetter', data.coverLetter);
                    window.location.href = 'cover-letter-results.html';
                } else {
                    throw new Error('No cover letter returned');
                }

            } catch (error) {
                console.error('Error:', error);
                alert(`Failed to generate cover letter. Error: ${error.message}\n\nEnsure the backend is running.`);
                generateBtn.disabled = false;
                generateBtn.innerHTML = originalText;
                lucide.createIcons();
            }
        });
    }

    // ==========================================
    // COVER LETTER RESULTS PAGE LOGIC
    // ==========================================
    const letterContent = document.getElementById('letter-content');
    const copyBtn = document.getElementById('copy-btn');
    const downloadBtn = document.getElementById('download-btn');

    if (letterContent) {
        const key = 'generatedCoverLetter';
        const content = localStorage.getItem(key);

        if (content) {
            letterContent.textContent = content;
        } else {
            letterContent.innerHTML = '<p class="text-red-500">No cover letter found. Please go back and generate one.</p>';
        }

        // Copy Functionality
        if (copyBtn) {
            copyBtn.addEventListener('click', () => {
                if (!letterContent.textContent) return;

                navigator.clipboard.writeText(letterContent.textContent).then(() => {
                    const originalText = copyBtn.innerHTML;
                    copyBtn.innerHTML = `<i data-lucide="check" class="w-4 h-4 mr-2"></i> Copied!`;
                    lucide.createIcons();

                    setTimeout(() => {
                        copyBtn.innerHTML = originalText;
                        lucide.createIcons();
                    }, 2000);
                }).catch(err => {
                    console.error('Failed to copy text: ', err);
                });
            });
        }

        // Download PDF Functionality (html2pdf)
        if (downloadBtn) {
            downloadBtn.addEventListener('click', () => {
                if (!letterContent) return;

                const element = letterContent;
                const opt = {
                    margin: 1,
                    filename: 'Cover_Letter.pdf',
                    image: { type: 'jpeg', quality: 0.98 },
                    html2canvas: { scale: 2 },
                    jsPDF: { unit: 'in', format: 'letter', orientation: 'portrait' }
                };

                // Show loading state
                const originalText = downloadBtn.innerHTML;
                downloadBtn.innerHTML = `<i data-lucide="loader-2" class="w-4 h-4 mr-2 animate-spin"></i> Converting...`;
                lucide.createIcons();

                html2pdf().set(opt).from(element).save().then(() => {
                    downloadBtn.innerHTML = originalText;
                    lucide.createIcons();
                }).catch(err => {
                    console.error("PDF generation failed:", err);
                    alert("Failed to download PDF. Please try again.");
                    downloadBtn.innerHTML = originalText;
                    lucide.createIcons();
                });
            });
        }
    }
});
