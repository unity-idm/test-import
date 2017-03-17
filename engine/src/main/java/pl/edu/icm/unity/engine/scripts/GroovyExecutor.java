/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import java.io.Reader;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Executes a given Groovy script using provided binding (context). 
 *
 * @author golbi
 */
public class GroovyExecutor
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, GroovyExecutor.class);
	
	public static void run(String phase, String name, 
			Reader scriptReader, Binding binding)
	{
		GroovyShell shell = new GroovyShell(binding);
		LOG.info("{} event triggers invocation of Groovy script: {}", phase, name);
		Stopwatch timer = Stopwatch.createStarted();
		try
		{
			shell.evaluate(scriptReader);
		} catch (Exception e)
		{
			throw new InternalException("Failed to initialize content from Groovy " 
					+ " script: " + name + ": reason: " + e.getMessage(), e);
		}
		LOG.info("Groovy script: {} finished in {}", name, timer);
	}
}
