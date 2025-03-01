package com.kaige.model.vo;

import com.kaige.model.entity.SpaceUser;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceUserVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long spaceId;

    private Long userId;

    private String spaceRole;

    private Date createTime;

    private Date updateTime;

    // 用户信息
    private UserVo userVo;

    // 空间信息
    private SpaceVO spaceVo;

    // 封装类转对象
    public static SpaceUser convertToSpaceUserVo(SpaceUserVo spaceUserVo) {
        if (spaceUserVo == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVo, spaceUser);
        return spaceUser;
    }

    // 对象转封装类
    public static SpaceUserVo convertToSpaceUserVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVo spaceUserVo = new SpaceUserVo();
        BeanUtils.copyProperties(spaceUser, spaceUserVo);
        return spaceUserVo;
    }


}
