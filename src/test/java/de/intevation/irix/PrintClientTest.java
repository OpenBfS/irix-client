/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.test.irix;

import de.intevation.irix.PrintClient;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

public class PrintClientTest
{
    @Before
    public void setupLogging() {
        ConsoleAppender console = new ConsoleAppender(); //create appender
        String PATTERN = "[%p|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.ERROR); // Change here for testing ;-)
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }

    @Test(expected=IOException.class)
    public void testNoConnection() throws IOException {
        PrintClient.getReport("http://192.0.2.0/foo", "");
    }
}
