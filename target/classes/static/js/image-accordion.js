class ImageAccordion {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;

        this.items = this.container.querySelectorAll('.accordion-item');
        this.init();
    }

    init() {
        this.items.forEach((item, index) => {
            item.addEventListener('mouseenter', () => this.setActive(index));
        });
    }

    setActive(activeIndex) {
        this.items.forEach((item, index) => {
            const isActive = index === activeIndex;
            const caption = item.querySelector('.caption-text');

            if (isActive) {
                // Active State
                item.classList.remove('w-[60px]');
                item.classList.add('w-[400px]');

                // Caption: Horizontal, bottom
                caption.classList.remove('rotate-90', 'bottom-24', 'left-1/2', '-translate-x-1/2');
                caption.classList.add('rotate-0', 'bottom-6', 'left-6', 'translate-x-0');
                caption.style.transform = 'none'; // Clear complex transforms if needed

            } else {
                // Inactive State
                item.classList.remove('w-[400px]');
                item.classList.add('w-[60px]');

                // Caption: Vertical, rotated
                caption.classList.remove('rotate-0', 'bottom-6', 'left-6', 'translate-x-0');
                caption.classList.add('rotate-90', 'bottom-24', 'left-1/2', '-translate-x-1/2');
                caption.style.transform = ''; // Reset
            }
        });
    }
}

// Auto-init
document.addEventListener('DOMContentLoaded', () => {
    new ImageAccordion('hero-accordion');
});
