package com.kaige.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kaige.model.dto.space.analyze.dto.*;
import com.kaige.model.dto.space.analyze.vo.*;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.User;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {

    //校验空间权限，填充查询字段
    void checkSpaceAnalyzeAuth(SpaceAnalyzeDto spaceAnalyzeDto, User loginUser);

    //空间资源使用分析
    SpaceUsageAnalyzeVo getSpaceUsageAnalyze(SpaceUsageAnalyzeDto spaceUsageAnalyzeDto, User loginUser);

    //图片分类分析
    List<SpaceCategoryAnalyzeVo> getSpaceCategoryAnalyze(SpaceAnalyzeDto spaceAnalyzeDto, User loginUser);

    //图片标签分析
    List<SpaceTagAnalyzeVo> getSpaceTagAnalyze(SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto, User loginUser);

    //图片大小范围分析
    List<SpaceSizeAnalyzeVo> getSpaceSizeAnalyze(SpaceAnalyzeDto spaceAnalyzeDto, User loginUser);

    //用户上传行为分析 按照时间维度 统计上传数量
    List<SpaceUserAnalyzeVo> getUserUploadAnalyze(SpaceUserAnalyzeDto spaceUserAnalyzeDto, User loginUser);

    //限制：管理员可用 查看空间排名
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeDto spaceRankAnalyzeDto, User loginUser);
}
