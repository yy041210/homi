package com.yy.homi.thirdparty.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Kaptcha图片验证码配置类
 * 用于自定义验证码生成的样式、大小、文本等属性
 **/
@Configuration
public class KaptchaConfig {

    //图片验证码生成器，使用开源的kaptcha
    @Bean
    public DefaultKaptcha producer() {
        Properties properties = new Properties();

        // --- 验证码外观配置 ---

        // 是否有边框，默认为 yes。这里设置为 no 表示不显示边框
        properties.put("kaptcha.border", "no");

        // 验证码文本字体颜色，设置为黑色
        properties.put("kaptcha.textproducer.font.color", "black");

        // 验证码字符间的间距，设置为 10 像素
        properties.put("kaptcha.textproducer.char.space", "10");

        // 验证码文本字符长度，这里设置为 5 位字符
        properties.put("kaptcha.textproducer.char.length","5");

        // 验证码图片的高度，单位为像素
        properties.put("kaptcha.image.height","34");

        // 验证码图片的宽度，单位为像素
        properties.put("kaptcha.image.width","138");

        // 验证码字体的字号大小，设置为 25 像素
        properties.put("kaptcha.textproducer.font.size","25");

        // --- 干扰处理 ---

        // 验证码噪点/干扰实现类。这里配置为 NoNoise（无干扰），
        // 这样生成的图片会比较干净，容易识别。如果需要防机器暴力破解建议开启干扰。
        properties.put("kaptcha.noise.impl","com.google.code.kaptcha.impl.NoNoise");

        // 将配置属性加载到 Kaptcha 的 Config 对象中
        Config config = new Config(properties);

        // 实例化 DefaultKaptcha 并装载配置
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        defaultKaptcha.setConfig(config);

        return defaultKaptcha;
    }
}