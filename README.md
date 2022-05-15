## SpringBoot项目—电脑商城
### 系统概述
#### 系统开发及运行环境
1. 项目管理工具：Maven3.6.3
2. 服务器架构：SpringBoot2.4.7+MaBatis2.1.4+AJAX
#### 项目分析
1. 本项目涉及到的数据：用户、收货地址、商品、购物车、订单
2. 数据开发流程：用户-收货地址-商品-购物车-订单
3. 用户功能：登录、注册、修改密码、修改个人资料、上传头像
4. 收货地址：增加收货地址、删除收货地址、设为默认收货地址、显示地址列表
5. 商品：热销商品排行、商品加入购物车、查看商品信息
6. 购物车：查看当前用户的购物车、删除商品、增加商品数量
7. 订单：购物车勾选商品进行订单提交、计算提交订单的商品数量和总价格
8. 数据库：当处理一种新的数据时，先创建该数据在数据库中的数据表，然后在项目中创建该数据表对应的实体
9. 功能开发顺序：持久层-业务层-控制器-前端页面
#### 项目亮点
- MD5算法加密
- 自定义异常处理
- 处理拦截器HandInterceptor
- Session暂存传输数据、Cookie保存用户头像
- SpringBoot+Mybatis实现CRUD功能
- 通过Json、Ajax实现前后端数据传输
#### 创建数据库
```mysql
create database store character set utf8
```
#### 创建Spring Initializr项目
>本质上Spring Initializr是一个Web应用程序，它提供了一个基本的项目结构，能够帮助开发者快速构建一个基础的Spring Boot项目。在创建Spring Initializr类型的项目时需在计算机连网的状态下进行创建

