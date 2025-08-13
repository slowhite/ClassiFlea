package com.example.ClassiFlea.security;

import com.example.ClassiFlea.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository repo) {
    return username -> repo.findByEmail(username)
        .map(u -> org.springframework.security.core.userdetails.User
            .withUsername(u.getEmail())
            .password(u.getPasswordHash())
            .roles(u.getRole().replace("ROLE_", "")) // e.g. ROLE_USER -> USER
            .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Bean
  DaoAuthenticationProvider authProvider(UserDetailsService uds, PasswordEncoder encoder) {
    var p = new DaoAuthenticationProvider();
    p.setUserDetailsService(uds);
    p.setPasswordEncoder(encoder);
    return p;
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      // 演示环境先关 CSRF；之后要开的话需要前端携带 CSRF token
      .csrf(csrf -> csrf.disable())
      // 允许 H2 控制台在 iframe 中打开
      .headers(h -> h.frameOptions(f -> f.sameOrigin()))
      .authorizeHttpRequests(auth -> auth
        // 静态资源 & 登录/退出
        .requestMatchers("/adminlte/**", "/uploads/**", "/doLogin", "/logout").permitAll()
        // 仅开发期放开 H2；上线务必移除
        .requestMatchers("/h2/**").permitAll()

        // 放行验证码发送 & 注册（匿名可访问）
        .requestMatchers("/account/verification-code", "/account/register", "/register").permitAll()
        .requestMatchers(HttpMethod.POST, "/account/verification-code", "/account/register", "/register").permitAll()

        // 匿名只读
        .requestMatchers(HttpMethod.GET, "/listings/**").permitAll()

        // 私有：必须登录
        .requestMatchers("/my/**").authenticated()
        .requestMatchers("/orders/**").authenticated()
        .requestMatchers("/profile").authenticated() // 可选：/profile 需要登录

        // 写操作必须登录
        .requestMatchers(HttpMethod.POST,   "/listings/**", "/upload/**").authenticated()
        .requestMatchers(HttpMethod.PUT,    "/listings/**").authenticated()
        .requestMatchers(HttpMethod.PATCH,  "/my/**", "/listings/**").authenticated()
        .requestMatchers(HttpMethod.DELETE, "/listings/**", "/orders/**").authenticated()

        .anyRequest().authenticated()
      )
      .formLogin(f -> f
        .loginPage("/adminlte/login.html")
        .loginProcessingUrl("/doLogin")
        .defaultSuccessUrl("/adminlte/market.html", true)
        .failureUrl("/adminlte/login.html?error")
        .permitAll()
      )
      .logout(l -> l
        .logoutUrl("/logout")
        .logoutSuccessUrl("/adminlte/login.html?logout")
        .permitAll()
      );

    return http.build();
  }
}
