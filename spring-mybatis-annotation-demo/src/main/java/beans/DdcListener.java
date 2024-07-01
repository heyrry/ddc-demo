package beans;

import com.annotation.EventListen;
import com.annotation.EventParam;
import com.runtime.DomainEventNotifyContext;
import org.springframework.stereotype.Component;

/**
 * @author baofeng
 * @date 2023/11/19
 */
@Component
public class DdcListener {

    @EventListen(listenDomain = {"testDomain"}, listenEvent = {"update"})
    public void listen1(@EventParam(name = DomainEventNotifyContext.ENTITY_ID) String billNo){
        System.out.println("listen1 exec, billNo:" + billNo);
    }
}
