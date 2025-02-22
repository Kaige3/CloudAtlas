package com.kaige.utils;

import java.awt.*;

// 颜色相似度工具类
public class ColorSimilarUtils {

    private ColorSimilarUtils() {
    }

    /**
     * 计算两个颜色的相似度
     *
     * @param color1 第一个颜色
     * @param color2 第二个颜色
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();

        // 计算欧氏距离
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

        // 计算相似度
        return 1 - distance / Math.sqrt(3 * Math.pow(255, 2));
    }

    public static double calculateSimilarity(String color1, String color2) {
        Color c1 = Color.decode(color1);
        Color c2 = Color.decode(color2);
        return calculateSimilarity(c1, c2);

    }

    public static void main(String[] args) {
         // 测试颜色
        Color c1 = Color.decode("0xFF0000");
        Color c2 = Color.decode("0xFF0101");
        double similarity = calculateSimilarity(c1, c2);
        System.out.println("颜色相似度：" + similarity);
        System.out.println("0xFF0000".length());

        // 测试颜色
        double similarity1 = calculateSimilarity("0xFF0000", "0xFF0101");
        System.out.println("颜色相似度：" + similarity1);
    }
}
