package com.yy.homi.thirdparty.interfaces.impl;

import cn.hutool.core.util.StrUtil;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.common.utils.EncryptUtil;
import com.yy.homi.thirdparty.domain.dto.request.CheckCodeGenerateReqDTO;
import com.yy.homi.thirdparty.interfaces.CheckCodeGenerator;
import com.yy.homi.thirdparty.interfaces.CheckCodeStore;
import com.yy.homi.thirdparty.interfaces.KeyGenerator;
import com.yy.homi.thirdparty.interfaces.AbstractCheckCodeGenerateStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;


//具体的策略，生成图片验证码策略
@Slf4j
@Component
public class PicCheckCodeGenerateStrategy extends AbstractCheckCodeGenerateStrategy {
    @Autowired
    private DefaultKaptcha kaptcha; //快捷生成验证码图片

    @Override
    public String getCheckCodeGenerateType() {
        return "pic";  //pic
    }


    //构造方法注入需要的 验证码生成器，key生成器，验证码存储方式
    @Autowired
    public PicCheckCodeGenerateStrategy(
            @Qualifier("numberLetterCheckCodeGenerator") CheckCodeGenerator checkCodeGenerator,
            @Qualifier("uuidKeyGenerator") KeyGenerator keyGenerator,
            @Qualifier("redisCheckCodeStore") CheckCodeStore checkCodeStore
    ) {
        this.checkCodeGenerator = checkCodeGenerator;
        this.keyGenerator = keyGenerator;
        this.checkCodeStore = checkCodeStore;
    }

    @Override
    public R generate(CheckCodeGenerateReqDTO checkCodeGenerateReqDTO) {
        //1.生成验证码存入Redis 有效期5分钟
        Map<String, String> result = generateAndStore(RedisConstants.THIRD.CHECK_CODE_PREFIX, 5, 300);//300s有效期
        String code = result.get("code");
        String uuid = result.get("uuid");
        if(StrUtil.isBlank(code) || StrUtil.isBlank(uuid)){
            throw new ServiceException("生成验证码异常");
        }
        //2.将验证码生成图片并返回url
        String picUrl = createPic(code);
        result.put("picUrl",picUrl);
        return R.ok(result);
    }

    private String createPic(String code) {
        // 生成图片验证码
        ByteArrayOutputStream outputStream = null;
        BufferedImage image = kaptcha.createImage(code);

        outputStream = new ByteArrayOutputStream();
        String imgBase64Encoder = null;
        try {
            // 对字节数组Base64编码
            BASE64Encoder base64Encoder = new BASE64Encoder();
            ImageIO.write(image, "png", outputStream);
            imgBase64Encoder = "data:image/png;base64," + EncryptUtil.encodeBase64(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return imgBase64Encoder;
    }

}
