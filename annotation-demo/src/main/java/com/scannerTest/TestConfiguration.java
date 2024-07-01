package com.scannerTest;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * @author baofeng
 * @date 2022/08/29
 */
@ComponentScan(basePackages = {"com"}
,excludeFilters =
@ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.person.*")
)
public class TestConfiguration {

}
