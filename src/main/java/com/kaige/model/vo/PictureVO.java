package com.kaige.model.vo;

import cn.hutool.json.JSONUtil;
import com.kaige.model.entity.Picture;
import com.kaige.model.entity.User;
import com.kaige.service.UserService;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String url;
    private String thumbnailUrl;
    private String name;
    private String introduction;
    private List<String> tags;
    private String category;
    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private Double picScale;
    private String picFormat;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    private Date editTime;

    private UserVo user;
    private Long spaceId;
    private String picColor;


    /**
     * 封装类转对象
     * @param pictureVO
     * @return
     */
    public static Picture voToObj(PictureVO pictureVO){
        if(pictureVO == null){
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO,picture);
        picture.setTags(JSONUtil.toJsonStr(
                pictureVO.getTags()
        ));
        return picture;
    }

    /**
     * 对象转封装类
     * @param picture
     * @return
     */
    public static PictureVO objToVo(Picture picture){
        if(picture == null){
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture,pictureVO);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(),String.class));
        return pictureVO;
    }



    // 分页获取图片封装类

}
