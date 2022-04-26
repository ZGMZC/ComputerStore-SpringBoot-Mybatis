package com.cy.store.controller;

import com.cy.store.controller.ex.*;
import com.cy.store.service.ex.*;
import com.cy.store.util.JsonResult;
import com.sun.corba.se.impl.protocol.AddressingDispositionException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpSession;

/*控制层类的基类*/
public class BaseController {
    /*操作成功的状态码*/
    public static final int OK=200;

    //请求处理方法，这个方法的返回值就是需要传递给全端的数据
    //自动将异常对象传递给此方法的参数上
    @ExceptionHandler({ServiceException.class,FileUploadException.class})
    public JsonResult<Void> handleException(Throwable e){
        JsonResult<Void> result=new JsonResult<>(e);
        if(e instanceof UsernameDuplicatedException){
            result.setState(4000);
        }else if (e instanceof UserNotFoundException){
            result.setState(4001);
        }else if (e instanceof PasswordNotMatchException){
            result.setState(4002);
        } else if (e instanceof AddressCountLimitException){
            result.setState(4003);
        }else if (e instanceof AddressNotFoundException){
            result.setState(4004);
        }else if (e instanceof AccessDeniedException){
            result.setState(4005);
        }else if (e instanceof ProductNotFoundException) {
            result.setState(4006);
        }else if (e instanceof CartNotFoundException) {
            result.setState(4007);
        }else if (e instanceof InsertException){
            result.setState(5000);
        }else if (e instanceof UpdateException){
            result.setState(5001);
        }else if (e instanceof DeleteException){
            result.setState(5002);
        }else if (e instanceof FileEmptyException) {
            result.setState(6000);
        } else if (e instanceof FileSizeException) {
            result.setState(6001);
        } else if (e instanceof FileTypeException) {
            result.setState(6002);
        } else if (e instanceof FileStateException) {
            result.setState(6003);
        } else if (e instanceof FileUploadIOException) {
            result.setState(6004);
        }
        return result;
    }

    public final Integer getuidFromSession(HttpSession session){
        return Integer.valueOf(session.getAttribute("uid").toString());
    }
    public final String getUsernameFromSession(HttpSession session){
        return session.getAttribute("username").toString();
    }
}
