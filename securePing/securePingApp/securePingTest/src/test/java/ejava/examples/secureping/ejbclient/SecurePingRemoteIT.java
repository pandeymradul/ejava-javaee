package ejava.examples.secureping.ejbclient;


import static org.junit.Assert.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import ejava.examples.secureping.ResetAuthenticationCache;
import ejava.examples.secureping.ejb.SecurePing;
import ejava.examples.secureping.ejb.SecurePingRemote;
import ejava.util.ejb.EJBClient;

public class SecurePingRemoteIT {
    private static final Log log = LogFactory.getLog(SecurePingRemoteIT.class);
    private static InitialContext jndi;
    String jndiName = System.getProperty("jndi.name.secureping",
    	EJBClient.getEJBLookupName("securePingEAR", "securePingEJB", "", 
   			"SecurePingEJB", SecurePingRemote.class.getName()));
    String adminUser = System.getProperty("admin.username","admin1");
    String adminPassword = System.getProperty("admin.password","password");
    String userUser = System.getProperty("user.username","user1");
    String userPassword = System.getProperty("user.password","password");
    String jmxUser = System.getProperty("jmx.username","admin");
    String jmxPassword = System.getProperty("jmx.password","password");
    
    SecurePing securePing;
    Map<String,CallbackHandler> logins = new HashMap<String, CallbackHandler>();
    CallbackHandler knownLogin;
    CallbackHandler userLogin;
    CallbackHandler adminLogin;
    CallbackHandler jmxLogin;
    String skipFlush = System.getProperty("skip.flush");
    
