/**
 * 音频管理器（第3期完整实现）。
 * 使用 Web Audio API 播放游戏音效和背景音乐。
 */
class AudioManager {
    constructor() {
        this.audioCtx = null;
        this.enabled = true;
        this.bgmGain = null;
    }

    /** 初始化音频上下文（需要用户交互后调用） */
    init() {
        try {
            this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        } catch (e) {
            console.warn('Web Audio API 不可用:', e.message);
            this.enabled = false;
        }
    }

    /** 播放简单音效（使用振荡器生成，无需音频文件） */
    playTone(frequency, duration, type = 'square', volume = 0.15) {
        if (!this.enabled || !this.audioCtx) return;

        const oscillator = this.audioCtx.createOscillator();
        const gainNode = this.audioCtx.createGain();

        oscillator.type = type;
        oscillator.frequency.setValueAtTime(frequency, this.audioCtx.currentTime);

        gainNode.gain.setValueAtTime(volume, this.audioCtx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.001,
            this.audioCtx.currentTime + duration);

        oscillator.connect(gainNode);
        gainNode.connect(this.audioCtx.destination);

        oscillator.start(this.audioCtx.currentTime);
        oscillator.stop(this.audioCtx.currentTime + duration);
    }

    /** 吃食物音效：短促高频音 */
    playEatSound() {
        this.playTone(800, 0.1, 'square', 0.12);
        setTimeout(() => this.playTone(1200, 0.08, 'square', 0.10), 50);
    }

    /** 游戏结束音效：下降音 */
    playGameOverSound() {
        this.playTone(400, 0.3, 'sawtooth', 0.15);
        setTimeout(() => this.playTone(300, 0.2, 'sawtooth', 0.12), 150);
        setTimeout(() => this.playTone(200, 0.3, 'sawtooth', 0.10), 300);
    }

    /** 加速激活音效 */
    playBoostOnSound() {
        this.playTone(600, 0.05, 'square', 0.08);
    }

    /** 背景音乐（简单的循环旋律 — 第3期完善） */
    startBGM() {
        // 待实现
    }

    stopBGM() {
        // 待实现
    }
}

// 全局实例
window.audioManager = new AudioManager();
