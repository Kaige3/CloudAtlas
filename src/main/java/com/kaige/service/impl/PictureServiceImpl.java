package com.kaige.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.manager.CosManager;
import com.kaige.manager.upload.FilePictureUpload;
import com.kaige.manager.upload.PictureUploadTemplate;
import com.kaige.manager.upload.UrlPictureUpload;
import com.kaige.model.dto.file.UploadPictureDto;
import com.kaige.model.dto.picture.*;
import com.kaige.model.entity.Picture;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.User;
import com.kaige.model.enums.PictureReviewStatusEnum;
import com.kaige.model.vo.PictureVO;
import com.kaige.model.vo.UserVo;
import com.kaige.service.PictureService;
import com.kaige.mapper.PictureMapper;
import com.kaige.service.UserService;
import com.kaige.utils.ColorSimilarUtils;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 15336
* @description 针对表【picture】的数据库操作Service实现
* @createDate 2025-01-24 21:43:57
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private SpaceServiceImpl spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadDto pictureUploadDto, User loginUser) {

        if(inputSource == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片不能为空");
        }
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验空间 和是否具有空间权限
        Long spaceId = pictureUploadDto.getSpaceId();
        if(spaceId!= null){
            // 校验空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.PARAMS_ERROR,"空间不存在");
            // 校验用户是否具有空间权限,必须是空间管理员
            if(!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"您没有空间权限");
            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间额度不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间额度不足");
            }
        }
        // 判断是新增 还是 更新图片
        Long pictureId = null;
        pictureId = pictureUploadDto.getId();
        // 如果是更新图片，首先检验图片是否存在
        if(pictureId != null){
            Picture oldPicture = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .eq(Picture::getUserId, loginUser.getId())
                    .oneOpt()
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在"));
            // 校验空间id是否一致
            // 没穿spaceId,就复用原有图片的空间id，能够兼容公共图库
            if (spaceId == null){
                if(oldPicture.getSpaceId() !=null){
                    spaceId = oldPicture.getSpaceId();
                }
            }else {
                // 有传spaceId,校验空间id是否一致
                if(!spaceId.equals(oldPicture.getSpaceId())){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间id不一致");
                }
            }
        }
        // 上传图片，设置图片基本信息
        // 按照用户id 划分目录
        String uploadPathPrefix;
        if(spaceId == null){
            // 没有spaceId 是公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else{
            // 有spaceId 是私人空间图库
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        // 根据inputSource 类型判断是上传的图片还是url
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if(inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureDto upload = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(upload.getUrl());
        String picName = upload.getPicName();
        if(StrUtil.isNotBlank(pictureUploadDto.getPicName())){
            picName = pictureUploadDto.getPicName();
        }
        picture.setSpaceId(spaceId);
        picture.setName(picName);
        picture.setPicSize(upload.getPicSize());
        picture.setPicWidth(upload.getPicWidth());
        picture.setPicHeight(upload.getPicHeight());
        picture.setPicScale(upload.getPicScale());
        picture.setPicFormat(upload.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setThumbnailUrl(upload.getThumbnailUrl());
        picture.setPicColor(upload.getPicColor());
        // 审核信息
        this.fillReviewInfo(picture,loginUser);
        // 如果 pictureId 不为空,是更新图片
        if(pictureId!= null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());

        }else{
            picture.setCreateTime(new Date());
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        log.info("finalSpaceId:{}",finalSpaceId);
        System.out.println("finalSpaceId:{}====================="+finalSpaceId);
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR,"图片上传失败");
            if (finalSpaceId != null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount +1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.SYSTEM_ERROR,"图片额度更新失败");
            }
            return picture;
        });

        // 返回封装类图片信息
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryDto pictureQueryDto) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryDto == null) {
            return queryWrapper;
        }

        // 从对象中取值
        Long id = pictureQueryDto.getId();
        String name = pictureQueryDto.getName();
        String introduction = pictureQueryDto.getIntroduction();
        String category = pictureQueryDto.getCategory();
        List<String> tags = pictureQueryDto.getTags();
        Long picSize = pictureQueryDto.getPicSize();
        Integer picWidth = pictureQueryDto.getPicWidth();
        Integer picHeight = pictureQueryDto.getPicHeight();
        Double picScale = pictureQueryDto.getPicScale();
        String picFormat = pictureQueryDto.getPicFormat();
        String searchText = pictureQueryDto.getSearchText();
        Long userId = pictureQueryDto.getUserId();
        String sortField = pictureQueryDto.getSortField();
        String sortOrder = pictureQueryDto.getSortOrder();
        Long spaceId = pictureQueryDto.getSpaceId();
        boolean spaceIdIsNull = pictureQueryDto.isSpaceIdIsNull();

        // 补充审核信息
        Long reviewerId = pictureQueryDto.getReviewerId();
        String reviewMessage = pictureQueryDto.getReviewMessage();
        Integer reviewStatus = pictureQueryDto.getReviewStatus();

        // 多字段查询
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText))
                    .or()
                    .like("introduction", searchText);
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(introduction), "introduction", introduction)
                .like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat)
                .eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth)
                .eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight)
                .eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale)
                .eq(StrUtil.isNotBlank(category), "category", category)
                .eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize)
                .eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus)
                .eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId)
                .like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage)
                .eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId)
                .isNull(spaceIdIsNull,"spaceId")
        ;
        Date startEditTime = pictureQueryDto.getStartEditTime();
        Date endEditTime = pictureQueryDto.getEndEditTime();
        // 补充时间范围
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        // JSON 数组查询
        if(CollUtil.isNotEmpty(tags)){
            for (String tag:tags){
                queryWrapper.like("tags","/" + tag+ "/");
            }
        }

        // 排序
        String safeSortOrder = (sortOrder == null) ? "ascend" : sortOrder;  // 提供默认排序方式
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(safeSortOrder), sortField);

