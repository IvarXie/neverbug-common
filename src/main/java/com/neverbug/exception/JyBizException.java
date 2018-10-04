package com.neverbug.exception;

/**
 * 金色家园网自定义根异常
 * 
 * @author guo.guanfei
 *
 */
public class JyBizException extends Exception {

	private static final long serialVersionUID = -2954708683056459053L;

	// 统一错误代码
	protected final int code;

	public JyBizException(int code) {
		super("JYAll Business Exception");
		this.code = code;
	}

	public JyBizException(int code, String msg) {
		super(msg);
		this.code = code;
	}

	public JyBizException(int code, Throwable t) {
		super(t);
		this.code = code;
	}

	public JyBizException(int code, String msg, Throwable t) {
		super(msg, t);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	@Override
	public String toString() {
		return super.toString() + "\nError Code: " + this.code;
	}

}
