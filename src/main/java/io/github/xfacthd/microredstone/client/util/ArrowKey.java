package io.github.xfacthd.microredstone.client.util;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public record ArrowKey(Direction dir, long startTime)
{
    public enum Direction
    {
        UP(GLFW.GLFW_KEY_UP, 0, -1),
        DOWN(GLFW.GLFW_KEY_DOWN, 0, 1),
        LEFT(GLFW.GLFW_KEY_LEFT, -1, 0),
        RIGHT(GLFW.GLFW_KEY_RIGHT, 1, 0);

        private static final Direction[] DIRECTIONS = values();

        private final int keyCode;
        private final int xDiff;
        private final int yDiff;

        Direction(int keyCode, int xDiff, int yDiff)
        {
            this.keyCode = keyCode;
            this.xDiff = xDiff;
            this.yDiff = yDiff;
        }

        public int getDiffX()
        {
            return xDiff;
        }

        public int getDiffY()
        {
            return yDiff;
        }

        @Nullable
        public static Direction of(int keyCode)
        {
            for (Direction direction : DIRECTIONS)
            {
                if (keyCode == direction.keyCode)
                {
                    return direction;
                }
            }
            return null;
        }
    }
}
