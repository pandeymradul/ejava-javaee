package myorg.javaeeex.blimpl;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import myorg.javaeeex.bl.TestUtil;
import myorg.javaeeex.cdi.JavaeeEx2;
import myorg.javaeeex.cdi.JavaeeEx;
import myorg.javaeeex.jpa.DBUtil;

public class TestUtilImpl implements TestUtil {
    protected static final String DROP_SCRIPT = 
        "ddl/javaeeExImpl-dropJPA.ddl";  
    protected static final String CREATE_SCRIPT = 
        "ddl/javaeeExImpl-createJPA.ddl";
    
    @Inject //@JavaeeEx2    
    protected EntityManager em;

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
    
    protected DBUtil getDBUtil() {
        DBUtil dbUtil = new DBUtil();
        dbUtil.setEntityManager(em);
        dbUtil.setDropPath(DROP_SCRIPT);
        dbUtil.setCreatePath(CREATE_SCRIPT);
        return dbUtil;
    }

    public void resetAll() throws Exception {
        DBUtil dbUtil = getDBUtil();
        dbUtil.dropAll();
        dbUtil.createAll();
    }
}
