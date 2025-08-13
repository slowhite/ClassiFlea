package com.example.ClassiFlea;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 浏览器访问 /adminlte/uploads/xxx.png
    // 实际读取 程序当前运行目录下的 ./uploads/xxx.png
    registry.addResourceHandler("/adminlte/uploads/**")
            .addResourceLocations("file:./uploads/");
  }
}
