package com.yy.homi.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "homi.auth")
@RefreshScope
public class WhiteListProperties {
    private List<String> ignoreUrls = new ArrayList<>();
}
