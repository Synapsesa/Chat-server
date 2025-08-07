package com.synapse.chat_service.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private static final String HEADER_MEMBER_ID = "X-Authenticated-Member-Id";
    private static final String HEADER_MEMBER_ROLE = "X-Authenticated-Member-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String memberId = request.getHeader(HEADER_MEMBER_ID);
        String memberRole = request.getHeader(HEADER_MEMBER_ROLE);

        // 현재는 게이트웨이로부터 받은 데이터에 헤더가 존재할 경우 신뢰함
        if (memberId != null && !memberId.isBlank()) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (memberRole != null && !memberRole.isBlank()) {
                authorities = Arrays.stream(memberRole.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            UUID principal = UUID.fromString(memberId);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
