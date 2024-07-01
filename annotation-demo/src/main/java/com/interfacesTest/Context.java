package com.interfacesTest;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author baofeng
 * @date 2022/02/14
 */
public interface Context {
    Context DEFAULT = (type) -> {
        try {
            switch (type) {
                case "a":
                    return new ACar();
                case "b":
                    return new BCar();
                default:
                    return new DefaultCar();
            }
        }
        catch (Exception ex) {
            throw new IllegalStateException("Unable create a default ApplicationContext instance, "
                    + "you may need a custom ApplicationContextFactory", ex);
        }
    };

    /**
     * Creates the {@link ConfigurableApplicationContext application context} for a
     * @param type the web application type
     * @return the newly created application context
     */
    CarType create(String type);
}
