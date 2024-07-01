package com.propertiesUtil;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

/**
 * @author baofeng
 * @date 2022/03/13
 */
@Component
//@Configuration
 @PropertySource("classpath:/dbconfig.properties")  		//指定 properties 文件，不是必须的
public class PropertiesUtil implements EmbeddedValueResolverAware {

    private StringValueResolver resolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 获取属性，直接传入属性名称即可
     * @param key
     * @return
     */
    public String getPropertiesValue(String key) {
        StringBuilder name = new StringBuilder("${").append(key).append("}");
        return resolver.resolveStringValue(name.toString());
    }

}

