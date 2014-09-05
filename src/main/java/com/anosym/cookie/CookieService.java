package com.anosym.cookie;

/**
 *
 * @author mochieng
 */
public interface CookieService {

    /**
     * Updates http cookie before request is complete.
     */
    void updateHttpCookie();

    /**
     * Updates object cookie from http cookie(s) when request is initialized.
     */
    void updateObjectCookie();
}
