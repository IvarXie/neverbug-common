package com.jyall.exception;

public class JypayBizException extends JyBizException {

	private static final long serialVersionUID = 1089760713509980665L;

	public JypayBizException(int code) {
		super(code, "JYPay Business Exception");
	}

	public JypayBizException(int code, String msg) {
		super(code, msg);
	}

	public JypayBizException(int code, Throwable t) {
		super(code, t);
	}

	public JypayBizException(int code, String msg, Throwable t) {
		super(code, msg, t);
	}
}
