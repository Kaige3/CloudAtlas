package com.kaige.manager.auth;

import com.kaige.model.entity.Picture;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.SpaceUser;
import com.kaige.model.entity.User;
import lombok.Data;


/**
 * 表示用户在特定空间的权限上下文，
 * 包含关联的图片，空间和用户信息
 */
@Data
public class SpaceUserAuthContext {

    /**
     * 临时参数，不同的请求对应不同的临时参数
     */
    private Long id;

    /**
     * 图片id
     */
    private Long pictureId;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 用户id
     */
    private Long userId;

    //空间用户id
    private Long spaceUserId;

    /**
     * 图片信息
     */
    private Picture picture;

    /**
     * 空间信息
     */
    private Space space;

    /**
     * 用户信息
     */
    private User user;

    /**
     * 空间用户信息
     */
    private SpaceUser spaceUser;



}
