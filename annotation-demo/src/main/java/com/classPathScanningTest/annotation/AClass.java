package com.classPathScanningTest.annotation;

import com.classPathScanningTest.interfaces.ScanClassInterface;

/**
 * @author baofeng
 * @date 2022/02/16
 */
@ScanAnnotation
public class AClass{
    static {
        System.out.println("AClass static");
    }

    public AClass() {
        System.out.println("AClass init");
    }
}
