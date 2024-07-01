package model;

import lombok.Data;

/**
 * @author baofeng
 * @date 2022/08/29
 */
@Data
public class Employee {

    private Integer id;

    private String name;

    private String state;

    private String age;

    public Employee(Integer id, String name, String state, String age) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.age = age;
    }
}
