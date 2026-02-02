package com.yy.homi.file.filters;


import com.yy.homi.common.constant.SecurityConstants;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 注意：这里不再去找 "Authorization" 头，因为 JWT 已经在网关被拆解了。
        // 我们直接拿网关传给我们的“成品”。
        String userId = request.getHeader(SecurityConstants.USER_ID_HEADER);
        String authorities = request.getHeader(SecurityConstants.USER_ROLE_HEADER);
        // 如果这两个头存在，说明请求是从网关合法转发过来的
        if (userId != null && authorities != null) {

            // 将字符串权限（如 "ROLE_USER,ROLE_ADMIN"）转为 List<GrantedAuthority>
            List<GrantedAuthority> authorityList = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

            // 构造身份令牌
            // 参数1：Principal (当前用户是谁，存用户ID)
            // 参数2：Credentials (凭证，已验过，传 null)
            // 参数3：Authorities (权限列表)
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorityList
            );

            // 存入当前线程的 Security 上下文，
            // 之后你就可以在 Controller 中通过 SecurityContextHolder 拿用户信息了
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 继续执行后续过滤器（如：UsernamePasswordAuthenticationFilter 等）
        filterChain.doFilter(request, response);
    }
}