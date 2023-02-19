package com.qiandao.messagingauththird.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qiandao.messagingcommon.valid.AddGroup;
import com.qiandao.messagingcommon.valid.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("userInfo")
public class UserInfoEntity implements Serializable {

    @TableId(value = "no", type = IdType.ASSIGN_ID)
    @Null(message = "用户序号不能指定,而是通过雪花算法自动插入id",groups = {AddGroup.class, UpdateGroup.class})
    private Long no;

    @NotNull(message = "用户id不能为空",groups = {AddGroup.class, UpdateGroup.class})
    @Size(min = 8,max = 20)
    private String userId;


    @NotNull(message = "用户密码不能为空",groups = {AddGroup.class, UpdateGroup.class})
    @Size(min = 8,max = 16)
    private String password;

    private String userName;

    @URL(message = "不是一个合法的url地址",groups = {AddGroup.class, UpdateGroup.class})
    private String userHead;

    private String userWork;

    @Min(0)
    @Max(1)
    @Pattern(regexp = "\\d", message = "格式必须为整数,且值只能为0、1")
    private Integer userSex;

    private String signature;

    @Pattern(regexp = "^1[345789]\\d{9}$",message = "不是11位标准手机号")
    @NotNull(message = "手机号不能为空",groups = {AddGroup.class, UpdateGroup.class})
    private String phone;

    private String birthday;

    private String email;

    private String qq;

    private String wechat;

    private String weibo;

    private String socialUid;

    private String weiboDes;

    private String giteeId;

    private String giteeName;

    private String giteeBio;

    private String giteeUrl;

    private String githubId;

    private String githubName;

    private String githubBio;

    private String githubUrl;

    private Integer blogNum;

    private String charRoomId;

    private Integer speechLevel;

    private Integer state;

    private String violationRecord;

    private String perm;

    private String role;

    private Integer fans;

    private Integer concerns;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableField(exist = false)
    private String code;

}
