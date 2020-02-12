/**
 * Copyright (c) Elastic Path Software Inc., 2010
 */
package com.elasticpath.service.changeset.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.elasticpath.domain.objectgroup.BusinessObjectDescriptor;
import com.elasticpath.domain.objectgroup.impl.BusinessObjectDescriptorImpl;
import com.elasticpath.persistence.api.PersistenceEngine;

/**
 * Test that catalog metadata gets resolved as expected.
 */
public class CatalogMetadataResolverTest {
	
	private CatalogMetadataResolver resolver;
	
	private PersistenceEngine persistenceEngine;
	
	@Rule
	public final JUnitRuleMockery context = new JUnitRuleMockery();
	
	/**
	 * Set up required for each test.
	 */
	@Before
	public void setUp()  {
		resolver = new CatalogMetadataResolver();
		persistenceEngine = context.mock(PersistenceEngine.class);
		resolver.setPersistenceEngine(persistenceEngine);
	}

	/**
	 * Test validation behaves as required.
	 */
	@Test
	public void testIsValidResolverForObjectType() {
		assertTrue("Catalog should be valid", resolver.isValidResolverForObjectType("Catalog"));
		assertFalse("Null should not be valid", resolver.isValidResolverForObjectType(null));
		assertFalse("Empty string should not be valid", resolver.isValidResolverForObjectType(StringUtils.EMPTY));
		assertFalse("A different object string should not be valid", resolver.isValidResolverForObjectType("Product"));
		assertFalse("Abritrary string should not be valid", resolver.isValidResolverForObjectType("anything"));
	}

	/**
	 * Test name metadata for catalog is retrieved by a named query.
	 */
	@Test
	public void testResolveMetaDataCatalogAssignment() {
		final String dcGuid = "DC1";
		final BusinessObjectDescriptor objectDescriptor = new BusinessObjectDescriptorImpl();
		objectDescriptor.setObjectIdentifier(dcGuid);
		objectDescriptor.setObjectType("Catalog");
		
		final String name = "Test Content";
		final List<String> nameList = new ArrayList<>();
		nameList.add(name);
		context.checking(new Expectations() {
			{
				oneOf(persistenceEngine).retrieveByNamedQuery("VIRTUAL_CATALOG_NAME_BY_GUID", dcGuid);
				will(returnValue(nameList));
			}
		});
		
		Map<String, String> metadata = resolver.resolveMetaData(objectDescriptor);
		assertEquals("There should be metadata returned", 1, metadata.size());
		assertEquals("The catalog name should be as expected", name, metadata.get("objectName"));
	}

	/**
	 * Test no metadata returned if the query returns no results (e.g. catalog does not exist). 
	 */
	@Test
	public void testResolveMetaDataNonExistentCatalog() {
		final String dcGuid = "NOSUCHCODE";
		final BusinessObjectDescriptor objectDescriptor = new BusinessObjectDescriptorImpl();
		objectDescriptor.setObjectIdentifier(dcGuid);
		objectDescriptor.setObjectType("Catalog");
		
		context.checking(new Expectations() {
			{
				oneOf(persistenceEngine).retrieveByNamedQuery("VIRTUAL_CATALOG_NAME_BY_GUID", dcGuid);
				will(returnValue(Collections.emptyList()));
			}
		});
		
		Map<String, String> metadata = resolver.resolveMetaData(objectDescriptor);
		assertEquals("There should be no metadata returned", 0, metadata.size());
	}
	
	/**
	 * Test that no metadata is returned when the object is not a valid type.
	 */
	@Test
	public void testResolveMetaDataForNonCatalogObject() {
		final BusinessObjectDescriptor objectDescriptor = new BusinessObjectDescriptorImpl();
		objectDescriptor.setObjectIdentifier("WHATEVER");
		objectDescriptor.setObjectType("Product");
		Map<String, String> metadata = resolver.resolveMetaData(objectDescriptor);
		assertEquals("There should be no metadata returned", 0, metadata.size());
	}

}
