package com.kaige.Aspect;

import com.kaige.annotation.AuthCheck;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.entity.User;
import com.kaige.model.enums.UserRoleEnum;
import com.kaige.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userservice;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userservice.getLoginUser(request);
        UserRoleEnum enumByValue = UserRoleEnum.getEnumByValue(mustRole);
        if (enumByValue == null) {
            return joinPoint.proceed(); // 无权限，放行
        }
        // 必须有该角色
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 无权限
        if (userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 必须是管理员,但是用户不是管理员
        if (UserRoleEnum.ADMIN.equals(enumByValue) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }


}
