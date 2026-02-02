package com.yy.homi.rbac.test;

import com.yy.homi.common.utils.Base64Utils;
import org.junit.jupiter.api.Test;


public class Base64UtilTest {
    @Test
    public void testImageToBase64(){
        String s = Base64Utils.imageToBase64("static/images/logo.png");
        System.out.println(s);
    }
}
