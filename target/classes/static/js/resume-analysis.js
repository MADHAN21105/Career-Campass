document.addEventListener('DOMContentLoaded', () => {
    // --- File Uploader Logic ---
    const fileInput = document.getElementById('file-input');
    const uploadArea = document.getElementById('upload-area');
    const fileListForUpload = document.getElementById('uploaded-files-list');
    const clearAllBtn = document.getElementById('clear-all-btn');
    const totalFilesCount = document.getElementById('total-files-count');
    const totalSizeDisplay = document.getElementById('total-size-display');
    const statsContainer = document.getElementById('stats-container');
    const chooseFilesBtn = document.getElementById('choose-files-btn');
    let uploadedFiles = [];

    if (fileInput && uploadArea) {
        // Handle click on custom button
        chooseFilesBtn.addEventListener('click', () => {
            fileInput.click();
        });

        // Handle File Selection
        fileInput.addEventListener('change', (e) => {
            handleFiles(e.target.files);
        });

        // Drag & Drop Events
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            uploadArea.addEventListener(eventName, preventDefaults, false);
        });

        function preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }

        ['dragenter', 'dragover'].forEach(eventName => {
            uploadArea.addEventListener(eventName, highlight, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            uploadArea.addEventListener(eventName, unhighlight, false);
        });

        function highlight(e) {
            uploadArea.classList.add('bg-blue-50', 'border-primary');
            uploadArea.classList.remove('border-gray-300');
        }

        function unhighlight(e) {
            uploadArea.classList.remove('bg-blue-50', 'border-primary');
            uploadArea.classList.add('border-gray-300');
        }

        uploadArea.addEventListener('drop', handleDrop, false);

        function handleDrop(e) {
            const dt = e.dataTransfer;
            const files = dt.files;
            handleFiles(files);
        }

        function handleFiles(files) {
            const newFiles = Array.from(files).map(file => ({
                id: Date.now() + Math.random(),
                name: file.name,
                size: file.size,
                type: file.type,
                file: file,
                uploadedAt: new Date()
            }));

            uploadedFiles = [...uploadedFiles, ...newFiles];
            updateUI();
        }

        // --- UI Updates ---
        function updateUI() {
            // Update Stats
            if (statsContainer) {
                if (uploadedFiles.length > 0) {
                    statsContainer.classList.remove('hidden');
                } else {
                    statsContainer.classList.add('hidden');
                }
            }
            if (totalFilesCount) totalFilesCount.textContent = uploadedFiles.length;
            if (totalSizeDisplay) totalSizeDisplay.textContent = formatFileSize(uploadedFiles.reduce((acc, file) => acc + file.size, 0));

            // Update List
            renderFileList();
        }

        function renderFileList() {
            if (!fileListForUpload) return;
            fileListForUpload.innerHTML = '';

            // Show header if list not empty
            if (uploadedFiles.length > 0) {
                const header = document.createElement('h4');
                header.className = 'text-lg font-semibold mb-4';
                header.textContent = `Uploaded Files (${uploadedFiles.length})`;
                fileListForUpload.appendChild(header);
            }

            uploadedFiles.forEach(file => {
                const fileItem = document.createElement('div');
                fileItem.className = 'flex items-center gap-4 p-4 rounded-xl border border-gray-200 bg-gray-50 transition-all duration-200';

                const icon = getFileIcon(file.name);

                fileItem.innerHTML = `
                    <div class="flex-shrink-0 text-gray-500">
                        ${icon}
                    </div>
                    <div class="flex-1 min-w-0">
                        <p class="font-medium truncate text-gray-900">${file.name}</p>
                        <div class="flex items-center gap-4 mt-1 text-sm text-gray-500">
                            <span>${formatFileSize(file.size)}</span>
                            <span>•</span>
                            <span>${file.uploadedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                        </div>
                    </div>
                    <div class="flex items-center gap-2">
                         <button class="remove-btn p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors" data-id="${file.id}">
                            <i data-lucide="x" class="w-4 h-4"></i>
                        </button>
                    </div>
                `;
                fileListForUpload.appendChild(fileItem);
            });

            // Re-initialize icons for new elements
            if (window.lucide) lucide.createIcons();

            // Attach listeners to remove buttons
            document.querySelectorAll('.remove-btn').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const id = parseFloat(e.currentTarget.dataset.id);
                    removeFile(id);
                });
            });
        }

        function removeFile(id) {
            uploadedFiles = uploadedFiles.filter(f => f.id !== id);
            updateUI();
        }

        if (clearAllBtn) {
            clearAllBtn.addEventListener('click', () => {
                uploadedFiles = [];
                updateUI();
            });
        }
    }


    // --- Resume Tabs Logic (Upload vs Paste) ---
    const resumeTabUpload = document.getElementById('resume-tab-upload');
    const resumeTabPaste = document.getElementById('resume-tab-paste');
    const resumeModeUpload = document.getElementById('resume-mode-upload');
    const resumeModePaste = document.getElementById('resume-mode-paste');

    let resumeMode = 'upload'; // 'upload' or 'paste'

    if (resumeTabUpload && resumeTabPaste && resumeModeUpload && resumeModePaste) {
        resumeTabUpload.addEventListener('click', () => switchResumeTab('upload'));
        resumeTabPaste.addEventListener('click', () => switchResumeTab('paste'));

        function switchResumeTab(mode) {
            resumeMode = mode;
            if (mode === 'upload') {
                resumeTabUpload.className = "px-3 py-1.5 text-sm font-medium rounded-md bg-white shadow-sm text-primary transition-all";
                resumeTabPaste.className = "px-3 py-1.5 text-sm font-medium rounded-md text-muted-foreground hover:text-gray-700 transition-all";
                resumeModeUpload.classList.remove('hidden');
                resumeModePaste.classList.add('hidden');
            } else {
                resumeTabPaste.className = "px-3 py-1.5 text-sm font-medium rounded-md bg-white shadow-sm text-primary transition-all";
                resumeTabUpload.className = "px-3 py-1.5 text-sm font-medium rounded-md text-muted-foreground hover:text-gray-700 transition-all";
                resumeModePaste.classList.remove('hidden');
                resumeModeUpload.classList.add('hidden');
            }
        }
    }

    // --- Helpers ---
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    function getFileIcon(fileName) {
        // Simplified icon logic, using Lucide icon names class
        // In real implementations, you'd return the SVG string or element
        return '<i data-lucide="file-text" class="w-6 h-6"></i>';
    }
    // --- Analysis Logic ---
    const analyzeBtn = document.getElementById('analyze-btn');

    if (analyzeBtn) {
        new ShatterButton(analyzeBtn, {
            shatterColor: '#6366f1',
            onClick: async () => {
                // 1. Gather Data
                const jdText = document.getElementById('jd-text-input')?.value || '';

                if (!jdText.trim()) {
                    alert('Please enter a job description.');
                    // Reset ShatterButton if validation fails
                    return;
                }

                let resumeTextToAnalyze = "";

                // Determine Resume Source
                if (resumeMode === 'paste') {
                    const pastedText = document.getElementById('resume-text-input')?.value || "";
                    if (!pastedText.trim()) {
                        alert('Please paste your resume text.');
                        return;
                    }
                    resumeTextToAnalyze = pastedText;
                } else {
                    if (uploadedFiles.length === 0) {
                        alert('Please upload a resume.');
                        return;
                    }
                }

                // --- PROGRESS UI LOGIC (Rock Solid) ---
                const progressContainer = document.getElementById('analysis-progress');
                const steps = ['step-1', 'step-2', 'step-3', 'step-4'];
                let completedSteps = new Set();
                let activeTimeouts = [];

                const updateStepUI = (stepId, status) => {
                    const stepEl = document.getElementById(stepId);
                    if (!stepEl) return;

                    const iconContainer = stepEl.querySelector('.step-icon');
                    const textSpan = stepEl.querySelector('span');

                    if (status === 'active') {
                        stepEl.classList.remove('opacity-40');
                        if (textSpan) {
                            textSpan.classList.remove('text-slate-500');
                            textSpan.classList.add('text-slate-900', 'font-bold');
                        }
                        if (iconContainer) {
                            iconContainer.classList.add('ring-2', 'ring-indigo-100');
                            iconContainer.innerHTML = '<i data-lucide="loader-2" class="w-4 h-4 animate-spin text-indigo-600"></i>';
                        }
                    } else if (status === 'done') {
                        completedSteps.add(stepId);
                        stepEl.classList.remove('opacity-40');
                        if (textSpan) {
                            textSpan.classList.remove('text-slate-900', 'font-bold');
                            textSpan.classList.add('text-slate-500', 'font-medium');
                        }
                        if (iconContainer) {
                            iconContainer.classList.remove('bg-indigo-50', 'text-indigo-600', 'ring-2', 'ring-indigo-100');
                            iconContainer.classList.add('bg-indigo-600', 'text-white', 'border-indigo-600');
                            iconContainer.innerHTML = '<i data-lucide="check" class="w-4 h-4"></i>';
                        }
                    }
                    if (window.lucide) lucide.createIcons();
                };

                // Reveal Container
                if (progressContainer) {
                    progressContainer.classList.remove('hidden');
                    progressContainer.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }

                // Start Fake Progressive Reveal
                const startFakeProgress = () => {
                    const delays = [0, 2000, 4500, 7000];
                    steps.forEach((stepId, i) => {
                        const t = setTimeout(() => {
                            if (!completedSteps.has(stepId)) {
                                updateStepUI(stepId, 'active');
                                // Mark previous as done
                                if (i > 0 && !completedSteps.has(steps[i - 1])) {
                                    updateStepUI(steps[i - 1], 'done');
                                }
                            }
                        }, delays[i]);
                        activeTimeouts.push(t);
                    });
                };

                startFakeProgress();

                try {
                    // 2. If Upload Mode -> Upload Resume First
                    if (resumeMode === 'upload') {
                        const resumeFile = uploadedFiles[0].file;
                        const formData = new FormData();
                        formData.append('file', resumeFile);

                        const uploadRes = await fetch(`${CONFIG.API_BASE_URL}/api/upload-resume`, {
                            method: 'POST',
                            body: formData
                        });

                        if (!uploadRes.ok) throw new Error('Resume upload failed');
                        const uploadData = await uploadRes.json();
                        resumeTextToAnalyze = uploadData.text;
                    }

                    // 3. Analyze
                    const analyzeRes = await fetch(`${CONFIG.API_BASE_URL}/api/analyze`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            resumeText: resumeTextToAnalyze,
                            jobDescription: jdText
                        })
                    });

                    if (!analyzeRes.ok) throw new Error('Analysis failed');
                    const analysisData = await analyzeRes.json();

                    // 4. Cleanup & Save
                    localStorage.setItem('analysisResults', JSON.stringify(analysisData));
                    localStorage.setItem('analysisContext', JSON.stringify({
                        resumeText: resumeTextToAnalyze,
                        jobDescription: jdText
                    }));

                    // Clear all timeouts and mark all as done
                    activeTimeouts.forEach(clearTimeout);
                    steps.forEach(id => updateStepUI(id, 'done'));

                    // Redirect
                    setTimeout(() => {
                        window.location.href = 'resume-result.html';
                    }, 1200);

                } catch (error) {
                    activeTimeouts.forEach(clearTimeout);
                    console.error('Analysis error:', error);
                    alert(`Analysis Error: ${error.message}`);
                    if (progressContainer) progressContainer.classList.add('hidden');
                }
            }
        });
    }
});
