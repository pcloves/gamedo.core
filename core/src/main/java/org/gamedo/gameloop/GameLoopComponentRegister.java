package org.gamedo.gameloop;

import lombok.Value;

import java.util.List;

@Value
public class GameLoopComponentRegister<T> {
    List<Class<? super T>> clazzList;
    T object;
}
