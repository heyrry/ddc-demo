package com.enums;

/**
 * 切点状态枚举类
 *
 * @author baofeng
 */
public enum PointcutStateEnum {
    pending("待检查"),
    checking("检查中"),
    checked("已检查"),
    noChanged("无更改"),
    created("已创建"),
    submitted("已提交"),
    rolledBack("已回滚"),
    cancelled("已取消");

    private String desc;

    private PointcutStateEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return this.desc;
    }

    public static PointcutStateEnum getPointcutStateEnum(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PointcutStateEnum e : PointcutStateEnum.values()) {
            if (e.name().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
