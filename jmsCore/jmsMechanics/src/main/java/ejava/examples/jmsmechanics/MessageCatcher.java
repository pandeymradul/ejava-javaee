package ejava.examples.jmsmechanics;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a support class uses to receive messages by test cases that
 * are sending messages either to a queue or a topic.
 *
 * @author jcstaff
 */
public class MessageCatcher implements Runnable {
    private static final Log log = LogFactory.getLog(MessageCatcher.class);
    protected ConnectionFactory connFactory;
    protected Session sharedSession;
    protected Destination destination;
    protected int ackMode = Session.AUTO_ACKNOWLEDGE;
    protected boolean stop = false;
    protected boolean stopped = false;
    protected boolean started = false;
    protected List<Message> messages = new ArrayList<Message>();
    protected String name;
        
    public MessageCatcher(String name) {
        this.name = name;
    }
    public void setConnFactory(ConnectionFactory connFactory) {
        this.connFactory = connFactory;
    }
    public void setSession(Session session) {
        this.sharedSession = session;
    }
    public void setDestination(Destination destination) {
        this.destination = destination;
    }    
    public void setAckMode(int ackMode) {
        this.ackMode = ackMode;
    }
    public int getCount() {
        return messages.size();
    }
    public void clearMessages() {
        messages.clear();
    }
    public List<Message> getMessages() {
        return messages;
    }
    public void stop() {
        this.stop = true;
    }
    public boolean isStopped() {
        return stopped;
    }
    public boolean isStarted() {
        return started;
    }
    public void execute() throws JMSException {
        Connection connection = null;
        Session session = this.sharedSession;
        MessageConsumer consumer = null;
        try {
            if (session == null) {
                connection = connFactory.createConnection();
                session = connection.createSession(false, ackMode);
            }
            consumer = session.createConsumer(destination);
            if (this.sharedSession == null) {
                connection.start();
            }
            stopped = stop = false;
            log.info("catcher " + name + " starting (ackMode=" + ackMode + ")");
            started = true;
            while (!stop) {
                log.debug("catcher looking for message");
                Message message = consumer.receive(3000);
                if (message != null) {
                    messages.add(message);
                    log.debug(name + " received message #" + messages.size() +
                            ", msgId=" + message.getJMSMessageID());
                    Thread.yield();
                }      
            }
            log.info("catcher " + name + " stopping (ackMode=" + ackMode + ")");
            if (ackMode == Session.CLIENT_ACKNOWLEDGE && messages.size() > 0) {
                log.debug("catcher " + name + " acknowledging messages");
                messages.get(messages.size()-1).acknowledge();
            }
            if (this.sharedSession == null) {
                connection.stop();
            }
        }
        finally {
            stopped = true;
            //started = false;
            if (consumer != null)   { consumer.close(); }
            if (this.sharedSession == null && session!=null){ session.close();}
            if (connection != null) { connection.close(); }
        }
    }
    
    public void run() {
        try {
            execute();
        }
        catch (Exception ex) {
            log.fatal("error running " + name, ex);
        }
    }    

    public static void main(String args[]) {
        try {
            String connFactoryJNDI=null;
            String destinationJNDI=null;
            String name="";
            for (int i=0; i<args.length; i++) {
                if ("-jndi.name.connFactory".equals(args[i])) {
                    connFactoryJNDI = args[++i];
                }
                else if ("-jndi.name.destination".equals(args[i])) {
                    destinationJNDI=args[++i];
                }
                else if ("-name".equals(args[i])) {
                    name=args[++i];
                }
            }
            if (connFactoryJNDI==null) { 
                throw new Exception("jndi.name.connFactory not supplied");
            }
            else if (destinationJNDI==null) {
                throw new Exception("jndi.name.destination not supplied");
            }            
            MessageCatcher catcher = new MessageCatcher(name);
            Context jndi = new InitialContext();
            catcher.setConnFactory(
                    (ConnectionFactory)jndi.lookup(connFactoryJNDI));
            catcher.setDestination((Destination)jndi.lookup(destinationJNDI));
            catcher.execute();
        }
        catch (Exception ex) {
            log.fatal(ex);
            System.exit(-1);            
        }
    }


}
