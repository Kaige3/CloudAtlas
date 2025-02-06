package com.kaige.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.space.SpaceAddDto;
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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Resource
    private TransactionTemplate transactionTemplate;

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

    /**
     * 用户创建空间
     * @param spaceAddDto
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddDto spaceAddDto, User loginUser) {
        // 将dto转换为实体类
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddDto,space);

        // 提供默认值
        if(StrUtil.isBlank(spaceAddDto.getSpaceName())){
            space.setSpaceName("我的空间");
        }
        if (spaceAddDto.getSpaceLevel() == null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        // 检验数据
        this.validSpace(space,true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验
        if(SpaceLevelEnum.COMMON.getValue() != spaceAddDto.getSpaceLevel() &&!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"不能创建指定级别的空间");
        }
        // 对用户加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock){
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "用户已创建空间");
                // 操作数据库
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "创建空间失败");
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }

    }

}




