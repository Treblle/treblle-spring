package com.treblle.spring.annotation;

import com.treblle.spring.configuration.TreblleAutoConfiguration;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({TreblleAutoConfiguration.class})
public @interface EnableTreblle {

  Class<? extends Annotation> annotation() default Annotation.class;
}
