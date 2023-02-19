package com.qiandao.messagingcore.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.qiandao.messagingcommon.enumConstant.AuthEnum;
import com.qiandao.messagingcommon.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class MessagingExceptionControllerAdvice {

    @ExceptionHandler(value = NotRoleException.class)
    public R NotRoleException(NotRoleException e, HttpServletRequest request){
        log.error("角色认证出现异常:{},异常类型:{},请求的url:{}",e.getMessage(),e.getClass(),request.getRequestURL());
        return R.error(AuthEnum.AUTH_ENUM_ROLE_FAIL.getCode(),AuthEnum.AUTH_ENUM_ROLE_FAIL.getMsg());
    }

    @ExceptionHandler(value = NotPermissionException.class)
    public R NotPermissionException(NotPermissionException e, HttpServletRequest request){
        log.error("权限认证出现异常:{},异常类型:{},请求的url:{}",e.getMessage(),e.getClass(),request.getRequestURL());
        return R.error(AuthEnum.AUTH_ENUM_PERM_FAIL.getCode(),AuthEnum.AUTH_ENUM_PERM_FAIL.getMsg());
    }

    @ExceptionHandler(value = NotLoginException.class)
    public R NotLoginException(NotLoginException e, HttpServletRequest request){
        log.error("Token认证出现异常:{},异常类型:{},请求的url:{}",e.getMessage(),e.getClass(),request.getRequestURL());
        return R.error(AuthEnum.AUTH_ENUM_TOKEN.getCode(),AuthEnum.AUTH_ENUM_TOKEN.getMsg());
    }

//    @ExceptionHandler(value = RedisCommandTimeoutException.class)
//    public R NotLoginException(RedisCommandTimeoutException e, HttpServletRequest request){
//        log.error("redis执行超时:{},异常类型:{},请求的url:{}",e.getMessage(),e.getClass(),request.getRequestURL());
//        return R.error(AuthEnum.AUTH_ENUM_REDIS_OVERTIME.getCode(),AuthEnum.AUTH_ENUM_REDIS_OVERTIME.getMsg());
//    }

//    @ExceptionHandler(value = Throwable.class)
//    public R exception(Throwable e) {
//        log.error("系统未知异常:{},异常类型:{},栈信息:{}",e.getMessage(),e.getClass(),e.getStackTrace());
//        return R.error(AuthEnum.AUTH_ENUM_ERROR.getCode(), AuthEnum.AUTH_ENUM_ERROR.getMsg());
//    }
}
