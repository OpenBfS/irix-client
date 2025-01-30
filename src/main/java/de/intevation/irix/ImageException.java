/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */

package de.intevation.irix;

/** Exception for external image errors. */
public class ImageException extends Exception {
    /** Exception when trying to get external images.
     *
     * @param message the error message. Can be the servers repsonse.
     */
    public ImageException(String message) {
        super(message);
    }
};
