package com.kaige.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.mapper.SpaceMapper;
import com.kaige.model.dto.space.analyze.dto.*;
import com.kaige.model.dto.space.analyze.vo.*;
import com.kaige.model.entity.Picture;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.User;
import com.kaige.service.PictureService;
import com.kaige.service.SpaceAnalyzeService;
import com.kaige.service.SpaceService;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {


    @Resource
    private UserService userService;
    @Resource
    private  SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    /**
     * 空间资源使用分析
     * @param spaceUsageAnalyzeDto
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeVo getSpaceUsageAnalyze(SpaceUsageAnalyzeDto spaceUsageAnalyzeDto, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceUsageAnalyzeDto == null,ErrorCode.PARAMS_ERROR,"参数为空");
        // 检验权限
        // 管理员公共图库
        if(spaceUsageAnalyzeDto.isQueryAll() || spaceUsageAnalyzeDto.isQueryPublic()) {
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeDto, loginUser);
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 设置查询范围
//            if (!spaceUsageAnalyzeDto.isQueryAll()) {
//                queryWrapper.isNull("spaceId");
//            }
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeDto,queryWrapper);
            // 查询目标值
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            // 统计
            Long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Integer ? (Integer) result : 0).sum();
            long usedCount = pictureObjList.size();

            // 封装返回结果
            SpaceUsageAnalyzeVo spaceUsageAnalyzeVo = new SpaceUsageAnalyzeVo();
            spaceUsageAnalyzeVo.setUsedSize(usedSize);
            spaceUsageAnalyzeVo.setUsedCount(usedCount);
            // 公共图库没有上限和比例
            spaceUsageAnalyzeVo.setMaxSize(null);
            spaceUsageAnalyzeVo.setMaxCount(null);
            spaceUsageAnalyzeVo.setCountUsageRatio(null);
            spaceUsageAnalyzeVo.setSizeUsageRatio(null);
            return spaceUsageAnalyzeVo;
        } else {
            // 查询指定空间
            Long spaceId = spaceUsageAnalyzeDto.getSpaceId();
            ThrowUtils.throwIf(spaceId == null,ErrorCode.PARAMS_ERROR,"参数为空");
            // 获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            // 校验权限
            spaceService.checkSpaceAuth(space,loginUser);
            // 查询目标值
            SpaceUsageAnalyzeVo spaceUsageAnalyzeVo = new SpaceUsageAnalyzeVo();
            spaceUsageAnalyzeVo.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeVo.setMaxCount(space.getMaxCount());
            // 百分比
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeVo.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeVo.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeVo.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeVo.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeVo;
        }
    }

    /**
    图片分类分析
     **/
    @Override
    public List<SpaceCategoryAnalyzeVo> getSpaceCategoryAnalyze(SpaceAnalyzeDto spaceAnalyzeDto, User loginUser) {
        // 参数检验
        ThrowUtils.throwIf(spaceAnalyzeDto == null,ErrorCode.PARAMS_ERROR,"参数为空");
        // 校验权限
        checkSpaceAnalyzeAuth(spaceAnalyzeDto,loginUser);
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        // 按照权限填充查询范围
        fillAnalyzeQueryWrapper(spaceAnalyzeDto,pictureQueryWrapper);

        // 查询目标值
        pictureQueryWrapper.select("category,COUNT(*) as count,SUM(picSize) as totalSize")
                .groupBy("category");

        // 封装返回结果
        return pictureService.getBaseMapper().selectMaps(pictureQueryWrapper).stream()
                .map(result -> {
                   String category =  result.get("category") != null ? result.get("category").toString():"未分类";
                   Long count = ((Number) result.get("count")).longValue();
                   Long totalSize = ((Number) result.get("totalSize")).longValue();
                   return new SpaceCategoryAnalyzeVo(category,count,totalSize);
                }).collect(Collectors.toList());
    }

    /**
     * 图片标签分析
     * @param spaceCategoryAnalyzeDto
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeVo> getSpaceTagAnalyze(SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceCategoryAnalyzeDto == null,ErrorCode.PARAMS_ERROR,"参数为空");
        //权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeDto,loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeDto,queryWrapper);

        //查询目标值
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        // 合并所有标签 并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //封装返回结果
        return tagCountMap.entrySet().stream()
                .sorted((e1,e2) -> Long.compare(e2.getValue(),e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeVo(entry.getKey(),entry.getValue()))
                .collect(Collectors.toList());

    }

    @Override
    public List<SpaceSizeAnalyzeVo> getSpaceSizeAnalyze(SpaceAnalyzeDto spaceAnalyzeDto, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(spaceAnalyzeDto == null,ErrorCode.PARAMS_ERROR,"参数为空");
        // 校验权限
        checkSpaceAnalyzeAuth(spaceAnalyzeDto,loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 按照权限填充查询范围
        fillAnalyzeQueryWrapper(spaceAnalyzeDto,queryWrapper);
        // 查询目标值
        queryWrapper.select("picSize");
        List<Long> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number)size).longValue())
                .collect(Collectors.toList());

        LinkedHashMap<String, Long> sizeRange = new LinkedHashMap<>();
        sizeRange.put("<100KB",pictureObjList.stream().filter(size -> size < (100 * 1024)).count());
        sizeRange.put("100KB~500KB",pictureObjList.stream().filter(size -> size >= (100 * 1024) && size < (500 * 1024)).count());
        sizeRange.put("500KB~1MB",pictureObjList.stream().filter(size -> size >= (500 * 1024) && size < (1024 * 1024)).count());
        sizeRange.put(">10MB",pictureObjList.stream().filter(size -> size >= (1024 * 1024)).count());

        // 封装返回结果
        return sizeRange.entrySet().stream()
               .map(entry -> new SpaceSizeAnalyzeVo(entry.getKey(),entry.getValue()))
               .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeVo> getUserUploadAnalyze(SpaceUserAnalyzeDto spaceUserAnalyzeDto, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeDto == null,ErrorCode.PARAMS_ERROR,"参数为空");
        // 校验权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeDto,loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 按照权限填充查询范围
        Long userId = spaceUserAnalyzeDto.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeDto,queryWrapper);
        // 查询目标值
        String timeDimension = spaceUserAnalyzeDto.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period, COUNT(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period, COUNT(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') as period, COUNT(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"不支持的时间范围");
        }
        // 分组排序
        queryWrapper.groupBy("period")
                .orderByDesc("period");
        // 封装返回结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper).stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number)result.get("count")).longValue();
                    return new SpaceUserAnalyzeVo(period,count);
                }).collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeDto spaceRankAnalyzeDto, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeDto == null,ErrorCode.PARAMS_ERROR,"参数为空");
        // 校验权限
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.PARAMS_ERROR,"无权限访问");
        // 查询目标值
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id,spaceName,userId,totalSize")
               .orderByDesc("totalSize")
               .last("limit "+ spaceRankAnalyzeDto.getTopN());
        return spaceService.list(queryWrapper);
    }


    /**
     * 校验权限
     * @param spaceAnalyzeDto
     * @param loginUser
     */
    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeDto spaceAnalyzeDto, User loginUser) {
        // 校验权限
        if(spaceAnalyzeDto.isQueryAll() || spaceAnalyzeDto.isQueryPublic()){
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.PARAMS_ERROR,"无权限访问");
        } else {
            // 私有空间权限
            Long spaceId = spaceAnalyzeDto.getSpaceId();
            ThrowUtils.throwIf(spaceId == null,ErrorCode.PARAMS_ERROR,"参数为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            spaceService.checkSpaceAuth(space,loginUser);
        }
    }



    // 填充查询范围
    public static void fillAnalyzeQueryWrapper(SpaceAnalyzeDto spaceAnalyzeDto, QueryWrapper<Picture> queryWrapper) {
        if(spaceAnalyzeDto.isQueryAll()){
            return;
        }
        if(spaceAnalyzeDto.isQueryPublic()){
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeDto.getSpaceId();
        if(spaceId != null){
            queryWrapper.eq("spaceId",spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR,"未指定查询范围呢");
    }




}
