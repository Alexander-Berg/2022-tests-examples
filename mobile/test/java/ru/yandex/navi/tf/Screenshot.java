package ru.yandex.navi.tf;

import org.openqa.selenium.Point;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class Screenshot {
    private final BufferedImage image;

    private Screenshot(BufferedImage image) {
        this.image = image;
    }

    static Screenshot fromBytes(byte[] bytes) {
        try {
            final BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            return new Screenshot(image);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    int getGrayColor() {
        final int[] weights = {11, 59, 30};
        final int height = image.getHeight(), width = image.getWidth();
        final int[] grayColors = new int[height * width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int color = image.getRGB(x, y);
                int grayColor = 0;
                for (int weight : weights) {
                    grayColor += weight * (color & 0xff);
                    color >>= 8;
                }
                grayColors[y * width + x] = grayColor / 100;
            }
        }
        Arrays.sort(grayColors);
        return grayColors[grayColors.length / 2];
    }

    public double getFractionOfColors(Set<Integer> colors) {
        int count = 0;
        final int height = image.getHeight(), width = image.getWidth();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int color = image.getRGB(x, y) & 0xffffff;
                if (colors.contains(color))
                    ++count;
            }
        }

        return (double) count / image.getWidth() / image.getHeight();
    }

    public boolean isMapPoint(Point point, Set<Integer> mapColors) {
        final int height = image.getHeight(), width = image.getWidth();
        final int dx = 25, dy = 25, step = 25;

        for (int x = point.x - dx; x <= point.x + dx; x += step) {
            if (x < 0 || x >= width)
                continue;

            for (int y = point.y - dy; y <= point.y + dy; y += step) {
                if (y < 0 || y >= height)
                    continue;

                int color = image.getRGB(x, y) & 0xffffff;
                if (!mapColors.contains(color))
                    return false;
            }
        }

        return true;
    }
}
