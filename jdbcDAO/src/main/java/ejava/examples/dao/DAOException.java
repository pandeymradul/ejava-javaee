package ejava.examples.dao;

/**
 * This is the base exception for example DAOs.
 *
 * @author jcstaff
 */
public class DAOException extends Exception {
    private static final long serialVersionUID = 1L;
    public DAOException() {}
    public DAOException(String message) { super(message); }
    public DAOException(String message, Throwable rootCause) {
        super(message, rootCause);
    }
    public DAOException(Throwable rootCause) {
        super(rootCause);
    }
}
