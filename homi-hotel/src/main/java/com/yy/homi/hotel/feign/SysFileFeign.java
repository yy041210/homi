package com.yy.homi.hotel.feign;

import com.yy.homi.common.domain.entity.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "homi-file",path = "/homi-file/sysfile")
public interface SysFileFeign {

    //根据url集合上传文件
    @PostMapping("/uploadBatchUrls")
    R uploadBatchByUrls(@RequestBody List<String> urls);
}
