package com.qiandao.messagingcommon.enumConstant;

public enum CoreEnum {

    CORE_ENUM_RPC_FAIL("C0001", "服务调用异常"),
    CORE_ENUM_TABOO_FAIL("A2008", "禁言用户失败"),
    CORE_ENUM_ADMIN_NO("A2064", "非房间管理员操作");

    private final String code;
    private final String msg;

    CoreEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
