// Mobile Menu Toggle
const mobileMenuBtn = document.getElementById('mobile-menu-btn');
const mobileMenu = document.getElementById('mobile-menu');

if (mobileMenuBtn && mobileMenu) {
    mobileMenuBtn.addEventListener('click', () => {
        mobileMenu.classList.toggle('hidden');
    });
}

// Animated Text & Rotation Logic
document.addEventListener('DOMContentLoaded', () => {
    const container = document.querySelector('.animated-text-container');

    if (!container) return;

    // Words to rotate (excluding "10× " which is now static)
    const words = ["faster", "successful", "brighter", "impactful"];
    let currentWordIndex = 0;

    // Helper to create an animated word wrapper
    function createAnimatedWord(word) {
        const wordSpan = document.createElement('span');
        wordSpan.className = 'inline-block whitespace-nowrap mr-2'; // Keep word together, add margin for space

        word.split('').forEach((char) => {
            const span = document.createElement('span');
            span.textContent = char;
            span.className = 'inline-block transition-transform duration-300 ease-spring';
            span.style.transitionTimingFunction = 'cubic-bezier(0.34, 1.56, 0.64, 1)';
            wordSpan.appendChild(span);
        });

        return wordSpan;
    }

    // Function to apply animation structure to the container
    function initializeAnimation() {
        container.innerHTML = '';

        // Part 1: Static Text "Let AI accelerate your career 10×"
        const staticSentence = "Let AI accelerate your career 10×";
        const staticWords = staticSentence.split(' ');

        staticWords.forEach((word) => {
            const wordWrapper = createAnimatedWord(word);
            container.appendChild(wordWrapper);
        });

        // Part 2: Rotating Text
        const newRotatingSpan = document.createElement('span');
        newRotatingSpan.id = 'rotating-text';
        newRotatingSpan.className = 'text-primary inline-block whitespace-nowrap text-xl md:text-3xl';

        const currentWord = words[currentWordIndex];
        currentWord.split('').forEach(char => {
            const span = document.createElement('span');
            span.textContent = char;
            span.className = 'inline-block transition-transform duration-300 ease-spring';
            span.style.transitionTimingFunction = 'cubic-bezier(0.34, 1.56, 0.64, 1)';
            newRotatingSpan.appendChild(span);
        });

        container.appendChild(newRotatingSpan);

        // Re-attach hover listeners to ALL spans
        attachHoverEffects();
    }

    function attachHoverEffects() {
        // Select all individual character spans
        const allCharSpans = container.querySelectorAll('span.inline-block:not(.whitespace-nowrap)');

        container.onmouseenter = () => {
            allCharSpans.forEach((span, index) => {
                span.style.transitionDelay = `${index * 30}ms`;
                span.style.transform = 'translateY(-4px) scale(1.2)';
            });
        };

        container.onmouseleave = () => {
            allCharSpans.forEach((span, index) => {
                span.style.transitionDelay = `${index * 10}ms`;
                span.style.transform = 'translateY(0) scale(1)';
            });
        };
    }

    // Initial Setup
    initializeAnimation();

    // Rotation Interval (2000ms = 2s)
    setInterval(() => {
        currentWordIndex = (currentWordIndex + 1) % words.length;
        initializeAnimation();
    }, 2000);
});
