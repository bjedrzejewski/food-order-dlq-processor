package com.e4developer.foodorderdlqprocessor;

import org.springframework.cloud.sleuth.SpanTextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MySpanTextMap implements SpanTextMap {

    private final Map<String, String> map = new HashMap<>();

    public MySpanTextMap(Map<String, Object> input) {

        for(Map.Entry<String, Object> e : input.entrySet()){
            map.put(e.getKey(), e.getValue().toString());
        }
    }


    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return map.entrySet().iterator();
    }

    @Override
    public void put(String s, String s1) {
        map.put(s, s1);
    }
}
