package com.kaige.controller;

import com.kaige.Result.BaseResponse;
import com.kaige.Result.ResultUtils;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.space.analyze.dto.SpaceCategoryAnalyzeDto;
import com.kaige.model.dto.space.analyze.dto.SpaceRankAnalyzeDto;
import com.kaige.model.dto.space.analyze.dto.SpaceUsageAnalyzeDto;
import com.kaige.model.dto.space.analyze.dto.SpaceUserAnalyzeDto;
import com.kaige.model.dto.space.analyze.vo.*;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.User;
import com.kaige.service.SpaceAnalyzeService;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;
    @Resource
    private UserService userService;

    /**
     * 获取空间使用状态
     */
    @PostMapping("/getSpaceUsageAnalyze")
    public BaseResponse<SpaceUsageAnalyzeVo> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeDto spaceUsageAnalyzeDto,
            HttpServletRequest request
            ) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数为空");
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeVo spaceUsageAnalyzeVo = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeDto, loginUser);
        return ResultUtils.success(spaceUsageAnalyzeVo);
    }
    /**
     * 图片分类分析
     */
    @PostMapping("/getSpaceCategoryAnalyze")
    public BaseResponse<List<SpaceCategoryAnalyzeVo>> getSpaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto,
            HttpServletRequest request
            ){
        ThrowUtils.throwIf(spaceCategoryAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeVo> spaceCategoryAnalyzeVo = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeDto, loginUser);
        return ResultUtils.success(spaceCategoryAnalyzeVo);
    }

    //图片标签分析
    @PostMapping("/getSpaceTagAnalyze")
    public BaseResponse<List<SpaceTagAnalyzeVo>> getSpaceTagAnalyze(
            @RequestBody SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto,
            HttpServletRequest request
            ){
        ThrowUtils.throwIf(spaceCategoryAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeVo> spaceTagAnalyzeVo = spaceAnalyzeService.getSpaceTagAnalyze(spaceCategoryAnalyzeDto, loginUser);
        return ResultUtils.success(spaceTagAnalyzeVo);
    }

    //图片范围 数量
    @PostMapping("/getSpaceRangeAnalyze")
    public BaseResponse<List<SpaceSizeAnalyzeVo>> getSpaceRangeAnalyze(
            @RequestBody SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto,
            HttpServletRequest request
            ){
        ThrowUtils.throwIf(spaceCategoryAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeVo> spaceSizeAnalyze = spaceAnalyzeService.getSpaceSizeAnalyze(spaceCategoryAnalyzeDto, loginUser);
        return ResultUtils.success(spaceSizeAnalyze);
    }

    //图片上传时间分析
    @PostMapping("/getSpaceUploadTimeAnalyze")
    public BaseResponse<List<SpaceUserAnalyzeVo>> getSpaceUploadTimeAnalyze(
            @RequestBody SpaceUserAnalyzeDto spaceUserAnalyzeDto,
            HttpServletRequest request
            ){
        ThrowUtils.throwIf(spaceUserAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeVo> spaceUploadTimeAnalyze = spaceAnalyzeService.getUserUploadAnalyze(spaceUserAnalyzeDto, loginUser);
        return ResultUtils.success(spaceUploadTimeAnalyze);
    }
    //空间排名
    @PostMapping("/getSpaceRankAnalyze")
    public BaseResponse<List<com.kaige.model.entity.Space>> getSpaceRankAnalyze(
            @RequestBody SpaceRankAnalyzeDto spaceRankAnalyzeDto,
            HttpServletRequest request
            ){
                ThrowUtils.throwIf(spaceRankAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数为空");
                User loginUser = userService.getLoginUser(request);
                List<Space> spaceRankAnalyze = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeDto, loginUser);
                return ResultUtils.success(spaceRankAnalyze);
            }
}
