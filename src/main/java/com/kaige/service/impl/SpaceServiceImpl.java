package com.kaige.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.space.SpaceQueryDto;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.User;
import com.kaige.model.enums.SpaceLevelEnum;
import com.kaige.model.vo.SpaceVO;
import com.kaige.model.vo.UserVo;
import com.kaige.service.SpaceService;
import com.kaige.mapper.SpaceMapper;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 15336
* @description 针对表【space】的数据库操作Service实现
* @createDate 2025-02-03 16:33:59
*/
@Service
@Slf4j
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryDto spaceQueryDto) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryDto == null) {
            return queryWrapper;
        }
        Long id = spaceQueryDto.getId();
        Long userId = spaceQueryDto.getUserId();
        String spaceName = spaceQueryDto.getSpaceName();
        Integer spaceLevel = spaceQueryDto.getSpaceLevel();

        queryWrapper.eq(id != null,"id",id)
                .eq(userId!= null,"userId",userId)
                .like(StrUtil.isNotBlank(spaceName),"spaceName",spaceName)
               .eq(spaceLevel!= null,"spaceLevel",spaceLevel);

        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = space.getUserId();
        if(userId !=null && userId > 0){
            User userInfo = userService.getById(userId);
            UserVo userVo = userService.getUserVo(userInfo);
            spaceVO.setUser(userVo);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();

        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());

        if(CollUtil.isEmpty(spaceList)){
            return spaceVOPage;
        }
        // 对象列表 --> 封装类列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());

        // 收集用户id
        Set<Long> userIdSet = spaceList.stream()
               .map(Space::getUserId)
               .collect(Collectors.toSet());

        // 从数据库查询用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        log.info(userIdUserListMap.toString() + "======================");

        // 填充用户信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if(userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVo(user));
        });
        spaceVOPage.setRecords(spaceVOList);

        return spaceVOPage;
    }

    // 校验空间
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);

        // 创建
        if (add) {
          if (StrUtil.isBlank(spaceName)){
              throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空");
          }
          if (enumByValue == null){
              throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间等级不能");
          }
        }
        // 修改数据时
        if(spaceLevel != null && enumByValue == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间等级不存在");
        }
        if(StrUtil.isNotBlank(spaceName) && spaceName.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称过长");
        }

    }

    /**
     * 创建或者更新空间，根据空间级别，自动填充数据
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别填充数据
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if(enumByValue != null){
            long maxSize = enumByValue.getMaxSize();
            if(space.getMaxSize() == null){
                space.setMaxSize(maxSize);
            }
            long maxCount = enumByValue.getMaxCount();
            if(space.getMaxCount() == null){
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        //仅本人 或管理员可以编辑
        if(!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

}




