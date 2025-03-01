package com.kaige.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kaige.model.dto.picture.BatchEditePictureDto;
import com.kaige.model.dto.space.SpaceAddDto;
import com.kaige.model.dto.space.SpaceQueryDto;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kaige.model.entity.User;
import com.kaige.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 15336
* @description 针对表【space】的数据库操作Service
* @createDate 2025-02-03 16:33:59
*/
public interface SpaceService extends IService<Space> {

    // 将查询请求转换为QueryWrapper对象
    QueryWrapper<Space> getQueryWrapper(SpaceQueryDto spaceQueryDto);

    // 获取 单张空间封装类
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    // 获取 多张空间封装类
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 检验空间
     * @param Space
     * @param add 为true时表示新增，为false时表示修改
     */
    void validSpace(Space Space,boolean add);

    /**
     * 根据空间级别填充空间信息
     * @param Space
     */
    void fillSpaceBySpaceLevel(Space Space);


    /**
     * 校验空间权限
     * @param space
     */
    void checkSpaceAuth(Space space,User loginUser);

    // 用户创建空间
    long addSpace(SpaceAddDto spaceAddDto,User loginUser);

}
