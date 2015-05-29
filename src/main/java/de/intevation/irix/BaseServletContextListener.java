/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class BaseServletContextListener implements ServletContextListener {

    public static final String LOG4J_PROPERTIES = "IRIXCLIENT_LOG4J_PROPERIES";

    public static final Logger log = Logger.getLogger(BaseServletContextListener.class);

    @Override
    public void  contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();

        this.initLogging(sc);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private void initLogging(ServletContext sc) {
        String log4jProperties = System.getenv(LOG4J_PROPERTIES);

        if (log4jProperties == null || log4jProperties.length() == 0) {
            String file = sc.getInitParameter("log4j-properties");

            if (file != null && file.length() > 0) {
                log4jProperties = sc.getRealPath(file);
            }
        }
        PropertyConfigurator.configure(log4jProperties);
        log.debug("Logging initalized");
    }
}
