package com.kaige.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";

    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);

}
