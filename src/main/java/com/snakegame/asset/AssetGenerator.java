package com.snakegame.asset;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 游戏素材生成器。
 * 在 Maven generate-resources 阶段自动运行，
 * 使用 JDK 内置 API（BufferedImage + javax.sound.sampled）生成所有游戏素材。
 */
public class AssetGenerator {

    private static final int CELL_SIZE = 32; // 每格 32 像素
    private static final Path OUTPUT_DIR = Paths.get(
        "src/main/resources/static/assets"
    );

    public static void main(String[] args) throws Exception {
        System.out.println("[AssetGenerator] 开始生成游戏素材...");

        Files.createDirectories(OUTPUT_DIR.resolve("images"));
        Files.createDirectories(OUTPUT_DIR.resolve("audio"));

        generateSnakeHead();
        generateSnakeBody();
        generateSnakeTail();
        generateFoodNormal();
        generateFoodBonus();
        generateBackground();
        generateEatSound();
        generateGameOverSound();

        System.out.println("[AssetGenerator] 素材生成完成!");
    }

    // ==================== 图片生成 ====================

    /** 蛇头：绿色圆角矩形 + 白色眼睛 + 黑色瞳孔（面朝右） */
    static void generateSnakeHead() throws IOException {
        BufferedImage img = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        enableAntiAlias(g);

        // 身体底色
        g.setColor(new Color(76, 175, 80)); // Material Green 500
        g.fillRoundRect(1, 1, CELL_SIZE - 2, CELL_SIZE - 2, 8, 8);

        // 边框
        g.setColor(new Color(56, 142, 60));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(1, 1, CELL_SIZE - 2, CELL_SIZE - 2, 8, 8);

        // 左眼（上方）
        g.setColor(Color.WHITE);
        g.fillOval(18, 6, 10, 10);
        g.setColor(Color.BLACK);
        g.fillOval(21, 8, 5, 5);

        // 右眼（下方）
        g.setColor(Color.WHITE);
        g.fillOval(18, 17, 10, 10);
        g.setColor(Color.BLACK);
        g.fillOval(21, 19, 5, 5);

        g.dispose();
        ImageIO.write(img, "PNG", OUTPUT_DIR.resolve("images/snake-head.png").toFile());
        System.out.println("  ✓ snake-head.png (32×32)");
    }

    /** 蛇身：纯绿色圆角矩形 */
    static void generateSnakeBody() throws IOException {
        BufferedImage img = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        enableAntiAlias(g);

        g.setColor(new Color(76, 175, 80));
        g.fillRoundRect(1, 1, CELL_SIZE - 2, CELL_SIZE - 2, 6, 6);
        g.setColor(new Color(56, 142, 60));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(1, 1, CELL_SIZE - 2, CELL_SIZE - 2, 6, 6);

        g.dispose();
        ImageIO.write(img, "PNG", OUTPUT_DIR.resolve("images/snake-body.png").toFile());
        System.out.println("  ✓ snake-body.png (32×32)");
    }

    /** 蛇尾：深绿色渐变收窄 */
    static void generateSnakeTail() throws IOException {
        BufferedImage img = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        enableAntiAlias(g);

        // 渐变从绿色到深绿
        GradientPaint gp = new GradientPaint(0, 0, new Color(76, 175, 80),
                                              CELL_SIZE, 0, new Color(46, 125, 50));
        g.setPaint(gp);
        int[] xPoints = {4, CELL_SIZE - 2, CELL_SIZE - 2, 4};
        int[] yPoints = {CELL_SIZE / 2, 6, CELL_SIZE - 6, CELL_SIZE / 2};
        g.fillPolygon(xPoints, yPoints, 4);

        g.setColor(new Color(56, 142, 60));
        g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(xPoints, yPoints, 4);

        g.dispose();
        ImageIO.write(img, "PNG", OUTPUT_DIR.resolve("images/snake-tail.png").toFile());
        System.out.println("  ✓ snake-tail.png (32×32)");
    }

