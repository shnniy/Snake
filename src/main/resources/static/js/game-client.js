/**
 * 贪吃蛇 — 游戏客户端。
 * 使用原生 WebSocket + JSON 协议与服务器通信。
 */
class GameClient {
    constructor() {
        // ===== Canvas =====
        this.canvas = document.getElementById('game-canvas');
        this.ctx = this.canvas.getContext('2d');

        // ===== 游戏配置 =====
        this.gridWidth = 20;
        this.gridHeight = 15;
        this.cellSize = 32;
        this.gameId = null;

        // ===== 游戏状态 =====
        this.snakeBody = [];
        this.foodPosition = { x: 0, y: 0 };
        this.foodType = 'NORMAL';
        this.score = 0;
        this.highScore = 0;
        this.gameState = 'READY';
        this.speedBoost = false;

        // ===== WebSocket =====
        this.ws = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;

        // ===== UI 元素 =====
        this.overlay = document.getElementById('overlay');
        this.overlayTitle = document.getElementById('overlay-title');
        this.overlayMessage = document.getElementById('overlay-message');
        this.overlayBtn = document.getElementById('overlay-btn');
        this.overlayScore = document.getElementById('overlay-score');
        this.scoreEl = document.getElementById('score');
        this.highScoreEl = document.getElementById('high-score');
        this.speedIndicator = document.getElementById('speed-indicator');
        this.connectionStatus = document.getElementById('connection-status');

        // ===== 动画帧 =====
        this.animationId = null;

        this.init();
    }

    /** 初始化 */
    init() {
        this.resizeCanvas();
        window.addEventListener('resize', () => this.resizeCanvas());

        // 绑定 UI 事件
        this.overlayBtn.addEventListener('click', () => this.startGame());
        document.getElementById('btn-start').addEventListener('click', () => this.startGame());
        document.getElementById('btn-pause').addEventListener('click', () => this.togglePause());
        document.getElementById('btn-restart').addEventListener('click', () => this.restartGame());
        document.getElementById('btn-leaderboard').addEventListener('click', () => this.toggleLeaderboard());
        document.getElementById('btn-close-leaderboard').addEventListener('click', () => {
            document.getElementById('leaderboard-panel').style.display = 'none';
        });

        // 绑定键盘事件
        window.inputHandler.onDirection = (dir) => this.sendDirection(dir);
        window.inputHandler.onSpeedBoost = (active) => this.sendSpeedBoost(active);
        window.inputHandler.onPauseToggle = () => this.togglePause();
        window.inputHandler.onStart = () => {
            if (this.gameState === 'READY' || this.gameState === 'GAME_OVER') {
                this.startGame();
            } else {
                this.togglePause();
            }
        };
        window.inputHandler.attach();

        // 连接 WebSocket
        this.connectWebSocket();

        // 始终启动渲染循环
        this.renderLoop();
    }

    /** 自适应画布大小 */
    resizeCanvas() {
        const maxWidth = Math.min(window.innerWidth - 40, 800);
        const maxHeight = Math.min(window.innerHeight - 280, 500);
        const cellByWidth = Math.floor(maxWidth / this.gridWidth);
        const cellByHeight = Math.floor(maxHeight / this.gridHeight);
        this.cellSize = Math.min(cellByWidth, cellByHeight, 40);
        this.cellSize = Math.max(this.cellSize, 16);

        this.canvas.width = this.gridWidth * this.cellSize;
        this.canvas.height = this.gridHeight * this.cellSize;
    }

    // ===== WebSocket 连接 =====

    connectWebSocket() {
        this.setConnectionStatus('connecting', '🟡 连接中...');

        try {
            // 使用原生 WebSocket，连接到 /ws/game 端点
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const host = window.location.host;
            const url = protocol + '//' + host + '/ws/game';
            console.log('WebSocket 连接:', url);

            this.ws = new WebSocket(url);

            this.ws.onopen = () => {
                console.log('WebSocket 已连接');
                this.connected = true;
                this.reconnectAttempts = 0;
                this.setConnectionStatus('connected', '🟢 已连接');
            };

            this.ws.onmessage = (event) => {
                try {
                    const msg = JSON.parse(event.data);
                    this.handleMessage(msg);
                } catch (e) {
                    console.error('消息解析失败:', e);
                }
            };

            this.ws.onclose = (event) => {
                console.log('WebSocket 断开:', event.code, event.reason);
                this.connected = false;
                this.setConnectionStatus('disconnected', '🔴 断开 — 自动重连中...');
                // 自动重连
                setTimeout(() => {
                    if (!this.connected && this.reconnectAttempts < this.maxReconnectAttempts) {
                        this.reconnectAttempts++;
                        console.log('尝试重连 (' + this.reconnectAttempts + '/' + this.maxReconnectAttempts + ')...');
                        this.connectWebSocket();
                    }
                }, 2000);
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket 错误:', error);
                // 不要在这里设置状态，onclose 会在之后触发
            };

        } catch (e) {
            console.error('WebSocket 创建失败:', e);
            this.setConnectionStatus('disconnected', '🔴 连接失败');
        }
    }

