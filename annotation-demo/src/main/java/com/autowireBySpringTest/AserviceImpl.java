package com.autowireBySpringTest;

import com.invokeTest.SingerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author baofeng
 * @date 2022/09/08
 */
@Service
public class AserviceImpl implements AService{

    @Autowired
    SingerService singerService;


    @Override
    public void singTest(int no) {
        singerService.sing(no);
    }

}
