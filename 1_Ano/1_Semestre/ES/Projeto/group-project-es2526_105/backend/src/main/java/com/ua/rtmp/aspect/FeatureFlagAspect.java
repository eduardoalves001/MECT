package com.ua.rtmp.aspect;

import com.ua.rtmp.annotation.RequireFeatureFlag;
import com.ua.rtmp.exception.FeatureFlagException;
import com.ua.rtmp.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureFlagAspect {

    private final FeatureFlagService featureFlagService;

    @Around("@annotation(com.ua.rtmp.annotation.RequireFeatureFlag)")
    public Object checkFeatureFlag(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireFeatureFlag annotation = method.getAnnotation(RequireFeatureFlag.class);
        
        String featureFlagName = annotation.value();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();
        
        log.debug("Checking feature flag '{}' for method {}.{}", featureFlagName, className, methodName);
        
        boolean isEnabled = featureFlagService.isFeatureEnabled(featureFlagName);
        
        if (!isEnabled) {
            log.warn("Feature flag '{}' is disabled - blocking method {}.{}", featureFlagName, className, methodName);
            String errorMessage = annotation.message().isEmpty() 
                ? String.format("Feature '%s' is currently disabled", featureFlagName)
                : annotation.message();
            throw new FeatureFlagException(featureFlagName, errorMessage);
        }
        
        log.debug("Feature flag '{}' is enabled - proceeding with method {}.{}", featureFlagName, className, methodName);
        return joinPoint.proceed();
    }
}
