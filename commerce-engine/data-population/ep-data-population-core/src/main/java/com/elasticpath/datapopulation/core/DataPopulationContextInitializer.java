/*
 * Copyright (c) Elastic Path Software Inc., 2018
 */
package com.elasticpath.datapopulation.core;

import java.io.File;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * This utility class will create the application context used by data population.
 * It requires the following resources:
 * <ul>
 * <li>dataDirectory - the directory that contains the data, schema and the global configurations</li>
 * <li>configDirectory - the directory that contains environment specific configurations</li>
 * <li>workingDirectory - the temporary folder to store working files generated by Data Population</li>
 * <li>springConfigurationPath - the location of the spring configuration file used to initialize the application context</li>
 * </ul>
 */
public final class DataPopulationContextInitializer {

	private DataPopulationContextInitializer() {
		//initializer class
	}

	/**
	 * This method will create the application context used by data population.
	 * When data population starts, these directories are either passed in via command line or during runtime.
	 * As such, we need to create bean definitions of these at runtime. These beans are all of the type <code>java.io.File</code>
	 * <p>
	 * The beans are stored in the root application context and then the entire spring configuration context will be initialized.
	 *
	 * @param dataDirectory           the directory that contains the data, schema and the global configurations
	 * @param configDirectory         the directory that contains environment specific configurations
	 * @param workingDirectory        the temporary folder to store working files generated by Data Population
	 * @param springConfigurationPath the location of the spring configuration file used to initialize the application context
	 * @return applicationContext the application context with the directory beans
	 */
	public static ApplicationContext initializeContext(final String dataDirectory,
													   final String configDirectory,
													   final String workingDirectory,
													   final String springConfigurationPath) {
		BeanDefinition dataDirectoryBean = BeanDefinitionBuilder.rootBeanDefinition(File.class)
				.addConstructorArgValue(dataDirectory).getBeanDefinition();
		dataDirectoryBean.setAutowireCandidate(false);
		BeanDefinition configDirectoryBean = BeanDefinitionBuilder.rootBeanDefinition(File.class)
				.addConstructorArgValue(configDirectory).getBeanDefinition();
		configDirectoryBean.setAutowireCandidate(false);
		BeanDefinition workingDirectoryBean = BeanDefinitionBuilder.rootBeanDefinition(File.class)
				.addConstructorArgValue(workingDirectory).getBeanDefinition();
		workingDirectoryBean.setAutowireCandidate(false);

		DefaultListableBeanFactory directoryBeanFactory = new DefaultListableBeanFactory();
		directoryBeanFactory.registerBeanDefinition("dataDirectory", dataDirectoryBean);
		directoryBeanFactory.registerBeanDefinition("configDirectory", configDirectoryBean);
		directoryBeanFactory.registerBeanDefinition("workingDirectory", workingDirectoryBean);

		// create parent context
		GenericApplicationContext rootApplicationContext = new GenericApplicationContext(directoryBeanFactory);
		rootApplicationContext.refresh();

		return new ClassPathXmlApplicationContext(getXMLConfigLocation(springConfigurationPath), rootApplicationContext);
	}

	/**
	 * This method is necessary only because {@link org.springframework.context.support.ClassPathXmlApplicationContext} signature requires it.
	 *
	 * @param springConfigurationPath the path to the spring config file
	 * @return an array wrapper for the path
	 */
	private static String[] getXMLConfigLocation(final String springConfigurationPath) {
		return new String[]{springConfigurationPath};
	}
}
