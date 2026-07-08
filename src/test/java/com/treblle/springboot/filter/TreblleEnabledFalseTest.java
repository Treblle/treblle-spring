package com.treblle.springboot.filter;

import com.treblle.springboot.transport.TreblleClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * With {@code treblle.enabled=false}, the auto-configuration backs off entirely and nothing is sent.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "treblle.enabled=false",
                "treblle.sdk-token=test-token",
                "treblle.api-key=test-key"
        })
class TreblleEnabledFalseTest {

    // Provide a mock so verification works even though auto-config should not create the real bean.
    @MockBean
    private TreblleClient client;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ApplicationContext ctx;

    @Test
    void nothingSentWhenDisabled() throws Exception {
        rest.getForEntity("/api/users", String.class);
        TimeUnit.MILLISECONDS.sleep(400);
        verify(client, never()).send(any());
        // The Treblle filter registration must not be active.
        assert ctx.getBeanNamesForType(
                org.springframework.boot.web.servlet.FilterRegistrationBean.class).length == 0
                || java.util.Arrays.stream(ctx.getBeanDefinitionNames())
                    .noneMatch(n -> n.toLowerCase().contains("trebllefilter"));
    }
}
