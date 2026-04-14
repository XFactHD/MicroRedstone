package io.github.xfacthd.microredstone.client.util;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public record ArrowKey(Direction dir, long startTime) {
    public enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        private final int xDiff;
        private final int yDiff;

        Direction(int xDiff, int yDiff) {
            this.xDiff = xDiff;
            this.yDiff = yDiff;
        }

        public int getDiffX() {
            return xDiff;
        }

        public int getDiffY() {
            return yDiff;
        }

        public static @Nullable Direction of(int keyCode) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> UP;
                case GLFW.GLFW_KEY_DOWN -> DOWN;
                case GLFW.GLFW_KEY_LEFT -> LEFT;
                case GLFW.GLFW_KEY_RIGHT -> RIGHT;
                default -> null;
            };
        }
    }
}
