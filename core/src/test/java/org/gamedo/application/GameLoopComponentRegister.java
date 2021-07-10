package org.gamedo.application;

import lombok.Value;

import java.util.List;

@Value
public class GameLoopComponentRegister<T> {
    List<Class<? super T>> clazzList;
    T object;
}
