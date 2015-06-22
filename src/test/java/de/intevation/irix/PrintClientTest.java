/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.test.irix;

import de.intevation.irix.PrintClient;
import de.intevation.irix.PrintException;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import org.junit.Test;
import org.junit.Before;

public class PrintClientTest {
    @Before
    public void setupLogging() {
        ConsoleAppender console = new ConsoleAppender(); //create appender
        String pattern = "[%p|%C{1}] %m%n";
        console.setLayout(new PatternLayout(pattern));
        console.setThreshold(Level.ERROR); // Change here for testing ;-)
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }

    @Test(expected=IOException.class, timeout=1500)
    public void testNoConnection() throws IOException, PrintException {
        // This is less trivial then it appears, HTTPClient changed
        // the way in which the timeout is configured 3 times since
        // Version 3.1. If the timeout is not set correctly the
        // connection will be stalled indefinetly.
        PrintClient.getReport("http://192.0.2.0/foo", "", 500);
    }
}