//        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField),sortOrder.equals("ascend"),sortField);
        return queryWrapper;
    }

    @Override
    // 查询图片用户关联信息
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request){
        // 对象转 封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询 用户信息
        Long userId = picture.getUserId();
        if(userId!= null &&  userId > 0){
            User user = userService.getById(userId);
            UserVo userVo = userService.getUserVo(user);
            pictureVO.setUser(userVo);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> records = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if(CollUtil.isEmpty(records)){
            return pictureVOPage;
        }

        // 把对象列表转换为 —> 封装对象列表
        List<PictureVO> pictureVOList = records.stream().map(picture -> getPictureVO(picture, request)).collect(Collectors.toList());
        // 从图片列表中 搜集用户信息用于查询
        Set<Long> userIdSet = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 根据用户id查询用户信息Map集合
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 填充 信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if(userIdUserListMap.containsKey(userId)){
                List<User> userList = userIdUserListMap.get(userId);
                user = userList.get(0);
            }
            // 上面的操作 都是为了铺垫 下面的操作
            // 设置用户信息
            pictureVO.setUser(userService.getUserVo(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null , ErrorCode.PARAMS_ERROR);

        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id不能为空，有参数则校验
        ThrowUtils.throwIf(!ObjUtil.isNotNull(id), ErrorCode.PARAMS_ERROR,"id 不能为空");
        if(StrUtil.isNotBlank(url)){
            ThrowUtils.throwIf(url.length() > 1024 ,ErrorCode.PARAMS_ERROR,"url 不能太长");
        }
        if(StrUtil.isBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 800,ErrorCode.PARAMS_ERROR,"简介不能大于800字");
        }
    }

    @Override
    public void doPictureReview(PictureReviewDto pictureReviewDto, User loginUser) {
        Long id = pictureReviewDto.getId();
        Integer reviewStatus = pictureReviewDto.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        if(id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 判断是否存在
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR);

        // 判断是否已经审核过了
        if(picture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已经审核过了");
        }

        // 更新审核状态
        Picture updatePic = new Picture();
        BeanUtils.copyProperties(pictureReviewDto,updatePic);
        updatePic.setReviewTime(new Date());
        updatePic.setReviewerId(loginUser.getId());
        boolean result = this.updateById(updatePic);
        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);
    }

    @Override
    public void fillReviewInfo(Picture picture, User loginUser) {
        if(userService.isAdmin(loginUser)){
            // 管理员自动审核
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都需要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REJECT.getValue());

        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchDto pictureUploadByBatchDto, User loginUser) {
        String searchText = pictureUploadByBatchDto.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchDto.getCount();
        ThrowUtils.throwIf(count > 30 ,ErrorCode.PARAMS_ERROR,"最多只能上传30张");
        // 要抓取的地址
        String formatUrl = String.format("http://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(formatUrl).get();
//            log.info(document.outerHtml()+"------------------------");
        } catch (IOException e) {
            log.error("抓取bing图片失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"抓取页面失败");
        }

        Element div = document.getElementsByClass("dgControl").first();

        if (ObjUtil.isNull(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"抓取元素失败");
        }

        Elements elementsByClassList = div.select("img.mimg");

        int uploadCount = 0;

        for (Element element : elementsByClassList) {
            String firUrl = element.attr("src");
            if(StrUtil.isBlank(firUrl)){
                log.info("当前链接为空，已跳过：{}",firUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = firUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                firUrl = firUrl.substring(0, questionMarkIndex);
            }
            log.info(firUrl+","+"00000000000000000000");
            // 上传图片
            String namePrefix = pictureUploadByBatchDto.getNamePrefix();
            if(StrUtil.isBlank(namePrefix)){
                namePrefix = searchText;
            }
            try {
                // 上传图片
                PictureUploadDto pictureUploadDto = new PictureUploadDto();

                if (StrUtil.isNotBlank(namePrefix)) {
                    // 图片名称
                    pictureUploadDto.setPicName(namePrefix +  (uploadCount + 1 ));
                }

                PictureVO pictureVO = this.uploadPicture(firUrl, pictureUploadDto, loginUser);
                log.info("上传成功：{}",pictureVO.getId());
                uploadCount++;

            } catch (Exception e) {
                log.error("图片上传失败",e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public void deletePicture(long id, User loginUser) {
        Picture pictureOld = this.getById(id);

        ThrowUtils.throwIf(pictureOld == null,ErrorCode.NOT_FOUND_ERROR);

        // 只有本人 和 空间管理员可以删除
        this.checkPictureAuth(pictureOld, loginUser);

        // 操作数据库 删除
        if (pictureOld.getSpaceId() != null){
            transactionTemplate.execute(status -> {
                boolean result = this.removeById(id);
                ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);

                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, pictureOld.getSpaceId())
                        .setSql("totalSize = totalSize - " + pictureOld.getPicSize())
                        .setSql("totalCount = totalCount -1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"额度更新失败");
                return true;
            });
        } else {
            boolean result = this.removeById(id);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"删除图片失败");
        }

        this.clearPictureFile(pictureOld);
    }

    @Override
    public void editPicture(PictureEditDto editDto, HttpServletRequest request) {
        // 将实体类 和 封装类进行 转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(editDto, picture);
        // 接受的是 封装类，入库的是实体类
        // 将list 转为 string
        picture.setTags(JSONUtil.toJsonStr(editDto.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        User loginUser = userService.getLoginUser(request);

        // 判断是否存在
        Long id = editDto.getId();
        Picture byId = this.getById(id);
        ThrowUtils.throwIf(byId == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        // 只有本人 和 管理员可以编辑
        this.checkPictureAuth(byId, loginUser);
//        this.fillReviewInfo(picture, loginUser);
        // 操作数据库
        boolean b = this.updateById(picture);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void clearPictureFile(Picture picture) {
        // 判断土拍你是否被多条记录使用
        String pictureUrl = picture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 记录的条数多余于1，说明被多个用户使用了，不删除
        if(count > 1){
            return;
        }
        cosManager.deleteObject(picture.getUrl());
        // 清理缩略图
        String thumbnailUrl = picture.getThumbnailUrl();
        if(StrUtil.isNotBlank(thumbnailUrl)){
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(Picture pictureOld, User loginUser) {
        Long spaceId = pictureOld.getSpaceId();
        Long userId = loginUser.getId();
        if(spaceId == null){
            // 公共图片下，只有用户本人和管理员可以删除
            if (!pictureOld.getUserId().equals(userId) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }else {
            // 私人空间只有用户可以删除
            if (!pictureOld.getUserId().equals(userId)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String color, User loginUser) {
        // 判断参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(color),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.NOT_LOGIN_ERROR);
        // 判断权限
        // 当前用户是否是空间管理员
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null,ErrorCode.PARAMS_ERROR,"空间不存在");
        if(!space.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有访问空间的权限");
        }
        // 查询数据库
        List<Picture> pictureList = this.lambdaQuery()
               .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
               .list();
        // 如果没有图片，返回空
        if(CollUtil.isEmpty(pictureList)){
            return null;
        }
        // 调用utils
        // 将color的哈希值 转换为Color对象
        Color targetColor = Color.decode(color);
        // 计算相似度并排序
        List<Picture> collect = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    // 提取图片主色调
                    String picColor = picture.getPicColor();
                    // 将picColor的哈希值 转换为Color对象
                    Color picColorObj = Color.decode(picColor);
                    // 计算相似度
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, picColorObj);
                }))
                .limit(12)
                .collect(Collectors.toList());
        // 转为VO
        return collect.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchEditePicture(BatchEditePictureDto batchEditePictureDto, User loginUser) {
        // 检验参数
        List<Long> pictureIdList = batchEditePictureDto.getPictureIdList();
        Long spaceId = batchEditePictureDto.getSpaceId();
        String category = batchEditePictureDto.getCategory();
        List<String> tags = batchEditePictureDto.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(category),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(CollUtil.isEmpty(tags),ErrorCode.PARAMS_ERROR);
        // 校验空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null,ErrorCode.PARAMS_ERROR,"空间不存在");
        // 校验空间权限
        spaceService.checkSpaceAuth(space,loginUser);
        // 按IdList查询图片列表,仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList == null){
            return;
        }
        // 批量设置分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)){
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)){
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });

        // 批量重命名
        String nameRule = batchEditePictureDto.getNameRule();
        fillPictureWithNameRule(pictureList,nameRule);

        // 持久化
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
    }

    /**
     * 规则：名称 {序号}
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)){
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                picture.setName(nameRule.replaceAll("\\{序号}", String.valueOf(count)));
                count++;
            }
        } catch (Exception e) {
            log.error("批量重命名失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"批量重命名失败");
        }
    }


}




