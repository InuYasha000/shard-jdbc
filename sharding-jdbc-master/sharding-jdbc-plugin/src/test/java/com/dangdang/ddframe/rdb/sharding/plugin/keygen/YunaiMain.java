package com.dangdang.ddframe.rdb.sharding.plugin.keygen;

import org.junit.Test;

/**
 * 芋艿临时测试
 */
public class YunaiMain {

    @Test
    public void testHostNameKeyGenerator() {
        new HostNameKeyGenerator();
    }

    @Test
    public void testLeftShift() {
        System.out.println(Long.MIN_VALUE);
//        Long max = 1L << (62);
        Long max = (1L << 62) + (1L << 61);
        System.out.println(max);
    }

}
