package org.blockchain.enums;

public enum SdkErrorCodeEnum {
	
	SUCCESS(0, "成功"),
	FAIL(1, "失败"),
    NOT_EXIST(4, "交易不存在"),
    ;
	
	
    private final Integer code;
    private final String msg;

    private SdkErrorCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

	public String getMsg() {
		return msg;
	}
    
}
