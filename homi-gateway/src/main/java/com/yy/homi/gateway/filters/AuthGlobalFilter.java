package com.yy.homi.gateway.filters;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.AntPathMatcher;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.alibaba.fastjson.JSON;
import com.yy.homi.common.constant.SecurityConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.gateway.config.WhiteListProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(-100) //让他最先加载到过滤器链中
public class AuthGlobalFilter implements WebFilter {
    @Autowired
    private WhiteListProperties whiteListProperties;
    private static final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        //1.获取请求路径
        String path = exchange.getRequest().getURI().getPath();

        //2.白名单放行
        List<String> ignoreUrls = whiteListProperties.getIgnoreUrls();
        boolean isIgnore = ignoreUrls.stream().anyMatch(pattern -> matcher.match(pattern, path));
        if (isIgnore) {
            return chain.filter(exchange);
        }

        //3.不放行。校验jwt
        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTH_HEADER);
        if (StrUtil.isBlank(authorizationHeader) || !authorizationHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return onError(exchange, "未登录或凭证缺失");
        }
        String token = authorizationHeader.replace(SecurityConstants.TOKEN_PREFIX, "");
        boolean verify = JWTUtil.verify(token, SecurityConstants.JWT_SINGING_SECRET.getBytes());
        //3.1校验失败
        if (!verify) {
            //未登录，返回401
            return onError(exchange, "未登录或者token失效");
        }

        //3.2校验成功解析jwt
        JWT jwt = JWTUtil.parseToken(token);
        JSONObject payloads = jwt.getPayloads();
        String userId = payloads.getStr("userId");
        String userName = payloads.getStr("userName");
        List<String> authorities = payloads.getBeanList("authorities", String.class);
        String authStr = CollUtil.join(authorities, ",");

        //4.将用户信息通过Header传到下游服务器,
        // 注意：Gateway 的 Request 是不可变的，需要 mutate (变异) 产生一个新请求
        ServerHttpRequest mutateRquest = exchange.getRequest().mutate()
                .header(SecurityConstants.USER_ID_HEADER, userId)
                .header(SecurityConstants.USER_ROLE_HEADER, authStr)
                .build();
        return chain.filter(exchange.mutate().request(mutateRquest).build());
    }

    // 统一的错误响应处理
    private Mono<Void> onError(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED); // 设置http状态吗
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON); // 设置响应头内容类型为 JSON
        R r = R.fail(HttpStatus.UNAUTHORIZED.value(),msg);  //项目返回的R对象数据
        DataBuffer buffer = response.bufferFactory().wrap(JSON.toJSONString(r).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

}
