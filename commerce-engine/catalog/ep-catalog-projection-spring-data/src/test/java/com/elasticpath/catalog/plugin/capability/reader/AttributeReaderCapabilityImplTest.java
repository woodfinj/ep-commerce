/*
 * Copyright (c) Elastic Path Software Inc., 2018
 */
package com.elasticpath.catalog.plugin.capability.reader;

import static com.elasticpath.catalog.entity.constants.ProjectionIdentityTypeNames.ATTRIBUTE_IDENTITY_TYPE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.elasticpath.catalog.reader.ModifiedSince;
import com.elasticpath.catalog.reader.PaginationRequest;
import com.elasticpath.catalog.reader.impl.ModifiedSinceImpl;
import com.elasticpath.catalog.reader.impl.PaginationRequestImpl;
import com.elasticpath.catalog.spi.service.CatalogService;

/**
 * Test for {@link AttributeReaderCapabilityImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AttributeReaderCapabilityImplTest {
	public static final String STORE = "store";
	public static final String CODE = "code";
	@Mock
	private CatalogService catalogService;
	@InjectMocks
	private AttributeReaderCapabilityImpl readerCapability;

	@Test
	public void getTest() {
		when(catalogService.read(ATTRIBUTE_IDENTITY_TYPE, CODE, STORE)).thenReturn(Optional.empty());
		readerCapability.get(STORE, CODE);

		verify(catalogService).read(ATTRIBUTE_IDENTITY_TYPE, CODE, STORE);
	}

	@Test
	public void findAllTest() {
		PaginationRequest pagination = new PaginationRequestImpl();
		ModifiedSince modifiedSince = new ModifiedSinceImpl();
		readerCapability.findAll(STORE, pagination, modifiedSince);

		verify(catalogService).readAll(ATTRIBUTE_IDENTITY_TYPE, STORE, pagination, modifiedSince);
	}

	@Test
	public void findAllWithCodesTest() {
		readerCapability.findAllWithCodes(STORE, Collections.singletonList(CODE));

		verify(catalogService).readAll(ATTRIBUTE_IDENTITY_TYPE, STORE, Collections.singletonList(CODE));
	}
}
