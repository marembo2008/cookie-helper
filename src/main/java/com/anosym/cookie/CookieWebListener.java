package com.anosym.cookie;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 * @author mochieng
 */
@WebListener
@SessionScoped
public class CookieWebListener implements ServletRequestListener, Serializable, HttpSessionListener {

    private static final Logger LOG = Logger.getLogger(CookieWebListener.class.getName());

    private static final long serialVersionUID = -12424424242l;
    @Inject
    private CookieService cookieService;

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        LOG.info("Called Request destroyed");
        cookieService.updateHttpCookie();
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        LOG.info("Called request initialized...");
        cookieService.updateObjectCookie();
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        LOG.info("Session created........");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        LOG.info("Session destroyed........");
    }

}
