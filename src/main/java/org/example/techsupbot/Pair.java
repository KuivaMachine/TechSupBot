package org.example.techsupbot;

import lombok.Getter;

@Getter
public class Pair<K, V> {
    private K first;
    private V second;

    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

}