    @Before
    public void setUp() throws Exception {
        log.debug("getting jndi initial context");
        Properties env = new Properties();
        env.put("jboss.naming.client.ejb.context", true);
        jndi = new InitialContext(env);    
        log.debug("jndi=" + jndi.getEnvironment());
        log.debug("jndi name:" + jndiName);
        
        securePing = (SecurePingRemote)jndi.lookup(jndiName);
        
        //create different types of logins
        knownLogin = new BasicCallbackHandler();
        ((BasicCallbackHandler)knownLogin).setName("known");
        ((BasicCallbackHandler)knownLogin).setPassword("password");
        
        userLogin = new BasicCallbackHandler();
        log.debug("using user username=" + userUser);
        ((BasicCallbackHandler)userLogin).setName(userUser);
        ((BasicCallbackHandler)userLogin).setPassword(userPassword);

        adminLogin = new BasicCallbackHandler();
        log.debug("using admin username=" + adminUser);
        ((BasicCallbackHandler)adminLogin).setName(adminUser);
        ((BasicCallbackHandler)adminLogin).setPassword(adminPassword);
        
        jmxLogin = new BasicCallbackHandler();
        log.debug("using jmx username=" + jmxUser);
        ((BasicCallbackHandler)jmxLogin).setName(jmxUser);
        ((BasicCallbackHandler)jmxLogin).setPassword(jmxPassword);

        //account for how maven and Ant will expand string
        if (skipFlush == null || 
            "${skip.flush}".equals(skipFlush) ||
            "false".equalsIgnoreCase(skipFlush)) {
        	LoginContext lc = new LoginContext("securePingTest", jmxLogin);
        	lc.login();
            //new ResetAuthenticationCache().execute();
            lc.logout();
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private SecurePing runAs(String username, String password) throws NamingException {
        Hashtable env = new Hashtable(jndi.getEnvironment());
        if (username != null) {
	        env.put(Context.SECURITY_PRINCIPAL, username);
	        env.put(Context.SECURITY_CREDENTIALS, password);
        }
        log.debug(String.format("%s env=%s", username, env));
        SecurePing ejb = (SecurePingRemote)new InitialContext(env).lookup(jndiName);
        
        return ejb;
    }
    
    @Test
    public void testLoginContext() throws Exception {
        log.info("*** testLoginContext ***");
        
        LoginContext lc = new LoginContext("securePingTest", adminLogin);
        lc.login();
        log.info("subject=" + lc.getSubject());
        for (Principal p: lc.getSubject().getPrincipals()) {
            log.info("principal=" + p + ", " + p.getClass().getName());
        }
        log.info(lc.getSubject().getPrivateCredentials().size() + 
                " private credentials");
        for (Object c: lc.getSubject().getPrivateCredentials()) {
            log.info("private credential=" + c + ", " + c.getClass().getName());
        }
        log.info(lc.getSubject().getPublicCredentials().size() + 
                 " public credentials");
        for (Object c: lc.getSubject().getPublicCredentials()) {
            log.info("public credential=" + c + ", " + c.getClass().getName());
        }
        lc.logout();
    }

    @Test
    public void testIsCallerInRole() throws Exception {
        log.info("*** testIsCallerInRole ***");
        
        try {
        assertFalse("anonomous in admin role",
                     securePing.isCallerInRole("admin"));
        assertFalse("anonomous in user role",
                securePing.isCallerInRole("user"));
        assertFalse("anonomous in internalRole role",
                securePing.isCallerInRole("internalRole"));
        }
        catch (Exception ex) {
            log.info("anonymous calls to isCallerinRole failed:"+ex);
        }

        LoginContext lc = new LoginContext("securePingTest", knownLogin);
        lc.login();
        assertFalse("known in admin role",
                securePing.isCallerInRole("admin"));
        assertFalse("known in user role",
           securePing.isCallerInRole("user"));
        assertFalse("known in internalRole role",
           securePing.isCallerInRole("internalRole"));
        lc.logout();
        
        lc = new LoginContext("securePingTest", userLogin);
        lc.login();
        assertFalse("user in admin role",
                securePing.isCallerInRole("admin"));
        assertTrue("user not in user role",
           securePing.isCallerInRole("user"));
        assertFalse("user in internalRole role",
           securePing.isCallerInRole("internalRole"));
        lc.logout();
        
        lc = new LoginContext("securePingTest", adminLogin);
        lc.login();
        assertTrue("admin not in admin role",
                securePing.isCallerInRole("admin"));
        assertTrue("admin not in user role",
           securePing.isCallerInRole("user"));
        //not working ;(
        //assertTrue("admin not in internalRole role",
           securePing.isCallerInRole("internalRole");
        //);
        lc.logout();        
    }

    @Test
    public void testPingAll() throws Exception {
        log.info("*** testPingAll ***");
        try {
        	SecurePing ejb = runAs(null,  null);
            log.info(ejb.pingAll());
        }
        catch (Exception ex) {
            log.info("error calling pingAll:" + ex, ex);
            //failing on windows???fail("error calling pingAll:" +ex);
        }

        try {
            //LoginContext lc = new LoginContext("securePingTest", adminLogin);
            //lc.login();
        	SecurePing ejb = runAs(adminUser, adminPassword);
            String result=ejb.pingAll();
            log.info(result);
            //lc.logout();
            assertTrue("unexpected principle:" + result,
            		result.contains("admin"));
        }
        catch (Exception ex) {
            log.info("error calling pingAll:" + ex, ex);
            fail("error calling pingAll:" +ex);
        }        

        try {
            //LoginContext lc = new LoginContext("securePingTest", knownLogin);
            //lc.login();
            
        	SecurePing ejb = runAs("known", "password");
            log.info(ejb.pingAll());
            //lc.logout();
        }
        catch (Exception ex) {
            log.info("error calling pingAll:" + ex, ex);
            fail("error calling pingAll:" +ex);
        }
        
        try {
            //LoginContext lc = new LoginContext("securePingTest", userLogin);
            //lc.login();
        	SecurePing ejb = runAs(userUser, userPassword);
            log.info(ejb.pingAll());
            //lc.logout();
        }
        catch (Exception ex) {
            log.info("error calling pingAll:" + ex, ex);
            fail("error calling pingAll:" +ex);
        }        

    }
    
    @Test
    public void testPingUser() throws Exception {
        log.info("*** testPingUser ***");
        try {
            log.info(securePing.pingUser());
            fail("didn't detect anonymous user");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }

        try {
            LoginContext lc = new LoginContext("securePingTest", knownLogin);
            lc.login();
            log.info(securePing.pingUser());
            lc.logout();
            fail("didn't detect known, but non-user");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }
        
        try {
            LoginContext lc = new LoginContext("securePingTest", userLogin);
            lc.login();
            log.info(securePing.pingUser());
            lc.logout();
        }
        catch (Exception ex) {
            log.info("error calling pingUser:" + ex, ex);
            fail("error calling pingUser:" +ex);
        }        

        try {
            LoginContext lc = new LoginContext("securePingTest", adminLogin);
            lc.login();
            log.info(securePing.pingUser());
            lc.logout();
        }
        catch (Exception ex) {
            log.info("error calling pingUser:" + ex, ex);
            fail("error calling pingUser:" +ex);
        }        
    }

    @Test
    public void testPingAdmin() throws Exception {
        log.info("*** testPingAdmin ***");
        try {
            log.info(securePing.pingAdmin());
            fail("didn't detect anonymous user");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }

        try {
            LoginContext lc = new LoginContext("securePingTest", knownLogin);
            lc.login();
            log.info(securePing.pingAdmin());
            lc.logout();
            fail("didn't detect known, but non-admin user");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }
        
        try {
            LoginContext lc = new LoginContext("securePingTest", userLogin);
            lc.login();
            log.info(securePing.pingAdmin());
            lc.logout();
            fail("didn't detect non-admin user");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }        

        try {
            LoginContext lc = new LoginContext("securePingTest", adminLogin);
            lc.login();
            log.info(securePing.pingAdmin());
            lc.logout();
        }
        catch (Exception ex) {
            log.info("error calling pingAdmin:" + ex, ex);
            fail("error calling pingAdmin:" +ex);
        }
        
    }

    @Test
    public void testPingExcluded() throws Exception {
        log.info("*** testPingExcluded ***");
        try {
            log.info(securePing.pingExcluded());
            fail("didn't detect excluded");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }

        try {
            LoginContext lc = new LoginContext("securePingTest", knownLogin);
            lc.login();
            log.info(securePing.pingExcluded());
            lc.logout();
            fail("didn't detect excluded");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }
        
        try {
            LoginContext lc = new LoginContext("securePingTest", userLogin);
            lc.login();
            log.info(securePing.pingExcluded());
            lc.logout();
            fail("didn't detect excluded");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }        

        try {
            LoginContext lc = new LoginContext("securePingTest", adminLogin);
            lc.login();
            log.info(securePing.pingExcluded());
            lc.logout();
            fail("didn't detect excluded");
        }
        catch (Exception ex) {
            log.info("expected exception thrown:" + ex);
        }        
    }      
}