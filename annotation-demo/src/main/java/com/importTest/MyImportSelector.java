package com.importTest;

import com.invokeTest.SingerService;
import com.springFactoriesLoader.LoaderMain;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author baofeng
 * @date 2022/03/07
 */
public class MyImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        System.out.println("这里是选择导入类内部selectImports()：选择要导入的类");
        // Person类 需要导入
//        return new String[]{"com.importTest.Person"};
        List<String> result = new ArrayList<>();
        List<String> classList = SpringFactoriesLoader.loadFactoryNames(SingerService.class, this.getClass().getClassLoader());
        System.out.println("classList=" + classList);
        result.addAll(classList);
        result.add("com.importTest.Person");
//        classList.add("com.importTest.Person");
        String[] array = new String[result.size()];
        System.out.println("array=" + array);

        return result.toArray(array);
    }

}