    /** 普通食物：红色圆形（苹果） */
    static void generateFoodNormal() throws IOException {
        BufferedImage img = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        enableAntiAlias(g);

        int r = CELL_SIZE / 2 - 3;
        int cx = CELL_SIZE / 2, cy = CELL_SIZE / 2;

        // 阴影
        g.setColor(new Color(180, 30, 30, 100));
        g.fillOval(cx - r + 1, cy - r + 1, r * 2, r * 2);

        // 主体
        g.setColor(new Color(229, 57, 53)); // Material Red 600
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        // 边框
        g.setColor(new Color(198, 40, 40));
        g.setStroke(new BasicStroke(1f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);

        // 高光
        g.setColor(new Color(255, 255, 255, 120));
        g.fillOval(cx - r / 3, cy - r / 2, r / 2, r / 2);

        // 茎
        g.setColor(new Color(139, 195, 74));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(cx, cy - r, cx + 1, cy - r - 4);

        g.dispose();
        ImageIO.write(img, "PNG", OUTPUT_DIR.resolve("images/food-normal.png").toFile());
        System.out.println("  ✓ food-normal.png (32×32)");
    }

    /** 奖励食物：金色星形 */
    static void generateFoodBonus() throws IOException {
        BufferedImage img = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        enableAntiAlias(g);

        int cx = CELL_SIZE / 2, cy = CELL_SIZE / 2;
        int outerR = CELL_SIZE / 2 - 3;
        int innerR = outerR / 2;

        // 五角星的 10 个顶点
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            int radius = (i % 2 == 0) ? outerR : innerR;
            xPoints[i] = cx + (int)(radius * Math.cos(angle));
            yPoints[i] = cy + (int)(radius * Math.sin(angle));
        }

        // 阴影
        g.setColor(new Color(200, 150, 0, 100));
        g.fillPolygon(shiftPolygon(xPoints, 1), shiftPolygon(yPoints, 1), 10);

        // 主体
        g.setColor(new Color(255, 215, 0)); // Gold
        g.fillPolygon(xPoints, yPoints, 10);

        // 边框
        g.setColor(new Color(255, 160, 0));
        g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(xPoints, yPoints, 10);

        g.dispose();
        ImageIO.write(img, "PNG", OUTPUT_DIR.resolve("images/food-bonus.png").toFile());
        System.out.println("  ✓ food-bonus.png (32×32)");
    }

    /** 背景：深色网格 */
    static void generateBackground() throws IOException {
        int gridW = 20, gridH = 15;
        int w = gridW * CELL_SIZE;  // 640
        int h = gridH * CELL_SIZE;  // 480

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 背景色
        g.setColor(new Color(17, 17, 40)); // #111128
        g.fillRect(0, 0, w, h);

        // 网格线
        g.setColor(new Color(26, 26, 53)); // #1a1a35
        g.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x <= gridW; x++) {
            g.drawLine(x * CELL_SIZE, 0, x * CELL_SIZE, h);
        }
        for (int y = 0; y <= gridH; y++) {
            g.drawLine(0, y * CELL_SIZE, w, y * CELL_SIZE);
        }

        g.dispose();
        ImageIO.write(img, "PNG", OUTPUT_DIR.resolve("images/background.png").toFile());
        System.out.println("  ✓ background.png (640×480)");
    }

    // ==================== 音频生成 ====================

    /** 吃食物音效：短促高频音 */
    static void generateEatSound() throws Exception {
        byte[] audio = generateTone(800, 0.1, 0.3);
        writeWav(OUTPUT_DIR.resolve("audio/eat.wav"), audio);
        System.out.println("  ✓ eat.wav (800Hz, 0.1s)");
    }

    /** 游戏结束音效：下降音 */
    static void generateGameOverSound() throws Exception {
        byte[] audio = generateDescendingTone(400, 200, 0.5);
        writeWav(OUTPUT_DIR.resolve("audio/game-over.wav"), audio);
        System.out.println("  ✓ game-over.wav (400→200Hz, 0.5s)");
    }

    /** 生成单频率正弦波 */
    private static byte[] generateTone(double freqHz, double durationSec, double amplitude) {
        int sampleRate = 44100;
        int numSamples = (int)(sampleRate * durationSec);
        byte[] buffer = new byte[numSamples * 2]; // 16-bit PCM
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * freqHz * i / sampleRate;
            short sample = (short)(amplitude * Short.MAX_VALUE * Math.sin(angle));

            // 应用淡入淡出
            double fadeIn = Math.min(1.0, i / (sampleRate * 0.01)); // 10ms 淡入
            double fadeOut = Math.min(1.0, (numSamples - i) / (sampleRate * 0.02)); // 20ms 淡出
            sample = (short)(sample * fadeIn * fadeOut);

            bb.putShort(sample);
        }
        return buffer;
    }

    /** 生成频率下降的声音 */
    private static byte[] generateDescendingTone(double startHz, double endHz,
                                                  double durationSec) {
        int sampleRate = 44100;
        int numSamples = (int)(sampleRate * durationSec);
        byte[] buffer = new byte[numSamples * 2];
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            double t = (double)i / numSamples;
            double freq = startHz + (endHz - startHz) * t; // 线性下降

            // 相位累积（使用频率积分避免相位跳跃）
            double phase = 2.0 * Math.PI * (startHz * t + 0.5 * (endHz - startHz) * t * t) * durationSec;
            short sample = (short)(0.3 * Short.MAX_VALUE * Math.sin(phase));

            double fadeOut = Math.min(1.0, (numSamples - i) / (sampleRate * 0.03));
            sample = (short)(sample * fadeOut);

            bb.putShort(sample);
        }
        return buffer;
    }

    /** 写入 WAV 文件 */
    private static void writeWav(Path path, byte[] audioData) throws IOException {
        int sampleRate = 44100;
        int numSamples = audioData.length / 2;

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioInputStream ais = new AudioInputStream(bais, format, numSamples);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, path.toFile());
    }

    // ==================== 辅助方法 ====================

    private static void enableAntiAlias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static int[] shiftPolygon(int[] points, int offset) {
        int[] result = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            result[i] = points[i] + offset;
        }
        return result;
    }
}
