package com.kaige.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.model.entity.SpaceUser;
import com.kaige.service.SpaceUserService;
import com.kaige.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

/**
* @author 15336
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service实现
* @createDate 2025-03-01 16:47:37
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

}




