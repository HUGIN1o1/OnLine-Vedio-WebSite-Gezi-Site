package com.gezicoding.geligeli.utils;

import cn.hutool.bloomfilter.BitMapBloomFilter;

public class BitMapBloomUtil {

    private static final BitMapBloomFilter BIT_MAP_BLOOM_FILTER = new BitMapBloomFilter(10);

    public static boolean contains(String key) {
        return BIT_MAP_BLOOM_FILTER.contains(key);
    }

    public static void add(String key) {
        BIT_MAP_BLOOM_FILTER.add(key);
    }

}
