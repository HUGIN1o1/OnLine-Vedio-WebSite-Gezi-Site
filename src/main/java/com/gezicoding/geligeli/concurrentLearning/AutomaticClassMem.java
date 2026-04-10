package com.gezicoding.geligeli.concurrentLearning;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

public class AutomaticClassMem {
    AtomicInteger atomicInteger = new AtomicInteger(0);
    // AtomicReferenceArray array = new AtomicReferenceArray(10);
    AtomicStampedReference<Object> stampedReference = new AtomicStampedReference<>(null, 0);


}
