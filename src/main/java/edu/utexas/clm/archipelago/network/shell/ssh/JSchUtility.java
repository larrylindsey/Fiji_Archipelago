package edu.utexas.clm.archipelago.network.shell.ssh;

import com.jcraft.jsch.*;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.NodeShellListener;
import edu.utexas.clm.archipelago.network.node.NodeParameters;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


public class JSchUtility extends Thread
{
    private final NodeShellListener listener;
    private final Session session;
    private final Channel channel;
    private final long node;
    
    public JSchUtility(final NodeParameters param,
                       final NodeShellListener listener,
                       final String command)
            throws ShellExecutionException, JSchException
    {
        FijiArchipelago.debug("Creating JSchUtility to run " + command + " on " + param.getHost());
        try
        {
            final JSch jsch = new JSch();
            final UserInfo ui = new NodeShellUserInfo();
            final int port = param.getShellParams().getInteger("ssh-port");
            final String keyfile = param.getShellParams().getString("keyfile");

            this.listener = listener;

            node = param.getID();

            jsch.addIdentity(new File(keyfile).getAbsolutePath());

            session = jsch.getSession(param.getUser(), param.getHost(), port);

            session.setUserInfo(ui);

            FijiArchipelago.debug("Connecting session for " + param.getHost());

            session.connect();

            FijiArchipelago.debug("Opening a channel on " + param.getHost());

            channel = session.openChannel("exec");

            FijiArchipelago.debug("Channel opened on " + param.getHost());

            ((ChannelExec)channel).setCommand(command);
            ((ChannelExec)channel).setErrStream(System.err);
        }
        catch (JSchException jse)
        {
            throw jse;
        }
        catch (Exception e)
        {
            FijiArchipelago.debug("Ran into a problem while logging into  " + param.getHost() +
                ": " + e);
            Thread.dumpStack();
            throw new ShellExecutionException(e);
        }
    }
    
    public Channel getChannel()
    {
        return channel;
    }

    public void run()
    {
        String host = "";
        try
        {
            host = channel.getSession().getHost();

            FijiArchipelago.debug(host + ": connecting channel");
            channel.connect();

            FijiArchipelago.debug(host + ": connect() returned");

            while (channel.isConnected())
            {
                Thread.sleep(1000);
            }

            FijiArchipelago.debug(host + ": channel disconnected");

            listener.execFinished(node, null, channel.getExitStatus());
        }
        catch (JSchException jse)
        {
            FijiArchipelago.debug(host + ": ", jse);
            listener.execFinished(node, jse, -1);
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.debug(host + ": Interrupted");
            listener.execFinished(node, ie, -1);
        }

        channel.disconnect();
        session.disconnect();
    }
    
    public static boolean verifyParameters(final NodeParameters param,
                                           final String file)
            throws ShellExecutionException, JSchException
    {
        final AtomicInteger result = new AtomicInteger(-1);
        final ReentrantLock lock = new ReentrantLock();
        final Thread t = Thread.currentThread();

        FijiArchipelago.debug("JSchUtility: testing for existence of " + file + " on " +
                param.getHost());

        final NodeShellListener existListener = new NodeShellListener() {
            public void execFinished(final long nodeID, final Exception e, final int status)
            {
                lock.lock();
                result.set(status);
                t.interrupt();
                lock.unlock();
            }

            public void ioStreamsReady(InputStream is, OutputStream os) {}
        };

        try
        {
            JSchUtility jUtil;
            lock.lock();

            jUtil = new JSchUtility(param, existListener, "test -e " + file);
            jUtil.start();

            lock.unlock();
            Thread.sleep(Long.MAX_VALUE);
            // The Universe ends, and we return false.
            return false;
        }
        catch (InterruptedException ie)
        {
            FijiArchipelago.debug("Testing for file " + file + " on " + param.getHost() +
                    " resulted in code " + result.get());
            // We expect to be interrupted
            return (result.get() == 0);
        }
        catch (JSchException e)
        {
            lock.unlock();
            throw(e);
        }
        catch (ShellExecutionException e)
        {
            lock.unlock();
            throw(e);
        }
    }
    
}
