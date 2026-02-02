package com.yy.homi.file.constant;


public class FileConstants {


    public static final String CHUNK_MINIO_PATH = "/temp/uploadChunk/%d/chunk-%d.part";//分片文件的minio存储规则


    public static final String UPLOAD_CHUNK_LOCK_PREFIX = "homi:file:chunk_lock:upload:%d:%d"; //上传分片 锁key前缀

    public static final String MERGE_CHUNK_LOCK_PREFIX = "homi:file:chunk_lock:merge:%s";  //合并分片 锁key前缀

}

