package com.fenglin.springboottest.config;

import com.fenglin.springboottest.common.ApiResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理：保持 {ok:false, msg} 的统一返回结构
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 神尊相唯一键冲突（并发注册兜底） */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> onDuplicate(DuplicateKeyException e) {
        return ResponseEntity.status(409)
                .body(ApiResult.fail("该神尊相已存在于元界数据库"));
    }

    /** 其它异常统一 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onException(Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResult.fail(e.getMessage() == null ? "服务器内部错误" : e.getMessage()));
    }
}
