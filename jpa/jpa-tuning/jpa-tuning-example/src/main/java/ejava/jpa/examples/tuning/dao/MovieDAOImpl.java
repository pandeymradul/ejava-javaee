package ejava.jpa.examples.tuning.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ejava.jpa.examples.tuning.bo.Movie;
import ejava.jpa.examples.tuning.bo.MovieRating;
import ejava.jpa.examples.tuning.bo.Person;

public class MovieDAOImpl {
	private static final Log log = LogFactory.getLog(MovieDAOImpl.class);
	private EntityManager em;
	
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}
	
	private class QueryLogger<T> {
		private TypedQuery<T> query;
		private String jpaql;
		private Map<String, Object> params = new HashMap<String, Object>();
		
		public QueryLogger(String jpaql, Class<T> resultType) {
			query = em.createQuery(jpaql, resultType);
			this.jpaql = jpaql;
		}
		public QueryLogger<T> setParameter(String key, Object value) {
			params.put(key,  value);
			query.setParameter(key, value);
			return this;
		}
		public QueryLogger<T> setFirstResult(int offset) {
			query.setFirstResult(offset);
			return this;
		}
		public QueryLogger<T> setMaxResults(int limit) {
			query.setMaxResults(limit);
			return this;
		}
		public T getSingleResult() {
			log.info(jpaql + ", " + params);
			return query.getSingleResult();
		}
		public List<T> getResultList() {
			log.info(jpaql + ", " + params);
			return query.getResultList();
		}
	}
	
	/**
	 * Helper method to log the JPAQL portion of the query.
	 * @param jpaql
	 * @param resultClass
	 * @return
	 */
	protected <T> QueryLogger<T> createQuery(String jpaql, Class<T> resultClass) {
		return new QueryLogger<T>(jpaql, resultClass);
	}
	
	/**
	 * Helper method to add paging parameters to a query
	 * @param query
	 * @param offset
	 * @param limit
	 * @return
	 */
	protected <T> QueryLogger<T> withPaging(QueryLogger<T> query, Integer offset, Integer limit) {
    	if (offset != null && offset > 0) {
    		query.setFirstResult(offset);
    	}
    	if (limit != null && limit > 0) {
    		query.setMaxResults(limit);
    	}
    	return query;
	}

	/**
	 * Returns a Person instance for the Kevin Bacon actor who
	 * played in Tremors.
	 * @return
	 */
	public Person getKevinBacon() {
    	return createQuery(
    			"select r.actor.person " +
    			"from MovieRole r " +
    			"where r.movie.title = 'Tremors' and " +
    			"r.actor.person.lastName='Bacon' and " +
    			"r.actor.person.firstName='Kevin'", Person.class)
    			.getSingleResult();
	}

	/**
	 * Find people who are 1 step from Kevin Bacon.
	 * @param p
	 * @param offset
	 * @param limit
	 * @return
	 */
    public List<Person> oneStepFromPerson(Person p, Integer offset, Integer limit) {
    	return withPaging(createQuery(
			"select a.person from Actor a " +
			"join a.roles ar " +
			"join a.person ap " +
			"where ar.movie in (select m from Movie m " +
			    "inner join m.cast mr " +
		        "inner join mr.actor ma " +
		        "inner join ma.person mp " +
			    "where mp.id = :id))" +
			 "and ap.id not = :id", Person.class)
			 .setParameter("id", p.getId()), offset, limit)
			.getResultList();
    }

    /**
     * Returns a bulk, unordered page of movies. This will cause a full
     * table scan since there is no reason to consult the index.
     * @param offset
     * @param limit
     * @return
     */
	public List<Movie> getMovies(Integer offset, Integer limit) {
		return withPaging(createQuery(
				"select m from Movie m", Movie.class), 
				offset, limit).getResultList();
	}
	
	/**
	 * Returns an unordered page of movies matching the supplied rating -- but
	 * calling upper() on the DB value. This will cause an index to be bypassed 
	 * except for an upper() function index.
	 * @param rating
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Movie> getMoviesByRatingUpperFunction(MovieRating rating, Integer offset, Integer limit) {
		return withPaging(createQuery(
				"select m from Movie m " +
				"where upper(m.rating) = :rating", Movie.class)
				.setParameter("rating", rating.mpaa().toUpperCase()), 
				offset, limit).getResultList();
	}

	/**
	 * Returns an unordered page of movies matching the supplied rating -- but
	 * calling lower() on the DB value. This will cause an index to be bypassed
	 * except for a lower() function index.
	 * @param rating
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Movie> getMoviesByRatingLowerFunction(MovieRating rating, Integer offset, Integer limit) {
		return withPaging(createQuery(
				"select m from Movie m " +
				"where lower(m.rating) = :rating", Movie.class)
				.setParameter("rating", rating.mpaa().toLowerCase()), 
				offset, limit).getResultList();
	}

	/**
	 * Returns an unordered page of movies matching the supplied rating --
	 * without calling any function()s on the stored data. If the column 
	 * contains an index, it will be used. 
	 * @param rating
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Movie> getMoviesByRatingValue(MovieRating rating, Integer offset, Integer limit) {
		return withPaging(createQuery(
				"select m from Movie m " +
				"where m.rating = :rating", Movie.class)
				.setParameter("rating", rating.mpaa()), 
				offset, limit).getResultList();
	}

	/**
	 * Returns an unordered page of movies that have a title "like" the one passed
	 * in. Note the difference in index behavior when there is a wildcard at the 
	 * beginning or end of the searched title
	 * @param title
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Movie> getMoviesLikeTitle(String title, Integer offset, Integer limit) {
		return withPaging(createQuery(
				"select m from Movie m " +
				"where m.title like :title", Movie.class)
				.setParameter("title", title), 
				offset, limit).getResultList();
	}

	/**
	 * Returns movies exactly matching the provided title.
	 * @param title
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Movie> getMoviesEqualsTitle(String title, Integer offset, Integer limit) {
		return withPaging(createQuery(
				"select m from Movie m " +
				"where m.title = :title", Movie.class)
				.setParameter("title", title), 
				offset, limit).getResultList();
	}

	/**
	 * Returns an unordered page of titles that match a specified rating. This query
	 * will be impacted by a presence of an index on the rating column and the presence
	 * of the title column with the rating column as a part of a composite index.
	 * @param rating
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<String> getTitlesByRating(MovieRating rating, Integer offset, Integer limit) {
		return withPaging(createQuery(
				"select m.title from Movie m " +
				"where m.rating = :rating", String.class)
				.setParameter("rating", rating.mpaa()), 
				offset, limit).getResultList();
	}
}