    /** 处理服务器消息 */
    handleMessage(msg) {
        switch (msg.type) {
            case 'INIT':
                // 收到会话初始化信息
                this.gameId = msg.gameId;
                this.gridWidth = msg.gridWidth || 20;
                this.gridHeight = msg.gridHeight || 15;
                this.gameState = msg.gameState || 'READY';
                this.resizeCanvas();
                this.showOverlay('ready');
                console.log('会话初始化完成:', this.gameId);
                break;

            case 'STATE':
                // 游戏状态更新
                this.snakeBody = msg.snake || [];
                this.foodPosition = {
                    x: msg.food ? msg.food[0] : 0,
                    y: msg.food ? msg.food[1] : 0
                };
                this.foodType = msg.foodType || 'NORMAL';
                this.score = msg.score || 0;
                this.highScore = msg.highScore || 0;
                this.gameState = msg.gameState || 'READY';
                this.speedBoost = msg.speedBoost || false;

                // 更新 HUD
                this.scoreEl.textContent = this.score;
                this.highScoreEl.textContent = this.highScore;

                // 根据游戏状态显示覆盖层
                if (this.gameState === 'GAME_OVER') {
                    this.showOverlay('gameover');
                } else if (this.gameState === 'PAUSED') {
                    this.showOverlay('paused');
                } else if (this.gameState === 'RUNNING') {
                    this.hideOverlay();
                }
                break;

            case 'EVENT':
                console.log('游戏事件:', msg.action);
                switch (msg.action) {
                    case 'FOOD_EATEN':
                        window.audioManager.playEatSound();
                        break;
                    case 'GAME_OVER':
                        window.audioManager.playGameOverSound();
                        break;
                }
                break;
        }
    }

