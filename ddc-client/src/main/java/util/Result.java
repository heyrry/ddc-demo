package util;

import lombok.Data;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Data
public class Result<T> {
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 数据
     */
    private T data;
    /**
     * 错误码
     */
    private String code;
    /**
     * 错误消息
     */
    private String message;

    public Result() {
    }

    public Result(boolean success, T data, String code, String message) {
        this.success = success;
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public static <T>Result<T> success() {
        return new Result<T>(true, null, null, null);
    }

    public static <T>Result<T> success(T data) {
        return new Result<T>(true, data, null, null);
    }

    public static <T>Result<T> fail() {
        return new Result<>(false, null, null, null);
    }

    public static <T> Result<T> fail(Result<?> result) {
        if (result == null) {
            return fail(null, null);
        }
        return fail(result.getCode(), result.getMessage());
    }

    public static <T> Result<T> fail(String errorCode, String errorMessage) {
        return new Result<>(false, null, errorCode, errorMessage);
    }

    public static <T> Result<T> failByMessage(String errorMessage) {
        return new Result<>(false, null, null, errorMessage);
    }

}
