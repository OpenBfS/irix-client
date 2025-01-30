/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */

package de.intevation.irix;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import static java.lang.System.Logger.Level.DEBUG;

/**
 * Listener class for the IRIX-Client to be configured in web.xml.
 *
 */
public class BaseServletContextListener implements ServletContextListener {

    private static System.Logger log =
        System.getLogger(BaseServletContextListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void  contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();

        this.initLogging(sc);
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private void initLogging(ServletContext sc) {
        log.log(DEBUG, "Logging initalized");
    }
}
