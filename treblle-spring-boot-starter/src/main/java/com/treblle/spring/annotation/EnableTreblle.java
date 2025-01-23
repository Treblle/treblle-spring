package com.treblle.spring.annotation;

import com.treblle.spring.configuration.TreblleAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({TreblleAutoConfiguration.class})
public @interface EnableTreblle {

  Class<? extends Annotation> annotation() default Annotation.class;
}
