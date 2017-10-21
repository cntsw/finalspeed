package net.fs.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StaticUtil {
    private static Map<String, AtomicLong> counterMap = new LinkedHashMap<>();

    public static int delay;

    private static String last = "";
    public static void counterAdd(String name) {
        AtomicLong num;
        synchronized (counterMap) {
            num = counterMap.get(name);
            if (num == null) {
                counterMap.put(name, new AtomicLong());
                num = new AtomicLong();
            }
        }
        last = name;
        long cnt = num.addAndGet(1);
        if(cnt > 9999){
            num.set(0);
        }
    }

    public static Map<String, AtomicLong> getCounterMap() {
        return counterMap;
    }

    public static String getLastPortStatics(){
        return last +  ":" + counterMap.get(last);
    }
}
