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

import org.junit.Test;
import org.junit.Before;

public class PrintClientTest {
    static final int TEST_TIMEOUT = 1500;
    static final int CONN_TIMEOUT = 500;

    @Before
    public void setupLogging() {
    }

    @Test(expected = IOException.class, timeout = TEST_TIMEOUT)
    public void testNoConnection() throws IOException, PrintException {
        // This is less trivial then it appears, HTTPClient changed
        // the way in which the timeout is configured 3 times since
        // Version 3.1. If the timeout is not set correctly the
        // connection will be stalled indefinetly.
        PrintClient.getReport("http://192.0.2.0/foo", "", CONN_TIMEOUT);
    }
}
