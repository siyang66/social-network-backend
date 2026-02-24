package com.meet5.social.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * Routes to slave on readOnly transactions, master otherwise.
 * Order(1) ensures this runs before the transaction advisor.
 */
@Aspect
@Order(1)
@Component
public class DataSourceRoutingAspect {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || " +
            "@within(org.springframework.transaction.annotation.Transactional)")
    public Object route(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Transactional tx = method.getAnnotation(Transactional.class);
        if (tx == null) {
            tx = pjp.getTarget().getClass().getAnnotation(Transactional.class);
        }

        boolean readOnly = tx != null && tx.readOnly();
        if (readOnly) {
            DataSourceContext.setSlave();
        } else {
            DataSourceContext.setMaster();
        }

        try {
            return pjp.proceed();
        } finally {
            DataSourceContext.clear();
        }
    }
}
