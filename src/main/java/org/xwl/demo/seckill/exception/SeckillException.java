package org.xwl.demo.seckill.exception;

import org.xwl.demo.seckill.constant.SeckillStateEnum;

/**
 * 秒杀相关业务异常
 * Created by liushaoming on 2019-01-14.
 */
public class SeckillException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5184335682665464954L;
	
	private SeckillStateEnum seckillStateEnum;

    public SeckillException(SeckillStateEnum seckillStateEnum) {
        this.seckillStateEnum = seckillStateEnum;
    }

    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }

    public SeckillStateEnum getSeckillStateEnum() {
        return seckillStateEnum;
    }

    public void setSeckillStateEnum(SeckillStateEnum seckillStateEnum) {
        this.seckillStateEnum = seckillStateEnum;
    }
}
