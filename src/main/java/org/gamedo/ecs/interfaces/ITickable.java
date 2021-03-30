package org.gamedo.ecs.interfaces;

@FunctionalInterface
public interface ITickable
{
    void tick(long elapse);
}
