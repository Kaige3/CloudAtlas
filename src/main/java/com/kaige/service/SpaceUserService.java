package com.kaige.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kaige.model.dto.spaceUser.*;
import com.kaige.model.dto.spaceUser.SpaceUserEditDto;
import com.kaige.model.dto.spaceUser.SpaceUserQueryDto;
import com.kaige.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kaige.model.vo.SpaceUserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 15336
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service
* @createDate 2025-03-01 16:47:37
*/
public interface SpaceUserService extends IService<SpaceUser> {

    // 添加空间成员
    long addSpaceUser(SpaceUserAddDto spaceUserAddDto);



    // 将查询请求转换为QueryWrapper对象
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDto spaceUserQueryDto);

    // 获取 单张成员空间封装类
    SpaceUserVo getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    // 获取 多张成员空间封装类
    List<SpaceUserVo> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    // 成员空间数据校验

    /**
     * 为指定空间 添加 指定成员
     * add 字段主要针对创建时，和编辑时
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser ,boolean add);


}
