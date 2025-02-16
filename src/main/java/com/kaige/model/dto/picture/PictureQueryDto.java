package com.kaige.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureQueryDto implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private Double picScale;
    private String picFormat;

    private String searchText;
    private Long UserId;
    private String sortField;
    private String sortOrder;

    private long pageNum;
    private long current;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    private long pageSize;
    private List<String> tags;

    private Long spaceId;

    /**
     * 查询spaceId为null的图片
     */
    private boolean spaceIdIsNull;

    /**
     * 开始编辑时间
     */
    private Date startEditTime;

    /**
     * 结束编辑时间
     */
    private Date endEditTime;



}
