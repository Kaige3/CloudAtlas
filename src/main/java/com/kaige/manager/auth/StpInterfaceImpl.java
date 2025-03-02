package com.kaige.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.manager.auth.constant.SpaceUserPermissionsConstant;
import com.kaige.model.entity.Picture;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.SpaceUser;
import com.kaige.model.entity.User;
import com.kaige.model.enums.SpaceRoleEnum;
import com.kaige.model.enums.SpaceTypeEnum;
import com.kaige.service.PictureService;
import com.kaige.service.SpaceService;
import com.kaige.service.SpaceUserService;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.kaige.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
@Slf4j
public class StpInterfaceImpl implements StpInterface {


    @Value("${server.servlet.context-path}")
    private  String contextPath;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;

    /**
     * 返回一个账号所拥有的权限码集合
     * 需要兼容公共图库，私人空间，团队空间
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断loginType，仅对类型为：space进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        // 拿到管理员权限
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionListByRole(SpaceRoleEnum.ADMIN.getValue());
        log.info("ADMIN_PERMISSIONS:{}",ADMIN_PERMISSIONS);
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextRequest();
        log.info("authContext:{}",authContext);
        // 如果所有字段都为空，表示查询公共图库，可以通过
        if(isAllFieldEmpty(authContext)){
            return ADMIN_PERMISSIONS;
        }
        // 使用Sa-token api获取登录用户的 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"用户未登录");
        }
        // 获取当前登录用户id
        Long userId = loginUser.getId();
        // 优先从上下文获取 SpaceUser对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if(spaceUser != null){ // 返回团队空间成员的权限
            return spaceUserAuthManager.getPermissionListByRole(spaceUser.getSpaceRole());
        }
        // 从上下文获取SpaceUser对象的id ？？
        Long spaceUserId = authContext.getSpaceUserId();
        // 如果spaceUserId存在，是团队空间，通过数据库查询SpaceUser对象
        if(spaceUserId != null){
            spaceUser = spaceUserService.getById(spaceUserId);
            if(spaceUser == null){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"未找到空间用户信息");
            }
            // 取出当前登录用户对应的spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)// 联合索引，代表表中的唯一值
                    .one();
            if(loginSpaceUser == null){
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionListByRole(loginSpaceUser.getSpaceRole());
        }
        // 没有spaceUserId, 尝试用spaceId 或 pictureId 获取Space对象
        Long spaceId = authContext.getSpaceId();
        if(spaceId == null){
            Long pictureId = authContext.getPictureId();
            if(pictureId == null){
                // 默认通过权限校验 ？？
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if(picture == null){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"未找到图片信息");
            }
            // 拿到图片的spaceId
             spaceId = picture.getSpaceId();
            // null表示公共图库,返回所有权限
            if(spaceId == null){
                // 是否为当前用户的图片
                if(picture.getUserId().equals(userId) || userService.isAdmin(loginUser)){
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是改用户的图片,仅可查看
                    return Collections.singletonList(SpaceUserPermissionsConstant.PICTURE_VIEW);
                }
            }
        }
        Space space = spaceService.getById(spaceId);
        if(space == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"未找到空间信息");
        }
        // 根据空间类型判断权限
        if(space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()){
            // 私有空间，仅允许管理员和空间成员查看
            if(space.getUserId().equals(userId) || userService.isAdmin(loginUser)){
                return ADMIN_PERMISSIONS;
            } else {
                // 不是改用户的图片,仅可查看
                return new ArrayList<>();
            }
        } else{ // 团队空间
            // 先从数据库中查询
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                   .eq(SpaceUser::getSpaceId, spaceId)
                   .eq(SpaceUser::getUserId, userId)// 联合索引，代表表中的唯一值
                   .one();
            if(loginSpaceUser == null){
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionListByRole(loginSpaceUser.getSpaceRole());
        }
    }

    /**
     * 校验对象的所有字段是否为空
     * @param authContext
     * @return
     */
    private boolean isAllFieldEmpty(Object authContext) {
        if (authContext == null) {
            return true;
        }
        return Arrays.stream(ReflectUtil.getFields(authContext.getClass()))
                .map(field -> ReflectUtil.getFieldValue(authContext,field))
                .allMatch(ObjUtil::isEmpty);
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        List<String> list = new ArrayList<String>();    
        list.add("admin");
        list.add("super-admin");
        return list;
    }


    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextRequest(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authContext;
        // 兼容get / post
        if(ContentType.JSON.getValue().equals(contentType)) { // post请求
            String body = ServletUtil.getBody(request);
            authContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        Long id = authContext.getId();
        if(ObjUtil.isNotNull(id)){
            String requestURI = request.getRequestURI();
            String partUri = requestURI.replace(contextPath + "/", "");
            String modulName = StrUtil.subBefore(partUri, "/", false);
            switch (modulName){
                case "picture":
                    authContext.setPictureId(id);
                    break;
                case "space":
                    authContext.setSpaceId(id);
                    break;
                case "user":
                    authContext.setUserId(id);
                    break;
                default:
                    break;
            }
        }
        return authContext;
    }

}
