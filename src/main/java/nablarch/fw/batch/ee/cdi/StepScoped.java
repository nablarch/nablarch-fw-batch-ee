package nablarch.fw.batch.ee.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.enterprise.context.NormalScope;

/**
 * ステップ単位での値の共有を表すアノテーション。
 *
 * @author Naoki Yamamoto
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@NormalScope
@Inherited
public @interface StepScoped {
}
