package com.shuyixiao.gitstat.weekly.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用户身份配置持久化服务
 * 用于保存用户自定义的用户名和编码
 * 这是应用级别的配置，所有项目共享
 * 
 * @author PandaCoder Team
 * @since 2.2.0
 */
@State(
    name = "GitStatUserIdentityConfig",
    storages = @Storage(StoragePathMacros.NON_ROAMABLE_FILE)
)
public class UserIdentityConfigState implements PersistentStateComponent<UserIdentityConfigState> {
    
    /**
     * 用户自定义用户名
     */
    private String userName = "";
    
    /**
     * 用户自定义编码（工号、员工编号等）
     */
    private String userCode = "";
    
    /**
     * 用户邮箱（可选）
     */
    private String userEmail = "";
    
    /**
     * 用户部门（可选）
     */
    private String userDepartment = "";
    
    /**
     * 获取应用级别的配置实例
     */
    public static UserIdentityConfigState getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(UserIdentityConfigState.class);
    }
    
    @Nullable
    @Override
    public UserIdentityConfigState getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull UserIdentityConfigState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    // Getters and Setters
    
    public String getUserName() {
        return userName != null ? userName : "";
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserCode() {
        return userCode != null ? userCode : "";
    }
    
    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
    
    public String getUserEmail() {
        return userEmail != null ? userEmail : "";
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserDepartment() {
        return userDepartment != null ? userDepartment : "";
    }
    
    public void setUserDepartment(String userDepartment) {
        this.userDepartment = userDepartment;
    }
    
    /**
     * 检查用户信息是否已配置
     * 
     * @return 如果用户名和用户编码都已配置则返回true
     */
    public boolean isConfigured() {
        return userName != null && !userName.trim().isEmpty() 
            && userCode != null && !userCode.trim().isEmpty();
    }
    
    /**
     * 获取用户显示名称
     * 格式：用户名 (用户编码)
     * 
     * @return 用户显示名称
     */
    public String getDisplayName() {
        if (isConfigured()) {
            return userName + " (" + userCode + ")";
        }
        return "未配置用户信息";
    }
}

