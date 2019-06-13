package uk.newcastle.jiajie.ble.callback;

/**
 * 用于数据回传的基本回调接口
 */
public interface BaseResultCallback<D> {

    /**
     * 成功拿到数据
     *
     * @param data 回传的数据
     */
    void onSuccess(D data);

    /**
     * 操作失败
     *
     * @param msg 失败的返回的异常信息
     */
    void onFail(String msg);
}
