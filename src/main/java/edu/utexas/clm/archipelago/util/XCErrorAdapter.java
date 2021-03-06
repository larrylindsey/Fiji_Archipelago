/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.util;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.data.ClusterMessage;
import edu.utexas.clm.archipelago.listen.TransceiverExceptionListener;
import edu.utexas.clm.archipelago.network.MessageXC;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class XCErrorAdapter implements TransceiverExceptionListener
{

    private final HashSet<Class> throwablesSeenTX;
    private final HashSet<Class> throwablesSeenRX;

    private final AtomicBoolean isQuiet;
    
    public XCErrorAdapter()
    {
        throwablesSeenRX = new HashSet<Class>();
        throwablesSeenTX = new HashSet<Class>();
        isQuiet = new AtomicBoolean(false);
    }
    
    protected boolean handleCustomRX(final Throwable t, final MessageXC mxc,
                                     final ClusterMessage message)
    {
        return true;
    }

    protected boolean handleCustomTX(final Throwable t, final MessageXC mxc,
                                     final ClusterMessage message)
    {
        return true;
    }

    protected boolean handleCustom(final Throwable t, final MessageXC mxc,
                                   final ClusterMessage message)
    {
        return true;
    }

    public void report(final Throwable t, final String message,
                       final HashSet<Class> throwablesSeen)
    {
        FijiArchipelago.log(message);
        if (!throwablesSeen.contains(t.getClass()))
        {
            if (!isQuiet.get())
            {
                FijiArchipelago.err(message + "\nThis error dialog will only be shown once.");
            }
            throwablesSeen.add(t.getClass());
        }
    }
    
    protected void reportRX(final Throwable t, final String message, final MessageXC mxc)
    {
        report(t, "RX: " + mxc.getHostname() + ": " + message, throwablesSeenRX);
    }

    protected void reportTX(final Throwable t, final String message, final MessageXC mxc)
    {

        report(t, "TX: " + mxc.getHostname() + ": "  + message, throwablesSeenTX);
    }

    public void handleRXThrowable(final Throwable t, final MessageXC mxc,
                                  final ClusterMessage message) {

/*
        System.out.println("RX: Throwable " + t);
        for (StackTraceElement ste : t.getStackTrace())
        {
            System.out.println(ste);
        }
*/
        if (handleCustom(t, mxc, message) && handleCustomRX(t, mxc, message))
        {
            reportRX(t, t.toString(), mxc);
        }
    }

    public void handleTXThrowable(final Throwable t, final MessageXC mxc,
                                  final ClusterMessage message) {
/*
        System.out.println("TX: Throwable " + t);
        for (StackTraceElement ste : t.getStackTrace())
        {
            System.out.println(ste);
        }
*/
        if (handleCustom(t, mxc, message) && handleCustomTX(t, mxc, message))
        {
            reportTX(t, t.toString(), mxc);
        }
    }
    
    public void silence()
    {
        silence(true);
    }
    
    public void silence(boolean s)
    {
        isQuiet.set(s);
    }
}
