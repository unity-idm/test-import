/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.StorageCleaner;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.tx.TransactionTL;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public abstract class AbstractBasicDAOTest<T>
{
	@Autowired
	protected StorageCleaner dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	protected ImportExport ie;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	protected abstract BasicCRUDDAO<T> getDAO();
	protected abstract T getObject(String id);
	protected abstract void mutateObject(T src);
	protected abstract void assertAreEqual(T obj, T cmp);

	@Test
	public void shouldReturnCreatedByKey()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			long key1 = dao.create(obj);
			dao.deleteByKey(key1);
			long key2 = dao.create(obj);
			T ret = dao.getByKey(key2);

			assertThat(key1 != key2, is(true));
			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}
	
	@Test
	public void shouldReturnUpdatedByKey()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);

			mutateObject(obj);
			dao.updateByKey(key, obj);

			T ret = dao.getByKey(key);

			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}

	@Test
	public void shouldReturnTwoCreatedWithinCollections()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			int initial = dao.getAll().size();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.create(obj);
			dao.create(obj2);
			List<T> all = dao.getAll();

			assertThat(all, is(notNullValue()));

			assertThat(all.size(), is(initial + 2));

			boolean objFound = false;
			boolean obj2Found = false;
			for (T el: all)
			{
				try
				{
					assertAreEqual(el, obj);
					objFound = true;
				} catch (Throwable t) {}
				try
				{
					assertAreEqual(el, obj2);
					obj2Found = true;
				} catch (Throwable t) {}
			}
			assertThat(objFound, is(true));
			assertThat(obj2Found, is(true));
		});
	}

	@Test
	public void shouldReturnTwoCreatedWithinCollectionsWithCommit()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			int initial = dao.getAll().size();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.create(obj);
			dao.create(obj2);
	
			TransactionTL.manualCommit();
			
			List<T> all = dao.getAll();

			assertThat(all, is(notNullValue()));

			assertThat(all.size(), is(initial + 2));

			boolean objFound = false;
			boolean obj2Found = false;
			for (T el: all)
			{
				try
				{
					assertAreEqual(el, obj);
					objFound = true;
				} catch (Throwable t) {}
				try
				{
					assertAreEqual(el, obj2);
					obj2Found = true;
				} catch (Throwable t) {}
			}
			assertThat(objFound, is(true));
			assertThat(obj2Found, is(true));
		});
	}
	
	@Test
	public void shouldNotReturnRemovedByKey()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);

			dao.deleteByKey(key);

			catchException(dao).getByKey(key);

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void shouldFailOnRemovingAbsent()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();

			catchException(dao).deleteByKey(Integer.MAX_VALUE);

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void shouldFailOnUpdatingAbsent()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			catchException(dao).updateByKey(Integer.MAX_VALUE, obj);

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void importExportIsIdempotent()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try
			{
				ie.store(os);
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Export failed " + e);
			}

			dbCleaner.reset();
			
			String dump = new String(os.toByteArray(), StandardCharsets.UTF_8);
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			try
			{
				ie.load(is);
			} catch (Exception e)
			{
				e.printStackTrace();
				
				fail("Import failed " + e + "\nDump:\n" + dump);
			}

			List<T> all = dao.getAll();

			assertThat(all.size(), is(1));
			assertAreEqual(all.get(0), obj);
		});
	}
}