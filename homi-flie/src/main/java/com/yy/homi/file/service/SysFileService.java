package com.yy.homi.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.domain.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

public interface SysFileService extends IService<SysFile> {
    //上传单个文件
    R uploadOne(MultipartFile file);

    //删除一个文件
    R deleteOne(String fileId);

    //批量上传文件
    R uploadBatch(MultipartFile[] files);



    //秒传检查
    SysFile getReusableSysFile(String fileHash, String filename);

    //上传文件
    void uploadMinio(byte[] bytes, String fileHash,String bucketName, String extension, String fileName);


}
