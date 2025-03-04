package com.kaige.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.spaceUser.SpaceUserAddDto;
import com.kaige.model.dto.spaceUser.SpaceUserQueryDto;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.SpaceUser;
import com.kaige.model.entity.User;
import com.kaige.model.enums.SpaceRoleEnum;
import com.kaige.model.vo.SpaceUserVo;
import com.kaige.model.vo.SpaceVO;
import com.kaige.model.vo.UserVo;
import com.kaige.service.SpaceService;
import com.kaige.service.SpaceUserService;
import com.kaige.mapper.SpaceUserMapper;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author 15336
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service实现
* @createDate 2025-03-01 16:47:37
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Override
    public long addSpaceUser(SpaceUserAddDto spaceUserAddDto) {
        //参数校验
        ThrowUtils.throwIf(spaceUserAddDto == null , ErrorCode.PARAMS_ERROR,"参数不能为空");
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddDto,spaceUser);
        validSpaceUser(spaceUser,true);
        // 添加
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"添加失败");
        return spaceUser.getId();
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDto spaceUserQueryDto) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if(spaceUserQueryDto == null){
            return queryWrapper;
        }
        // 取值
        Long id = spaceUserQueryDto.getId();
        Long spaceId = spaceUserQueryDto.getSpaceId();
        Long userId = spaceUserQueryDto.getUserId();
        Integer spaceRole = spaceUserQueryDto.getSpaceRole();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    @Override
    public SpaceUserVo getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        SpaceUserVo spaceUserVo = SpaceUserVo.convertToSpaceUserVo(spaceUser);
        // 查询关联的用户信息
        User user = userService.getById(spaceUser.getUserId());
        UserVo userVo = userService.getUserVo(user);
        // 注入关联的用户信息
        spaceUserVo.setUserVo(userVo);
        // 查询关联的空间信息
        Space space = spaceService.getById(spaceUser.getSpaceId());
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        // 注入关联的空间信息
        spaceUserVo.setSpaceVo(spaceVO);
        return spaceUserVo;
    }

    @Override
    public List<SpaceUserVo> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 校验参数为空
        if(CollUtil.isEmpty(spaceUserList)){
            return Collections.emptyList();
        }
        // 对象列表 =》 封装类列表
        List<SpaceUserVo> spaceUserVoList = spaceUserList.stream().map(SpaceUserVo::convertToSpaceUserVo).collect(Collectors.toList());
        // 收集关联的用户id和空间id
        List<Long> userIdList = spaceUserVoList.stream().map(SpaceUserVo::getUserId).collect(Collectors.toList());
        List<Long> spaceIdList = spaceUserVoList.stream().map(SpaceUserVo::getSpaceId).collect(Collectors.toList());
        // 法一：每一次for 都要调用 userList.stream().filter()，时间复杂度为O(n),整体时间复杂度为O(n*m)
        /*        // 查询关联的用户信息
        List<User> userList = userService.listByIds(userIdList);
        // 查询关联的空间信息
        List<Space> spaceList = spaceService.listByIds(spaceIdList);
        // 封装类列表 =》 对象列表
        for (SpaceUserVo spaceUserVo : spaceUserVoList) {
            // 查询关联的用户信息
            User user = userList.stream().filter(u -> u.getId().equals(spaceUserVo.getUserId())).findFirst().orElse(null);
            UserVo userVo = userService.getUserVo(user);
            // 注入关联的用户信息
            spaceUserVo.setUserVo(userVo);
            // 查询关联的空间信息
            Space space = spaceList.stream().filter(s -> s.getId().equals(spaceUserVo.getSpaceId())).findFirst().orElse(null);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            // 注入关联的空间信息
            spaceUserVo.setSpaceVo(spaceVO);
        }*/
        // 法二：使用map，将id作为key，对象作为value，时间复杂度为O(1),整体时间复杂度为O(n)
        // 查询关联的用户信息
        Map<Long, List<User>> userIdListMap = userService.listByIds(userIdList).stream().collect(Collectors.groupingBy(User::getId));
        // 查询关联的空间信息
        Map<Long, List<Space>> spaceIdListMap = spaceService.listByIds(spaceIdList).stream().collect(Collectors.groupingBy(Space::getId));
        // 封装类列表 =》 对象列表
        for (SpaceUserVo spaceUserVo : spaceUserVoList) {
            // 查询关联的用户信息
            if(userIdListMap.containsKey(spaceUserVo.getUserId())){
                User user = userIdListMap.get(spaceUserVo.getUserId()).get(0);
                UserVo userVo = userService.getUserVo(user);
                // 注入关联的用户信息
                spaceUserVo.setUserVo(userVo);
            }
            if (spaceIdListMap.containsKey(spaceUserVo.getSpaceId())){
                // 查询关联的空间信息
                Space space = spaceIdListMap.get(spaceUserVo.getSpaceId()).get(0);
                SpaceVO spaceVO = spaceService.getSpaceVO(space);
                // 注入关联的空间信息
                spaceUserVo.setSpaceVo(spaceVO);
            }

        }
        return spaceUserVoList;
    }


    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        // 创建时，需要校验参数是否为空（user,space是否存在
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        if (add){
            ThrowUtils.throwIf(ObjUtil.hasEmpty(userId,spaceId),ErrorCode.PARAMS_ERROR,"参数不能为空");
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null,ErrorCode.PARAMS_ERROR,"用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.PARAMS_ERROR,"空间不存在");
        }
        // 编辑时，校验添加的角色是否存在
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        if(enumByValue == null && spaceRole != null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间角色不存在");
        }
    }
}




