package com.anosym.cookie.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import org.atteo.classindex.IndexAnnotated;

/**
 * If found on a pojo, signifies that the pojo is a cookie.
 *
 * The pojo must have a default constructor.
 *
 * This version only accepts primitive types (and the primitive wrappers), String.class, Calendar.class and
 * BigDecimal.class
 *
 * On a CDI environment, to get an instance of the cookie, simply do the following:
 *
 * <pre>
 *  <code>
 * @Inject
 * @Cookie private MyPojoCookie myCookie;
 * </code>
 * </pre>
 *
 * @author mochieng
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@IndexAnnotated
@SessionScoped
public @interface Cookie {

    /**
     * If true, all cookie variables with annotation will result into {@link IllegalArgumentException} on application
     * startup. The entire cookie is encoded into Base64 and the simple name of the pojo used as the cookie-name.
     *
     * If this cookie is encoded, new attributes must be added alphabetically, otherwise unspecified behaviour would
     * result.
     *
     * @return
     */
    @Nonbinding
    boolean encoded() default false;

    /**
     * The maximum age of the cookie in seconds. Default is session.
     *
     * @return
     */
    @Nonbinding
    int maxAge() default -1;

    /**
     * If the cookie is intended only for secure connections.
     *
     * @return
     */
    @Nonbinding
    boolean secure() default false;

    /**
     * The domain for the cookie. If not specified, current hosts only.
     *
     * @return
     */
    @Nonbinding
    String domain() default "";

    /**
     * The path of the cookie. If not specified, defaults to the path which first set the cookie.
     *
     * @return
     */
    @Nonbinding
    String path() default "";

    /**
     * If the cookie is meant for http only, or ajax and such are allowed.
     *
     * @return
     */
    @Nonbinding
    boolean httpOnly() default false;

}
