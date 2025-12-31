document.addEventListener('DOMContentLoaded', () => {
    // 1. Read data
    console.log('Loading results page...');
    const rawData = localStorage.getItem('analysisResults');
    if (!rawData) {
        console.warn('No analysis data found in localStorage');
        const summaryText = document.getElementById('summaryText');
        if (summaryText) summaryText.textContent = 'No analysis data found. Please upload a resume first.';
        return;
    }
    const data = JSON.parse(rawData);
    console.log('Analysis Data Loaded:', data);

    // ==========================================
    // SCORE & BADGE
    // ==========================================
    animateScore(data.score || 0, data.matchLevel);

    // ==========================================
    // SKILLS LISTS
    // ==========================================
    const matchedSkills = data.matchedSkills || [];
    const missingSkills = data.missingSkills || [];

    const matchedTags = document.getElementById('matchedTags');
    if (matchedTags) {
        matchedTags.innerHTML = matchedSkills.length ?
            matchedSkills.map(s => `<span class="tag tag-matched">${s}</span>`).join('') :
            '<span class="tag">No matched skills detected</span>';
    }

    const missingTags = document.getElementById('missingTags');
    if (missingTags) {
        missingTags.innerHTML = missingSkills.length ?
            missingSkills.map(s => `<span class="tag tag-missing">${s}</span>`).join('') :
            '<span class="tag">No gaps identified</span>';
    }

    // ==========================================
    // JOB MATCH BREAKDOWN
    // ==========================================
    const matchBreakdown = document.getElementById('matchBreakdown');
    if (matchBreakdown) {
        const matched = new Set((data.matchedSkills || []).map(s => s.toLowerCase()));

        const renderSkillsList = (skills) => {
            if (!skills || !skills.length) return '<div class="mini-tag" style="color:#94a3b8">No skills identified</div>';
            return skills.map(skill => {
                const isMatched = matched.has(skill.toLowerCase());
                return `<span class="mini-tag ${isMatched ? 'mini-tag-matched' : 'mini-tag-missing'}">${skill}</span>`;
            }).join('');
        };

        const getColorForScore = (score) => {
            return parseFloat(score) >= 50 ? '#10b981' : '#ef4444'; // Green if >= 50, Red if < 50
        };

        const mandMatch = data.mandatoryTotalCount > 0
            ? ((data.mandatoryMatchedCount / data.mandatoryTotalCount) * 100).toFixed(2)
            : "100.00";
        const prefMatch = data.preferredTotalCount > 0
            ? ((data.preferredMatchedCount / data.preferredTotalCount) * 100).toFixed(2)
            : "100.00";
        const overallMatch = data.overallTotalCount > 0
            ? ((data.overallMatchedCount / data.overallTotalCount) * 100).toFixed(2)
            : "100.00";
        const eduScore = (data.educationScore || 0).toFixed(2);

        matchBreakdown.innerHTML = `
            <div class="section-title" style="color: #4f46e5; margin-top: 20px;">
                👉 Job Match Breakdown
            </div>
            <div class="breakdown-container">
                <!-- 1. MANDATORY SKILLS -->
                <div class="breakdown-item" onclick="toggleBreakdownList(this)">
                    <div class="breakdown-header">
                        <span class="breakdown-label"><i class="bi bi-shield-check"></i> Mandatory Skills:</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span class="breakdown-value">${data.mandatoryMatchedCount || 0}/${data.mandatoryTotalCount || 0} (${mandMatch}%)</span>
                            <i class="bi bi-chevron-right breakdown-arrow" style="color: ${getColorForScore(mandMatch)}"></i>
                        </div>
                    </div>
                    <div class="breakdown-skills-list">
                        ${renderSkillsList(data.mandatorySkills)}
                    </div>
                </div>

                <!-- 2. PREFERRED SKILLS -->
                <div class="breakdown-item" onclick="toggleBreakdownList(this)">
                    <div class="breakdown-header">
                        <span class="breakdown-label"><i class="bi bi-star"></i> Preferred Skills:</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span class="breakdown-value">${data.preferredMatchedCount || 0}/${data.preferredTotalCount || 0} (${prefMatch}%)</span>
                            <i class="bi bi-chevron-right breakdown-arrow" style="color: ${getColorForScore(prefMatch)}"></i>
                        </div>
                    </div>
                    <div class="breakdown-skills-list">
                        ${renderSkillsList(data.preferredSkills)}
                    </div>
                </div>

                <!-- 3. EDUCATION MATCH -->
                <div class="breakdown-item" onclick="toggleBreakdownList(this)">
                    <div class="breakdown-header">
                        <span class="breakdown-label"><i class="bi bi-mortarboard"></i> Education Match (15%):</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span class="breakdown-value">${eduScore}%</span>
                            <i class="bi bi-chevron-right breakdown-arrow" style="color: ${getColorForScore(eduScore)}"></i>
                        </div>
                    </div>
                    <div class="breakdown-skills-list">
                        <div class="mini-tag mini-tag-matched" style="width: 100%; border-style: dashed;">
                            Requirement: ${data.educationRequirement || 'Not Specified'}
                        </div>
                    </div>
                </div>

                <!-- 4. OVERALL COVERAGE -->
                <div class="breakdown-item" onclick="toggleBreakdownList(this)">
                    <div class="breakdown-header">
                        <span class="breakdown-label"><i class="bi bi-cpu"></i> Overall Coverage:</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span class="breakdown-value">${data.overallMatchedCount || 0}/${data.overallTotalCount || 0} (${overallMatch}%)</span>
                            <i class="bi bi-chevron-right breakdown-arrow" style="color: ${getColorForScore(overallMatch)}"></i>
                        </div>
                    </div>
                    <div class="breakdown-skills-list">
                        ${renderSkillsList(data.jdSkills)}
                    </div>
                </div>
            </div>
        `;
    }

    // ==========================================
    // ==========================================
    // STRATEGIC ANALYSIS (4 PILLARS)
    // ==========================================
    const tipTextEl = document.getElementById('tipText');
    if (tipTextEl) {
        const pillars = [
            { label: "Overall Summary 📝", text: data.summary, color: "#4f46e5" },
            { label: "Top Strength 💪", text: data.strength, color: "#10b981" },
            { label: "Critical Gap ⚠️", text: data.improvementArea, color: "#ef4444" },
            { label: "Career Goal 🚀", text: data.recommendation, color: "#f59e0b" }
        ];

        let content = '<div style="font-weight: 800; font-size: 0.95rem; color: #1e293b; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;">' +
            '<i class="bi bi-rocket-takeoff"></i> Strategic Analysis Fit & Tips</div>';

        content += pillars.map(p => `
            <div class="pillar-card" style="border-left-color: ${p.color}">
                <div class="pillar-label">${p.label}</div>
                <div class="pillar-text">${p.text || "Analyzing..."}</div>
            </div>
        `).join('');

        tipTextEl.innerHTML = content;
    }

    // ==========================================
    // IMPROVEMENT AREAS (RIGHT PANEL)
    // ==========================================
    const areasList = document.getElementById('areasList');
    const finalResumeTips = data.resumeTips || data.resumeImprovementTips || [];
    if (areasList) {
        areasList.innerHTML = finalResumeTips.length ?
            finalResumeTips.map(Tip => `<li>${Tip}</li>`).join('') :
            '<li style="color:var(--text-muted)">No specific resume tips generated.</li>';
    }

    const recoList = document.getElementById('recoList');
    const finalSkillTips = data.skillTips || data.skillImprovementTips || [];
    if (recoList) {
        recoList.innerHTML = finalSkillTips.length ?
            finalSkillTips.map(Tip => `<li>${Tip}</li>`).join('') :
            '<li style="color:var(--text-muted)">No specific skill recommendations.</li>';
    }

    // Re-run lucide icons if they exist in dynamic content
    if (typeof lucide !== 'undefined') lucide.createIcons();
});

