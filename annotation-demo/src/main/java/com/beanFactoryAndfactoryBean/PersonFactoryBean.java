package com.beanFactoryAndfactoryBean;

import org.springframework.beans.factory.FactoryBean;

import java.util.Objects;

/**
 * @author baofeng
 * @date 2022/03/05
 */
public class PersonFactoryBean implements FactoryBean<Persons> {

    public PersonFactoryBean() {
        System.out.println("PersonFactoryBean construct");
    }

    /**
     * 初始化Str
     */
    private String initStr;

    @Override
    public Persons getObject() throws Exception {
        //这里我需要获取对应参数
        Objects.requireNonNull(initStr);
        String[] split = initStr.split(",");
        Persons p=new Persons();
        p.setAge(Integer.parseInt(split[0]));
        p.setName(split[1]);
        //这里可能需要还要有其他复杂事情需要处理
        return p;
    }

    @Override
    public Class<?> getObjectType() {
        return Persons.class;
    }

    @Override
    public boolean isSingleton() {
        return FactoryBean.super.isSingleton();
    }

    public String getInitStr() {
        return initStr;
    }

    public void setInitStr(String initStr) {
        this.initStr = initStr;
    }

}
