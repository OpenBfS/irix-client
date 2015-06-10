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

import org.junit.Test;
import org.junit.Assert;

public class PrintClientTest
{
    @Test(expected=IOException.class)
    public void testNoConnection() throws IOException {
        PrintClient.getReport("http://192.0.2.0/foo", "");
    }
}
