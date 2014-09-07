package com.anosym.cookie.impl;

import com.anosym.cookie.CookieService;
import com.anosym.cookie.annotation.Cookie;
import com.anosym.cookie.annotation.Domain;
import com.anosym.cookie.annotation.HttpOnly;
import com.anosym.cookie.annotation.MaxAge;
import com.anosym.cookie.annotation.Name;
import com.anosym.cookie.annotation.Path;
import com.anosym.cookie.annotation.Secure;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author mochieng
 */
@SessionScoped
public class CookieServiceImpl implements Serializable, CookieService {

    private static final Logger LOG = Logger.getLogger(CookieServiceImpl.class.getName());
    private static final long serialVersionUID = -143948934399l;
    private static final String ENCODED_SEPARATOR = ";";
    private static final String INDEX_SEPARATOR = "=";

    private static final class CookieValue implements Serializable {

        private static final long serialVersionUID = -1347834738l;
        //The field name, if this is not encoded, otherwise the simple class name.
        private final String name;
        private final boolean encoded;
        //The actual object
        private final Object cookieInstance;
        private final javax.servlet.http.Cookie cookie;
        private final Map<Integer, Field> cookieFieldsByIndex;
        private final Map<String, Field> cookieFieldsByName;

        public CookieValue(String name, boolean encoded, Object cookieInstance, javax.servlet.http.Cookie cookie) {
            this.name = name;
            this.encoded = encoded;
            this.cookieInstance = cookieInstance;
            this.cookie = cookie;
            this.cookieFieldsByIndex = new HashMap<>();
            cookieFieldsByName = new HashMap<>();
            final Field[] fields = cookieInstance.getClass().getDeclaredFields();
            Arrays.sort(fields, new Comparator<Field>() {

                @Override
                public int compare(Field o1, Field o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (int index = 0; index < fields.length; index++) {
                final Field f = fields[index];
                cookieFieldsByIndex.put(index, f);
                cookieFieldsByName.put(f.getName(), f);
            }
        }

    }
    @Inject
    private Instance<HttpServletResponse> servletResponse;
    @Inject
    private Instance<HttpServletRequest> servletRequest;
    @Inject
    @Cookie
    @SessionScoped
    private Instance<Object> cookies;

    private Map<String, CookieValue> cookieMap;

    @PostConstruct
    void initalizeCookieMap() {
        cookieMap = new HashMap<>();
        setCookieMap();
    }

    @Override
    public void updateHttpCookie() {
        setCookieMap();
        if (!servletResponse.isUnsatisfied() && !servletResponse.isAmbiguous()) {
            HttpServletResponse hsr = servletResponse.get();
            for (CookieValue cv : cookieMap.values()) {
                hsr.addCookie(cv.cookie);
            }
        } else {
            LOG.severe("Unable to retrieve current ServletResponse!");
        }
    }

    @Override
    public void updateObjectCookie() {
        for (javax.servlet.http.Cookie cookie : servletRequest.get().getCookies()) {
            final String name = cookie.getName();
            final CookieValue cookieValue = cookieMap.get(name);
            if (cookieValue != null) {
                cookieValue.cookie.setValue(cookie.getValue());
                updateObjectCookie(cookieValue);
            }
        }
    }

    private void updateObjectCookie(CookieValue cookieValue) {
        if (cookieValue.encoded) {
            updateEncodedObjectCookie(cookieValue);
        } else {
            updateIndividualObjectCookie(cookieValue);
        }
    }

    private void updateEncodedObjectCookie(CookieValue cookieValue) {
        //decode the cookie value from base64.
        final javax.servlet.http.Cookie cookie = cookieValue.cookie;
        final String value = cookie.getValue();
        final String[] data = value.split(ENCODED_SEPARATOR);
        for (String val : data) {
            final byte[] byteVal = BaseEncoding.base64().decode(val);
            final String fValue = new String(byteVal);
            final String fieldValuePair[] = fValue.split(INDEX_SEPARATOR);
            final int index = Integer.parseInt(fieldValuePair[0]);
            final String indexValue = fieldValuePair[0];
            final Field field = cookieValue.cookieFieldsByIndex.get(index);
            updateField(field, indexValue, cookieValue.cookieInstance);
        }
    }

    private void updateIndividualObjectCookie(CookieValue cookieValue) {
        final String name = cookieValue.name;
        final Field field = cookieValue.cookieFieldsByName.get(name);
        final String value = cookieValue.cookie.getValue();
        updateField(field, value, cookieValue.cookieInstance);
    }

    private void updateField(final Field field, final String value, final Object cookieObject) {
        field.setAccessible(true);

        final Class<?> type = field.getType();
        //default string
        try {
            if (String.class.isAssignableFrom(type)) {
                field.set(cookieObject, value);
            } else if (BigDecimal.class.isAssignableFrom(type)) {
                field.set(cookieObject, new BigDecimal(value));
            } else if (Calendar.class.isAssignableFrom(type)) {
                final long millis = Long.parseLong(value);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(millis);
                field.set(cookieObject, cal);
            } else {
                setPrimitiveOrPrimitiveWrappers(field, value, cookieObject);
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Error setting cookie values", ex);
        }
    }

    public void setPrimitiveOrPrimitiveWrappers(final Field field, final String value, final Object cookieObject) throws
            IllegalArgumentException, IllegalAccessException {

        final Class<?> type = field.getType();
        Object val = null;
        if (byte.class.isAssignableFrom(type)
                || Byte.class.isAssignableFrom(type)) {
            val = Byte.parseByte(value);
        } else if (short.class.isAssignableFrom(type)
                || Short.class.isAssignableFrom(type)) {
            val = Short.parseShort(value);
        } else if (int.class.isAssignableFrom(type)
                || Integer.class.isAssignableFrom(type)) {
            val = Integer.parseInt(value);
        } else if (long.class.isAssignableFrom(type)
                || Long.class.isAssignableFrom(type)) {
            val = Long.parseLong(value);
        } else if (float.class.isAssignableFrom(type)
                || Float.class.isAssignableFrom(type)) {
            val = Float.parseFloat(value);
        } else if (double.class.isAssignableFrom(type)
                || Double.class.isAssignableFrom(type)) {
            val = Double.parseDouble(value);
        } else if (char.class.isAssignableFrom(type)
                || Character.class.isAssignableFrom(type)) {
            val = !Strings.isNullOrEmpty(value) ? value.trim().charAt(0) : ' ';
        } else if (boolean.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)) {
            val = Boolean.valueOf(value);
        } else {
            throw new IllegalArgumentException("Unsupported cookie variable type: " + type);
        }
        field.set(cookieObject, val);
    }

    private void setCookieMap() {
        for (Object obj : cookies) {
            //this must be a cookie, otherwise it wont be injected.
            final Cookie cookie = obj.getClass().getAnnotation(Cookie.class); //guaranteed not to be null by CDI
            try {
                if (cookie.encoded()) {
                    setEncodedHttpCookie(obj);
                } else {
                    setIndividualHttpFieldCookies(obj);
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException("Failed to intialize cookie: " + obj.getClass());
            }
        }
    }

    private void setIndividualHttpFieldCookies(@Nonnull final Object cookieObj) throws IllegalArgumentException,
            IllegalAccessException {
        final Field[] fields = cookieObj.getClass().getDeclaredFields();
        Arrays.sort(fields, new Comparator<Field>() {

            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        final Cookie cookie = cookieObj.getClass().getAnnotation(Cookie.class);
        for (Field field : fields) {
            field.setAccessible(true);
            final Name cookieName = field.getAnnotation(Name.class);
            final String name = cookieName != null ? cookieName.value() : field.getName();
            final Object fieldValue = field.get(cookieObj);
            final String value = toString(fieldValue);
            javax.servlet.http.Cookie httpCookie = new javax.servlet.http.Cookie(name, value);
            final MaxAge maxAge = field.getAnnotation(MaxAge.class);
            final Path path = field.getAnnotation(Path.class);
            final Domain domain = field.getAnnotation(Domain.class);
            final boolean secure = field.isAnnotationPresent(Secure.class) || cookie.secure();
            final boolean httpOnly = field.isAnnotationPresent(HttpOnly.class) || cookie.httpOnly();
            final int maxAgeValue = maxAge != null ? maxAge.value() : cookie.maxAge();
            final String pathValue = path != null ? path.value() : cookie.path();
            final String domainValue = domain != null
                    ? domain.value() : (!Strings.isNullOrEmpty(cookie.domain())
                    ? cookie.domain() : servletRequest.get().getHeader("host"));
            httpCookie.setDomain(domainValue);
            httpCookie.setHttpOnly(httpOnly);
            httpCookie.setMaxAge(maxAgeValue);
            httpCookie.setPath(pathValue);
            httpCookie.setSecure(secure);
            cookieMap.put(name, new CookieValue(name, false, cookieObj, httpCookie));
        }
    }

    private void setEncodedHttpCookie(final Object cookieObj) throws IllegalArgumentException, IllegalAccessException {
        final Field[] fields = cookieObj.getClass().getDeclaredFields();
        Arrays.sort(fields, new Comparator<Field>() {

            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        final StringBuilder data = new StringBuilder();
        for (int index = 0; index < fields.length; index++) {
            final Field f = fields[index];
            f.setAccessible(true);
            final String value = toString(f.get(cookieObj));
            if (index > 0) {
                data.append(ENCODED_SEPARATOR);
            }
            final StringBuilder sb = new StringBuilder(index).append(INDEX_SEPARATOR).append(value);
            data.append(BaseEncoding.base64().encode(sb.toString().getBytes()));
        }
        final String name = cookieObj.getClass().getSimpleName();
        final String value = data.toString();
        Cookie cookie = cookieObj.getClass().getAnnotation(Cookie.class);
        javax.servlet.http.Cookie httpCookie = new javax.servlet.http.Cookie(cookieObj.getClass().getName(), value);
        String domain = cookie.domain();
        if (Strings.isNullOrEmpty(domain)) {
            domain = servletRequest.get().getHeader("host");
        }
        httpCookie.setDomain(domain);
        httpCookie.setHttpOnly(cookie.httpOnly());
        httpCookie.setMaxAge(cookie.maxAge());
        httpCookie.setPath(cookie.path());
        httpCookie.setSecure(cookie.secure());
        cookieMap.put(name, new CookieValue(name, true, cookieObj, httpCookie));
    }

    private String toString(final Object obj) {
        if (obj instanceof Calendar) {
            Calendar cal = (Calendar) obj;
            return String.valueOf(cal.getTimeInMillis());
        }
        return String.valueOf(obj);
    }

}
