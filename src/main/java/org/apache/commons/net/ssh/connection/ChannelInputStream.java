package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class ChannelInputStream extends InputStream implements ErrorNotifiable
{
    
    protected final Channel chan;
    protected final LocalWindow win;
    protected final Buffer buf;
    protected final byte[] b = new byte[1];
    protected boolean eof;
    protected SSHException error;
    
    public ChannelInputStream(Channel chan, LocalWindow win)
    {
        this.chan = chan;
        this.win = win;
        
        buf = new Buffer(chan.getLocalMaxPacketSize());
    }
    
    @Override
    public int available()
    {
        synchronized (buf) {
            return buf.available();
        }
    }
    
    @Override
    public void close()
    {
        eof();
    }
    
    public synchronized void notifyError(SSHException error)
    {
        this.error = error;
        eof();
    }
    
    @Override
    public int read() throws IOException
    {
        synchronized (b) {
            return read(b, 0, 1) == -1 ? -1 : b[0];
        }
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int growMax;
        synchronized (buf) {
            for (;;) {
                if (buf.available() > 0)
                    break;
                if (eof)
                    if (error != null)
                        throw error;
                    else
                        return -1;
                try {
                    buf.wait();
                } catch (InterruptedException e) {
                    throw (IOException) new InterruptedIOException().initCause(e);
                }
            }
            if (len > buf.available())
                len = buf.available();
            buf.getRawBytes(b, off, len);
            if (buf.rpos() > win.getMaxPacketSize() || buf.available() == 0)
                buf.compact();
            growMax = win.getInitialSize() - buf.available();
        }
        win.check(growMax);
        return len;
    }
    
    public void receive(byte[] data, int offset, int len) throws ConnectionException, TransportException
    {
        synchronized (buf) {
            if (eof)
                throw new ConnectionException("Getting data on EOF'ed stream");
            buf.putRawBytes(data, offset, len);
            buf.notifyAll();
        }
        synchronized (win) {
            win.consume(len);
            if (chan.getAutoExpand())
                win.check(len);
        }
    }
    
    @Override
    public String toString()
    {
        return "< ChannelInputStream for Channel #" + chan.getID() + " >";
    }
    
    protected void eof()
    {
        synchronized (buf) {
            if (!eof) {
                eof = true;
                buf.notifyAll();
            }
        }
    }
    
}
