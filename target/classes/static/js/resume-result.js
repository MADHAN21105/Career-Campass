document.addEventListener('DOMContentLoaded', () => {
    // 1. Read data
    console.log('Loading results page (Dashboard Mode)...');
    const rawData = localStorage.getItem('analysisResults');
    if (!rawData) {
        console.warn('No analysis data found in localStorage');
        document.getElementById('summaryText').textContent = 'No analysis data found. Please upload a resume first.';
        return;
    }
    const data = JSON.parse(rawData);
    console.log('Analysis Data Loaded:', data);

    // ==========================================
    // SCORE & BADGE
    // ==========================================
    const scoreVal = Math.round(data.score || 0);
    const scoreNumberEl = document.getElementById('scoreNumber');
    const scoreRingProgress = document.getElementById('scoreRingProgress');
    const scoreLabel = document.getElementById('scoreLabel');
    const scoreBadge = document.getElementById('scoreBadge');

    if (scoreNumberEl) scoreNumberEl.textContent = `${scoreVal}%`;
    if (scoreLabel) scoreLabel.textContent = data.matchLevel || (scoreVal > 70 ? 'Match' : 'Mismatch');

    // Ring Animation
    if (scoreRingProgress) {
        const circumference = 2 * Math.PI * 36;
        const offset = circumference - (scoreVal / 100) * circumference;
        setTimeout(() => { scoreRingProgress.style.strokeDashoffset = offset; }, 100);

        // Color classes
        scoreRingProgress.classList.remove('medium', 'weak');
        scoreBadge.classList.remove('strong', 'medium', 'weak');

        if (scoreVal >= 75) {
            scoreBadge.classList.add('strong');
        } else if (scoreVal >= 50) {
            scoreBadge.classList.add('medium');
            scoreRingProgress.classList.add('medium');
        } else {
            scoreBadge.classList.add('weak');
            scoreRingProgress.classList.add('weak');
        }
    }

    // ==========================================
    // SUMMARY TEXT
    // ==========================================
    const summaryEl = document.getElementById('summaryText');
    if (summaryEl) summaryEl.textContent = 'Analysis complete. Review your details below.';

    // ==========================================
    // SKILLS LISTS
    // ==========================================
    const matchedTags = document.getElementById('matchedTags');
    if (matchedTags) {
        matchedTags.innerHTML = '';
        (data.matchedSkills || []).forEach(skill => {
            const span = document.createElement('span');
            span.className = 'tag tag-matched';
            span.textContent = skill;
            matchedTags.appendChild(span);
        });
        if (!data.matchedSkills?.length) matchedTags.innerHTML = '<span class="tag">No matched skills</span>';
    }

    const missingTags = document.getElementById('missingTags');
    if (missingTags) {
        missingTags.innerHTML = '';
        (data.missingSkills || []).forEach(skill => {
            const span = document.createElement('span');
            span.className = 'tag tag-missing';
            span.textContent = skill;
            missingTags.appendChild(span);
        });
        if (!data.missingSkills?.length) missingTags.innerHTML = '<span class="tag">No gaps found</span>';
    }

    // ==========================================
    // PRO TIPS (SINGLE BOX)
    // ==========================================
    const tipTextEl = document.getElementById('tipText');
    if (tipTextEl) {
        let content = '';

        // Prioritize: Granular Data -> Raw Tip -> Default
        if (data.summary || data.strength || data.improvementArea || data.recommendation) {
            // Format granular data nicely
            content = '<div style="display: flex; flex-direction: column; gap: 6px;">';
            if (data.summary) content += `<div>${data.summary}</div>`;
            if (data.strength) content += `<div><strong>Strength:</strong> ${data.strength}</div>`;
            if (data.improvementArea) content += `<div><strong>Improvement:</strong> ${data.improvementArea}</div>`;
            if (data.recommendation) content += `<div><strong>Recommendation:</strong> ${data.recommendation}</div>`;
            content += '</div>';
        } else if (data.tip) {
            // Clean up prefix if present and handle newlines
            content = data.tip.replace(/^Pro Tip:\s*/i, '').replace(/\n/g, '<br>');
        } else {
            content = 'No specific tips generated for this resume.';
        }

        tipTextEl.innerHTML = content;
    }

    // ==========================================
    // IMPROVEMENT AREAS (RIGHT PANEL)
    // ==========================================
    const areasList = document.getElementById('areasList');
    if (areasList) {
        areasList.innerHTML = '';
        (data.resumeImprovementTips || []).forEach(tip => {
            const li = document.createElement('li');
            li.textContent = tip;
            areasList.appendChild(li);
        });
    }

    const recoList = document.getElementById('recoList');
    if (recoList) {
        recoList.innerHTML = '';
        (data.skillImprovementTips || []).forEach(tip => {
            const li = document.createElement('li');
            li.textContent = tip;
            recoList.appendChild(li);
        });
    }
});
