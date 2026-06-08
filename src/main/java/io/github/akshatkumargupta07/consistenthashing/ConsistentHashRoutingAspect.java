package io.github.akshatkumargupta07.consistenthashing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.annotation.Annotation;

@Aspect
public class ConsistentHashRoutingAspect {

    private final ConsistentHashRing ring;
    // reused across calls — SpelExpressionParser is thread-safe
    private final ExpressionParser parser = new SpelExpressionParser();

    public ConsistentHashRoutingAspect(ConsistentHashRing ring){
        this.ring = ring;
    }

    // intercepts every method annotated with @ConsistentHashRouted
    @Around("@annotation(consistentHashRouted)")
    public Object route(ProceedingJoinPoint joinPoint, ConsistentHashRouted consistentHashRouted) throws Throwable{
        String spelExpression = consistentHashRouted.key();

        // signature = method metadata (param names), joinPoint = this specific call (param values)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] params = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // bind param names to their runtime values so SpEL can resolve #userId → "user-99"
        StandardEvaluationContext context = new StandardEvaluationContext();
        for(int i=0 ; i< params.length; i++){
            context.setVariable(params[i] , args[i]);
        }

        String key = parser.parseExpression(spelExpression).getValue(context, String.class);
        Node node = ring.route(key);

        // find the @RoutedNode parameter and inject the resolved node before proceeding
        Annotation[][] paramAnnotations = signature.getMethod().getParameterAnnotations();
        Object[] modifiedArgs = args.clone();

        for(int i=0; i<paramAnnotations.length; i++){
            for(Annotation annotation: paramAnnotations[i]){
                if(annotation instanceof RoutedNode){
                    modifiedArgs[i] = node;
                    break;
                }
            }
        }

        return joinPoint.proceed(modifiedArgs);
    }
}
