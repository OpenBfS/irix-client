/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

/** Exception for mapfish-print errors. */
public class PrintException extends Exception {
    /** Exception when communicating with the print service.
     *
     * @param message the error message. Can be the servers repsonse.
     */
    public PrintException(String message) {
        super(message);
    }
};
