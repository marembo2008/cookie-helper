package com.anosym.cookie.annotation.processor;

import com.anosym.cookie.annotation.Cookie;
import com.anosym.cookie.annotation.Domain;
import com.anosym.cookie.annotation.HttpOnly;
import com.anosym.cookie.annotation.MaxAge;
import com.anosym.cookie.annotation.Name;
import com.anosym.cookie.annotation.Path;
import com.anosym.cookie.annotation.Secure;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author mochieng
 */
@SupportedAnnotationTypes({"com.anosym.cookie.annotation.Cookie"})
public class CookieValidationProcessor extends AbstractProcessor {

    private static final Logger LOG = Logger.getLogger(CookieValidationProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        LOG.info("CookieValidationProcessor: initialized");

        for (Element el : roundEnv.getElementsAnnotatedWith(Cookie.class)) {
            if (el.getKind() == ElementKind.CLASS) {
                Cookie cookie = el.getAnnotation(Cookie.class);
                TypeElement cookieClass = (TypeElement) el;

                LOG.log(Level.INFO, "TypeElement: {0}", cookieClass);

                if (cookie.encoded()) {
                    //check if the fields define any kind of cookie-annotations. Then abort.
                    for (Element ve : cookieClass.getEnclosedElements()) {
                        if (ve.getAnnotation(Domain.class) != null
                                || ve.getAnnotation(MaxAge.class) != null
                                || ve.getAnnotation(HttpOnly.class) != null
                                || ve.getAnnotation(Name.class) != null
                                || ve.getAnnotation(Path.class) != null
                                || ve.getAnnotation(Secure.class) != null) {
                            throw new IllegalArgumentException("Invalid variable annotations for an encoded cookie: " + ve.getSimpleName());
                        }
                    }
                    //check for method and class annotations, only one property can be annotated.
                    boolean isField = false;
                    boolean isMethod = false;
                    Element fieldEl = null;
                    Element methodEl = null;
                    for (Element ve : cookieClass.getEnclosedElements()) {
                        if (ve.getAnnotation(Domain.class) != null
                                || ve.getAnnotation(MaxAge.class) != null
                                || ve.getAnnotation(HttpOnly.class) != null
                                || ve.getAnnotation(Name.class) != null
                                || ve.getAnnotation(Path.class) != null
                                || ve.getAnnotation(Secure.class) != null) {
                            if (ve.getKind() == ElementKind.FIELD) {
                                isField = true;
                                fieldEl = el;
                            } else if (ve.getKind() == ElementKind.METHOD) {
                                isMethod = true;
                                methodEl = el;
                            }
                            if (isField && isMethod) {
                                throw new IllegalArgumentException(
                                        "Invalid variable annotations for cookie. Both field and method annotated=" + methodEl + ", " + fieldEl);
                            }
                        }
                    }
                }
                return true;
            }
            throw new IllegalArgumentException(el.getSimpleName() + " is not applicable as  Cookie");
        }
        return false;
    }

}
