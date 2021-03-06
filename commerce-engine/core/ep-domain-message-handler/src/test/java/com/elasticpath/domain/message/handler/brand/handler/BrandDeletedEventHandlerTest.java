/*
 * Copyright (c) Elastic Path Software Inc., 2018
 */
package com.elasticpath.domain.message.handler.brand.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.elasticpath.catalog.update.processor.capabilities.BrandUpdateProcessor;
import com.elasticpath.messaging.EventMessage;

/**
 * Tests {@link BrandDeletedEventHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BrandDeletedEventHandlerTest {

	private static final String GUID = "guid";

	@Mock
	private BrandUpdateProcessor brandUpdateProcessor;

	@InjectMocks
	private BrandDeletedEventHandler brandDeletedEventHandler;

	private EventMessage eventMessage;

	@Before
	public void setUp() {
		eventMessage = mock(EventMessage.class);

		when(eventMessage.getGuid()).thenReturn(GUID);
	}

	@Test
	public void shouldCallProcessBrandDeletedForBrand() {
		brandDeletedEventHandler.handleMessage(eventMessage);

		verify(brandUpdateProcessor).processBrandDeleted(GUID);
	}

}
