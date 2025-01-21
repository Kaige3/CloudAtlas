package com.kaige.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.model.entity.User;
import com.kaige.service.UserService;
import com.kaige.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author 15336
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-01-21 18:54:46
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




