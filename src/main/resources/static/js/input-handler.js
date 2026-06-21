/**
 * 键盘输入处理器。
 * 监听方向键和 Shift/Space 键，通过回调通知游戏客户端。
 */
class InputHandler {
    constructor() {
        this.onDirection = null;    // (direction: string) => void
        this.onSpeedBoost = null;   // (active: boolean) => void
        this.onPauseToggle = null;  // () => void
        this.onStart = null;        // () => void

        this.keysDown = new Set();
        this.lastDirectionSent = null;

        this._onKeyDown = this._onKeyDown.bind(this);
        this._onKeyUp = this._onKeyUp.bind(this);
        this._onBlur = this._onBlur.bind(this);
    }

    /** 绑定事件监听 */
    attach() {
        window.addEventListener('keydown', this._onKeyDown);
        window.addEventListener('keyup', this._onKeyUp);
        window.addEventListener('blur', this._onBlur);
    }

    /** 解绑事件监听 */
    detach() {
        window.removeEventListener('keydown', this._onKeyDown);
        window.removeEventListener('keyup', this._onKeyUp);
        window.removeEventListener('blur', this._onBlur);
        this.keysDown.clear();
    }

    /** 重置状态 */
    reset() {
        this.keysDown.clear();
        this.lastDirectionSent = null;
    }

    _onKeyDown(event) {
        // 防止方向键滚动页面
        const arrowKeys = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'];
        if (arrowKeys.includes(event.key) || event.key === ' ') {
            event.preventDefault();
        }

        // 防止重复触发（按住键时 keydown 会重复触发）
        if (this.keysDown.has(event.key)) return;
        this.keysDown.add(event.key);

        // 方向键处理
        const dirMap = {
            'ArrowUp':    'UP',
            'ArrowDown':  'DOWN',
            'ArrowLeft':  'LEFT',
            'ArrowRight': 'RIGHT',
            'w': 'UP',    'W': 'UP',
            's': 'DOWN',  'S': 'DOWN',
            'a': 'LEFT',  'A': 'LEFT',
            'd': 'RIGHT', 'D': 'RIGHT'
        };

        const direction = dirMap[event.key];
        if (direction) {
            // 只发送方向变更（避免重复发送相同方向）
            if (direction !== this.lastDirectionSent && this.onDirection) {
                this.onDirection(direction);
                this.lastDirectionSent = direction;
            }
        }

        // Shift 键加速
        if (event.key === 'Shift' && this.onSpeedBoost) {
            this.onSpeedBoost(true);
        }

        // 空格键暂停/开始
        if (event.key === ' ') {
            if (this.onPauseToggle) {
                this.onPauseToggle();
            } else if (this.onStart) {
                this.onStart();
            }
        }
    }

    _onKeyUp(event) {
        this.keysDown.delete(event.key);

        // 松开方向键时重置 lastDirectionSent（允许同一方向再次按下）
        const arrowKeys = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight',
                          'w', 'W', 's', 'S', 'a', 'A', 'd', 'D'];
        if (arrowKeys.includes(event.key)) {
            this.lastDirectionSent = null;
        }

        // 松开 Shift 键停止加速
        if (event.key === 'Shift' && this.onSpeedBoost) {
            this.onSpeedBoost(false);
        }
    }

    _onBlur() {
        // 窗口失去焦点时清除所有按键状态
        this.keysDown.clear();
        this.lastDirectionSent = null;
        if (this.onSpeedBoost) {
            this.onSpeedBoost(false);
        }
    }
}

// 全局实例
window.inputHandler = new InputHandler();
