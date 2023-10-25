/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Listener class for the IRIX-Client to be configured in web.xml.
 *
 */
public class BaseServletContextListener implements ServletContextListener {

    private static Logger log =
        Logger.getLogger(BaseServletContextListener.class);

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
        String file = sc.getInitParameter("log4j-properties");

        if (file != null && file.length() > 0) {
            String log4jProperties = sc.getRealPath(file);
            PropertyConfigurator.configure(log4jProperties);
            log.debug("Logging initalized");
        }
    }
}
