package io.github.xfacthd.microredstone.client.screen.workbench.part;

public enum PartSetMode
{
    ADD,
    ROTATE,
    MOVE,
    REMOVE,
    ;

    boolean addOrRemove()
    {
        return this == ADD || this == REMOVE;
    }

    boolean writeGrid()
    {
        return this != ROTATE;
    }
}
