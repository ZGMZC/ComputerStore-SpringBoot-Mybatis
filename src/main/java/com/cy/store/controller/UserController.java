package com.cy.store.controller;

import com.cy.store.controller.ex.*;
import com.cy.store.entity.User;
import com.cy.store.service.IUserService;
import com.cy.store.service.ex.InsertException;
import com.cy.store.service.ex.ServiceException;
import com.cy.store.service.ex.UsernameDuplicatedException;
import com.cy.store.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//@Controller
@RestController  //@Controller+@ResponseBody
@RequestMapping("users")
public class UserController extends BaseController {

    @Autowired
    private IUserService userService;

/*    @RequestMapping("reg")
    //@ResponseBody  //表示此方法的响应结果以json格式进行数据的响应给到前端--将json数据写入响应包
    public JsonResult<Void> reg(User user){
        //创建响应结果对象
        JsonResult<Void> result=new JsonResult<>();
        try {
            userService.reg(user);
            result.setState(200);
            result.setMessage("用户注册成功");
        } catch (UsernameDuplicatedException e) {
            result.setState(4000);
            result.setMessage("用户名被占用");
        }catch (InsertException e){
            result.setState(5000);
            result.setMessage("注册时产生未知异常");
        }
        return result;
    }*/
    /*
    * 1.接收数据方式：请求处理方法的参数列表设置为POJO类型来接受前端数据
    * Springboot会将前端的url地址中参数名称和POJO类的属性名进行比较*/
    @RequestMapping("reg")
    //@ResponseBody  //表示此方法的响应结果以json格式进行数据的响应给到前端--将json数据写入响应包
    public JsonResult<Void> reg(User user){
        userService.reg(user);
        return new JsonResult<>(OK);
    }
    /*
     * 2.接收数据方式：请求处理方法的参数列表设置为非POJO类型来接受前端数据
     * Springboot会将请求的参数名和方法的参数名进行比较*/
    @RequestMapping("login")
    public JsonResult<User> login(String username, String password, HttpSession session){
        User data = userService.login(username, password);
        //向session对象中完成数据的绑定
        session.setAttribute("uid",data.getUid());
        session.setAttribute("username",data.getUsername());
        //获取session中的数据
        System.out.println(getuidFromSession(session));
        System.out.println(getUsernameFromSession(session));

        return new JsonResult<User>(OK,data);
    }

    @RequestMapping("change_password")
    public JsonResult<Void> changePassword(String oldPassword,String newPassword,HttpSession session){
        Integer uid=getuidFromSession(session);
        String username=getUsernameFromSession(session);
        userService.changePassword(uid,username,oldPassword,newPassword);
        return new JsonResult<>(OK);
    }

    @RequestMapping("get_by_uid")
    public JsonResult<User> getByUid(HttpSession session){
        User data = userService.getByUid(getuidFromSession(session));
        return new JsonResult<>(OK,data);
    }

    @RequestMapping("change_info")
    public JsonResult<Void> changeInfo(User user,HttpSession session){
        Integer uid=getuidFromSession(session);
        String username=getUsernameFromSession(session);
        userService.changeInfo(uid,username,user);
        return new JsonResult<>(OK);
    }

    /**
     * MultipartFile接口是SpringMVC提供的一个接口，这个接口为我们包装了获取文件类型的数据
     * SpringBoot整合了SpringMVC，只需要在处理请求的方法参数列表上生声明一个参数类型为MultipartFile的参数
     * 然后自动将传递给服务的文件数据赋值给这个参数
     * @param session
     * @param file
     * @return
     */
    //设置上传文件的最大值
    public static final int AVATAR_MAX_SIZE=10*1024*1024;
    //限制上传文件的类型
    public static final List<String> AVATAR_TYPE=new ArrayList<>();
    static {
        AVATAR_TYPE.add("image/jpeg");
        AVATAR_TYPE.add("image/png");
        AVATAR_TYPE.add("image/jpg");
        AVATAR_TYPE.add("image/bmp");
        AVATAR_TYPE.add("image/gif");
    }
    @RequestMapping("change_avatar")
    public JsonResult<String> changeAvatar(HttpSession session, @RequestParam("file") MultipartFile file){
        //判断文件是否为空
        if(file.isEmpty()) throw new FileEmptyException("文件为空");
        String contentType = file.getContentType();
        if(!AVATAR_TYPE.contains(contentType)) throw new FileTypeException("文件类型不支持");
        if(file.getSize()>AVATAR_MAX_SIZE) throw new FileSizeException("文件大小超出限制");
        //获取上传的文件的目录结构
        String parent = session.getServletContext().getRealPath("upload");
        //file对象指向这个路径，file是否存在
        File dir=new File(parent);
        if(!dir.exists()){//检测目录是否存在
            dir.mkdirs();//创建当前目录
        }
        //获取文件名称，并随机重新命名
        String originalFilename = file.getOriginalFilename();
        int index=originalFilename.lastIndexOf(".");
        String suffix=originalFilename.substring(index);
        String filename = UUID.randomUUID().toString().toUpperCase()+suffix;
        File dest=new File(dir,filename);//空文件
        //将参数file中的数据写入到空文件中
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new FileUploadIOException("文件读写异常");
        }
        Integer uid=getuidFromSession(session);
        String username=getUsernameFromSession(session);
        //返回头像路径
        String avatar="/upload/"+filename;
        userService.changeAvatar(uid,avatar,username);
        //返回用户头像路径给前端
        return new JsonResult<>(OK,avatar);
    }
}
