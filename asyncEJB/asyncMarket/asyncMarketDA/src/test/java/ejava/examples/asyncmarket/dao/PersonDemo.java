package ejava.examples.asyncmarket.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ejava.examples.asyncmarket.DemoBase;
import ejava.examples.asyncmarket.bo.Person;

public class PersonDemo extends DemoBase {
    Log log = LogFactory.getLog(PersonDemo.class);
    
    public void testPerson() {
        log.info("*** testCreatePerson ***");
        
        Person person = new Person();
        person.setName("joe smith");
        person.setUserId("jsmith");
        
        personDAO.createPerson(person);
        
        log.debug("created person:" + person);        
        assertTrue("personId not assigned", person.getId() > 0);
    }
    
    public void testGetPerson() {
        log.info("*** testGetPerson ***");
                
        Person p = personDAO.getPerson(10101L);
        assertNull("unexpected person found", p);
        
        Person person = new Person();
        person.setName("joe smith");
        person.setUserId("jsmith");        
        personDAO.createPerson(person);        
        log.debug("created person:" + person);        
        assertTrue("personId not assigned", person.getId() > 0);
        
        Person person2 = personDAO.getPerson(person.getId());
        assertNotNull("person not found", person2);
        
        Person person3 = personDAO.getPersonByUserId(person.getUserId());
        assertNotNull("person not found", person3);        
    }

    public void testGetPeople() {
        log.info("*** testGetPeople ***");
        
        for (int i=0; i<10; i++) {
            Person person = new Person();
            person.setName("joe smith");
            person.setUserId("jsmith" + i);        
            personDAO.createPerson(person);        
        }
        
        List<Person> people = personDAO.getPeople(0, 100);
        assertEquals("unexpected number of people found:" + people.size(),
                10, people.size());
    }
    
}
