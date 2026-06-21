package com.snakegame.web.dto;

/**
 * 客户端→服务端：用户输入消息。
 * 通过 WebSocket 发送方向变更和加速状态。
 */
public class InputMessage {
    private String type;        // "INPUT"
    private String direction;   // "UP" | "DOWN" | "LEFT" | "RIGHT"
    private boolean speedBoost;
    private int sequence;       // 单调递增序列号

    public InputMessage() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public boolean isSpeedBoost() { return speedBoost; }
    public void setSpeedBoost(boolean speedBoost) { this.speedBoost = speedBoost; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
}
