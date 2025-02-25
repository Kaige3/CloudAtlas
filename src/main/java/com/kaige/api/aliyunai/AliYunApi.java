package com.kaige.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.kaige.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.kaige.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.picture.GetOutPaintingTaskResponse;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunApi {
    @Value("${aliYunAi.apiKey}")
    private   String apiKey;

    // 请求任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 任务查询地址
    public static final String QUERY_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks";

    /**
     * 请求任务
     * @param create
     * @return
     */
    public   CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest create) {
        ThrowUtils.throwIf(create == null, ErrorCode.PARAMS_ERROR,"请求失败");
        HttpRequest request = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(create));
        try(HttpResponse execute = request.execute()){
            if (!execute.isOk()){
                log.error("请求异常：{}",execute.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI阔图失败");
            }
            CreateOutPaintingTaskResponse bean = JSONUtil.toBean(execute.body(), CreateOutPaintingTaskResponse.class);
            String code = bean.getCode();
            if(StrUtil.isNotBlank(code)){
                String message = bean.getMessage();
                log.error("请求异常：{}",message);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI阔图失败");
            }
            return bean;
        }
    }

    public GetOutPaintingTaskResponse createOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR,"请求失败");
        String url = QUERY_OUT_PAINTING_TASK_URL + "/" + taskId;
        HttpResponse execute = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .execute();
        if (!execute.isOk()){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取请求任务失败");
        }
        return JSONUtil.toBean(execute.body(), GetOutPaintingTaskResponse.class);
    }
}
