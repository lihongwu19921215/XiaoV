/*
 * Copyright (c) 2012-2016, b3log.org & hacpai.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.xiaov;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import org.b3log.latke.Latkes;
import org.b3log.latke.ioc.LatkeBeanManager;
import org.b3log.latke.ioc.Lifecycle;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.servlet.AbstractServletListener;
import org.b3log.latke.util.Stopwatchs;
import org.b3log.latke.util.Strings;
import org.b3log.xiaov.service.QQService;

/**
 * XiaoV servlet listener.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, May 29, 2016
 * @since 1.0.0
 */
public final class XiaoVServletListener extends AbstractServletListener {

    /**
     * XiaoV version.
     */
    public static final String VERSION = "1.0.0";

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(XiaoVServletListener.class.getName());

    /**
     * JSONO print indent factor.
     */
    public static final int JSON_PRINT_INDENT_FACTOR = 4;

    /**
     * Bean manager.
     */
    private LatkeBeanManager beanManager;

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        Stopwatchs.start("Context Initialized");
        Latkes.setScanPath("org.b3log.xiaov");
        super.contextInitialized(servletContextEvent);

        beanManager = Lifecycle.getBeanManager();

        final QQService qqService = beanManager.getReference(QQService.class);
        qqService.initQQClient();

        LOGGER.info("Initialized the context");

        Stopwatchs.end();
        LOGGER.log(Level.DEBUG, "Stopwatch: {0}{1}", new Object[]{Strings.LINE_SEPARATOR, Stopwatchs.getTimingStat()});
        Stopwatchs.release();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        super.contextDestroyed(servletContextEvent);

        try {
            final QQService qqService = beanManager.getReference(QQService.class);
            qqService.closeQQClient();
        } catch (final Exception e) {
            // Ignore it
        }

        LOGGER.info("Destroyed the context");
    }

    @Override
    public void sessionCreated(final HttpSessionEvent httpSessionEvent) {
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent httpSessionEvent) {
        super.sessionDestroyed(httpSessionEvent);
    }

    @Override
    public void requestInitialized(final ServletRequestEvent servletRequestEvent) {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequestEvent.getServletRequest();

        Stopwatchs.start("Request initialized [" + httpServletRequest.getRequestURI() + "]");
    }

    @Override
    public void requestDestroyed(final ServletRequestEvent servletRequestEvent) {
        try {
            super.requestDestroyed(servletRequestEvent);
        } finally {
            Stopwatchs.release();
        }
    }
}
