package com.my.blog.project.myblogonboot.myblog.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MyBlogTestConfiguration.class)
public abstract class AbstractIntegrationTest {

}

