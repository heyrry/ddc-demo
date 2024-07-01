package beans;

import org.springframework.context.ApplicationEvent;

/**
 * @author baofeng
 * @date 2022/08/29
 */
public class MyTransactionEvent extends ApplicationEvent {
    private String name;

    public MyTransactionEvent(String name, Object source) {
        super(source);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
