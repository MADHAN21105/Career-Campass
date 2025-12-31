/**
 * Shatter Button Effect
 * Ports React/Framer Motion logic to Vanilla JS + Web Animations API
 */

class ShatterButton {
    constructor(element, options = {}) {
        this.button = element;
        // Default options matching the React component structure
        this.options = {
            shardCount: options.shardCount || 20,
            shatterColor: options.shatterColor || '#6366f1', // Default to primary color
            onClick: options.onClick || null
        };

        this.isShattered = false;

        this.init();
    }

    init() {
        // Apply initial styles to match the React component's "motion.button"
        this.button.style.position = 'relative';
        this.button.style.overflow = 'hidden';

        // Add click listener
        this.button.addEventListener('click', (e) => this.handleClick(e));

        // Add hover effect listeners
        this.button.addEventListener('mouseenter', () => this.handleHover(true));
        this.button.addEventListener('mouseleave', () => this.handleHover(false));
    }

    handleHover(isHovering) {
        if (this.isShattered) return;

        this.button.animate([
            { transform: isHovering ? 'scale(1.05)' : 'scale(1)' }
        ], {
            duration: 150,
            fill: 'forwards',
            easing: 'ease-out'
        });
    }

    async handleClick(e) {
        if (this.isShattered) return;

        this.isShattered = true;

        // Get button dimensions and position
        const rect = this.button.getBoundingClientRect();
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;

        // Container for shards (so they don't get clipped by button overflow)
        // We append this to the BODY so it sits visually on top
        const shardContainer = document.createElement('div');
        shardContainer.style.position = 'fixed';
        shardContainer.style.top = rect.top + 'px';
        shardContainer.style.left = rect.left + 'px';
        shardContainer.style.width = rect.width + 'px';
        shardContainer.style.height = rect.height + 'px';
        shardContainer.style.pointerEvents = 'none';
        shardContainer.style.zIndex = '9999';
        document.body.appendChild(shardContainer);

        // 1. Create Shards
        for (let i = 0; i < this.options.shardCount; i++) {
            this.createShard(shardContainer, centerX, centerY);
        }

        // 2. Create Explosion Ring
        this.createExplosionRing(shardContainer, centerX, centerY);

        // 3. Animate Button OUT
        const buttonAnim = this.button.animate([
            { opacity: 1, transform: 'scale(1.05)' },
            { opacity: 0, transform: 'scale(0)' }
        ], {
            duration: 150,
            fill: 'forwards',
            easing: 'ease-out'
        });

        // 4. Cleanup & Reset
        // Call the onClick handler and wait if it's a promise
        if (this.options.onClick) {
            try {
                await this.options.onClick();
            } catch (err) {
                console.error("ShatterButton onClick error:", err);
            }
        }

        setTimeout(() => {
            // Remove shards
            document.body.removeChild(shardContainer);

            // Reset button
            this.isShattered = false;
            buttonAnim.reverse();
            this.button.style.opacity = '1';
            this.button.style.transform = 'scale(1)';

        }, 1200); // Wait for shards to finish
    }

    createShard(container, startX, startY) {
        const shard = document.createElement('div');
        const size = 4 + Math.random() * 12; // 4-16px
        const color = this.options.shatterColor;

        shard.style.position = 'absolute';
        shard.style.left = '50%';
        shard.style.top = '50%'; // Start from center
        shard.style.width = `${size}px`;
        shard.style.height = `${size}px`;
        shard.style.backgroundColor = color;
        shard.style.boxShadow = `0 0 10px ${color}, 0 0 20px ${color}`;

        // Random Polygon Clip Path
        const p1 = Math.random() * 50;
        const p2 = Math.random() * 50;
        const p3 = 50 + Math.random() * 50;
        const p4 = 50 + Math.random() * 50;
        shard.style.clipPath = `polygon(${p1}% 0%, 100% ${p2}%, ${p3}% 100%, 0% ${p4}%)`;

        container.appendChild(shard);

        // Physics calculations
        const angle = (Math.PI * 2 * Math.random()); // Random angle
        const velocity = 100 + Math.random() * 200; // Speed
        const velocityX = Math.cos(angle) * velocity;
        const velocityY = Math.sin(angle) * velocity;
        const rotation = Math.random() * 720 - 360;

        // Animate Shard
        shard.animate([
            {
                transform: `translate(-50%, -50%) rotate(0deg) scale(1)`,
                opacity: 1
            },
            {
                transform: `translate(calc(-50% + ${velocityX}px), calc(-50% + ${velocityY}px)) rotate(${rotation}deg) scale(0.5)`,
                opacity: 0
            }
        ], {
            duration: 800,
            easing: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)', // easeOutQuad-ish
            fill: 'forwards'
        });
    }

    createExplosionRing(container, x, y) {
        const ring = document.createElement('div');
        const color = this.options.shatterColor;

        ring.style.position = 'absolute';
        ring.style.left = '50%';
        ring.style.top = '50%';
        ring.style.transform = 'translate(-50%, -50%)';
        ring.style.borderRadius = '50%';
        ring.style.border = `2px solid ${color}`;
        ring.style.boxShadow = `0 0 30px ${color}`;
        ring.style.opacity = '1';

        container.appendChild(ring);

        ring.animate([
            { width: '0px', height: '0px', opacity: 1, borderWidth: '2px' },
            { width: '300px', height: '300px', opacity: 0, borderWidth: '0px' }
        ], {
            duration: 600,
            easing: 'ease-out',
            fill: 'forwards'
        });
    }
}
