package com.classPathScanningTest.interfaces;

import com.classPathScanningTest.annotation.ScanAnnotation;

/**
 * @author baofeng
 * @date 2022/02/16
 */
@ScanAnnotation
public class ScanClassInterfaceImpl implements ScanClassInterface{
    static {
        System.out.println("CClass static");
    }

    public ScanClassInterfaceImpl() {
        System.out.println("CClass init");
    }
}
