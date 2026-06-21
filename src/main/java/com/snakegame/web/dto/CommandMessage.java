package com.snakegame.web.dto;

/**
 * 客户端→服务端：游戏命令消息。
 * 控制游戏生命周期。
 */
public class CommandMessage {
    private String type;    // "COMMAND"
    private String action;  // "START" | "PAUSE" | "RESUME" | "RESTART"

    public CommandMessage() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
