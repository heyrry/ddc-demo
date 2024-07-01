package com.invokeTest;


import org.springframework.stereotype.Service;

/**
 * @author baofeng
 * @date 2022/02/20
 */
@ServiceHy
public class SingerServiceImpl implements SingerService, SongerService {

    public SingerServiceImpl() {
        System.out.println("SingerServiceImpl init");
    }

    /**
     * @param no
     */
    @Override
    public void sing(int no) {
        if (no % 2 == 0) {
            System.out.println("偶数，sing <hello>，no=" + no);
        } else {
            System.out.println("sing <default>，no=" + no);
        }
    }
}
