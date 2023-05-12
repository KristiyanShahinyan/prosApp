package digital.paynetics.phos.dagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;
import javax.inject.Scope;

@Scope
@Retention(RetentionPolicy.SOURCE)
public @interface PhosScope {
}
