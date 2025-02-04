package com.kaige.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.manager.CosManager;
import com.kaige.manager.FileManager;
import com.kaige.manager.upload.FilePictureUpload;
import com.kaige.manager.upload.PictureUploadTemplate;
import com.kaige.manager.upload.UrlPictureUpload;
import com.kaige.model.dto.file.UploadPictureDto;
import com.kaige.model.dto.picture.PictureQueryDto;
import com.kaige.model.dto.picture.PictureReviewDto;
import com.kaige.model.dto.picture.PictureUploadByBatchDto;
import com.kaige.model.dto.picture.PictureUploadDto;
import com.kaige.model.entity.Picture;
import com.kaige.model.entity.User;
import com.kaige.model.enums.PictureReviewStatusEnum;
import com.kaige.model.vo.PictureVO;
import com.kaige.model.vo.UserVo;
import com.kaige.service.PictureService;
import com.kaige.mapper.PictureMapper;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private CosManager cosManager;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadDto pictureUploadDto, User loginUser) {

        if(inputSource == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片不能为空");
        }

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 判断是新增 还是 更新图片
        Long pictureId = null;
        if(pictureUploadDto != null){
            pictureId = pictureUploadDto.getId();
        }

        // 如果是更新图片，首先检验图片是否存在
        if(pictureId != null){
            this.lambdaQuery()
                    .eq(Picture::getId,pictureId)
                    .eq(Picture::getUserId,loginUser.getId())
                    .oneOpt()
                    .orElseThrow(()->new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在"));
        }
        // 上传图片，设置图片基本信息
        // 按照用户id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());

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
        if(pictureUploadDto != null && StrUtil.isNotBlank(pictureUploadDto.getPicName())){
            picName = pictureUploadDto.getPicName();
        }
        picture.setName(picName);

        picture.setPicSize(upload.getPicSize());
        picture.setPicWidth(upload.getPicWidth());
        picture.setPicHeight(upload.getPicHeight());
        picture.setPicScale(upload.getPicScale());
        picture.setPicFormat(upload.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setThumbnailUrl(upload.getThumbnailUrl());
        // 审核信息
        fillReviewInfo(picture,loginUser);

        // 如果 pictureId 不为空，更新图片,否则新增图片
        if(pictureId!= null){
//            picture.setId(pictureId);
//            picture.setEditTime(new Date());

            Picture oldPic = this.getById(pictureId);

            ThrowUtils.throwIf(oldPic == null,ErrorCode.NOT_FOUND_ERROR);

            if(!oldPic.getUserId().equals(loginUser.getId()) && userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }else{
            picture.setCreateTime(new Date());
            picture.setEditTime(new Date());

        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR,"图片上传失败");
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
        ;
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

}




