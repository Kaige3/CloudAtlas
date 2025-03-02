package com.kaige.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kaige.manager.auth.dto.SpaceUserAuthConfig;
import com.kaige.manager.auth.dto.SpaceUserRole;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.SpaceUser;
import com.kaige.model.entity.User;
import com.kaige.model.enums.SpaceRoleEnum;
import com.kaige.model.enums.SpaceTypeEnum;
import com.kaige.service.SpaceUserService;
import com.kaige.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 从配置文件中读取用户权限配置
 * 并根据用户角色获取权限列表
 * @author kaige
 * @create 2023-06-24 15:50:49
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;


    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        System.out.println("读取到的 JSON 内容：" + json); // 打印 JSON 内容
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
        System.out.println("读取到的 SPACE_USER_AUTH_CONFIG 对象：" + SPACE_USER_AUTH_CONFIG); // 打印 SPACE_USER_AUTH_CONFIG 对象
        System.out.println(SPACE_USER_AUTH_CONFIG.getRoles()); // 打印 SPACE_USER_AUTH_CONFIG 对象
    }

    /**
     * 根据角色获取权限列表
     * @return
     */
    public List<String> getPermissionListByRole(String role){
        if (StrUtil.isBlank(role)){
            return new ArrayList<>();
        }
        // 找到匹配的角色
        SpaceUserRole spaceUserRole = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> role.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (spaceUserRole == null){
            return new ArrayList<>();
        }
        return spaceUserRole.getPermissions();
    }

    public List<String> getPermissionList(Space space, User loginUser){
        if (loginUser == null){
            return new ArrayList<>();
        }
        List<String> ADMIN_PERMISSIONS = getPermissionListByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if(space == null){
            if(userService.isAdmin(loginUser)){
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null){
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum){
            case PRIVATE:
                if(space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)){
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null){
                    return new ArrayList<>();
                } else {
                    return getPermissionListByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }


}
