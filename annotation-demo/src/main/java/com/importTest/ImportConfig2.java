package com.importTest;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author baofeng
 * @date 2022/03/07
 */
@Configuration
@Import(MyImportBeanDefinitionRegistrar.class)
public class ImportConfig2 {
}
