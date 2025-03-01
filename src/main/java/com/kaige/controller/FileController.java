package com.kaige.controller;

import com.kaige.result.BaseResponse;
import com.kaige.result.ResultUtils;
import com.kaige.manager.CosManager;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@Slf4j
@Deprecated
public class FileController {

    @Resource
    private CosManager cosManager;

    // 测试上传
    @PostMapping("/test/upload")
    public BaseResponse<String> testUpload(@RequestPart("file") MultipartFile file){
        // 文件路劲
        String filename = file.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file2 = null;
        // 上传文件
        try {
            File tempFile = File.createTempFile(filepath, null);
            file.transferTo(tempFile);
            cosManager.putObject(filepath, tempFile);
            // 返回文件路径

            return ResultUtils.success(filepath);

        } catch (IOException e) {
            log.error("file upload error,filepath = {}", filepath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (file2 != null) {
                // 删除临时文件
                boolean delete = file2.delete();
                if (!delete) {
                    log.error("file delete error,filepath = {}", filepath);
                }
            }
        }
    }

    // 测试下载
    @GetMapping("/test/download")
    public void testDownload(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream objectContent = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            objectContent = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(objectContent);
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error,filepath = {}", filepath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        } finally {
            if(objectContent != null){
                objectContent.close();
            }
        }
    }
}