    /** 发送 JSON 消息 */
    send(msg) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(msg));
        } else {
            console.warn('WebSocket 未连接，无法发送消息');
        }
    }

    /** 发送方向 */
    sendDirection(direction) {
        this.send({
            direction: direction,
            speedBoost: this.speedBoost
        });
    }

    /** 发送加速状态 */
    sendSpeedBoost(active) {
        this.speedBoost = active;
        this.send({
            speedBoost: active
        });

        if (active) {
            window.audioManager.playBoostOnSound();
        }

        // UI 更新
        if (active) {
            this.speedIndicator.classList.add('boost');
            this.speedIndicator.textContent = '⚡⚡';
        } else {
            this.speedIndicator.classList.remove('boost');
            this.speedIndicator.textContent = '⚡';
        }
    }

    /** 发送命令 */
    sendCommand(action) {
        this.send({ action: action.toUpperCase() });
    }

    // ===== 游戏控制 =====

    startGame() {
        window.audioManager.init();
        // 游戏结束后使用 RESTART 命令（会重置蛇的位置）
        if (this.gameState === 'GAME_OVER') {
            this.sendCommand('RESTART');
        } else {
            this.sendCommand('START');
        }
        this.hideOverlay();
    }

    togglePause() {
        if (this.gameState === 'RUNNING') {
            this.sendCommand('PAUSE');
        } else if (this.gameState === 'PAUSED') {
            this.sendCommand('RESUME');
            this.hideOverlay();
        }
    }

    restartGame() {
        this.sendCommand('RESTART');
        this.hideOverlay();
    }

    // ===== 状态处理 =====

    showOverlay(type) {
        this.overlay.classList.remove('hidden');

        const lb = document.getElementById('leaderboard');
        if (lb) lb.style.display = 'none';

        switch (type) {
            case 'ready':
                this.overlayTitle.textContent = '🐍 贪吃蛇';
                this.overlayMessage.textContent = '按下「开始游戏」或按 空格键 开始';
                this.overlayBtn.textContent = '开始游戏';
                this.overlayBtn.style.display = 'inline-block';
                this.overlayScore.textContent = '';
                break;
            case 'paused':
                this.overlayTitle.textContent = '⏸ 已暂停';
                this.overlayMessage.textContent = '按 空格键 继续游戏';
                this.overlayBtn.textContent = '继续游戏';
                this.overlayBtn.style.display = 'inline-block';
                this.overlayScore.textContent = '当前分数: ' + this.score;
                break;
            case 'gameover':
                this.overlayTitle.textContent = '💀 游戏结束';
                this.overlayMessage.textContent = '最终得分';
                this.overlayBtn.textContent = '再来一局';
                this.overlayBtn.style.display = 'inline-block';
                this.overlayScore.textContent = this.score;
                if (this.score >= this.highScore && this.score > 0) {
                    this.overlayScore.textContent += ' 🏆 新最高分！';
                }
                this.fetchLeaderboard();
                break;
        }
    }

    /** 显示/隐藏排行榜面板 */
    async toggleLeaderboard() {
        const panel = document.getElementById('leaderboard-panel');
        if (panel.style.display === 'none' || !panel.style.display) {
            await this.fetchLeaderboard();
            panel.style.display = 'block';
        } else {
            panel.style.display = 'none';
        }
    }

    /** 获取排行榜数据并渲染到所有排行榜表格 */
    async fetchLeaderboard() {
        try {
            const resp = await fetch('/api/scores');
            const data = await resp.json();

            // 构建表格行 HTML
            const rowsHtml = data.length === 0
                ? '<tr><td colspan="5" style="color:#888;">暂无记录</td></tr>'
                : data.map((entry, idx) => {
                    const time = entry.createdAt ? new Date(entry.createdAt).toLocaleString('zh-CN') : '-';
                    return `<tr>
                        <td>${idx + 1}</td>
                        <td>${this.escapeHtml(entry.playerName)}</td>
                        <td>${entry.score}</td>
                        <td>${entry.snakeLength}节</td>
                        <td style="font-size:0.8em;color:#888;">${time}</td>
                    </tr>`;
                }).join('');

            // 填充独立排行榜面板的表格
            const panelTbody = document.querySelector('#panel-leaderboard-table tbody');
            if (panelTbody) panelTbody.innerHTML = rowsHtml;

            // 同时填充覆盖层中的排行榜（游戏结束时显示）
            const overlayTbody = document.querySelector('#leaderboard-table tbody');
            if (overlayTbody) overlayTbody.innerHTML = rowsHtml;

            // 游戏结束时在覆盖层中显示排行榜
            const lb = document.getElementById('leaderboard');
            if (lb && this.gameState === 'GAME_OVER') {
                lb.style.display = 'block';
            }
        } catch (e) {
            console.warn('获取排行榜失败:', e);
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    hideOverlay() {
        this.overlay.classList.add('hidden');
    }

    setConnectionStatus(status, text) {
        this.connectionStatus.textContent = text;
        this.connectionStatus.className = status;
    }

    // ===== Canvas 渲染 =====

    renderLoop() {
        this.render();
        this.animationId = requestAnimationFrame(() => this.renderLoop());
    }

    render() {
        const ctx = this.ctx;
        const w = this.canvas.width;
        const h = this.canvas.height;
        const cs = this.cellSize;

        if (!ctx || w === 0 || h === 0) return;

        // 清屏
        ctx.fillStyle = '#111128';
        ctx.fillRect(0, 0, w, h);

        // 绘制网格线
        ctx.strokeStyle = '#1a1a35';
        ctx.lineWidth = 0.5;
        for (let x = 0; x <= this.gridWidth; x++) {
            ctx.beginPath();
            ctx.moveTo(x * cs, 0);
            ctx.lineTo(x * cs, h);
            ctx.stroke();
        }
        for (let y = 0; y <= this.gridHeight; y++) {
            ctx.beginPath();
            ctx.moveTo(0, y * cs);
            ctx.lineTo(w, y * cs);
            ctx.stroke();
        }

        // 绘制食物
        this.renderFood(ctx, cs);

        // 绘制蛇
        this.renderSnake(ctx, cs);

        // 绘制游戏未开始时的提示
        if (this.gameState === 'READY') {
            ctx.fillStyle = 'rgba(255, 255, 255, 0.2)';
            ctx.font = `${cs * 0.6}px "Microsoft YaHei", sans-serif`;
            ctx.textAlign = 'center';
            ctx.fillText('按 空格键 或点击「开始游戏」', w / 2, h / 2);
            ctx.textAlign = 'start';
        }
    }

    /** 渲染食物 */
    renderFood(ctx, cs) {
        const fx = this.foodPosition.x * cs;
        const fy = this.foodPosition.y * cs;
        const radius = cs * 0.35;

        if (this.foodType === 'BONUS') {
            ctx.fillStyle = '#FFD700';
            ctx.shadowColor = '#FFD700';
            ctx.shadowBlur = 8;
        } else {
            ctx.fillStyle = '#E53935';
            ctx.shadowColor = '#E53935';
            ctx.shadowBlur = 5;
        }

        ctx.beginPath();
        ctx.arc(fx + cs / 2, fy + cs / 2, radius, 0, Math.PI * 2);
        ctx.fill();

        // 高光
        ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        ctx.beginPath();
        ctx.arc(fx + cs / 2 - radius * 0.3, fy + cs / 2 - radius * 0.3,
                radius * 0.3, 0, Math.PI * 2);
        ctx.fill();

        ctx.shadowBlur = 0;
    }

    /** 渲染蛇 */
    renderSnake(ctx, cs) {
        if (!this.snakeBody || this.snakeBody.length === 0) return;

        const body = this.snakeBody;
        const padding = cs * 0.08;

        for (let i = body.length - 1; i >= 0; i--) {
            const seg = body[i];
            // 兼容 [x, y] 和 {x, y} 两种格式
            const x = Array.isArray(seg) ? seg[0] : seg.x;
            const y = Array.isArray(seg) ? seg[1] : seg.y;
            const sx = x * cs + padding;
            const sy = y * cs + padding;
            const size = cs - padding * 2;

            if (i === 0) {
                // 头部
                ctx.fillStyle = this.speedBoost ? '#66BB6A' : '#4CAF50';
                if (this.speedBoost) {
                    ctx.shadowColor = '#66BB6A';
                    ctx.shadowBlur = 10;
                }
                this.roundRect(ctx, sx, sy, size, size, 6);
                ctx.fill();

                // 眼睛
                this.renderEyes(ctx, x, y, cs, body, i);
                ctx.shadowBlur = 0;
            } else if (i === body.length - 1) {
                // 尾部
                ctx.fillStyle = '#2E7D32';
                this.roundRect(ctx, sx, sy, size, size, 4);
                ctx.fill();
            } else {
                // 身体
                const ratio = i / (body.length - 1);
                const g = Math.floor(100 + 60 * (1 - ratio));
                ctx.fillStyle = `rgb(30, ${g}, 30)`;
                this.roundRect(ctx, sx, sy, size, size, 4);
                ctx.fill();
            }
        }
    }

    /** 渲染蛇眼睛 */
    renderEyes(ctx, x, y, cs, body, index) {
        if (body.length < 2) return;

        const seg1 = Array.isArray(body[0]) ? body[0] : [body[0].x, body[0].y];
        const seg2 = Array.isArray(body[1]) ? body[1] : [body[1].x, body[1].y];

        let dx = seg1[0] - seg2[0];
        let dy = seg1[1] - seg2[1];
        if (dx === 0 && dy === 0) { dx = 1; dy = 0; }

        const cx = x * cs + cs / 2;
        const cy = y * cs + cs / 2;
        const eyeR = cs * 0.13;
        const eyeDist = cs * 0.22;

        let eye1x, eye1y, eye2x, eye2y;
        if (dx === 1) {
            eye1x = cx + eyeDist; eye1y = cy - eyeDist;
            eye2x = cx + eyeDist; eye2y = cy + eyeDist;
        } else if (dx === -1) {
            eye1x = cx - eyeDist; eye1y = cy - eyeDist;
            eye2x = cx - eyeDist; eye2y = cy + eyeDist;
        } else if (dy === -1) {
            eye1x = cx - eyeDist; eye1y = cy - eyeDist;
            eye2x = cx + eyeDist; eye2y = cy - eyeDist;
        } else {
            eye1x = cx - eyeDist; eye1y = cy + eyeDist;
            eye2x = cx + eyeDist; eye2y = cy + eyeDist;
        }

        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath();
        ctx.arc(eye1x, eye1y, eyeR, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(eye2x, eye2y, eyeR, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#000000';
        ctx.beginPath();
        ctx.arc(eye1x, eye1y, eyeR * 0.6, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(eye2x, eye2y, eyeR * 0.6, 0, Math.PI * 2);
        ctx.fill();
    }

    /** 绘制圆角矩形 */
    roundRect(ctx, x, y, w, h, r) {
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.lineTo(x + w - r, y);
        ctx.quadraticCurveTo(x + w, y, x + w, y + r);
        ctx.lineTo(x + w, y + h - r);
        ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
        ctx.lineTo(x + r, y + h);
        ctx.quadraticCurveTo(x, y + h, x, y + h - r);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
        ctx.closePath();
    }
}

// 启动游戏客户端
document.addEventListener('DOMContentLoaded', () => {
    window.gameClient = new GameClient();
});