function toggleBreakdownList(element) {
    const list = element.querySelector('.breakdown-skills-list');
    const arrow = element.querySelector('.breakdown-arrow');
    if (!list) return;

    const isVisible = list.style.display === 'flex';
    list.style.display = isVisible ? 'none' : 'flex';

    if (arrow) {
        if (isVisible) arrow.classList.remove('expanded');
        else arrow.classList.add('expanded');
    }

    // Optional: add a subtle scale effect on click
    element.style.transform = 'scale(0.98)';
    setTimeout(() => element.style.transform = 'scale(1)', 100);
}

function animateScore(targetScore, matchLevel) {
    const scoreNumberEl = document.getElementById("scoreNumber");
    const scoreRingProgress = document.getElementById("scoreRingProgress");
    const scoreBadge = document.getElementById("scoreBadge");
    const scoreLabel = document.getElementById("scoreLabel");

    if (!scoreNumberEl || !scoreRingProgress) return;

    if (!targetScore || isNaN(targetScore)) {
        scoreNumberEl.textContent = "–";
        return;
    }

    const target = parseInt(targetScore, 10);
    const circumference = 2 * Math.PI * 36; // r=36
    const offset = circumference - (target / 100) * circumference;

    // Badge Label
    if (scoreLabel) scoreLabel.textContent = matchLevel || (target > 70 ? 'Strong Match' : 'Match');

    // Ring & Badge Colors
    scoreRingProgress.classList.remove("medium", "weak");
    scoreBadge?.classList.remove("strong", "medium", "weak");

    if (target >= 75) {
        scoreBadge?.classList.add("strong");
    } else if (target >= 50) {
        scoreBadge?.classList.add("medium");
        scoreRingProgress.classList.add("medium");
    } else {
        scoreBadge?.classList.add("weak");
        scoreRingProgress.classList.add("weak");
    }

    // Animate the ring
    setTimeout(() => {
        scoreRingProgress.style.strokeDashoffset = offset;
    }, 100);

    // Animate the number count-up
    let current = 0;
    const duration = 1200;
    const stepTime = 20;
    const steps = duration / stepTime;
    const increment = target / steps;

    const counter = setInterval(() => {
        current += increment;
        if (current >= target) {
            current = target;
            clearInterval(counter);
        }
        scoreNumberEl.textContent = Math.round(current) + "%";
    }, stepTime);
}