>向项目中添加SpringWeb、MyBatis Framework、MySQL Driver依赖
1. 导入数据库
在resources文件下的application.properties文件中添加数据源的配置
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/store?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=guaiying1002
```
2. Json数据处理
为了便于查询JSON数据，隐藏没有值的属性，减少流量的消耗，服务器不应该向客户端响应为null的属性。可以在属性或类之前添加@JsonInclude(value=Include.NON_NULL)，也可以在application.properties中添加全局的配置
```properties
# 服务器向客户端不响应为null的属性
spring.jackson.default-property-inclusion=NON_NULL
```
3. 访问路径
SpringBoot项目的默认访问路径名称是"/"，如果需要修改可以手动在配置文件中指定访问项目路径的项目名
```properties
server.servlet.context-path=/store
```

### 用户功能模块
#### 用户注册
##### 数据库
```mysql
use store;
CREATE TABLE t_user (
	uid INT AUTO_INCREMENT COMMENT '用户id',
	username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
	password CHAR(32) NOT NULL COMMENT '密码',
	salt CHAR(36) COMMENT '盐值',
	phone VARCHAR(20) COMMENT '电话号码',
	email VARCHAR(30) COMMENT '电子邮箱',
	gender INT COMMENT '性别:0-女，1-男',
	avatar VARCHAR(50) COMMENT '头像',
	is_delete INT COMMENT '是否删除：0-未删除，1-已删除',
	created_user VARCHAR(20) COMMENT '日志-创建人',
	created_time DATETIME COMMENT '日志-创建时间',
	modified_user VARCHAR(20) COMMENT '日志-最后修改执行人',
	modified_time DATETIME COMMENT '日志-最后修改时间',
	PRIMARY KEY (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
##### 实体类
1. BaseEntity
> 项目中许多实体类都会有日志相关的四个属性，所以在创建实体类之前，应先创建这些实体类的基类，将4个日志属性声明在基类中BaseEntity
```java
package com.cy.store.entity;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
//作为实体类的基类
@Data
public class BaseEntity implements Serializable {
    private String createdUser;
    private Date createdTime;
    private String modifiedUser;
    private Date modifiedTime;
}
```
> 该类的作用就是用于被其它实体类继承，可以声明为抽象类
2. 用户数据实体类
> 继承BaseEntity类，在类中声明与数据表中对应的属性。
```java
package com.cy.store.entity;
import lombok.Data;
import org.springframework.stereotype.Component;
//用户的实体类
@Data
public class User extends BaseEntity{
    private Integer uid;
    private String username;
    private String password;
    private String salt;
    private String  phone;
    private String email;
    private Integer gender;
    private String avatar;
    private Integer isDelete;
}
```

##### 持久层
###### 1. 测试数据库连接
在StoreApplicationTests测试类中编写并执行“获取数据库连接”的单元测试，以检查数据库连接的配置是否正确。
```java
package com.cy.store;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import java.sql.SQLException;
@SpringBootTest
class StoreApplicationTests {
    @Autowired
    private DataSource dataSource;
    @Test
    void contextLoads() {
    }
    @Test
    void getConnection() throws SQLException {
        System.out.println(dataSource.getConnection());
    }
}
```
###### 2. SQL语句
由于数据表中用户名字段被设计为UNIQE，在执行插入数据之前，还应该检查该用户名是否已经被注册，因此需要有“根据用户名查询用户数据的功能”
```mysql
select * from t_user where username=?
```
> select * 搜索效率不低且数据量大，不推荐在项目中使用。但是由于需要额外创建与搜索结果对应的实体，故本项目中使用select *

###### 3. 接口与抽象方法
创建UserMapper接口，并在接口中添加抽象方法
```java
package com.cy.store.mapper;

import com.cy.store.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import java.util.Date;
//用户模块的持久层接口
@Repository
public interface UserMapper {
    /**
     * 插入用户的数据
     * @param user  用户的数据
     * @return 受影响的行数，可以根据返回值判断是否成功
     */
    Integer insert(User user);
    /**
     * 根据用户名来查询用户的数据
     * @param username 用户名
     * @return 如果找到对应的用户则返回这个用户的数据，如果没有找到则返回null
     */
    User findByUsername(String username);
}
```
> 可以使用注解方式，
@Insert({"INSERT INTO  t_user 
(username, password, salt, phone, email, gender, avatar, is_delete, created_user, created_time, modified_user, modified_time)
VALUES
(#{username}, #{password}, #{salt}, #{phone}, #{email}, #{gender}, #{avatar}, #{isDelete}, #{createdUser}, #{createdTime}, #{modifiedUser}, #{modifiedTime}) "})

>本项目中使用SQL映射文件方式，及创建UserMapper.xml映射文件
>MyBatis与Spring整合后需要实现实体与数据表的映射关系。实现实体与数据表的映射关系可以在Mapper接口上添加@Mapper注解，但是建议直接在SpringBoot启动类中加@MapperScan("mapper接口所在包")注解，这样不需要对每个Mapper接口都添加@Mapper注解
```java
@SpringBootApplication
@MapperScan("com.cy.store.mapper")
public class StoreApplication {
	public static void main(String[] args) {
		SpringApplication.run(StoreApplication.class, args);
	}
}
```

###### 4. 配置SQL映射
在src/main/resources下创建mapper文件夹，并在该文件夹下创建UserMapper.xml映射文件，进行以上两个抽象方法的映射配置。
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cy.store.mapper.UserMapper">
    <resultMap id="UserEntityMap" type="com.cy.store.entity.User">
        <id column="uid" property="uid"/>
        <result column="is_delete" property="isDelete"/>
        <result column="created_user" property="createdUser"/>
        <result column="created_time" property="createdTime"/>
        <result column="modified_user" property="modifiedUser"/>
        <result column="modified_time" property="modifiedTime"/>
    </resultMap>

    <!-- 插入用户数据：Integer insert(User user) -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="uid">
        INSERT INTO
            t_user (username, password, salt, phone, email, gender, avatar, is_delete, created_user, created_time, modified_user, modified_time)
        VALUES
        (#{username}, #{password}, #{salt}, #{phone}, #{email}, #{gender}, #{avatar}, #{isDelete}, #{createdUser}, #{createdTime}, #{modifiedUser}, #{modifiedTime})
    </insert>

    <!-- 根据用户名查询用户数据：User findByUsername(String username) -->
    <select id="findByUsername" resultMap="UserEntityMap">
        SELECT
            *
        FROM
            t_user
        WHERE
            username = #{username}
    </select>
</mapper>
```
>由于这是项目中第一次使用SQL映射，所以需要在application.properties中添加mybatis.mapper-locations属性的配置，以指定xml文件的位置
```properties
mybatis.mapper-locations=classpath:mapper/*.xml
```

###### 5. 编写测试方法
> 单元测试方法必须为public修饰，方法的返回值类型为void，方法不能有参数列表，并且方法被@Test注解修饰。

##### 业务层
###### 1. 业务定位
1. 业务：一套完整的数据处理流程，通常表现为用户认为的一个功能，但是在开发时对应多项数据操作，在项目中，通过业务控制每个“功能”（例如注册、登录等）的处理流程和相关逻辑。
2. 流程：先做什么，再做什么。例如：注册时，需要先判断用户名是否被占用，再决定是否完成注册。
3. 逻辑：能干什么，不能干什么。例如：注册时，如果用户名被占用，则不允许注册；反之；允许注册。
4. 业务的主要作用是保障数据安全和数据的完整性、有效性。

###### 2. 规划异常
1. 为了便于统一管理自定义异常，应先创建自定义异常的基类异常ServiceException，继承自RuntimeException类，并从父类生成子类的五个构造方法。
2. 当用户进行注册时，可能会因为用户名被占用而导致无法正常注册，此时需要抛出**用户名被占用**的异常，UsernameDuplicateException，继承ServiceException类。
3. 当用户进行注册时，会执行数据库的**Insert**操作，该操作可能会失败，此时抛出**插入异常**，InsertException。
###### 3. 接口与抽象方法
创建业务层接口，IUserService，并添加抽象方法
```java
package com.cy.store.service;
import com.cy.store.entity.User;
/** 处理用户数据的业务层接口 */
public interface IUserService {
    /**
     * 用户注册
     * @param user 用户数据
     */
    void reg(User user);
}
```
> 创建业务层接口目的是为了解耦，关于业务层抽象方法的设计原则：
> 仅以操作成功为前提来设计返回值类型，不考虑操纵失败的情况
> 方法名称可以自定义，通常与用户操作的功能相关
> 方法的参数列表根据执行的具体业务功能来确定，需要哪些数据就设计哪些数据。通常情况下参数需要足以调用持久层对应的相关功能，同时还要满足参数是客户端可以传递给控制器
> 方法中使用抛出异常的方式来表示操作失败

###### 4. 实现抽象方法
创建ServiceImpl业务层实现类，并实现IUserService接口，在类之前添加@Service注解，并在类中添加持久层UserMapper对象
```java
package com.cy.store.service.impl;
import com.cy.store.entity.User;
import com.cy.store.mapper.UserMapper;
import com.cy.store.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/** 处理用户数据的业务层实现类 */
@Service
public class UserServiceImpl implements IUserService {
	@Autowired
	private UserMapper userMapper;
	@Override
	public void reg(User user) {
		// 根据参数user对象获取注册的用户名
		String username = user.getUsername();
		// 调用持久层的User findByUsername(String username)方法，根据用户名查询用户数据
		User result = userMapper.findByUsername(username);
		// 判断查询结果是否不为null
		if (result != null) {
			// 是：表示用户名已被占用，则抛出UsernameDuplicateException异常
			throw new UsernameDuplicateException("尝试注册的用户名[" + username + "]已经被占用");
		}
		// 创建当前时间对象
		Date now = new Date();
		// 补全数据：加密后的密码
		String salt = UUID.randomUUID().toString().toUpperCase();
		String md5Password = getMd5Password(user.getPassword(), salt);
		user.setPassword(md5Password);
		// 补全数据：盐值
		user.setSalt(salt);
		// 补全数据：isDelete(0)
		user.setIsDelete(0);
		// 补全数据：4项日志属性
		user.setCreatedUser(username);
		user.setCreatedTime(now);
		user.setModifiedUser(username);
		user.setModifiedTime(now);
		// 表示用户名没有被占用，则允许注册
		// 调用持久层Integer insert(User user)方法，执行注册并获取返回值(受影响的行数)
		Integer rows = userMapper.insert(user);
		// 判断受影响的行数是否不为1
		if (rows != 1) {
			// 是：插入数据时出现某种错误，则抛出InsertException异常
			throw new InsertException("添加用户数据出现未知错误，请联系系统管理员");
		}
	}
	/**
	 * 执行密码加密
	 * @param password 原始密码
	 * @param salt 盐值
	 * @return 加密后的密文
	 */
	private String getMd5Password(String password, String salt) {
		/*
		 * 加密规则：
		 * 1、无视原始密码的强度
		 * 2、使用UUID作为盐值，在原始密码的左右两侧拼接
		 * 3、循环加密3次
		 */
		for (int i = 0; i < 3; i++) {
			password = DigestUtils.md5DigestAsHex((salt + password + salt).getBytes()).toUpperCase();
		}
		return password;
	}
}
```
###### 5. 编写测试方法
##### 控制层
###### 1.创建响应结果类
创建JsonResult响应结果类型
```java
package com.cy.store.util;
import java.io.Serializable;

/**
 * 响应结果类
 * @param <E> 响应数据的类型
 */
 @Data
public class JsonResult<E> implements Serializable {
    /** 状态码 */
    private Integer state;
    /** 状态描述信息 */
    private String message;
    /** 数据 */
    private E data;
    public JsonResult() {
        super();
    }
    public JsonResult(Integer state) {
        super();
        this.state = state;
    }
    /** 出现异常时调用 */
    public JsonResult(Throwable e) {
        super();
        // 获取异常对象中的异常信息
        this.message = e.getMessage();
    }
    public JsonResult(Integer state, E data) {
        super();
        this.state = state;
        this.data = data;
    }
}
```
###### 2. 设计请求
```
>请求路径：/users/reg
>请求参数：User user
>请求类型：POST
>响应结果：JsonResult<Void>
```
###### 3.处理请求
创建提供控制器类的基类BaseController，在其中定义响应成功的状态码及统一处理异常的方法
> @ExceptionHandler注解用于统一处理方法抛出的异常，当我们使用这个注解时，需要定义一个异常的处理方法，再给这个方法加上@ExceptionHandler注解，这个方法就会处理类中其它方法（被@RequestMappering注解）抛出的异常，@ExceptionHandler注解中可以添加参数，参数是某个异常类的class，代表这个方法专门处理该类异常
```java
package com.cy.store.controller;
import com.cy.store.service.ex.InsertException;
import com.cy.store.service.ex.ServiceException;
import com.cy.store.service.ex.UsernameDuplicateException;
import com.cy.store.util.JsonResult;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** 控制器类的基类 */
public class BaseController {
    /** 操作成功的状态码 */
    public static final int OK = 200;
    /** @ExceptionHandler用于统一处理方法抛出的异常 */
    @ExceptionHandler(ServiceException.class)
    public JsonResult<Void> handleException(Throwable e) {
        JsonResult<Void> result = new JsonResult<Void>(e);
        if (e instanceof UsernameDuplicateException) 		           				result.setState(4000);
        else if (e instanceof InsertException) result.setState(5000);
        return result;
    }
}
```
创建UserController控制器类，在类的声明之前添加@RestController和@RequestMapping("users")注解，在类中添加IUserService业务对象并使用@Autowired注解修饰
```java
package com.cy.store.controller;
import com.cy.store.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** 处理用户相关请求的控制器类 */
@RestController
@RequestMapping("users")
public class UserController {
    @Autowired
    private IUserService userService;
}
```
在类中添加处理请求的用户注册方法
```java
@RequestMapping("reg")
public JsonResult<Void> reg(User user) {
        // 调用业务对象执行注册
        userService.reg(user);
        // 返回
        return new JsonResult<Void>(OK);
}
```
###### 4. 编写测试
启动项目，访问打开浏览器访问http://localhost:8080/users/reg?username=XXX&password=XXX 进行测试
##### 前端页面
###### 1. 前后端通过Json传送数据
在register.html页面中body标签内部的最后，添加script标签用于编写JavaScript程序。请求的url中需要添加项目的访问名称
```js
<script type="text/javascript">
    $("#btn-reg").click(function() {
        console.log($("#form-reg").serialize());
        $.ajax({
            url: "/users/reg",
            type: "POST",
            data: $("#form-reg").serialize(),
            dataType: "json",
            success: function(json) {
                if (json.state == 200) {
                    alert("注册成功！");
                    // location.href = "login.html";
                } else {
                    alert("注册失败！" + json.message);
                }
            }
        });
	});
</script>
```
> serialize()方法通过序列化表单值，创建URL编码文本字符串

###### 2. 编写测试
完成后启动项目，打开浏览器访问http://localhost:8080/web/register.html 页面并进行注册
> 由于没有验证数据，即使没有填写用户名或密码，也可以注册成功

#### 用户登录
##### 持久层
###### 1. 规划SQL语句
用户登录功能需要执行的SQL语句是根据用户名查询用户数据，再判断密码是否正确
```mysql
select * from t_user where username=?
```
> 以上SQL语句对应的后台开发已经完成， 无需再次开发

###### 2. 接口与抽象方法
> 无需再次开发

###### 3.配置SQL映射
> 无需再次开发

##### 业务层
###### 1. 规划异常
1. 如果用户名不存在则登录失败，抛出UserNotFoundException异常
2. 如果用户的isDelete字段的值为1，则表示当前用户数据被标记为“已删除”，需进行登录失败操作同时抛出UserNotFoundException
3. 如果密码错误则进行登录失败操作，同时抛出PasswordNotMatchException异常
###### 2. 接口与抽象方法
在IUserService接口中添加登录功能的抽象方法
```java
/**
 * 用户登录
 * @param username 用户名
 * @param password 密码
 * @return 登录成功的用户数据
 */
User login(String username, String password);
```
> 当登录成功后需要获取该用户的id，以便于后续识别该用户的身份，并且还需要获取该用户的用户名、头像等数据，用于显示在软件的界面中，需使用可以封装用户id、用户名和头像的数据类型来作为登录方法的返回值

###### 3. 实现抽象方法
在UserServiceImpl类中添加login(String username, String password)方法并分析业务逻辑
```java
@Override
public User login(String username, String password) {
    // 调用userMapper的findByUsername()方法，根据参数username查询用户数据
    User result = userMapper.findByUsername(username);
    // 判断查询结果是否为null
    if (result == null) {
        // 是：抛出UserNotFoundException异常
        throw new UserNotFoundException("用户数据不存在的错误");
    }
    // 判断查询结果中的isDelete是否为1
    if (result.getIsDelete() == 1) {
        // 是：抛出UserNotFoundException异常
        throw new UserNotFoundException("用户数据不存在的错误");
    }
    // 从查询结果中获取盐值
    String salt = result.getSalt();
    // 调用getMd5Password()方法，将参数password和salt结合起来进行加密
    String md5Password = getMd5Password(password, salt);
    // 判断查询结果中的密码，与以上加密得到的密码是否不一致
    if (!result.getPassword().equals(md5Password)) {
        // 是：抛出PasswordNotMatchException异常
        throw new PasswordNotMatchException("密码验证失败的错误");
    }
    // 创建新的User对象
    User user = new User();
    // 将查询结果中的uid、username、avatar封装到新的user对象中
    user.setUid(result.getUid());
    user.setUsername(result.getUsername());
    user.setAvatar(result.getAvatar());
    // 返回新的user对象
    return user;
}
```
###### 4. 编写测试
##### 控制层
###### 1.处理异常
处理用户登录功能时，在业务层抛出了UserNotFoundException和PasswordNotMatchException异常，而这两个异常均未被处理过。则应在BaseController类的处理异常的方法中，添加这两个分支进行处理
```java
@ExceptionHandler(ServiceException.class)
public JsonResult<Void> handleException(Throwable e) {
	JsonResult<Void> result = new JsonResult<Void>(e);
	if (e instanceof UsernameDuplicateException) {
		result.setState(4000);
	} else if (e instanceof UserNotFoundException) {
		result.setState(4001);
	} else if (e instanceof PasswordNotMatchException) {
		result.setState(4002);
	} else if (e instanceof InsertException) {
		result.setState(5000);
	}
	return result;
}
```
###### 2.设计请求
设计用户提交的请求，并设计响应的方式
```
请求路径：/users/login
请求参数：String username, String password
请求类型：POST
响应结果：JsonResult<User>
```
###### 3.处理请求
1. 在UserController类中添加处理登录请求的login方法
```java
@RequestMapping("login")
public JsonResult<User> login(String username, String password) {
	// 调用业务对象的方法执行登录，并获取返回值
	User data = userService.login(username, password);
	// 将以上返回值和状态码OK封装到响应结果中并返回
	return new JsonResult<User>(OK, data);
}
```
###### 4. 编写测试
完成后启动项目，访问http://localhost:8080/users/login?username=XXX&password=XXX 请求进行登录。
##### 前端页面
###### 1. 前后端通过Json传送数据
在login.html页面中body标签内部的最后，添加script标签用于编写JavaScript程序
```js
<script type="text/javascript">
    $("#btn-login").click(function() {
    $.ajax({
        url: "/users/login",
        type: "POST",
        data: $("#form-login").serialize(),
        dataType: "json",
        success: function(json) {
            if (json.state == 200) {
                alert("登录成功！");
                location.href = "index.html";
            } else {
                alert("登录失败！" + json.message);
            }
        }
    });
});
</script>
```
###### 2.编写测试
完成后启动项目，打开浏览器访问http://localhost:8080/web/login.html 页面并进行登录。

#### 拦截器Interceptor
在SpringMVC中拦截请求是通过处理器拦截器HandlerInterceptor来实现的，它拦截的目标是请求的地址，在SpringMVC中定义一个拦截器，需要实现HandlerInterceptor接口
##### 1. HandlerInterceptor
###### 1. preHandler()
该方法是在请求处理之前调用。SpringMVC中的Interceptor是链式的调用，在一个应用或一个请求中可以同时存在多个Interceptor。每个Interceptor的调用会依据它的声明顺序依次执行，而且最先执行的都是Interceptor中的preHandler()方法，所以可以在这个方法中进行一些前置初始化操作或者是对当前请求的一个预处理，也可以在这个方法中进行一些判断来决定请求是否要继续进行下去。该方法的返回值是boolean类型，当返回false时，表示请求结束，后续的Interceptor和Controller都不会再执行；当返回值true时，就会继续调用下一个Interceptor的preHandle方法，如果已经是最后一个Interceptor时，就会调用当前请求的Controller方法。
###### 2. postHandle()
该方法将在当前请求进行处理之后，也就是Controller方法调用之后执行，但是它会在DispatcherServlet进行视图返回渲染之前被调用，所以我们可以在这个方法中对Controller处理之后的ModelAndView对象进行操作。postHandle方法被调用的方向跟preHandle是相反的，也就是说先声明的Interceptor的postHandle方法反而会后执行。如果当前Interceptor的preHandle()方法返回值为false，则此方法不会被调用。
###### 3. afterCompletion()
该方法将在整个当前请求结束之后，也就是在DispatcherServlet渲染了对应的视图之后执行。这个方法的主要作用是用于进行资源清理工作。如果当前Interceptor的preHandle()方法返回值为false，则此方法不会被调用。
##### 2. WebMvcConfigurer
在SpringBoot项目中，如果想要自定义一些Interceptor、ViewResolver、MessageConverter，该如何实现呢？在SpringBoot 1.5版本都是靠重写WebMvcConfigurerAdapter类中的方法来添加自定义拦截器、视图解析器、消息转换器等。而在SpringBoot 2.0版本之后，该类被标记为@Deprecated。因此我们只能靠实现WebMvcConfigurer接口来实现。

WebMvcConfigurer接口中的核心方法之一addInterceptors(InterceptorRegistry registry)方法表示添加拦截器。主要用于进行用户登录状态的拦截，日志的拦截等。
- addInterceptor：需要一个实现HandlerInterceptor接口的拦截器实例
- addPathPatterns：用于设置拦截器的过滤路径规则；addPathPatterns("/**") 对所有请求都拦截
- excludePathPatterns：用于设置不需要拦截的过滤规则
##### 3. 项目添加拦截器功能
1. 分析：项目中很多操作都是需要先登录才可以执行的，如果在每个请求处理之前都编写代码检查
Session中有没有登录信息是不现实的，所以应使用拦截器解决该问题
2. 创建拦截器类，LoginInterceptor，并实现org.springframework.web.servlet.HandlerInterceptor接口
```java
package com.cy.store.interceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/** 定义处理器拦截器 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getSession().getAttribute("uid") == null) {
            response.sendRedirect("/web/login.html");
            return false;
        }
        return true;
    }
}
```
3. 创建LoginInterceptorConfigurer拦截器的配置类并实现org.springframework.web.servlet.config.annotation.WebMvcConfigurer接口，配置类需要添加@Configruation注解修饰。
```java
package com.cy.store.config;
import com.cy.store.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.ArrayList;
import java.util.List;
/** 注册处理器拦截器 */
@Configuration
public class LoginInterceptorConfigurer implements WebMvcConfigurer {
    /** 拦截器配置 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 创建拦截器对象
        HandlerInterceptor interceptor = new LoginInterceptor();
        // 白名单
        List<String> patterns = new ArrayList<String>();
        patterns.add("/bootstrap3/**");
        patterns.add("/css/**");
        patterns.add("/images/**");
        patterns.add("/js/**");
        patterns.add("/web/register.html");
        patterns.add("/web/login.html");
        patterns.add("/web/index.html");
        patterns.add("/web/product.html");
        patterns.add("/users/reg");
        patterns.add("/users/login");
        patterns.add("/districts/**");
        patterns.add("/products/**");
        // 通过注册工具添加拦截器
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**") // 表示拦截的url是什么
                .excludePathPatterns(patterns); //白名单
    }
}
```
#### 会话Session
1. 重新构建login()方法，登录成功后将uid和username存入到HttpSession对象中
```java
@RequestMapping("login")
public JsonResult<User> login(String username, String password, HttpSession session) {
    // 调用业务对象的方法执行登录，并获取返回值
    User data = userService.login(username, password);
    //登录成功后，将uid和username存入到HttpSession中
    session.setAttribute("uid", data.getUid());
    session.setAttribute("username", data.getUsername());
    // 将以上返回值和状态码OK封装到响应结果中并返回
    return new JsonResult<User>(OK, data);
}
```
2. 在父类BaseController中添加从HttpSession对象中获取uid和username的方法，以便于后续快捷的获取这两个属性的值
```java
/**
 * 从HttpSession对象中获取uid
 * @param session HttpSession对象
 * @return 当前登录的用户的id
 */
protected final Integer getUidFromSession(HttpSession session) {
	return Integer.valueOf(session.getAttribute("uid").toString());
}

/**
 * 从HttpSession对象中获取用户名
 * @param session HttpSession对象
 * @return 当前登录的用户名
 */
protected final String getUsernameFromSession(HttpSession session) {
	return session.getAttribute("username").toString();
}
```
#### 修改密码
##### 持久层
###### 1. 规划SQL语句
用户修改密码时需要执行的SQL语句大致是：
```mysql
update t_user set password=?, modified_user=?,modified_time=? where uid=?
```
在执行修改密码之前，还应检查用户数据是否存在、并检查用户数据是否被标记为“已删除”、并检查原密码是否正确，这些检查都可以通过查询用户数据来辅助完成：
```mysql
select * from t_user where uid=?
```
###### 2. 接口与抽象方法
在UserMapper接口添加updatePasswordByUid(Integer uid,String password,String modifiedUser,Date modifiedTime)抽象方法
```java
/**
 * 根据uid更新用户的密码
 * @param uid 用户的id
 * @param password 新密码
 * @param modifiedUser 最后修改执行人
 * @param modifiedTime 最后修改时间
 * @return 受影响的行数
 */
Integer updatePasswordByUid(
		@Param("uid") Integer uid, 
		@Param("password") String password, 
		@Param("modifiedUser") String modifiedUser, 
		@Param("modifiedTime") Date modifiedTime);

/**
 * 根据用户id查询用户数据
 * @param uid 用户id
 * @return 匹配的用户数据，如果没有匹配的用户数据，则返回null
 */
User findByUid(Integer uid);
```
> 用注解来简化xml配置时，@Param注解的作用是给参数命名，参数命名后就能根据名字得到参数值，正确的将参数传入sql语句中。
> @Param("参数名")注解中的参数名需要和sql语句中的#{参数名}的参数名保持一致

###### 3. 配置SQL映射
在UserMapper.xml中配置updatePassWordByUid()、findByUid()抽象方法的映射
```xml
<!-- 根据uid更新用户的密码：
	 Integer updatePasswordByUid(
		@Param("uid") Integer uid, 
		@Param("password") String password, 
		@Param("modifiedUser") String modifiedUser, 
		@Param("modifiedTime") Date modifiedTime) -->
<update id="updatePasswordByUid">
	UPDATE t_user SET
		password = #{password},
		modified_user = #{modifiedUser},
		modified_time = #{modifiedTime} 
	WHERE uid = #{uid}
</update>
<!-- 根据用户id查询用户数据：User findByUid(Integer uid) -->
<select id="findByUid" resultMap="UserEntityMap">
	SELECT * FROM t_user WHERE uid = #{uid} </select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
1. 用户在修改密码前，需要检查用户数据是否存在及是否被标记为“已删除”。如果检查不通过则应抛出UserNotFoundException异常。
2. 用户修改密码时，可能会因为输入的原密码错误导致修改失败，则应抛出PasswordNotMatchException异常。
3. 在执行修改密码时，如果返回的受影响行数与预期值不同，则应抛出UpdateException异常。
4. 创建com.cy.store.service.ex.UpdateException异常类，继承自ServiceException类。
###### 2. 接口与抽象方法
在IUserService中添加changePassword(Integer uid, String username, String oldPassword, String newPassword)抽象方法
```java
/**
 * 修改密码
 * @param uid 当前登录的用户id
 * @param username 用户名
 * @param oldPassword 原密码
 * @param newPassword 新密码
 */
public void changePassword(Integer uid, String username, String oldPassword, String newPassword);
```
###### 3. 实现抽象方法
在UserServiceImpl类中实现changePassword()抽象方法
```java
@Override
public void changePassword(Integer uid, String username, String oldPassword, String newPassword) {
	// 调用userMapper的findByUid()方法，根据参数uid查询用户数据
	User result = userMapper.findByUid(uid);
	// 检查查询结果是否为null
	if (result == null) {
		// 是：抛出UserNotFoundException异常
		throw new UserNotFoundException("用户数据不存在");
	}
	// 检查查询结果中的isDelete是否为1
	if (result.getIsDelete().equals(1)) {
		// 是：抛出UserNotFoundException异常
		throw new UserNotFoundException("用户数据不存在");
	}
	// 从查询结果中取出盐值
	String salt = result.getSalt();
	// 将参数oldPassword结合盐值加密，得到oldMd5Password
	String oldMd5Password = getMd5Password(oldPassword, salt);
	// 判断查询结果中的password与oldMd5Password是否不一致
	if (!result.getPassword().contentEquals(oldMd5Password)) {
		// 是：抛出PasswordNotMatchException异常
		throw new PasswordNotMatchException("密码错误");
	}
	// 将参数newPassword结合盐值加密，得到newMd5Password
	String newMd5Password = getMd5Password(newPassword, salt);
	// 创建当前时间对象
	Date now = new Date();
	// 调用userMapper的updatePasswordByUid()更新密码，并获取返回值
	Integer rows = userMapper.updatePasswordByUid(uid, newMd5Password, username, now);
	// 判断以上返回的受影响行数是否不为1
	if (rows != 1) {
		// 是：抛出UpdateException异常
		throw new UpdateException("更新用户数据时出现未知错误，请联系系统管理员");
	}
}
```
> String中的equals()和contentEquals()方法，都可以用来比较String对象内容是否相同

###### 4. 编写测试
##### 控制层
###### 1. 处理异常
在用户修改密码的业务中抛出了新的UpdateException异常，需要在BaseController类中进行处理
###### 2. 设计请求
```
请求路径：/users/change_password
请求参数：String oldPassword, String newPassword, HttpSession session
请求类型：POST
响应结果：JsonResult<Void>
```
###### 3. 处理请求
在UserController类中添加处理请求的changePassword(String oldPassword, String newPassword, HttpSession session)方法
```java
@RequestMapping("change_password")
public JsonResult<Void> changePassword(String oldPassword, String newPassword, HttpSession session) {
	// 调用session.getAttribute("")获取uid和username
	Integer uid = getUidFromSession(session);
	String username = getUsernameFromSession(session);
	// 调用业务对象执行修改密码
	iUserService.changePassword(uid, username, oldPassword, newPassword);
	// 返回成功
	return new JsonResult<Void>(OK);
}
```
###### 4. 编写测试
启动项目先登录，再访问http://localhost:8080/users/change_password？oldPassword=xx&newPassword=xx 进行测试。
##### 前端页面
###### 1. 前后端通过Json传输数据
在password.html页面中body标签内部的最后，添加script标签用于编写JavaScript程序
```js
<script type="text/javascript">
    $("#btn-change-password").click(function() {
        $.ajax({
            url: "/users/change_password",
            type: "POST",
            data: $("#form-change-password").serialize(),
            dataType: "json",
            success: function(json) {
                if (json.state == 200) {
                    alert("修改成功！");
                } else {
                    alert("修改失败！" + json.message);
                }
            }
        });
	});
</script>
```
###### 2. 编写测试
启动项目先登录，再访问http://localhost:8080/web/password.html 页面并进行修改密码
> 问题：如果无法正常将数据传递给后台，重启动系统和IDEA开发工具，登陆后便可修改密码。
> 在操作前端页面时用户进入修改密码页面，长时间停留在当前页面未进行任何操作，将导致登录信息过期。此时点击修改按钮时，仍会向/users/change_password发送请求，会被拦截器重定向到登录页面。由于整个过程是由$.ajax()函数采用异步的方式处理的，所以重定向也是由异步任务完成的，在页面中没有任何表现就会出现“用户登录信息超时后点击按钮没有任何反应”的问题。
> 解决方案：可以在password.html页面的$.ajax()中补充error属性的配置，该属性的值是一个回调函数。当服务器未正常响应状态码时，例如出现302、400、404、405、500等状态码时，将会调用该函数。

#### 修改个人资料
##### 持久层
###### 1. 规矩SQL语句
执行修改用户个人资料的SQL语句
```mysql
update t_user set phone=?,email=?, gender=?, modified_user=?, modified_time=? WHERE uid=?
```
在执行修改用户资料之前，当用户刚打开修改资料的页面时，就应把当前登录的用户信息显示到页面中。
> 查询功能已经实现，无需再次开发
> 在执行修改用户资料之前，还应检查用户数据是否存在、是否标记为“已删除”，也可以通过以上查询功能实现

######2. 接口与抽象方法
在UserMapper接口中添加updateInfoByUid(User user)方法
```java
/**
 * 根据uid更新用户资料
 * @param user 封装了用户id和新个人资料的对象
 * @return 受影响的行数
 */
Integer updateInfoByUid(User user);
```
###### 3. 配置SQL映射
在UserMapper.xml中配置Integer updateInfoByUid(User user)抽象方法的映射
```xml
<!-- 根据uid更新用户个人资料：Integer updateInfoByUid(User user) -->
<update id="updateInfoByUid">
	UPDATE t_user SET
		<if test="phone != null">phone = #{phone},</if>
		<if test="email != null">email = #{email},</if>
		<if test="gender != null">gender = #{gender},</if>
		modified_user = #{modifiedUser},
		modified_time = #{modifiedTime}
	WHERE uid = #{uid}
</update>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
1. 关于用户修改个人资料是由两个功能组成的
	- 打开页面时显示当前登录用户的信息
	- 点击修改按钮时更新用户的信息
2. 关于打开页面时显示当前登录用户的信息，可能会因为用户数据不存在、用户被标记为“已删除”而无法正确的显示页面，则抛出UserNotFoundException异常
3. 关于点击修改按钮时更新用户的信息，在执行修改资料之前仍需再次检查用户数据是否存在、用户是否被标记为“已删除”，则可能抛出UserNotFoundException异常。并且在执行修改资料过程中，还可能抛出UpdateException异常
######2. 接口与抽象方法
在IUserService接口中添加两个抽象方法，分别对应以上两个功能
```java
/**
 * 获取当前登录的用户的信息
 * @param uid 当前登录的用户的id
 * @return 当前登录的用户的信息
 */
User getByUid(Integer uid);
/**
 * 修改用户资料
 * @param uid 当前登录的用户的id
 * @param username 当前登录的用户名
 * @param user 用户的新的数据
 */
void changeInfo(Integer uid, String username, User user);
```
###### 3. 实现抽象方法
在UserServiceImpl实现类中实现getByUid(Integer uid)和changeInfo(Integer uid, String username, User user)以上两个抽象方法
```java
@Override
public User getByUid(Integer uid) {
	// 调用userMapper的findByUid()方法，根据参数uid查询用户数据
	User result = userMapper.findByUid(uid);
	// 判断查询结果是否为null
	if (result == null) {
		// 是：抛出UserNotFoundException异常
		throw new UserNotFoundException("用户数据不存在");
	}
	// 判断查询结果中的isDelete是否为1
	if (result.getIsDelete().equals(1)) {
		// 是：抛出UserNotFoundException异常
		throw new UserNotFoundException("用户数据不存在");
	}
	// 创建新的User对象
	User user = new User();
	// 将以上查询结果中的username/phone/email/gender封装到新User对象中
	user.setUsername(result.getUsername());
	user.setPhone(result.getPhone());
	user.setEmail(result.getEmail());
	user.setGender(result.getGender());
	// 返回新的User对象
	return user;
}
@Override
public void changeInfo(Integer uid, String username, User user) {
	// 调用userMapper的findByUid()方法，根据参数uid查询用户数据
	User result = userMapper.findByUid(uid);
	// 判断查询结果是否为null
	if (result == null) {
		// 是：抛出UserNotFoundException异常
		throw new UserNotFoundException("用户数据不存在");
	}
	// 判断查询结果中的isDelete是否为1
	if (result.getIsDelete().equals(1)) {
		// 是：抛出UserNotFoundException异常
		throw new UserNotFoundException("用户数据不存在");
	}
	// 向参数user中补全数据：uid
	user.setUid(uid);
	// 向参数user中补全数据：modifiedUser(username)
	user.setModifiedUser(username);
	// 向参数user中补全数据：modifiedTime(new Date())
	user.setModifiedTime(new Date());
	// 调用userMapper的updateInfoByUid(User user)方法执行修改，并获取返回值
	Integer rows = userMapper.updateInfoByUid(user);
	// 判断以上返回的受影响行数是否不为1
	if (rows != 1) {
		// 是：抛出UpdateException异常
		throw new UpdateException("更新用户数据时出现未知错误，请联系系统管理员");
	}
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
> 无需再次开发

###### 2. 设计请求
1. 设计用户提交显示当前登录的用户信息的请求，并设计响应的方式
```
请求路径：/users/get_by_uid
请求参数：HttpSession session
请求类型：GET
响应结果：JsonResult<User>
```
2. 设计用户提交执行修改用户信息的请求，并设计响应的方式
```
请求路径：/users/change_info
请求参数：User user, HttpSession session
请求类型：POST
响应结果：JsonResult<Void>
```
###### 3. 处理请求
1. 处理获取用户信息请求
	在UserController类中添加处理请求的geiByUid()方法，并导入org.springframework.web.bind.annotation.GetMapping包
```java
@GetMapping("get_by_uid")
public JsonResult<User> getByUid(HttpSession session) {
    // 从HttpSession对象中获取uid
    Integer uid = getUidFromSession(session);
    // 调用业务对象执行获取数据
    User data = userService.getByUid(uid);
    // 响应成功和数据
    return new JsonResult<User>(OK, data);
}
```
2. 处理修改用户个人信息的请求
	在UserController类中添加处理请求的changeInfo(User user, HttpSession session)方法
```java
@RequestMapping("change_info")
public JsonResult<Void> changeInfo(User user, HttpSession session) {
	// 从HttpSession对象中获取uid和username
	Integer uid = getUidFromSession(session);
	String username = getUsernameFromSession(session);
	// 调用业务对象执行修改用户资料
	userService.changeInfo(uid, username, user);
	// 响应成功
	return new JsonResult<Void>(OK);
}
```
###### 4. 编写测试
##### 前端页面
##### 1. 前后端通过Json传输数据
在userdata.html页面中body标签内部的最后，添加script标签用于编写JavaScript程序
```js
<script type="text/javascript">
    $(document).ready(function() {
        $.ajax({
            url: "/users/get_by_uid",
            type: "GET",
            dataType: "json",
            success: function(json) {
                if (json.state == 200) {
                    console.log("username=" + json.data.username);
                    console.log("phone=" + json.data.phone);
                    console.log("email=" + json.data.email);
                    console.log("gender=" + json.data.gender);
                    $("#username").val(json.data.username);
                    $("#phone").val(json.data.phone);
                    $("#email").val(json.data.email);
                    let radio = json.data.gender == 0 ? $("#gender-female") : $("#gender-male");
                    radio.prop("checked", "checked");
                } else {
                    alert("获取用户信息失败！" + json.message);
                }
            }
        });
	});
    $("#btn-change-info").click(function() {
        $.ajax({
            url: "/users/change_info",
            type: "POST",
            data: $("#form-change-info").serialize(),
            dataType: "json",
            success: function(json) {
                if (json.state == 200) {
                    alert("修改成功！");
                    location.href = "login.html";
                } else {
                    alert("修改失败！" + json.message);
                }
            },
            error: function(xhr) {
                alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + xhr.status);
                location.href = "login.html";
            }
        });
    });
</script>
```
##### 2. 编写测试
完成后启动项目，打开浏览器先登录，再访问http://localhost:8080/web/userdata.html 页面并进行用户个人资料的修改测试
#### 上传头像
##### 基于SpringMVC的文件上传
###### 1. MutipartFile接口
MutipartFile接口常用的API见下表
| 方法                         | 功能描述                                                     |
| :--------------------------- | ------------------------------------------------------------ |
| String getOriginalFilename() | 获取上传文件的原始文件名，即该文件在客户端中的文件名         |
| boolean isEmpty()            | 判断上传的文件是否为空，当没有选择文件就直接上传，或者选中的文件是0字节的空文件时，返回true，否则返回false |
| long getSize()               | 获取上传的文件大小，以字节为单位                             |
| String getContentType()      | 根据所上传的文件的扩展名决定该文件的MIME类型，例如上传.jpg格式的图片，将返回image/jpeg |
| InputStream getInputStream() | 获取上传文件的输入字节流，通常用于自定义读取所上传的文件的过程，该方法与transferTo()方法不可以同时使用 |
| void transferTo(File dest)   | 保存上传的文件，该方法与getInputStream()方法不可以同时使用   |
###### 2. MutipartResolver接口
1.MultipartResolver可以将上传过程中产生的数据封装为MultipartFile类型的对象中。
2.在配置MultipartResovler时，可以为其中的几个属性注入值：
- maxUploadSize：上传文件的最大大小，假设设置值为10M，一次性上传5个文件，则5个文件的大小总和不允许超过10M。
- maxUploadSizePerFile：每个上传文件的最大大小，假设设置值为10M，一次性上传5个文件，则每个文件的大小都不可以超过10M，但是5个文件的大小总和可以接近50M。
- defaultEncoding：默认编码。
##### 持久层
###### 1. 规划SQL语句
上传文件的操作其实先将用户上传的文件保存到服务器端的某个位置，然后将保存文件的路径记录在数据库中，当后续需要使用该文件时，从数据库中读出文件路径，即可实现在线访问该文件
在持久层处理数据库中的数据时，只需要关心如果记录头像文件的路径，并不需要考虑上传时保存文件的过程
```mysql
update t_user set avatar=?, modified_user=?, modified_time=? where uid=?
```
###### 2. 接口与抽象方法
在UserMapper接口中添加updateAvatorByUid()抽象方法
```java
/**
 * 根据uid更新用户的头像
 * @param uid 用户的id
 * @param avatar 新头像的路径
 * @param modifiedUser 修改执行人
 * @param modifiedTime 修改时间
 * @return 受影响的行数
 */
Integer updateAvatarByUid(
		@Param("uid") Integer uid,
		@Param("avatar") String avatar,
		@Param("modifiedUser") String modifiedUser,
		@Param("modifiedTime") Date modifiedTime);
```
###### 3. 配置SQL映射
在UserMapper.xml中配置updateAvatorByUid()抽象方法的映射
```xml
<!-- 根据uid更新用户的头像
	 Integer updateAvatarByUid(
		@Param("uid") Integer uid,
		@Param("avatar") String avatar,
		@Param("modifiedUser") String modifiedUser,
		@Param("modifiedTime") Date modifiedTime) -->
<update id="updateAvatarByUid">
	UPDATE t_user SET
		avatar = #{avatar},
		modified_user = #{modifiedUser},
		modified_time = #{modifiedTime}
	WHERE uid = #{uid}
</update>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
在修改头像值前先检查用户数据状态，可能抛UserNotFoundException异常；由于最终执行的是修改操作还可能抛UpdateException异常
###### 2. 实现抽象方法
在IUserService中添加changeAvatar(Integer uid, String username, String avatar)抽象方法
```java
/**
 * 修改用户头像
 * @param uid 当前登录的用户的id
 * @param username 当前登录的用户名
 * @param avatar 用户的新头像的路径
 */
void changeAvatar(Integer uid, String username, String avatar);
```
在UserServiceImpl类中实现changeAvatar(Integer uid, String username, String avatar)方法
```java
@Override
public void changeAvatar(Integer uid, String username, String avatar) {
	// 调用userMapper的findByUid()方法，根据参数uid查询用户数据
	User result = userMapper.findByUid(uid);
	// 检查查询结果是否为null
	if (result == null) {
		// 是：抛出UserNotFoundException
		throw new UserNotFoundException("用户数据不存在");
	}
	
	// 检查查询结果中的isDelete是否为1
	if (result.getIsDelete().equals(1)) {
		// 是：抛出UserNotFoundException
		throw new UserNotFoundException("用户数据不存在");
	}
	
	// 创建当前时间对象
	Date now = new Date();
	// 调用userMapper的updateAvatarByUid()方法执行更新，并获取返回值
	Integer rows = userMapper.updateAvatarByUid(uid, avatar, username, now);
	// 判断以上返回的受影响行数是否不为1
	if (rows != 1) {
		// 是：抛出UpdateException
		throw new UpdateException("更新用户数据时出现未知错误，请联系系统管理员");
	}
}
```
###### 3. 编写测试
##### 控制层
###### 1. 处理异常
1. 在处理上传文件的过程中，用户可能会选择错误的文件上传，此时就应该抛出对应的异常并进行处理。所以需要创建文件上传相关异常的基类，即在com.cy.store.controller.ex包下创建FileUploadException类，并继承自RuntimeException类
2. 在处理上传的文件过程中，经分析可能会产生以下异常，这些异常类都需要继承自FileUploadException类
> FileEmptyException 上传的文件为空
> FileSizeException 上传的文件大小超出了限制值
> FileTypeException 上传的文件类型超出了限制
> FileStateException 上传的文件状态异常
> FileUploadIOException 上传文件时读写异常
3. 然后在BaseController的handleException()的@ExceptionHandler注解中添加FileUploadException.class异常的处理；最后在方法中处理这些异常

###### 2. 设计请求
```
请求路径：/users/change_avatar
请求参数：MultipartFile file, HttpSession session
请求类型：POST
响应结果：JsonResult<String>
```
###### 3. 处理请求
在UserController类中添加处理请求的changeAvatar(@RequestParam("file") MultipartFile file, HttpSession session)方法
```java
/** 头像文件大小的上限值(10MB) */
public static final int AVATAR_MAX_SIZE = 10 * 1024 * 1024;
/** 允许上传的头像的文件类型 */
public static final List<String> AVATAR_TYPES = new ArrayList<String>();
/** 初始化允许上传的头像的文件类型 */
static {
	AVATAR_TYPES.add("image/jpeg");
	AVATAR_TYPES.add("image/png");
	AVATAR_TYPES.add("image/bmp");
	AVATAR_TYPES.add("image/gif");
}
@PostMapping("change_avatar")
public JsonResult<String> changeAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
	// 判断上传的文件是否为空
	if (file.isEmpty()) {
		// 是：抛出异常
		throw new FileEmptyException("上传的头像文件不允许为空");
	}
	// 判断上传的文件大小是否超出限制值
	if (file.getSize() > AVATAR_MAX_SIZE) { // getSize()：返回文件的大小，以字节为单位
		// 是：抛出异常
		throw new FileSizeException("不允许上传超过" + (AVATAR_MAX_SIZE / 1024) + "KB的头像文件");
	}
	// 判断上传的文件类型是否超出限制
	String contentType = file.getContentType();
	// public boolean list.contains(Object o)：当前列表若包含某元素，返回结果为true；若不包含该元素，返回结果为false。
	if (!AVATAR_TYPES.contains(contentType)) {
		// 是：抛出异常
		throw new FileTypeException("不支持使用该类型的文件作为头像，允许的文件类型：\n" + AVATAR_TYPES);
	}
	// 获取当前项目的绝对磁盘路径
	String parent = session.getServletContext().getRealPath("upload");
	// 保存头像文件的文件夹
	File dir = new File(parent);
	if (!dir.exists()) {
		dir.mkdirs();
	}
	// 保存的头像文件的文件名
	String suffix = "";
	String originalFilename = file.getOriginalFilename();
	int beginIndex = originalFilename.lastIndexOf(".");
	if (beginIndex > 0) {
		suffix = originalFilename.substring(beginIndex);
	}
	String filename = UUID.randomUUID().toString() + suffix;
	// 创建文件对象，表示保存的头像文件
	File dest = new File(dir, filename);
	// 执行保存头像文件
	try {
		file.transferTo(dest);
	} catch (IllegalStateException e) {
		// 抛出异常
		throw new FileStateException("文件状态异常，可能文件已被移动或删除");
	} catch (IOException e) {
		// 抛出异常
		throw new FileUploadIOException("上传文件时读写错误，请稍后重尝试");
	}
	// 头像路径
	String avatar = "/upload/" + filename;
	// 从Session中获取uid和username
	Integer uid = getUidFromSession(session);
	String username = getUsernameFromSession(session);
	// 将头像写入到数据库中
	userService.changeAvatar(uid, username, avatar);
	// 返回成功头像路径
	return new JsonResult<String>(OK, avatar);
}
```
###### 4. 设置上传文件大小
1. SpringBoot中默认MultipartResolver的最大文件大小值为1M。如果上传的文件的大小超过1M，会抛FileSizeLimitExceededException异常
2. 如果需要调整上传的限制值，直接在启动类中添加getMultipartConfigElement()方法，并且在启动类之前添加@Configuration注解
```java
    @Bean
    public MultipartConfigElement getMultipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // DataSize dataSize = DataSize.ofMegabytes(10);
        // 设置文件最大10M，DataUnit提供5中类型B,KB,MB,GB,TB
        factory.setMaxFileSize(DataSize.of(10, DataUnit.MEGABYTES));
        factory.setMaxRequestSize(DataSize.of(10, DataUnit.MEGABYTES));
        // 设置总上传数据总大小10M
        return factory.createMultipartConfig();
    }
```
3. 除了以上编写方法配置上传的上限值以外，还可以通过在application.properties或application.yml中添加配置来实现
```properties
#方式1
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
#方式2
spring.servlet.multipart.maxFileSize=10MB
spring.servlet.multipart.maxRequestSize=10MB
```
##### 前端页面
###### 1. 配置form表单
在upload.html页面中配置用户上传头像的form表单
头像上传成功后，显示上传的头像。在upload.html页面中，是使用img标签来显示头像图片的。首先确定img标签是否添加有id="img-avatar"属性，便于后续访问该标签；而img标签是通过src属性来决定显示哪张图片的，所以修改src该属性的值即可设置需要显示的图片。修改表单添加id="form-change-avatar"属性。修改input标签，添加id="btn-change-avatar"和type="button"属性
```html
<div class="panel-body">
	<!--上传头像表单开始-->
	<form id="form-change-avatar" class="form-horizontal" role="form">
		<div class="form-group">
			<label class="col-md-2 control-label">选择头像:</label>
			<div class="col-md-5">
				<img id="img-avatar" src="../images/index/user.jpg" class="img-responsive" />
			</div>
			<div class="clearfix"></div>
			<div class="col-md-offset-2 col-md-4">
				<input type="file" name="file">
			</div>
		</div>
		<div class="form-group">
			<div class="col-sm-offset-2 col-sm-10">
				<input id="btn-change-avatar" type="button" class="btn btn-primary" value="上传" />
			</div>
		</div>
	</form>
</div>
```
###### 2. 上传后显示头像
在upload.html页面中body标签内部的最后，添加script标签用于编写JavaScript程序
- processData：处理数据。默认情况下，processData的值是true，其代表以对象的形式上传的数据都会被转换为字符串的形式上传。而当上传文件的时候，则不需要把其转换为字符串，因此要改成false
- contentType：发送数据的格式。其代表的是前端发送数据的格式，默认值application/x-www-form-urlencoded。代表的是ajax的 data是以字符串的形式传递，使用这种传数据的格式，无法传输复杂的数据，比如多维数组、文件等。把contentType设置为false就会改掉之前默认的数据格式，在上传文件时就不会报错
```js
<script type="text/javascript">
    $("#btn-change-avatar").click(function() {
        $.ajax({
            url: "/users/change_avatar",
            type: "POST",
            data: new FormData($("#form-change-avatar")[0]),
            dataType: "JSON",
            processData: false, // processData处理数据
            contentType: false, // contentType发送数据的格式
            success: function(json) {
                if (json.state == 200) {
                    $("#img-avatar").attr("src", json.data);
                } else {
                    alert("修改失败！" + json.message);
                }
            },
            error: function(xhr) {
                alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + xhr.status);
                location.href = "login.html";
            }
        });
	});
</script>
```
###### 3. 登陆后显示头像
1. 首先检查登录成功后是否返回了头像的数据。访问http://localhost:8080/users/login?username=XXX&password=XXX 测试
2. 用户名、用户Id、用户头像等数据，属于常用数据，在客户端的许多页面都可能需要使用，如果每次都向服务器提交请求获取这些数据，是非常不合适的。可以在用户登录成功后，将这些数据存储在客户端本地，后续在客户端中需要显示这些数据时，直接从本地获取即可，无需再向服务器请求这些数据。在客户端本地存取数据时，可以使用Cookie技术。
3. 设计思路：当用户登录成功后，将服务器返回的头像路径存储到本地的Cookie中，在打开“上传头像”页面时，从本地的Cookie中读取头像路径并显示即可。在登录login.html页面中，当登录成功后，将用户头像路径保存到Cookie中
```js
$("#btn-login").click(function() {
    $.ajax({
        url: "/users/login",
        type: "POST",
        data: $("#form-login").serialize(),
        dataType: "json",
        success: function(json) {
            if (json.state == 200) {
                alert("登录成功！");
                $.cookie("avatar", json.data.avatar, {expires: 7});
                console.log("cookie中的avatar=" + $.cookie("avatar"));
                location.href = "index.html";
            } else {
                alert("登录失败！" + json.message);
            }
        }
    });
});
```
> 语法：$.cookie(名称,值,[option])。[option]参数说明：
expires：有限日期，可以是一个整数或一个日期(单位天)。如果不设置这个值，默认情况下浏览器关闭之后此Cookie就会失效。
path：表示Cookie值保存的路径，默认与创建页路径一致。
domin：表示Cookie域名属性，默认与创建页域名一样。要注意跨域的概念，如果要主域名二级域名有效则要设置“.xxx.com”。
secrue：布尔类型的值，表示传输Cookie值时，是否需要一个安全协议。
4. 在upload.html页面中，默认并没有引用jqueyr.cookie.js文件，因此无法识别$.cookie()函数；所以需要在upload.html页面head标签内添加jqueyr.cookie.js文件
```js
<script src="../bootstrap3/js/jquery.cookie.js" type="text/javascript" charset="utf-8"></script>
```
5. 在打开页面时自动读取显示用户图像。获取Cookie中头像的路径，然后将获取到的头像路径设置给img标签的src属性以显示头像。在upload.html页面中的script标签的内部添加自动读取用户图像的jquery代码
```js
$(document).ready(function () {
    console.log("cookie中的avatar=" + $.cookie("avatar"));
    $("#img-avatar").attr("src", $.cookie("avatar"));
});
```

###### 4. 显示最新头像
以上代码表示“每次打开页面时，读取Cookie中的头像并显示”，如果此时重新上传用户头像，而Cookie中所保存的头像还是之前上传的头像路径值，无法显示最新的用户头像。所以当用户重新上传头像后，还应把新头像的路径更新到Cookie中
在upload.html页面中，用户头像修改成功后，并将新的用户头像路径保存到Cookie中，该代码放在**修改成功**判断代码段中

```js
$.cookie("avatar", json.data, {expires: 7});
```
###### 5. 编写测试
### 收货地址模块
#### 新增收货地址
##### 创建数据表
使用use命令先选中store数据库，在store数据库中创建t_address用户数据表
```mysql
USE store;
CREATE TABLE t_address (
	aid INT AUTO_INCREMENT COMMENT '收货地址id',
	uid INT COMMENT '归属的用户id',
	name VARCHAR(20) COMMENT '收货人姓名',
	province_name VARCHAR(15) COMMENT '省-名称',
	province_code CHAR(6) COMMENT '省-行政代号',
	city_name VARCHAR(15) COMMENT '市-名称',
	city_code CHAR(6) COMMENT '市-行政代号',
	area_name VARCHAR(15) COMMENT '区-名称',
	area_code CHAR(6) COMMENT '区-行政代号',
	zip CHAR(6) COMMENT '邮政编码',
	address VARCHAR(50) COMMENT '详细地址',
	phone VARCHAR(20) COMMENT '手机',
	tel VARCHAR(20) COMMENT '固话',
	tag VARCHAR(6) COMMENT '标签',
	is_default INT COMMENT '是否默认：0-不默认，1-默认',
	created_user VARCHAR(20) COMMENT '创建人',
	created_time DATETIME COMMENT '创建时间',
	modified_user VARCHAR(20) COMMENT '修改人',
	modified_time DATETIME COMMENT '修改时间',
	PRIMARY KEY (aid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
##### 创建实体类
新增收获地址的实体类Address，继承自BaseEntity类，在类中声明与数据表中对应的属性
```java
package com.cy.store.entity;
/** 收货地址数据的实体类 */
@Data
public class Address extends BaseEntity implements Serializable {
    private Integer aid;
    private Integer uid;
    private String name;
    private String provinceName;
    private String provinceCode;
    private String cityName;
    private String cityCode;
    private String areaName;
    private String areaCode;
    private String zip;
    private String address;
    private String phone;
    private String tel;
    private String tag;
    private Integer isDefault;
}
```
##### 持久层
###### 1. 功能开发顺序
关于收货地址数据的管理，涉及的功能有：增加、删除、修改、设为默认、显示列表，顺序为增加-显示列表-设为默认-删除-修改
######2. 规划SQl语句
增加收货地址的本质是插入新的收货地址数据
```mysql
insert into t_address(...) values (...)
```
后续在处理业务时，还需要确定“即将增加的收货地址是不是默认收货地址”；可以设定规则“用户的第1条收货地址是默认的，以后添加的每一条都不是默认的”；要应用该规则，就必须知道“即将增加的收货地址是不是第1条”，可以“根据用户id统计收货地址的数量”，如果统计结果为0，则即将增加的就是该用户的第1条收货地址，如果统计结果不是0，则该用户已经有若干条收货地址了，即将增加的就一定不是第1条
```mysql
SELECT count(*) FROM t_address WHERE uid=?
```
一般电商平台都会限制每个用户可以创建的收货地址的数量，如“每个用户最多只允许创建20个收货地址”，也可以通过以上查询来实现
###### 3. 接口与抽象方法
创建AddressMapper接口，并在接口中添加抽象方法
```java
package com.cy.store.mapper;
import com.cy.store.entity.Address;
/** 处理收货地址数据的持久层接口 */
public interface AddressMapper {
    /**
     * 插入收货地址数据
     * @param address 收货地址数据
     * @return 受影响的行数
     */
    Integer insert(Address address);
    /**
     * 统计某用户的收货地址数据的数量
     * @param uid 用户的id
     * @return 该用户的收货地址数据的数量
     */
    Integer countByUid(Integer uid);
}
```
###### 4. 配置SQL映射
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cy.store.mapper.AddressMapper">
    <resultMap id="AddressEntityMap" type="com.cy.store.entity.Address">
        <id column="aid" property="aid"/>
        <result column="province_code" property="provinceCode"/>
        <result column="province_name" property="provinceName"/>
        <result column="city_code" property="cityCode"/>
        <result column="city_name" property="cityName"/>
        <result column="area_code" property="areaCode"/>
        <result column="area_name" property="areaName"/>
        <result column="is_default" property="isDefault"/>
        <result column="created_user" property="createdUser"/>
        <result column="created_time" property="createdTime"/>
        <result column="modified_user" property="modifiedUser"/>
        <result column="modified_time" property="modifiedTime"/>
    </resultMap>
    <!-- 插入收货地址数据：Integer insert(Address address) -->
	<insert id="insert" useGeneratedKeys="true" keyProperty="aid">
    INSERT INTO t_address (
        uid, name, province_name, province_code, city_name, city_code, area_name, area_code, zip,
        address, phone, tel,tag, is_default, created_user, created_time, modified_user, modified_time
    ) VALUES (
        #{uid}, #{name}, #{provinceName}, #{provinceCode}, #{cityName}, #{cityCode}, #{areaName},
        #{areaCode}, #{zip}, #{address}, #{phone}, #{tel}, #{tag}, #{isDefault}, #{createdUser},
        #{createdTime}, #{modifiedUser}, #{modifiedTime}
    )
	</insert>
<!-- 统计某用户的收货地址数据的数量：Integer countByUid(Integer uid) -->
	<select id="countByUid" resultType="java.lang.Integer">
    SELECT COUNT(*) FROM t_address WHERE uid=#{uid}
	</select>
</mapper>
```
###### 5. 编写测试
##### 业务层
###### 1. 规划异常
1. 无论用户将要增加的收货地址是不是默认收货地址，都需正常增加。即通过countByUid()方法统计的结果不管是不是0，都不能代表是错误的操作
2. 在执行插入收货地址数据之前，需判断countByUid()方法返回值是否超出上限值，如果超出上限值则抛AddressCountLimitException异常
3. 在执行插入数据时，还可能抛出InsertException异常，此异常无需再次创建
4. 创建com.cy.store.service.ex.AddressCountLimitException类后，需继承自ServiceException类
######2. 接口与抽象方法
创建IAddressService业务层接口，并添加抽象方法
```java
package com.cy.store.service;
import com.cy.store.entity.Address;
/** 处理收货地址数据的业务层接口 */
public interface IAddressService {
    /**
     * 创建新的收货地址
     * @param uid 当前登录的用户的id
     * @param username 当前登录的用户名
     * @param address 用户提交的收货地址数据
     */
    void addNewAddress(Integer uid, String username, Address address);
}
```
###### 3. 实现抽象方法
创建AddressServiceImpl业务层实现类，在类定义之前添加@Service注解，并实现IAddressService接口，最后在类中添加持久层对象并使用@Autowired注解修饰
```java
package com.cy.store.service.impl;
import com.cy.store.entity.Address;
import com.cy.store.mapper.AddressMapper;
import com.cy.store.service.IAddressService;
import com.cy.store.service.ex.AddressCountLimitException;
import com.cy.store.service.ex.InsertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;
@Service
public class AddressServiceImpl implements IAddressService {
    @Autowired
    private AddressMapper addressMapper;
    @Value("${user.address.max-count}")
    private int maxCount;
    @Override
    public void addNewAddress(Integer uid, String username, Address address) {
        // 根据参数uid调用addressMapper的countByUid(Integer uid)方法，统计当前用户的收货地址数据的数量
        Integer count = addressMapper.countByUid(uid);
        // 判断数量是否达到上限值
        if (count > maxCount) {
            // 是：抛出AddressCountLimitException
            throw new AddressCountLimitException("收货地址数量已经达到上限(" + maxCount + ")！");
        }
        // 补全数据：将参数uid封装到参数address中
        address.setUid(uid);
        // 补全数据：根据以上统计的数量，得到正确的isDefault值(是否默认：0-不默认，1-默认)，并封装
        Integer isDefault = count == 0 ? 1 : 0;
        address.setIsDefault(IsDefault);
        // 补全数据：4项日志
        Date now = new Date();
        address.setCreatedUser(username);
        address.setCreatedTime(now);
        address.setModifiedUser(username);
        address.setModifiedTime(now);
        // 调用addressMapper的insert(Address address)方法插入收货地址数据，并获取返回的受影响行数
        Integer rows = addressMapper.insert(address);
        // 判断受影响行数是否不为1
        if (rows != 1) {
            // 是：抛出InsertException
            throw new InsertException("插入收货地址数据时出现未知错误，请联系系统管理员！");
        }
    }
}
```
在application.properties文件中添加收货地址数据上限值的配置
```propreties
user.address.max-count=20
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
在控制器层新增收货地址时，如果收货地址已经达到上限值，则抛出AddressCountLimitException异常，并在BaseController类中添加处理AddressCountLimitException的异常
###### 2. 设计请求
```
请求路径：/addresses/add_new_address
请求参数：Address address, HttpSession session
请求类型：POST
响应结果：JsonResult<Void>
```
###### 3. 处理请求
创建com.cy.store.controller.AddressController控制器类继承自BaseController类，在类的声明添加@RequestMapping("addresses")和@RestController注解，在类中声明业务层对象并添加Autowired注解修饰
```java
package com.cy.store.controller;
import com.cy.store.service.IAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("addresses")
public class AddressController extends BaseController {
    @Autowired
    private IAddressService addressService;    
}
```
然后在AddressController类中添加处理请求的addNewAddress(Address address, HttpSession session)方法
```java
@RequestMapping("add_new_address")
public JsonResult<Void> addNewAddress(Address address, HttpSession session) {
    // 从Session中获取uid和username
    Integer uid = getUidFromSession(session);
    String username = getUsernameFromSession(session);

    // 调用业务对象的方法执行业务
    addressService.addNewAddress(uid, username, address);
    // 响应成功
    return new JsonResult<Void>(OK);
}
```
###### 4. 编写测试
##### 前端页面
1. 在addAddress.html页面中配置新增收货地址表单的属性。给form表单添加id="form-add-new-address"属性、"请输入收货人姓名"添加name="name"属性、"请输入邮政编码"添加name="zip"属性、"输入详细的收货地址，小区名称、门牌号等"添加name="address"属性、"请输入手机号码"添加name="phone"属性、"请输入固定电话号码"添加name="tel"属性、"请输入地址类型，如：家、公司或者学校"添加name="tag"属性、"保存"按钮添加id="btn-add-new-address"属性
2. 在addAddress.html页面中body标签内部的最后，添加script标签用于编写JavaScript程序
```js
<script type="text/javascript">
    $("#btn-add-new-address").click(function() {
        $.ajax({
            url: "/addresses/add_new_address",
            type: "POST",
            data: $("#form-add-new-address").serialize(),
            dataType: "JSON",
            success: function(json) {
                if (json.state == 200) {
                    alert("新增收货地址成功！");
                } else {
                    alert("新增收货地址失败！" + json.message);
                }
            },
            error: function(xhr) {
                alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + xhr.status);
                location.href = "login.html";
            }
        });
	});
</script>
```
###### 5. 编写测试
#### 获取省/市/区的列表
##### 数据库
1. 导入省/市/区数据t_dict_district.sql文件，创建数据表
2. 创建实体类，在类中声明与数据表中对应的属性
```java
package com.cy.store.entity;
import java.io.Serializable;
/** 省/市/区数据的实体类 */
@Data
public class District implements Serializable {
    private Integer id;
    private String parent;
    private String code;
    private String name;
}
```
##### 持久层
###### 1. 规划SQL语句
```mysql
select * from t_dict_district where parent=? order by code ASC;
```
###### 2. 接口与抽象方法
创建DistrictMapper接口，添加抽象方法
```java
package com.cy.store.mapper;
import com.cy.store.entity.District;
import java.util.List;
/** 处理省/市/区数据的持久层接口 */
public interface DistrictMapper {
    /**
     * 获取全国所有省/某省所有市/某市所有区
     * @param parent 父级代号，当获取某市所有区时，使用市的代号；当获取省所有市时，使用省的代号；当获取全国所有省时，使用"86"作为父级代号
     * @return 全国所有省/某省所有市/某市所有区的列表
     */
    List<District> findByParent(String parent);
}
```
###### 3. 配置SQL映射
DistrictMapper.xml
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cy.store.mapper.DistrictMapper">
    <!-- 获取全国所有省/某省所有市/某市所有区：List<District> findByParent(String parent) -->
    <select id="findByParent" resultType="com.cy.store.entity.District">
        SELECT * FROM t_dict_district
        WHERE parent=#{parent} ORDER BY code ASC
    </select>
</mapper>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
> 无异常

###### 2. 接口与抽象方法
创建IDistrictService接口，并添加抽象方法
```java
package com.cy.store.service;
import com.cy.store.entity.District;
import java.util.List;
/** 处理省/市/区数据的业务层接口 */
public interface IDistrictService {
    /**
     * 获取全国所有省/某省所有市/某市所有区
     * @param parent 父级代号，当获取某市所有区时，使用市的代号；当获取某省所有市时，使用省的代号；当获取全国所有省时，使用"86"作为父级代号
     * @return 全国所有省/某省所有市/某市所有区的列表
     */
    List<District> getByParent(String parent);
}
```
###### 3. 实现抽象方法
创建DistrictServiceImpl类，实现IDistrictService接口，在类之前添加@Service注解，以及在类中添加持久层对象并使用@Autowired修饰
```java
package com.cy.store.service.impl;
import com.cy.store.entity.District;
import com.cy.store.mapper.DistrictMapper;
import com.cy.store.service.IDistrictService;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service;
import java.util.List;
/** 处理省/市/区数据的业务层实现类 */
@Service
public class DistrictServiceImpl implements IDistrictService {
    @Autowired
    private DistrictMapper districtMapper;
	@Override
	public List<District> getByParent(String parent) {
		List<District> list = districtMapper.findByParent(parent);
		for (District district : list) {
			district.setId(null);
			district.setParent(null);
		}
	return list;
	}
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
> 无异常

###### 2. 设计请求
```
请求路径：/districts/
请求参数：String parent
请求类型：GET
响应结果：JsonResult<List<District>>
是否拦截：否，需要在拦截器的配置中添加白名单
```
###### 3. 处理请求
创建DistrictController控制器类，继承自BaseController类，在类之前添加@RequestMapping("districts")和@RestController注解，并在类中添加业务层对象，对其使用@Autowired注解修饰
在类中添加处理请求的方法getByParent(String parent)及方法的实现
> @GetMapping：是一个组合注解，等价于@RequestMapping(method={RequestMethod.GET})，它将HTTP的GET请求映射到特定的处理方法上。“/”表示方法将处理所有传入的URI请求。简化代码
```java
package com.cy.store.controller;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.tedu.store.entity.District;
import cn.tedu.store.service.IDistrictService;
import cn.tedu.store.util.JsonResult;
@RequestMapping("districts")
@RestController
public class DistrictController extends BaseController {
	@Autowired
	private IDistrictService districtService;
	@GetMapping({"", "/"})
	public JsonResult<List<District>> getByParent(String parent) {
   		List<District> data = districtService.getByParent(parent);
    	return new JsonResult<>(OK, data);
	}
}
```
> 在拦截器LoginInterceptorConfigurer类的addInterceptors(InterceptorRegistry registry)方法中将“districts”请求添加为白名单。如果已经添加无需重复添加
> patterns.add("/districts/**")

###### 4. 编写测试
##### 前端页面
1. 在addAddress.html页面中的head标签内导入的distpicker.data.js和distpicker.js文件注释掉
> JQuery实现中国省市区地址三级联动插件Distpicker
```js
<!--
<script type="text/javascript" src="../js/distpicker.data.js"></script>
<script type="text/javascript" src="../js/distpicker.js"></script>
-->
```
2. 在新增收货地址表单中，给"选择省"控件添加name="provinceCode"和id="province-list"属性，给"选择市"添加name="cityCode"和id="city-list"属性，给"选择区"控件添加name="areaCode"和id="area-list"属性。以上属性如果已经添加无需重复添加
3. 在addAddress.html页面中body标签内的script标签中添加获取省/市/区列表的代码
```js
<script type="text/javascript">
    let defaultOption = '<option value="0">----- 请选择 -----</option>';
    $(document).ready(function() {
        showProvinceList();
        $("#city-list").append(defaultOption);
        $("#area-list").append(defaultOption);
    });
    $("#province-list").change(function() {
        showCityList();
    });
    $("#city-list").change(function() {
        showAreaList();
    });
    function showProvinceList() {
        $("#province-list").append(defaultOption);
        $.ajax({
            url: "/districts",
            type: "GET",
            data: "parent=86",
            dataType: "JSON",
            success: function(json) {
                if (json.state == 200) {
                    let list = json.data;
                    console.log("count=" + list.length);
                    for (let i = 0; i < list.length; i++) {
                        console.log(list[i].name);
                        let option = '<option value="' + list[i].code + '">' + list[i].name + '</option>';
                        $("#province-list").append(option);
                    }
                }
            }
        });
    }
    function showCityList() {
        let parent = $("#province-list").val();
        $("#city-list").empty();
        $("#area-list").empty();
        $("#city-list").append(defaultOption);
        $("#area-list").append(defaultOption);
        if (parent == 0) {
            return;
        }
        $.ajax({
            url: "/districts",
            type: "GET",
            data: "parent=" + parent,
            dataType: "JSON",
            success: function(json) {
                if (json.state == 200) {
                    let list = json.data;
                    console.log("count=" + list.length);
                    for (let i = 0; i < list.length; i++) {
                        console.log(list[i].name);
                        let option = '<option value="' + list[i].code + '">' + list[i].name + '</option>';
                        $("#city-list").append(option);
                    }
                }
            }
        });
    }
    function showAreaList() {
        let parent = $("#city-list").val();
        $("#area-list").empty();
        $("#area-list").append(defaultOption);
        if (parent == 0)  return;
        $.ajax({
            url: "/districts",
            type: "GET",
            data: "parent=" + parent,
            dataType: "JSON",
            success: function(json) {
                if (json.state == 200) {
                    let list = json.data;
                    console.log("count=" + list.length);
                    for (let i = 0; i < list.length; i++) {
                        console.log(list[i].name);
                        let option = '<option value="' + list[i].code + '">' + list[i].name + '</option>';
                        $("#area-list").append(option);
                    }
                }
            }
        });
    }
</script>
```
> **JQuery事件-change()方法**
> 1.定义和用法
（1）当元素的值发生改变时，会发生change事件。
（2）该事件仅适用于文本域(textfield)，以及textarea和select元素。
（3）change()函数触发change事件，或规定当发生change事件时运行的函数。
当用于select元素时，change事件会在选择某个选项时发生。当用于textfield或textarea时，该事件会在元素失去焦点时发生。
2.触发change事件
触发被选元素的change事件。语法：$(selector).change()
3.将函数绑定到change事件
规定当被选元素的 change 事件发生时运行的函数。语法：$(selector).change(function)

###### 编写测试
#### 获取省/市/区名称
##### 持久层
###### 1. 规划SQL语句
根据省/市/区的行政代号获取省/市/区的名称
```mysql
select name from t_dict_district where code=?
```
###### 2. 接口与抽象方法
在DistrictMapper接口中添加根据省/市/区的行政代号获取省/市/区的名称findNameByCode(String code)抽象方法
```java
/**
 * 根据省/市/区的行政代号获取省/市/区的名称
 * @param code 省/市/区的行政代号
 * @return 匹配的省/市/区的名称，如果没有匹配的数据则返回null
 */
String findNameByCode(String code);
```
###### 3. 配置SQL映射
```xml
<!-- 根据省/市/区的行政代号获取省/市/区的名称：String findNameByCode(String code) -->
<select id="findNameByCode" resultType="java.lang.String">
    SELECT name FROM t_dict_district WHERE code=#{code}
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
> 无异常

###### 2. 接口与抽象方法
在业务层IDistrictService接口中添加getNameByCode(String code)抽象方法
```java
/**
 * 根据省/市/区的行政代号获取省/市/区的名称
 * @param code 省/市/区的行政代号
 * @return 匹配的省/市/区的名称，如果没有匹配的数据则返回null
 */
String getNameByCode(String code);
```
###### 3. 实现抽象方法
在业务层DistrictServiceImpl类中重写getNameByCode(String code)方法
```java
@Override
public String getNameByCode(String code) {
    return districtMapper.findNameByCode(code);
}
```
###### 4. 编写测试
##### 业务层优化
在AddressServiceImpl类中声明处理省/市/区数据的业务层对象
```java
@Autowired
private IDistrictService districtService;
```
addNewAddress(Integer uid, String username, Address address)方法中补全省/市/区数据
```java
// 补全数据：省、市、区的名称
String provinceName = districtService.getNameByCode(address.getProvinceCode());
String cityName = districtService.getNameByCode(address.getCityCode());
String areaName = districtService.getNameByCode(address.getAreaCode());
address.setProvinceName(provinceName);
address.setCityName(cityName);
address.setAreaName(areaName);
```
##### 前端页面
#### 收货地址列表
##### 持久层
###### 1. 规划SQL语句
显示当前登录用户的收货地址列表
```mysql
select * from t_address where uid=? order by is_default desc, created_time desc;
```
###### 2. 接口与抽象方法
在AddressMapper接口中添加findByUid(Integer uid)抽象方法
```java
/**
 * 查询某用户的收货地址列表数据
 * @param uid 收货地址归属的用户id
 * @return 该用户的收货地址列表数据
 */
List<Address> findByUid(Integer uid);
```
###### 3. 配置SQL映射
在AddressMapper.xml文件中配置findByUid(Integer uid)方法的映射
```xml
<!--
<resultMap id="AddressEntityMap" type="cn.tedu.store.entity.Address">
	<id column="aid" property="aid"/>
	<result column="province_code" property="provinceCode"/>
	<result column="province_name" property="provinceName"/>
	<result column="city_code" property="cityCode"/>
	<result column="city_name" property="cityName"/>
	<result column="area_code" property="areaCode"/>
	<result column="area_name" property="areaName"/>
	<result column="is_default" property="isDefault"/>
	<result column="created_user" property="createdUser"/>
	<result column="created_time" property="createdTime"/>
	<result column="modified_user" property="modifiedUser"/>
	<result column="modified_time" property="modifiedTime"/>
</resultMap>
-->
<!-- 查询某用户的收货地址列表数据：List<Address> findByUid(Integer uid) -->
<select id="findByUid" resultMap="AddressEntityMap">
	SELECT * FROM t_address
	WHERE uid=#{uid} ORDER BY is_default DESC, created_time DESC
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
> 无异常

###### 2. 接口与抽象方法
在IAddressService接口中添加getByUid(Integer uid)抽象方法
```java
/**
 * 查询某用户的收货地址列表数据
 * @param uid 收货地址归属的用户id
 * @return 该用户的收货地址列表数据
 */
List<Address> getByUid(Integer uid);
```
###### 3. 实现抽象方法
在AddressServiceImpl类中实现getByUid(Integer uid)抽象方法
```java
@Override
public List<Address> getByUid(Integer uid) {
	List<Address> list = addressMapper.findByUid(uid);
	for (Address address : list) {
		address.setUid(null);
		address.setProvinceCode(null);
		address.setCityCode(null);
		address.setAreaCode(null);
		address.setCreatedUser(null);
		address.setCreatedTime(null);
		address.setModifiedUser(null);
		address.setModifiedTime(null);
	}
	return list;
}
```
###### 4. 编写测试
#####控制层
###### 1. 处理异常
> 无异常

###### 2. 设计请求
```java
请求路径：/addresses
请求参数：HttpSession session
请求类型：GET
响应结果：JsonResult<List<Address>>
```
###### 3. 处理请求
在AddressController类中添加处理请求的getByUid(HttpSession session)方法
```java
@GetMapping({"", "/"})
public JsonResult<List<Address>> getByUid(HttpSession session) {
	Integer uid = getUidFromSession(session);
	List<Address> data = addressService.getByUid(uid);
	return new JsonResult<>(OK, data);
}
```
###### 4. 编写测试
##### 前端页面
在address.html页面中body标签内部的最后，添加展示用户收货地址列表数据的JavaScript代码
```js
<script type="text/javascript">
$(document).ready(function () {
    showAddressList();
});
function showAddressList() {
    $("#address-list").empty();
    $.ajax({
        url: "/addresses",
        type: "GET",
        dataType: "JSON",
        success: function (json) {
            let list = json.data;
            for (let i = 0; i < list.length; i++) {
                console.log(list[i].name);
                let address = '<tr>'
                    + '<td>#{tag}</td>'
                    + '<td>#{name}</td>'
                    + '<td>#{province}#{city}#{area}#{address}</td>'
                    + '<td>#{phone}</td>'
                    + '<td><a class="btn btn-xs btn-info"><span class="fa fa-edit"></span> 修改</a></td>'
                    + '<td><a class="btn btn-xs add-del btn-info"><span class="fa fa-trash-o"></span> 删除</a></td>'
                    + '<td><a class="btn btn-xs add-def btn-default">设为默认</a></td>'
                + '</tr>';
                address = address.replace(/#{aid}/g, list[i].aid);
                address = address.replace(/#{tag}/g, list[i].tag);
                address = address.replace("#{name}", list[i].name);
                address = address.replace("#{province}", list[i].provinceName);
                address = address.replace("#{city}", list[i].cityName);
                address = address.replace("#{area}", list[i].areaName);
                address = address.replace("#{address}", list[i].address);
                address = address.replace("#{phone}", list[i].phone);

                $("#address-list").append(address);
            }
            $(".add-def:eq(0)").hide();
        }
    });
}
</script>
```
#### 默认收货地址
##### 持久层
###### 1. 规划SQL语句
将某用户的所有收货地址设置为非默认地址（是否默认：0-不默认，1-默认）
```mysql
update t_address set is_default=0 where uid=?
```
将某用户指定的收货地址设置为默认地址
```mysql
update t_address set is_default=1, modified_user=?, modified_time=? where aid=?
```
检查该收货地址是否存在，并检查数据归属是否正确。可根据收货地址aid值，查询收货地址详情数据
```mysql
select * from t_address where aid=?
```
###### 2. 接口与抽象方法
在AddressMapper接口中声明三个抽象方法
```java
/**
 * 将某用户的所有收货地址设置为非默认地址
 * @param uid 收货地址归属的用户id
 * @return 受影响的行数
 */
Integer updateNonDefaultByUid(Integer uid);

/**
 * 将指定的收货地址设置为默认地址
 * @param aid 收货地址id
 * @param modifiedUser 修改执行人
 * @param modifiedTime 修改时间
 * @return 受影响的行数
 */
Integer updateDefaultByAid(
        @Param("aid") Integer aid,
        @Param("modifiedUser") String modifiedUser,
        @Param("modifiedTime") Date modifiedTime);

/**
 * 根据收货地址aid值，查询收货地址详情
 * @param aid 收货地址id
 * @return 匹配的收货地址详情，如果没有匹配的数据，则返回null
 */
Address findByAid(Integer aid);
```
###### 3. 配置SQL映射
在AddressMapper.xml映射文件，配置以上三个抽象方法的映射
```xml
<!-- 将某用户的所有收货地址设置为非默认地址：Integer updateNonDefaultByUid(Integer uid) -->
<update id="updateNonDefaultByUid">
    UPDATE t_address SET is_default=0 WHERE uid=#{uid}
</update>
<!-- 将指定的收货地址设置为默认地址：
         Integer updateDefaultByAid(
            @Param("aid") Integer aid,
            @Param("modifiedUser") String modifiedUser,
            @Param("modifiedTime") Date modifiedTime) -->
<update id="updateDefaultByAid">
    UPDATE t_address
    SET
        is_default=1,
        modified_user=#{modifiedUser},
        modified_time=#{modifiedTime}
    WHERE aid=#{aid}
</update>
<!-- 根据收货地址aid值，查询收货地址详情：Address findByAid(Integer aid) -->
<select id="findByAid" resultMap="AddressEntityMap">
    SELECT * FROM t_address WHERE aid=#{aid}
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
1.在执行设置默认收货地址之前，需要先检查该收货地址数据是否存在，如果不存在则抛出AddressNotFoundException异常。
2.然后还需要检查数据归属是否正确，也就是不可以操作他人的数据，如果该数据中记录的uid与当前登录的用户的uid不一致，则抛出AccessDeniedException异常。
3.检查通过后先全部设置为非默认，然后将指定的收货地址设置为默认；这两种操作都是更新数据的操作，则可能抛出UpdateException异常
###### 2. 接口与抽象方法
在IAddressService接口中添加setDefault(Integer aid, Integer uid, String username)抽象方法
```java
/**
 * 设置默认收货地址
 * @param aid 收货地址id
 * @param uid 归属的用户id
 * @param username 当前登录的用户名
 */
void setDefault(Integer aid, Integer uid, String username);
```
###### 3. 实现抽象方法
在AddressServiceImpl类中重写setDefault(Integer aid, Integer uid, String username)方法。该方法需要添加@Transactional注解
> 事务：基于Spring JDBC的事务（Transaction）处理，使用事务可以保证一系列的增删改操作，要么全部执行成功，要么全部执行失败。@Transactional注解可以用来修饰类也可以用来修饰方法。如果添加在业务类之前，则该业务类中的方法均以事务的机制运行，但是一般并不推荐这样处理
```java
@Transactional
@Override
public void setDefault(Integer aid, Integer uid, String username) {
    // 根据参数aid，调用addressMapper中的findByAid()查询收货地址数据
    Address result = addressMapper.findByAid(aid);
    // 判断查询结果是否为null
    if (result == null) {
        // 是：抛出AddressNotFoundException
        throw new AddressNotFoundException("尝试访问的收货地址数据不存在");
    }

    // 判断查询结果中的uid与参数uid是否不一致(使用equals()判断)
    if (!result.getUid().equals(uid)) {
        // 是：抛出AccessDeniedException
        throw new AccessDeniedException("非法访问的异常");
    }

    // 调用addressMapper的updateNonDefaultByUid()将该用户的所有收货地址全部设置为非默认，并获取返回受影响的行数
    Integer rows = addressMapper.updateNonDefaultByUid(uid);
    // 判断受影响的行数是否小于1(不大于0)
    if (rows < 1) {
        // 是：抛出UpdateException
        throw new UpdateException("设置默认收货地址时出现未知错误[1]");
    }

    // 调用addressMapper的updateDefaultByAid()将指定aid的收货地址设置为默认，并获取返回的受影响的行数
    rows = addressMapper.updateDefaultByAid(aid, username, new Date());
    // 判断受影响的行数是否不为1
    if (rows != 1) {
        // 是：抛出UpdateException
        throw new UpdateException("设置默认收货地址时出现未知错误[2]");
    }
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
在BaseController类中添加处理AddressNotFoundException和AccessDeniedException的异常
```java
// ...
else if (e instanceof AddressNotFoundException) {
    result.setState(4004);
} else if (e instanceof AccessDeniedException) {
    result.setState(4005);
}
// ...
```
###### 2. 设计请求
```
请求路径：/addresses/{aid}/set_default
请求参数：@PathVaraible("aid") Integer aid, HttpSession sesion
请求类型：POST
响应结果：JsonResult<Void>
```
> REST即表述性状态传递（Representational State Transfer，简称REST）是Roy Fielding博士在2000年他的博士论文中提出来的一种软件架构风格。它是一种针对网络应用的设计和开发方式，可以降低开发的复杂性，提高系统的可伸缩性。

###### 3. 处理请求
在AddressController类中添加处理请求的setDefault(@PathVariable("aid") Integer aid, HttpSession session)方法
```java
@RequestMapping("{aid}/set_default")
public JsonResult<Void> setDefault(@PathVariable("aid") Integer aid, HttpSession session) {
    Integer uid = getUidFromSession(session);
    String username = getUsernameFromSession(session);
    addressService.setDefault(aid, uid, username);
    return new JsonResult<Void>(OK);
}
```
##### 前端页面
在address.html页面中body标签内部的script标签内，添加设置用户默认收货地址的代码
```js
function setDefault(aid) {
    $.ajax({
        url: "/addresses/" + aid + "/set_default",
        type: "POST",
        dataType: "JSON",
        success: function(json) {
            if (json.state == 200) {
                showAddressList();
            } else {
                alert("设置默认收货地址失败！" + json.message);
            }
        },
        error: function(xhr) {
            alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + xhr.status);
            location.href = "login.html";
        }
    });
}
```
给showAddressList()方法中的“设为默认”超链接按钮添加设置默认收货地址的点击事件
```html
<td><a onclick="setDefault(#{aid})" class="btn btn-xs add-def btn-default">设为默认</a></td>
```
#### 删除收货地址
##### 持久层
###### 1. 规划SQL语句
在删除之前，需检查数据是否存在，数据归属是否正确。此功能已完成，无需再次开发
.删除指定的收货地址
```mysql
delete from t_address where aid=?
```
如果删除的这条数据是默认收货地址，则应该将剩余的收货地址中的某一条设置为默认收货地址，可以设定规则“将最近修改的设置为默认收货地址”，要实现此功能就必须要知道“最近修改的收货地址的id是多少”。则通过以下查询语句完成
```mysql
select * from t_address where uid=? order by modified_time desc limit 0,1
```
在执行以上操作之前，还需检查该用户的收货地址数据的数量，如果删除的收货地址是最后一条收货地址，则删除成功后无需再执行其他操作。统计收货地址数量的功能此前已经完成，无需再次开发
###### 2. 接口与抽象方法
在AddressMapper接口中添加抽象方法
```java
/**
 * 根据收货地址id删除数据
 * @param aid 收货地址id
 * @return 受影响的行数
 */
Integer deleteByAid(Integer aid);

/**
 * 查询某用户最后修改的收货地址
 * @param uid 归属的用户id
 * @return 该用户最后修改的收货地址，如果该用户没有收货地址数据则返回null
 */
Address findLastModified(Integer uid);
```
###### 3. 配置SQL映射
在AddressMapper.xml文件中添加以上两个抽象方法的映射
```xml
<!-- 根据收货地址id删除数据：Integer deleteByAid(Integer aid) -->
<delete id="deleteByAid">
    DELETE FROM
        t_address
    WHERE
        aid=#{aid}
</delete>
<!-- 查询某用户最后修改的收货地址：Address findLastModified(Integer uid) -->
<select id="findLastModified" resultMap="AddressEntityMap">
    SELECT * FROM t_address
    WHERE uid=#{uid}
    ORDER BY modified_time DESC LIMIT 0,1
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
在执行删除操作时，可能会删除数据失败，此时抛出DeleteException异常
###### 2. 接口与抽象方法
在IAddressService接口中添加删除收货地址的抽象方法
```java
/**
 * 删除收货地址
 * @param aid 收货地址id
 * @param uid 归属的用户id
 * @param username 当前登录的用户名
 */
void delete(Integer aid, Integer uid, String username);
```
###### 3. 实现抽象方法
在AddressServiceImpl实现类中实现以上两个抽象方法
```java
@Transactional
@Override
public void delete(Integer aid, Integer uid, String username) {
    // 根据参数aid，调用findByAid()查询收货地址数据
    Address result = addressMapper.findByAid(aid);
    // 判断查询结果是否为null
    if (result == null) {
        // 是：抛出AddressNotFoundException
        throw new AddressNotFoundException("尝试访问的收货地址数据不存在");
    }
    // 判断查询结果中的uid与参数uid是否不一致(使用equals()判断)
    if (!result.getUid().equals(uid)) {
        // 是：抛出AccessDeniedException：非法访问
        throw new AccessDeniedException("非常访问");
    }
    // 根据参数aid，调用deleteByAid()执行删除
    Integer rows1 = addressMapper.deleteByAid(aid);
    if (rows1 != 1) {
        throw new DeleteException("删除收货地址数据时出现未知错误，请联系系统管理员");
    }

    // 判断查询结果中的isDefault是否为0
    if (result.getIsDefault() == 0) {
        return;
    }
    // 调用持久层的countByUid()统计目前还有多少收货地址
    Integer count = addressMapper.countByUid(uid);
    // 判断目前的收货地址的数量是否为0
    if (count == 0) {
        return;
    }
    // 调用findLastModified()找出用户最近修改的收货地址数据
    Address lastModified = addressMapper.findLastModified(uid);
    // 从以上查询结果中找出aid属性值
    Integer lastModifiedAid = lastModified.getAid();
    // 调用持久层的updateDefaultByAid()方法执行设置默认收货地址，并获取返回的受影响的行数
    Integer rows2 = addressMapper.updateDefaultByAid(lastModifiedAid, username, new Date());
    // 判断受影响的行数是否不为1
    if (rows2 != 1) {
        // 是：抛出UpdateException
        throw new UpdateException("更新收货地址数据时出现未知错误，请联系系统管理员");
    }
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
在BaseController类中添加DeleteException异常的处理
```java
else if (e instanceof DeleteException) {
    result.setState(5002);
}
```
###### 2. 设计请求
```
请求路径：/addresses/{aid}/delete
请求参数：@PathVariable("aid") Integer aid, HttpSession session
请求类型：POST
响应结果：JsonResult<Void>
```
###### 3. 处理请求
在AddressController类中添加处理请求的delete()方法
```java
@RequestMapping("{aid}/delete")
public JsonResult<Void> delete(@PathVariable("aid") Integer aid, HttpSession session) {
    Integer uid = getUidFromSession(session);
    String username = getUsernameFromSession(session);
    addressService.delete(aid, uid, username);
    return new JsonResult<Void>(OK);
}
```
##### 前端页面
在address.html页面中body标签内部的script标签内，添加设置用户删除收货地址的代码

```javascript
function deleteByAid(aid) {
    $.ajax({
        url: "/addresses/" + aid + "/delete",
        type: "POST",
        dataType: "JSON",
        success: function(json) {
            if (json.state == 200) {
                showAddressList();
            } else {
                alert("删除收货地址失败！" + json.message);
            }
        },
        error: function(json) {
            alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + json.status);
            location.href = "login.html";
        }
    });
}
```
### 商品功能模块
#### 商品热销排行
##### 创建数据库
在store数据库中创建t_product数据表

```mysql
CREATE TABLE t_product (
  id int(20) NOT NULL COMMENT '商品id',
  category_id int(20) DEFAULT NULL COMMENT '分类id',
  item_type varchar(100) DEFAULT NULL COMMENT '商品系列',
  title varchar(100) DEFAULT NULL COMMENT '商品标题',
  sell_point varchar(150) DEFAULT NULL COMMENT '商品卖点',
  price bigint(20) DEFAULT NULL COMMENT '商品单价',
  num int(10) DEFAULT NULL COMMENT '库存数量',
  image varchar(500) DEFAULT NULL COMMENT '图片路径',
  status int(1) DEFAULT '1' COMMENT '商品状态  1：上架   2：下架   3：删除',
  priority int(10) DEFAULT NULL COMMENT '显示优先级',
  created_time datetime DEFAULT NULL COMMENT '创建时间',
  modified_time datetime DEFAULT NULL COMMENT '最后修改时间',
  created_user varchar(50) DEFAULT NULL COMMENT '创建人',
  modified_user varchar(50) DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
##### 创建实体类
创建Product类，并继承自BaseEntity类。在类中声明与数据表中对应的属性
```java
package com.cy.store.entity;

/** 商品数据的实体类 */
@Data
public class Product extends BaseEntity implements Serializable {
    private Integer id;
    private Integer categoryId;
    private String itemType;
    private String title;
    private String sellPoint;
    private Long price;
    private Integer num;
    private String image;
    private Integer status;
    private Integer priority;
}
```
##### 持久层
###### 1. 规划SQL语句
```mysql
SELECT * FROM t_product WHERE status=1 ORDER BY priority DESC LIMIT 0,4
```
###### 2. 接口与抽象方法
创建ProductMapper接口并在接口中添加查询热销商品findHotList()的方法
```java
package com.cy.store.mapper;
import com.cy.store.entity.Product;
import java.util.List;

/** 处理商品数据的持久层接口 */
public interface ProductMapper {
    /**
     * 查询热销商品的前四名
     * @return 热销商品前四名的集合
     */
    List<Product> findHotList();
}
```
###### 3. 配置SQL映射
创建ProductMapper.xml文件，并在文件中配置findHotList()方法的映射
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cy.store.mapper.ProductMapper">
    <resultMap id="ProductEntityMap" type="com.cy.store.entity.Product">
        <id column="id" property="id"/>
        <result column="category_id" property="categoryId"/>
        <result column="item_type" property="itemType"/>
        <result column="sell_point" property="sellPoint"/>
        <result column="created_user" property="createdUser"/>
        <result column="created_time" property="createdTime"/>
        <result column="modified_user" property="modifiedUser"/>
        <result column="modified_time" property="modifiedTime"/>
    </resultMap>
    <!-- 查询热销商品的前四名：List<Product> findHostList() -->
    <select id="findHotList" resultMap="ProductEntityMap">
        SELECT * FROM t_product
        WHERE status=1 ORDER BY priority DESC LIMIT 0,4
    </select>
</mapper>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
> 无异常

###### 2. 接口与抽象方法
创建IProductService接口，并在接口中添加findHotList()方法
```java
package com.cy.store.service;
import com.cy.store.entity.Product;
import java.util.List;

/** 处理商品数据的业务层接口 */
public interface IProductService {
    /**
     * 查询热销商品的前四名
     * @return 热销商品前四名的集合
     */
    List<Product> findHotList();
}
```
###### 3. 实现抽象方法
创建ProductServiceImpl类，并添加@Service注解；在类中声明持久层对象以及实现接口中的方法
```java
package com.cy.store.service.impl;
import com.cy.store.entity.Product;
import com.cy.store.mapper.ProductMapper;
import com.cy.store.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/** 处理商品数据的业务层实现类 */
@Service
public class ProductServiceImpl implements IProductService {
    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Product> findHotList() {
        List<Product> list = productMapper.findHotList();
        for (Product product : list) {
            product.setPriority(null);
            product.setCreatedUser(null);
            product.setCreatedTime(null);
            product.setModifiedUser(null);
            product.setModifiedTime(null);
        }
        return list;
    }
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
> 无异常

###### 2. 设计请求
```
请求路径：/products/hot_list
请求参数：无
请求类型：GET
响应结果：JsonResult<List<Product>>
是否拦截：否，需要将index.html和products/**添加到白名单
```
在LoginInterceptorConfigurer类中将index.html页面和products/**请求添加到白名单
```java
patterns.add("/web/index.html");
patterns.add("/products/**");
```
###### 3. 处理请求
创建ProductController类继承自BaseController类，类添加@RestController和@RequestMapping("products")注解，并在类中添加业务层对象
```java
package com.cy.store.controller;
import com.cy.store.entity.Product;
import com.cy.store.service.IProductService;
import com.cy.store.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("products")
public class ProductController extends BaseController {
    @Autowired
    private IProductService productService;
}
```
在类中添加处理请求的getHotList()方法
```java
@RequestMapping("hot_list")
public JsonResult<List<Product>> getHotList() {
    List<Product> data = productService.findHotList();
    return new JsonResult<List<Product>>(OK, data);
}
```
##### 前端页面
在index.html页面给“热销排行”列表的div标签设置id属性值
```html
<div id="hot-list" class="panel-body panel-item">
	<!-- ... -->
</div>
```
在index.html页面中body标签内部的最后，添加展示热销排行商品的代码
```javascript
<script type="text/javascript">
$(document).ready(function() {
    showHotList();
});
function showHotList() {
    $("#hot-list").empty();
    $.ajax({
        url: "/products/hot_list",
        type: "GET",
        dataType: "JSON",
        success: function(json) {
            let list = json.data;
            console.log("count=" + list.length);
            for (let i = 0; i < list.length; i++) {
                console.log(list[i].title);
                let html = '<div class="col-md-12">'
                  + '<div class="col-md-7 text-row-2"><a href="product.html?id=#{id}">#{title}</a></div>'
                  + '<div class="col-md-2">¥#{price}</div>'
                  + '<div class="col-md-3"><img src="..#{image}collect.png" class="img-responsive" /></div>'
                + '</div>';
                html = html.replace(/#{id}/g, list[i].id);
                html = html.replace(/#{title}/g, list[i].title);
                html = html.replace(/#{price}/g, list[i].price);
                html = html.replace(/#{image}/g, list[i].image);
                $("#hot-list").append(html);
            }
        }
    });
}
</script>
```
#### 显示商品详情
##### 持久层
###### 1. 规划SQL语句
```mysql
SELECT * FROM t_product WHERE id=?
```
###### 2. 接口与抽象方法
在ProductMapper接口中添加抽象方法
```java
/**
 * 根据商品id查询商品详情
 * @param id 商品id
 * @return 匹配的商品详情，如果没有匹配的数据则返回null
 */
Product findById(Integer id);
```
###### 3. 配置SQL映射
在ProductMapper.xml文件中配置findById(Integer id)方法的映射

```xml
<!-- 根据商品id查询商品详情：Product findById(Integer id) -->
<select id="findById" resultMap="ProductEntityMap">
    SELECT * FROM t_product WHERE id=#{id}
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
如果商品数据不存在，应该抛出ProductNotFoundException，需要创建com.cy.store.service.ex.ProductNotFoundException异常
###### 2. 接口与抽象方法
在业务层IProductService接口中添加findById(Integer id)抽象方法

```java
/**
 * 根据商品id查询商品详情
 * @param id 商品id
 * @return 匹配的商品详情，如果没有匹配的数据则返回null
 */
Product findById(Integer id);
```
###### 3. 实现抽象方法
在ProductServiceImpl类中，实现接口中的findById(Integer id)抽象方法

```java
@Override
public Product findById(Integer id) {
    // 根据参数id调用私有方法执行查询，获取商品数据
    Product product = productMapper.findById(id);
    // 判断查询结果是否为null
    if (product == null) {
        // 是：抛出ProductNotFoundException
        throw new ProductNotFoundException("尝试访问的商品数据不存在");
    }
    // 将查询结果中的部分属性设置为null
    product.setPriority(null);
    product.setCreatedUser(null);
    product.setCreatedTime(null);
    product.setModifiedUser(null);
    product.setModifiedTime(null);
    // 返回查询结果
    return product;
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
在BaseController类中的handleException()方法中添加处理ProductNotFoundException的异常

```java
// ...
else if (e instanceof ProductNotFoundException) {
	result.setState(4006);
}
// ...
```
###### 2. 设计请求
```
请求路径：/products/{id}/details
请求参数：@PathVariable("id") Integer id
请求类型：GET
响应结果：JsonResult<Product>
```
###### 3. 处理请求
在ProductController类中添加处理请求的getById()方法。
```java
@GetMapping("{id}/details")
public JsonResult<Product> getById(@PathVariable("id") Integer id) {
    // 调用业务对象执行获取数据
    Product data = productService.findById(id);
    // 返回成功和数据
    return new JsonResult<Product>(OK, data);
}
```
##### 前端页面
检查在product.html页面body标签内部的最后是否引入jquery-getUrlParam.js文件，如果引入无需重复引入
```java
<script type="text/javascript" src="../js/jquery-getUrlParam.js"></script>
```
在product.html页面中body标签内部的最后添加获取当前商品详情的代码
```javascript
<script type="text/javascript">
let id = $.getUrlParam("id");
console.log("id=" + id);
$(document).ready(function() {
    $.ajax({
        url: "/products/" + id + "/details",
        type: "GET",
        dataType: "JSON",
        success: function(json) {
            if (json.state == 200) {
                console.log("title=" + json.data.title);
                $("#product-title").html(json.data.title);
                $("#product-sell-point").html(json.data.sellPoint);
                $("#product-price").html(json.data.price);
                for (let i = 1; i <= 5; i++) {
                    $("#product-image-" + i + "-big").attr("src", ".." + json.data.image + i + "_big.png");
                    $("#product-image-" + i).attr("src", ".." + json.data.image + i + ".jpg");
                }
            } else if (json.state == 4006) { // 商品数据不存在的异常
                location.href = "index.html";
            } else {
                alert("获取商品信息失败！" + json.message);
            }
        }
    });
});
</script>
```
### 购物车功能模块
#### 加入购物车
##### 创建数据表
在store数据库中创建t_cart用户数据表。

```mysql
CREATE TABLE t_cart (
	cid INT AUTO_INCREMENT COMMENT '购物车数据id',
	uid INT NOT NULL COMMENT '用户id',
	pid INT NOT NULL COMMENT '商品id',
	price BIGINT COMMENT '加入时商品单价',
	num INT COMMENT '商品数量',
	created_user VARCHAR(20) COMMENT '创建人',
	created_time DATETIME COMMENT '创建时间',
	modified_user VARCHAR(20) COMMENT '修改人',
	modified_time DATETIME COMMENT '修改时间',
	PRIMARY KEY (cid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
##### 创建实体类
在com.cy.store.entity包下创建购物车的Cart实体类。
```java
package com.cy.store.entity;
import java.io.Serializable;
/** 购物车数据的实体类 */
@Data
public class Cart extends BaseEntity implements Serializable {
    private Integer cid;
    private Integer uid;
    private Integer pid;
    private Long price;
    private Integer num;

}
```
##### 持久层
###### 1. 规划SQL语句
向购物车表中插入商品数据

```mysql
insert into t_cart (除了cid以外的字段列表) values (匹配的值列表);
```
如果用户曾经将某个商品加入到购物车过，则点击“加入购物车”按钮只会对购物车中相同商品数量做递增操作
```mysql
update t_cart set num=? where cid=?
```
关于判断“到底应该插入数据，还是修改数量”，可以通过“查询某用户是否已经添加某商品到购物车”来完成。如果查询到某结果，就表示该用户已经将该商品加入到购物车了，如果查询结果为null，则表示该用户没有添加过该商品
```mysql
select * from t_cart where uid=? and pid=?
```
###### 2. 接口与抽象方法
创建CartMapper接口，并添加抽象相关的方法。
```java
package com.cy.store.mapper;
import com.cy.store.entity.Cart;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
/** 处理购物车数据的持久层接口 */
public interface CartMapper {
    /**
     * 插入购物车数据
     * @param cart 购物车数据
     * @return 受影响的行数
     */
    Integer insert(Cart cart);
    /**
     * 修改购物车数据中商品的数量
     * @param cid 购物车数据的id
     * @param num 新的数量
     * @param modifiedUser 修改执行人
     * @param modifiedTime 修改时间
     * @return 受影响的行数
     */
    Integer updateNumByCid(
            @Param("cid") Integer cid,
            @Param("num") Integer num,
            @Param("modifiedUser") String modifiedUser,
            @Param("modifiedTime") Date modifiedTime);
    /**
     * 根据用户id和商品id查询购物车中的数据
     * @param uid 用户id
     * @param pid 商品id
     * @return 匹配的购物车数据，如果该用户的购物车中并没有该商品，则返回null
     */
    Cart findByUidAndPid(
            @Param("uid") Integer uid,
            @Param("pid") Integer pid);
}
```
###### 3. 配置SQL映射
在resources.mapper文件夹下创建CartMapper.xml文件，并在文件中配置以上三个方法的映射
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cy.store.mapper.CartMapper">
    <resultMap id="CartEntityMap" type="com.cy.store.entity.Cart">
        <id column="cid" property="cid"/>
        <result column="created_user" property="createdUser"/>
        <result column="created_time" property="createdTime"/>
        <result column="modified_user" property="modifiedUser"/>
        <result column="modified_time" property="modifiedTime"/>
    </resultMap>
    <!-- 插入购物车数据：Integer insert(Cart cart) -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="cid">
        INSERT INTO t_cart (uid, pid, price, num, created_user, created_time, modified_user, modified_time)
        VALUES (#{uid}, #{pid}, #{price}, #{num}, #{createdUser}, #{createdTime}, #{modifiedUser}, #{modifiedTime})
    </insert>
    <!-- 修改购物车数据中商品的数量：
         Integer updateNumByCid(
            @Param("cid") Integer cid,
            @Param("num") Integer num,
            @Param("modifiedUser") String modifiedUser,
            @Param("modifiedTime") Date modifiedTime) -->
    <update id="updateNumByCid">
        UPDATE t_cart SET
            num=#{num},
            modified_user=#{modifiedUser},
            modified_time=#{modifiedTime}
        WHERE cid=#{cid}
    </update>
    <!-- 根据用户id和商品id查询购物车中的数据：
         Cart findByUidAndPid(
            @Param("uid") Integer uid,
            @Param("pid") Integer pid) -->
    <select id="findByUidAndPid" resultMap="CartEntityMap">
        SELECT * FROM t_cart WHERE uid=#{uid} AND pid=#{pid}
    </select>
</mapper>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
在插入数据时，可能抛出InsertException异常；在修改数据时，可能抛出UpdateException异常。如果不限制购物车中的记录的数量，则没有其它异常
###### 2. 接口与抽象方法
创建ICartService接口，并添加抽象方法。

```java
package com.cy.store.service;
/** 处理商品数据的业务层接口 */
public interface ICartService {
    /**
     * 将商品添加到购物车
     * @param uid 当前登录用户的id
     * @param pid 商品的id
     * @param amount 增加的数量
     * @param username 当前登录的用户名
     */
    void addToCart(Integer uid, Integer pid, Integer amount, String username);
}
```
###### 3. 实现抽象方法
创建CartServiceImpl类，并实现ICartService接口，并在类的定义前添加@Service注解。在类中声明CartMapper持久层对象和IProductService处理商品数据的业务对象，并都添加@Autowired注修饰。
```java
package com.cy.store.service.impl;
import com.cy.store.entity.Cart;
import com.cy.store.entity.Product;
import com.cy.store.mapper.CartMapper;
import com.cy.store.service.ICartService;
import com.cy.store.service.IProductService;
import com.cy.store.service.ex.InsertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
/** 处理购物车数据的业务层实现类 */
@Service
public class CartServiceImpl implements ICartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private IProductService productService;
}
```
在CartServiceImpl类中实现业务层ICartService接口中定义的抽象方法
```java
@Override
public void addToCart(Integer uid, Integer pid, Integer amount, String username) {
    // 根据参数pid和uid查询购物车中的数据
    Cart result = cartMapper.findByUidAndPid(uid, pid);
    Integer cid = result.getCid();
    Date now = new Date();
    // 判断查询结果是否为null
    if (result == null) {
        // 是：表示该用户并未将该商品添加到购物车
        // 创建Cart对象
        Cart cart = new Cart();
        // 封装数据：uid,pid,amount
        cart.setUid(uid);
        cart.setPid(pid);
        cart.setNum(amount);
        // 调用productService.findById(pid)查询商品数据，得到商品价格
        Product product = productService.findById(pid);
        // 封装数据：price
        cart.setPrice(product.getPrice());
        // 封装数据：4个日志
        cart.setCreatedUser(username);
        cart.setCreatedTime(now);
        cart.setModifiedUser(username);
        cart.setModifiedTime(now);
        // 调用insert(cart)执行将数据插入到数据表中
        Integer rows = cartMapper.insert(cart);
        if (rows != 1) {
            throw new InsertException("插入商品数据时出现未知错误，请联系系统管理员");
        }
    } else {
        // 否：表示该用户的购物车中已有该商品
        // 从查询结果中获取购物车数据的id
        Integer cid = result.getCid();
        // 从查询结果中取出原数量，与参数amount相加，得到新的数量
        Integer num = result.getNum() + amount;
        // 执行更新数量
        Integer rows = cartMapper.updateNumByCid(cid, num, username, now);
        if (rows != 1) {
            throw new InsertException("修改商品数量时出现未知错误，请联系系统管理员");
        }
    }
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
> 无异常

###### 2. 设计请求
```
请求路径：/carts/add_to_cart
请求参数：Integer pid, Integer amount, HttpSession session
请求类型：POST
响应结果：JsonResult<Void>
```
###### 3. 处理请求
创建CartController类并继承自BaseController类，添加@RequestMapping("carts")和@RestController注解；在类中声明ICartService业务对象，并使用@Autowired注解修饰
```java
package com.cy.store.controller;
import com.cy.store.service.ICartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("carts")
public class CartController extends BaseController {
    @Autowired
    private ICartService cartService;
}
```
在CartController类中添加处理请求的addToCart()方法
```java
@RequestMapping("add_to_cart")
public JsonResult<Void> addToCart(Integer pid, Integer amount, HttpSession session) {
    // 从Session中获取uid和username
    Integer uid = getUidFromSession(session);
    String username = getUsernameFromSession(session);
    // 调用业务对象执行添加到购物车
    cartService.addToCart(uid, pid, amount, username);
    // 返回成功
    return new JsonResult<Void>(OK);
}
```
##### 前端页面
在product.html页面中的body标签内的script标签里为“加入购物车”按钮添加点击事件
```javascript
$("#btn-add-to-cart").click(function() {
    $.ajax({
        url: "/carts/add_to_cart",
        type: "POST",
        data: {
            "pid": id,
            "amount": $("#num").val()
        },
        dataType: "JSON",
        success: function(json) {
            if (json.state == 200) {
                alert("增加成功！");
            } else {
                alert("增加失败！" + json.message);
            }
        },
        error: function(xhr) {
            alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + xhr.status);
            location.href = "login.html";
        }
    });
});
```
> $.ajax函数中参数data提交请参数的方式：
>
> ```
> // 1.适用于参数较多，且都在同一个表单中
> data: $("#form表单id属性值").serialize()
> // 2.仅适用于上传文件
> data: new FormData($("##form表单id属性值")[0])
> // 3.参数拼接形式提交
> data: "pid=10000005&amount=3"
> // 4.使用JSON格式提交参数
> data: {
>    	"pid": 10000005,
>    	"amount": 3
> }
> ```

#### 显示购物车列表
##### 持久层
###### 1. 规划SQL语句
显示某用户的购物车列表数据的SQL语句
```mysql
SELECT cid,uid,pid,t_cart.price,t_cart.num,t_product.title,t_product.price AS realPrice,t_product.image
FROM t_cart LEFT JOIN t_product ON t_cart.pid = t_product.id 
WHERE uid = #{uid} 
ORDER BY t_cart.created_time DESC
```
###### 2. 接口与抽象方法
由于涉及多表关联查询，必然没有哪个实体类可以封装此次的查询结果，因此需要创建VO类。创建com.cy.store.vo.CartVO类
```java
package com.cy.store.vo;
import java.io.Serializable;
/** 购物车数据的Value Object类 */
@Data
public class CartVO implements Serializable {
    private Integer cid;
    private Integer uid;
    private Integer pid;
    private Long price;
    private Integer num;
    private String title;
    private Long realPrice;
    private String image;

}
```
在CartMapper接口中添加抽象方法
```java
/**
 * 查询某用户的购物车数据
 * @param uid 用户id
 * @return 该用户的购物车数据的列表
 */
List<CartVO> findVOByUid(Integer uid);
```
###### 3. 配置SQL映射
在CartMapper.xml文件中添加findVOByUid()方法的映射
```xml
<!-- 查询某用户的购物车数据：List<CartVO> findVOByUid(Integer uid) -->
<select id="findVOByUid" resultType="com.cy.store.vo.CartVO">
    SELECT
        cid,
        uid,
        pid,
        t_cart.price,
        t_cart.num,
        t_product.title,
        t_product.price AS realPrice,
        t_product.image
    FROM t_cart  LEFT JOIN t_product ON t_cart.pid = t_product.id 
    WHERE
        uid = #{uid}
    ORDER BY t_cart.created_time DESC
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
> 无异常

###### 2. 接口与抽象方法
在ICartService接口中添加findVOByUid()抽象方法
```java
/**
 * 查询某用户的购物车数据
 * @param uid 用户id
 * @return 该用户的购物车数据的列表
 */
List<CartVO> getVOByUid(Integer uid);
```
###### 3. 实现抽象方法
在CartServiceImpl类中重写业务接口中的抽象方法

```java
@Override
public List<CartVO> getVOByUid(Integer uid) {
    return cartMapper.findVOByUid(uid);
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
> 无异常

###### 2. 设计请求
```
请求路径：/carts/
请求参数：HttpSession session
请求类型：GET
响应结果：JsonResult<List<CartVO>>
```
###### 3. 处理请求
在CartController类中编写处理请求的代码
```java
@GetMapping({"", "/"})
public JsonResult<List<CartVO>> getVOByUid(HttpSession session) {
    // 从Session中获取uid
    Integer uid = getUidFromSession(session);
    // 调用业务对象执行查询数据
    List<CartVO> data = cartService.getVOByUid(uid);
    // 返回成功与数据
    return new JsonResult<List<CartVO>>(OK, data);
}
```
##### 前端页面
将cart.html页面的head头标签内引入的cart.js文件注释掉。
```javascript
<!-- <script src="../js/cart.js" type="text/javascript" charset="utf-8"></script> -->
```
给form标签添加action="orderConfirm.html"属性、tbody标签添加id="cart-list"属性、结算按钮的类型改为type="submit"值。如果以上属性值已经添加过无需重复添加
```js
$(document).ready(function() {
	showCartList();
});

function showCartList() {
	$("#cart-list").empty();
	$.ajax({
		url: "/carts",
		type: "GET",
		dataType: "JSON",
		success: function(json) {
			let list = json.data;
			for (let i = 0; i < list.length; i++) {
				let tr = '<tr>'
						+ '<td>'
						+ 	'<input name="cids" value="#{cid}" type="checkbox" class="ckitem" />'
						+ '</td>'
						+ '<td><img src="..#{image}collect.png" class="img-responsive" /></td>'
						+ '<td>#{title}#{msg}</td>'
						+ '<td>¥<span id="price-#{cid}">#{price}</span></td>'
						+ '<td>'
						+ 	'<input type="button" value="-" class="num-btn" onclick="reduceNum(1)" />'
						+ 	'<input id="num-#{cid}" type="text" size="2" readonly="readonly" class="num-text" value="#{num}">'
						+ 	'<input class="num-btn" type="button" value="+" onclick="addNum(#{cid})" />'
						+ '</td>'
						+ '<td>¥<span id="total-price-#{cid}">#{totalPrice}</span></td>'
						+ '<td>'
						+ 	'<input type="button" onclick="delCartItem(this)" class="cart-del btn btn-default btn-xs" value="删除" />'
						+ '</td>'
						+ '</tr>';
				tr = tr.replace(/#{cid}/g, list[i].cid);
				tr = tr.replace(/#{title}/g, list[i].title);
				tr = tr.replace(/#{image}/g, list[i].image);
				tr = tr.replace(/#{price}/g, list[i].price);
				tr = tr.replace(/#{num}/g, list[i].num);
				tr = tr.replace(/#{totalPrice}/g, list[i].price * list[i].num);
				$("#cart-list").append(tr);
			}
		}
	});
}
```
#### 增加商品数量
##### 持久层
###### 1. 规划SQL语句
首先进行查询需要操作的购物车数据信息
```mysql
SELECT * FROM t_cart WHERE cid=?
```
然后计算出新的商品数量值，如果满足更新条件则执行更新操作。此SQL语句无需重复开发

```mysql
UPDATE t_cart SET num=?, modified_user=?, modified_time=? WHERE cid=?
```
###### 2. 接口与抽象方法
在CartMapper接口中添加抽象方法
```java
/**
 * 根据购物车数据id查询购物车数据详情
 * @param cid 购物车数据id
 * @return 匹配的购物车数据详情，如果没有匹配的数据则返回null
 */
Cart findByCid(Integer cid);
```
###### 3. 配置SQL映射
在CartMapper文件中添加findByCid(Integer cid)方法的映射
```xml
<!-- 根据购物车数据id查询购物车数据详情：Cart findByCid(Integer cid) -->
<select id="findByCid" resultMap="CartEntityMap">
    SELECT
   		*
    FROM
    	t_cart
    WHERE
    	cid = #{cid}
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
如果尝试访问的购物车数据不存在，则抛出CartNotFoundException异常。创建com.cy.store.service.ex.CartNotFoundException类
如果尝试访问的数据并不是当前登录用户的数据，则抛出AccessDeniedException异常。此异常类无需再次创建
最终执行更新操作时，可能会抛出UpdateException异常。此异常类无需再次创建
###### 2. 接口与抽象方法
在业务层ICartService接口中添加addNum()抽象方法。
```java
/**
 * 将购物车中某商品的数量加1
 * @param cid 购物车数量的id
 * @param uid 当前登录的用户的id
 * @param username 当前登录的用户名
 * @return 增加成功后新的数量
 */
Integer addNum(Integer cid, Integer uid, String username);
```
###### 3. 实现抽象方法
在CartServiceImpl类中，实现接口中的抽象方法并规划业务逻辑
```java
@Override
public Integer addNum(Integer cid, Integer uid, String username) {
    // 调用findByCid(cid)根据参数cid查询购物车数据
    Cart result = cartMapper.findByCid(cid);
    // 判断查询结果是否为null
    if (result == null) {
        // 是：抛出CartNotFoundException
        throw new CartNotFoundException("尝试访问的购物车数据不存在");
    }
    // 判断查询结果中的uid与参数uid是否不一致
    if (!result.getUid().equals(uid)) {
        // 是：抛出AccessDeniedException
        throw new AccessDeniedException("非法访问");
    }
    // 可选：检查商品的数量是否大于多少(适用于增加数量)或小于多少(适用于减少数量)
    // 根据查询结果中的原数量增加1得到新的数量num
    Integer num = result.getNum() + 1;
    // 创建当前时间对象，作为modifiedTime
    Date now = new Date();
    // 调用updateNumByCid(cid, num, modifiedUser, modifiedTime)执行修改数量
    Integer rows = cartMapper.updateNumByCid(cid, num, username, now);
    if (rows != 1) {
        throw new InsertException("修改商品数量时出现未知错误，请联系系统管理员");
    }
    // 返回新的数量
    return num;
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
在BaseController类中添加CartNotFoundException异常类的统一管理
```java
else if (e instanceof CartNotFoundException) {
    result.setState(4007);
}
```
###### 2. 设计请求
```
请求路径：/carts/{cid}/num/add
请求参数：@PathVariable("cid") Integer cid, HttpSession session
请求类型：POST
响应结果：JsonResult<Integer>
```
###### 3. 处理请求
在CartController类中添加处理请求的addNum()方法
```java
@RequestMapping("{cid}/num/add")
public JsonResult<Integer> addNum(@PathVariable("cid") Integer cid, HttpSession session) {
    // 从Session中获取uid和username
    Integer uid = getUidFromSession(session);
    String username = getUsernameFromSession(session);
    // 调用业务对象执行增加数量
    Integer data = cartService.addNum(cid, uid, username);
    // 返回成功
    return new JsonResult<Integer>(OK, data);
}
```
##### 前端页面
首先确定在showCartList()函数中动态拼接的增加购物车按钮是绑定了addNum()事件，如果已经添加无需重复添加
```javascript
<input class="num-btn" type="button" value="+" onclick="addNum(#{cid})" />
```
在script标签中定义addNum()函数并编写增加购物车数量的逻辑代码。
```javascript
function addNum(cid) {
    $.ajax({
        url: "/carts/" + cid + "/num/add",
        type: "POST",
        dataType: "JSON",
        success: function(json) {
            if (json.state == 200) {
                $("#num-" + cid).val(json.data);
                let price = $("#price-" + cid).html();
                let totalPrice = price * json.data;
                $("#total-price-" + cid).html(totalPrice);
            } else {
                alert("增加商品数量失败！" + json.message);
            }
        },
        error: function(xhr) {
            alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + xhr.status);
            location.href = "login.html";
        }
    });
}
```
#### 显示勾选的购物车数据
##### 持久层
###### 1. 规划SQL语句
在“确认订单页”显示的商品信息，应来自前序页面（购物车列表）中勾选的数据，所以显示的信息其实是购物车中的数据。到底需要显示哪些取决于用户的勾选操作，当用户勾选了若干条购物车数据后，这些数据的id应传递到当前“确认订单页”中，该页面根据这些id获取需要显示的数据列表。
所以在持久层需要完成“根据若干个不确定的id值，查询购物车数据表，显示购物车中的数据信息”
```mysql
SELECT
	cid,
	uid,
	pid,
	t_cart.price,
	t_cart.num,
	t_product.title,
	t_product.price AS realPrice,
	t_product.image
FROM
	t_cart
	LEFT JOIN t_product ON t_cart.pid = t_product.id 
WHERE
	cid IN (?, ?, ?)
ORDER BY
	t_cart.created_time DESC	
```
###### 2. 接口与抽象方法
在CartMapper接口中添加findVOByCids(Integer[] cids)方法

```java
/**
 * 根据若干个购物车数据id查询详情的列表
 * @param cids 若干个购物车数据id
 * @return 匹配的购物车数据详情的列表
 */
List<CartVO> findVOByCids(Integer[] cids);
```
###### 3. 配置SQL映射
在CartMapper.xml文件中添加SQL语句的映射配置
```xml
<!-- 根据若干个购物车数据id查询详情的列表：List<CartVO> findVOByCids(Integer[] cids) -->
<select id="findVOByCids" resultType="com.cy.store.vo.CartVO">
    SELECT
        cid,
        uid,
        pid,
        t_cart.price,
        t_cart.num,
        t_product.title,
        t_product.price AS realPrice,
        t_product.image
    FROM
        t_cart
            LEFT JOIN t_product ON t_cart.pid = t_product.id
    WHERE
        cid IN (
            <foreach collection="array" item="cid" separator=",">
                #{cid}
            </foreach>
        )
    ORDER BY
        t_cart.created_time DESC
</select>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
> 无异常

######2. 接口与抽象方法
在ICartService接口中添加getVOByCids()抽象方法
```java
/**
 * 根据若干个购物车数据id查询详情的列表
 * @param uid 当前登录的用户的id
 * @param cids 若干个购物车数据id
 * @return 匹配的购物车数据详情的列表
 */
List<CartVO> getVOByCids(Integer uid, Integer[] cids);
```
######3. 实现抽象方法
在CartServiceImpl类中重写业务接口中的抽象方法
```java
@Override
public List<CartVO> getVOByCids(Integer uid, Integer[] cids) {
    List<CartVO> list = cartMapper.findVOByCids(cids);
    /**
    for (CartVO cart : list) {
		if (!cart.getUid().equals(uid)) {
			list.remove(cart);
		}
	}
	*/
    Iterator<CartVO> it = list.iterator();
    while (it.hasNext()) {
        CartVO cart = it.next();
        if (!cart.getUid().equals(uid)) {
            it.remove();
        }
    }
    return list;
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
> 无异常

###### 2. 设计请求
```
请求路径：/carts/list
请求参数：Integer[] cids, HttpSession session
请求类型：GET
响应结果：JsonResult<List<CartVO>>
```
###### 3. 处理请求
.在CartController类中添加处理请求的getVOByCids()方法
```java
@GetMapping("list")
public JsonResult<List<CartVO>> getVOByCids(Integer[] cids, HttpSession session) {
    // 从Session中获取uid
    Integer uid = getUidFromSession(session);
    // 调用业务对象执行查询数据
    List<CartVO> data = cartService.getVOByCids(uid, cids);
    // 返回成功与数据
    return new JsonResult<>(OK, data);
}
```
##### 前端页面
###### 1. 显示勾选的购物车数据
在orderConfirm.html页面的head标签里注释掉引入外部的orderConfirm.js文件
```javascript
<!-- <script src="../js/orderConfirm.js" type="text/javascript" charset="utf-8"></script> -->
```
在orderConfirm.html页面中检查必要控件的属性是否添加，如果已添加无需重复添加
在orderConfirm.html页面中的body标签内的最后添加srcipt标签并在标签内部添加处理购物车“订单商品信息”列表展示的代码
```js
function showCartList() {
	$("#cart-list").empty();
	$.ajax({
		url: "/carts/list",
		type: "GET",
		data:location.search.substr(1),
		dataType: "JSON",
		success: function(json) {
			let list = json.data;
			let allCount = 0;
			let allPrice = 0;
			for (let i = 0; i < list.length; i++) {
				let tr = '<tr>'
						+ '<td><img src="..#{image}collect.png" class="img-responsive" /></td>'
						+ '<td>#{title}</td>'
						+ '<td>¥<span>#{price}</span></td>'
						+ '<td>#{num}</td>'
						+ '<td>¥<span>#{totalPrice}</span></td>'
						+ '</tr>';
				tr = tr.replace(/#{image}/g, list[i].image);
				tr = tr.replace(/#{title}/g, list[i].title);
				tr = tr.replace(/#{price}/g, list[i].price);
				tr = tr.replace(/#{num}/g, list[i].num);
				tr = tr.replace(/#{totalPrice}/g, list[i].price * list[i].num);
				$("#cart-list").append(tr);
				allCount += list[i].num;
				allPrice += list[i].price * list[i].num;
			}
			$("#all-count").html(allCount);
			$("#all-price").html(allPrice);
		}
	});
}
```
###### 2. 显示选择收货地址
在orderConfirm.html页面中的body标签内的srcipt标签中添加获取收货地址列表方法的定义
```js
function showAddressList() {
	$("#address-list").empty();
	$.ajax({
		url: "/addresses",
		type: "GET",
		dataType: "JSON",
		success: function(json) {
			let list = json.data;
			console.log("count=" + list.length);
			for (let i = 0; i < list.length; i++) {
				console.log(list[i].name);
				let opt = '<option value="#{aid}">#{name} | #{tag} | #{province}#{city}#{area}#{address} | #{phone}</option>';
				opt = opt.replace(/#{aid}/g, list[i].aid);
				opt = opt.replace(/#{tag}/g, list[i].tag);
				opt = opt.replace("#{name}", list[i].name);
				opt = opt.replace("#{province}", list[i].provinceName);
				opt = opt.replace("#{city}", list[i].cityName);
				opt = opt.replace("#{area}", list[i].areaName);
				opt = opt.replace("#{address}", list[i].address);
				opt = opt.replace("#{phone}", list[i].phone);
			$("#address-list").append(opt);
			}
		}
	});
}
```
在orderConfirm.html页面中的body标签内的srcipt标签中添加展示收货地址列表方法的调用
```javascript
<script type="text/javascript">
    $(document).ready(function() {
        showAddressList();
        showCartList();
    });
</script>
```
### 订单功能模块
#### 创建订单
##### 创建数据表
在store数据库中创建t_order和t_order_item数据表
```mysql
CREATE TABLE t_order (
	oid INT AUTO_INCREMENT COMMENT '订单id',
	uid INT NOT NULL COMMENT '用户id',
	recv_name VARCHAR(20) NOT NULL COMMENT '收货人姓名',
	recv_phone VARCHAR(20) COMMENT '收货人电话',
	recv_province VARCHAR(15) COMMENT '收货人所在省',
	recv_city VARCHAR(15) COMMENT '收货人所在市',
	recv_area VARCHAR(15) COMMENT '收货人所在区',
	recv_address VARCHAR(50) COMMENT '收货详细地址',
	total_price BIGINT COMMENT '总价',
	status INT COMMENT '状态：0-未支付，1-已支付，2-已取消，3-已关闭，4-已完成',
	order_time DATETIME COMMENT '下单时间',
	pay_time DATETIME COMMENT '支付时间',
	created_user VARCHAR(20) COMMENT '创建人',
	created_time DATETIME COMMENT '创建时间',
	modified_user VARCHAR(20) COMMENT '修改人',
	modified_time DATETIME COMMENT '修改时间',
	PRIMARY KEY (oid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE t_order_item (
	id INT AUTO_INCREMENT COMMENT '订单中的商品记录的id',
	oid INT NOT NULL COMMENT '所归属的订单的id',
	pid INT NOT NULL COMMENT '商品的id',
	title VARCHAR(100) NOT NULL COMMENT '商品标题',
	image VARCHAR(500) COMMENT '商品图片',
	price BIGINT COMMENT '商品价格',
	num INT COMMENT '购买数量',
	created_user VARCHAR(20) COMMENT '创建人',
	created_time DATETIME COMMENT '创建时间',
	modified_user VARCHAR(20) COMMENT '修改人',
	modified_time DATETIME COMMENT '修改时间',
	PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
##### 创建实体类
创建Order实体类
```java
package com.cy.store.entity;
import java.io.Serializable;
import java.util.Date;
/** 订单数据的实体类 */
@Data
public class Order extends BaseEntity implements Serializable {
    private Integer oid;
    private Integer uid;
    private String recvName;
    private String recvPhone;
    private String recvProvince;
    private String recvCity;
    private String recvArea;
    private String recvAddress;
    private Long totalPrice;
    private Integer status;
    private Date orderTime;
    private Date payTime;
}    
```
创建OrderItem实体类。

```java
package com.cy.store.entity;
import java.io.Serializable;

/** 订单中的商品数据 */
@Data
public class OrderItem extends BaseEntity implements Serializable {
    private Integer id;
    private Integer oid;
    private Integer pid;
    private String title;
    private String image;
    private Long price;
    private Integer num;
}    
```
##### 持久层
###### 1. 规划SQL语句
插入订单数据
```mysql
INSERT INTO t_order (
	uid,
	recv_name,
	recv_phone,
	recv_province,
	recv_city,
	recv_area,
	recv_address,
	total_price,
	status,
	order_time,
	pay_time,
	created_user,
	created_time,
	modified_user,
	modified_time 
)
VALUES (
	#对应字段的值列表
)
```
入订单商品数据
```mysql
INSERT INTO t_order_item ( 
	oid, 
	pid, 
	title, 
	image, 
	price, 
	num, 
	created_user, 
	created_time, 
	modified_user, 
	modified_time 
)
VALUES ( 
	#对应字段的值列表
)
```
###### 2. 接口与抽象方法
创建OrderMapper接口并在接口中添加抽象方法
```java
package com.cy.store.mapper;
import com.cy.store.entity.Order;
import com.cy.store.entity.OrderItem;

/** 处理订单及订单商品数据的持久层接口 */
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param order 订单数据
     * @return 受影响的行数
     */
    Integer insertOrder(Order order);

    /**
     * 插入订单商品数据
     * @param orderItem 订单商品数据
     * @return 受影响的行数
     */
    Integer insertOrderItem(OrderItem orderItem);
}
```
###### 3. 配置SQL映射
创建OrderMapper.xml文件，并添加抽象方法的映射
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cy.store.mapper.OrderMapper">
    <!-- 插入订单数据：Integer insertOrder(Order order) -->
    <insert id="insertOrder" useGeneratedKeys="true" keyProperty="oid">
        INSERT INTO t_order (
            uid, recv_name, recv_phone, recv_province, recv_city, recv_area, recv_address,
            total_price,status, order_time, pay_time, created_user, created_time, modified_user,
            modified_time
        ) VALUES (
            #{uid}, #{recvName}, #{recvPhone}, #{recvProvince}, #{recvCity}, #{recvArea},
            #{recvAddress}, #{totalPrice}, #{status}, #{orderTime}, #{payTime}, #{createdUser},
            #{createdTime}, #{modifiedUser}, #{modifiedTime}
        )
    </insert>

    <!-- 插入订单商品数据：Integer insertOrderItem(OrderItem orderItem) -->
    <insert id="insertOrderItem" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO t_order_item (
            oid, pid, title, image, price, num, created_user,
            created_time, modified_user, modified_time
        ) VALUES (
            #{oid}, #{pid}, #{title}, #{image}, #{price}, #{num}, #{createdUser},
            #{createdTime}, #{modifiedUser}, #{modifiedTime}
        )
    </insert>
</mapper>
```
###### 4. 编写测试
##### 业务层
###### 1. 规划异常
> 无异常

###### 2. 接口与抽象方法
由于处理过程中还需要涉及收货地址数据的处理，所以需要先在IAddressService接口中添加getByAid()方法
```java
/**
 * 根据收货地址数据的id，查询收货地址详情
 * @param aid 收货地址id
 * @param uid 归属的用户id
 * @return 匹配的收货地址详情
 */
Address getByAid(Integer aid, Integer uid);
```
创建IOrderService业务层接口并添加抽象方法
```java
package com.cy.store.service;
import com.cy.store.entity.Order;
/** 处理订单和订单数据的业务层接口 */
public interface IOrderService {
    /**
     * 创建订单
     * @param aid 收货地址的id
     * @param cids 即将购买的商品数据在购物车表中的id
     * @param uid 当前登录的用户的id
     * @param username 当前登录的用户名
     * @return 成功创建的订单数据
     */
    Order create(Integer aid, Integer[] cids, Integer uid, String username);
}
```
###### 3. 实现抽象方法
在AddressServiceImpl类中实现接口中的getByAid()抽象方法
```java
@Override
public Address getByAid(Integer aid, Integer uid) {
    // 根据收货地址数据id，查询收货地址详情
    Address address = addressMapper.findByAid(aid);

    if (address == null) {
        throw new AddressNotFoundException("尝试访问的收货地址数据不存在");
    }
    if (!address.getUid().equals(uid)) {
        throw new AccessDeniedException("非法访问");
    }
    address.setProvinceCode(null);
    address.setCityCode(null);
    address.setAreaCode(null);
    address.setCreatedUser(null);
    address.setCreatedTime(null);
    address.setModifiedUser(null);
    address.setModifiedTime(null);
    return address;
}
```
在com.cy.store.service.impl包下创建OrderServiceImpl业务层实现类并实现IOrderService接口；在类定义之前添加@Service注解，在类中添加OrderMapper订单持久层对象、IAddressService处理收货地址对象、ICartService购物车数据对象，并都添加@Autowired注解进行修饰
```java
package com.cy.store.service.impl;
import com.cy.store.entity.Address;
import com.cy.store.entity.Order;
import com.cy.store.entity.OrderItem;
import com.cy.store.mapper.OrderMapper;
import com.cy.store.service.IAddressService;
import com.cy.store.service.ICartService;
import com.cy.store.service.IOrderService;
import com.cy.store.service.ex.InsertException;
import com.cy.store.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;

/** 处理订单和订单数据的业务层实现类 */
@Service
public class OrderServiceImpl implements IOrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private IAddressService addressService;
    @Autowired
    private ICartService cartService;
    
    @Transactional
	@Override
	public Order create(Integer aid, Integer[] cids, Integer uid, String username) {
    	// 创建当前时间对象
    	Date now = new Date();
    	// 根据cids查询所勾选的购物车列表中的数据
    	List<CartVO> carts = cartService.getVOByCids(uid, cids);
    	// 计算这些商品的总价
    	long totalPrice = 0;
    	for (CartVO cart : carts) {
        	totalPrice += cart.getRealPrice() * cart.getNum();
    	}
    	// 创建订单数据对象
    	Order order = new Order();
    	// 补全数据：uid
    	order.setUid(uid);
    	// 查询收货地址数据
    	Address address = addressService.getByAid(aid, uid);
    	// 补全数据：收货地址相关的6项
    	order.setRecvName(address.getName());
    	order.setRecvPhone(address.getPhone());
    	order.setRecvProvince(address.getProvinceName());
    	order.setRecvCity(address.getCityName());
    	order.setRecvArea(address.getAreaName());
   		order.setRecvAddress(address.getAddress());
    	// 补全数据：totalPrice
    	order.setTotalPrice(totalPrice);
    	// 补全数据：status
    	order.setStatus(0);
    	// 补全数据：下单时间
    	order.setOrderTime(now);
    	// 补全数据：日志
    	order.setCreatedUser(username);
    	order.setCreatedTime(now);
    	order.setModifiedUser(username);
    	order.setModifiedTime(now);
    	// 插入订单数据
    	Integer rows1 = orderMapper.insertOrder(order);
    	if (rows1 != 1) {
        	throw new InsertException("插入订单数据时出现未知错误，请联系系统管理员");
    	}
    	// 遍历carts，循环插入订单商品数据
    	for (CartVO cart : carts) {
        	// 创建订单商品数据
        	OrderItem item = new OrderItem();
        	// 补全数据：setOid(order.getOid())
        	item.setOid(order.getOid());
        	// 补全数据：pid, title, image, price, num
        	item.setPid(cart.getPid());
        	item.setTitle(cart.getTitle());
        	item.setImage(cart.getImage());
        	item.setPrice(cart.getRealPrice());
        	item.setNum(cart.getNum());
        	// 补全数据：4项日志
        	item.setCreatedUser(username);
        	item.setCreatedTime(now);
        	item.setModifiedUser(username);
        	item.setModifiedTime(now);
        	// 插入订单商品数据
        	Integer rows2 = orderMapper.insertOrderItem(item);
        	if (rows2 != 1) {
            	throw new InsertException("插入订单商品数据时出现未知错误，请联系系统管理员");
        	}
    	}

    // 返回
    	return order;
	}
}
```
###### 4. 编写测试
##### 控制层
###### 1. 处理异常
> 无异常

###### 2. 设计请求
```
请求路径：/orders/create
请求参数：Integer aid, Integer[] cids, HttpSession session
请求类型：POST
响应结果：JsonResult<Order>
```
###### 3. 处理请求
创建OrderController类，并继承自BaseController类；并在类前添加@RequestMapping("orders")注解和@RestController注解；在类中声明IOrderService业务对象，然后添加@Autowired注解修饰；最后在类中添加处理请求的方法
```java
package com.cy.store.controller;
import com.cy.store.entity.Order;
import com.cy.store.service.IOrderService;
import com.cy.store.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("orders")
public class OrderController extends BaseController {
    @Autowired
    private IOrderService orderService;

    @RequestMapping("create")
    public JsonResult<Order> create(Integer aid, Integer[] cids, HttpSession session) {
        // 从Session中取出uid和username
        Integer uid = getUidFromSession(session);
        String username = getUsernameFromSession(session);
        // 调用业务对象执行业务
        Order data = orderService.create(aid, cids, uid, username);
        // 返回成功与数据
        return new JsonResult<Order>(OK, data);
    }
}
```
##### 前端页面
在orderConfirm.xml页面中的body标签内的script标签内添加“在线支付”按钮的点击事件
```javascript
$("#btn-create-order").click(function() {
	let aid=$("#address-list").val();
	let cids=location.search.substr(1);
	$.ajax({
		url: "/orders/create",
		data: "aid="+aid+"&"+cids,
		type: "GET",
		dataType: "JSON",
		success: function(json) {
			if (json.state == 200) {
				alert("创建订单成功！");
				location.href="payment.html"
				console.log(json.data);
			} else {
				alert("创建订单失败！" + json.message);
				}
			},
			error: function(xhr) {
			alert("您的登录信息已经过期，请重新登录！HTTP响应码：" + xhr.status);
			location.href = "login.html";
		}
	});
});
```
#### AOP
##### 1. Spring AOP
AOP：面向切面（Aspect）编程。AOP并不是Spring框架的特性，只是Spring很好的支持了AOP。

如果需要在处理每个业务时，都执行特定的代码，则可以假设在整个数据处理流程中存在某个切面，切面中可以定义某些方法，当处理流程执行到切面时，就会自动执行切面中的方法。最终实现的效果就是：只需要定义好切面方法，配置好切面的位置（连接点），在不需要修改原有数据处理流程的代码的基础之上，就可以使得若干个流程都执行相同的代码。
##### 2. 切面方法
1.切面方法的访问权限是public。

2.切面方法的返回值类型可以是void或Object，如果使用的注解是@Around时，必须使用Object作为返回值类型，并返回连接点方法的返回值；如果使用的注解是@Before或@After等其他注解时，则自行决定。

3.切面方法的名称可以自定义。

4.切面方法的参数列表中可以添加ProceedingJoinPoint接口类型的对象，该对象表示连接点，也可以理解调用切面所在位置对应的方法的对象，如果使用的注解是@Around时，必须添加该参数，反之则不是必须添加。
##### 3. 统计业务执行时长
在使用Spring AOP编程时，需要先在pom.xml文件中添加两个关于AOP的依赖aspectjweaver和aspectjtools
```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjtools</artifactId>
</dependency>
```
创建TimerAspect切面类，在类之前添加@Aspect和@Component注解修饰
```java
package com.cy.store.aop;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimerAspect {

}
```
在类中添加切面方法around(ProceedingJoinPoint pjp
```java
public Object around(ProceedingJoinPoint pjp) throws Throwable {
    // 记录起始时间
    long start = System.currentTimeMillis();
    // 执行连接点方法，即切面所在位置对应的方法。本项目中表示执行注册或执行登录等
    Object result = pjp.proceed();
    // 记录结束时间
    long end = System.currentTimeMillis();
    // 计算耗时
    System.err.println("耗时：" + (end - start) + "ms.");
    // 返回连接点方法的返回值
    return result;
}
```
最后需要在方法之前添加@Around注解，以配置连接点，即哪些方法需要应用该切面
```java
@Around("execution(* com.cy.store.service.impl.*.*(..))")
```
##### 4. 编写测试
