/*
 * Copyright (c) Elastic Path Software Inc., 2018
 */
package com.elasticpath.domain.message.handler.fieldmetadata.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.elasticpath.catalog.update.processor.capabilities.ModifierGroupUpdateProcessor;
import com.elasticpath.domain.modifier.ModifierGroup;
import com.elasticpath.domain.message.handler.EventMessageHandlerHelper;
import com.elasticpath.messaging.EventMessage;

/**
 * Tests {@link ModifierGroupCreatedEventHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ModifierGroupCreatedEventHandlerTest {
	private static final String GUID = "guid";

	@Mock
	private EventMessageHandlerHelper<ModifierGroup> eventMessageHandlerHelper;

	@Mock
	private ModifierGroupUpdateProcessor cartItemModifierGroupUpdateProcessor;

	@InjectMocks
	private ModifierGroupCreatedEventHandler cartItemModifierGroupCreatedEventHandler;

	private EventMessage eventMessage;
	private ModifierGroup cartItemModifierGroup;

	@Before
	public void setUp() {
		eventMessage = mock(EventMessage.class);
		cartItemModifierGroup = mock(ModifierGroup.class);

		when(cartItemModifierGroup.getGuid()).thenReturn(GUID);
		when(eventMessageHandlerHelper.getExchangedEntity(eventMessage)).thenReturn(cartItemModifierGroup);
	}

	@Test
	public void shouldCallGetExchangedEntityForEventMessage() {
		cartItemModifierGroupCreatedEventHandler.handleMessage(eventMessage);

		verify(eventMessageHandlerHelper).getExchangedEntity(eventMessage);
	}

	@Test
	public void shouldCallProcessModifierGroupCreatedForModifierGroup() {
		cartItemModifierGroupCreatedEventHandler.handleMessage(eventMessage);

		verify(cartItemModifierGroupUpdateProcessor).processModifierGroupCreated(cartItemModifierGroup);
	}

}