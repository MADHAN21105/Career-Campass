/**
 * Hover Slider Logic
 * Replicates the Framer Motion "TextStaggerHover" and "HoverSliderImage" logic in Vanilla JS.
 */

document.addEventListener('DOMContentLoaded', () => {
    initHoverSlider();
});

function initHoverSlider() {
    const textItems = document.querySelectorAll('.hover-slider-text-item');
    const images = document.querySelectorAll('.hover-slider-img');
    
    if (!textItems.length || !images.length) return;

    // 1. Hydrate Text: Split text into characters for staggered animation
    textItems.forEach((item, index) => {
        const rawText = item.getAttribute('data-text');
        if (!rawText) return;

        // Split text and wrap in spans
        const chars = splitText(rawText);
        
        // Clear original text content and append the structure
        item.innerHTML = ''; 
        
        chars.forEach((char, charIndex) => {
            const container = document.createElement('span');
            container.className = 'hover-slider-char-container';
            if (char === ' ') container.style.width = '0.3em'; // Adjust space width
            
            // Calculate Stagger Delay
            const delay = charIndex * 0.025; // 0.025s staggered delay per char

            // Primary Character (Initial State)
            const primary = document.createElement('span');
            primary.className = 'hover-slider-char-primary';
            primary.innerText = char;
            primary.style.transitionDelay = `${delay}s`;

            // Secondary Character (Hover State)
            const secondary = document.createElement('span');
            secondary.className = 'hover-slider-char-secondary';
            secondary.innerText = char;
            secondary.style.transitionDelay = `${delay}s`;

            container.appendChild(primary);
            container.appendChild(secondary);
            item.appendChild(container);
        });

        // 2. Add Hover Listeners
        item.addEventListener('mouseenter', () => {
            setActiveSlide(index);
        });
    });

    // Helper: Split text into array of characters, preserving spaces
    function splitText(text) {
        return text.split('');
    }

    // 3. Set Active Slide Function
    function setActiveSlide(activeIndex) {
        // Update Text Items
        textItems.forEach((item, index) => {
            if (index === activeIndex) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Update Images (Clip Path)
        images.forEach((img, index) => {
            if (index === activeIndex) {
                img.classList.add('active');
            } else {
                img.classList.remove('active');
            }
        });
    }

    // Initialize first slide as active
    setActiveSlide(0);
}
