package com.yy.homi.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.file.domain.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
    //根据文件hash值查询是否有一摸一样的文件
    @Select("select * from sys_file where file_hash = #{fileHash} and del_flag = 0 limit 1")
    SysFile findOneByFileHash(@Param("fileHash") String fileHash);

    @Select("select * from sys_file where file_hash = #{fileHash} and extension=#{extension} and del_flag = 0 limit 1")
    SysFile findOneFileAndExtension(String fileHash, String extension);
}
