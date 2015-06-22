package de.intevation.irix;

public class PrintException extends Exception
{
    /** Exception when communicating with the print service.
     *
     * @param message the error message. Can be the servers repsonse.
     */
    public PrintException (String message) {
        super(message);
    }
};
