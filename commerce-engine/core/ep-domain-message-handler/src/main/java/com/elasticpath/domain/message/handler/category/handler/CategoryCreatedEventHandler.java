/*
 * Copyright (c) Elastic Path Software Inc., 2018
 */
package com.elasticpath.domain.message.handler.category.handler;

import org.apache.log4j.Logger;

import com.elasticpath.catalog.update.processor.capabilities.CategoryUpdateProcessor;
import com.elasticpath.domain.catalog.Category;
import com.elasticpath.domain.message.handler.EventMessageHandler;
import com.elasticpath.domain.message.handler.EventMessageHandlerHelper;
import com.elasticpath.messaging.EventMessage;

/**
 * Implementation of {@link EventMessageHandler} for {@link Category} created event.
 */
public class CategoryCreatedEventHandler implements EventMessageHandler {

	private static final Logger LOGGER = Logger.getLogger(CategoryCreatedEventHandler.class);

	private final EventMessageHandlerHelper<Category> eventMessageHandlerHelper;
	private final CategoryUpdateProcessor categoryUpdateProcessor;

	/**
	 * Constructor.
	 *
	 * @param eventMessageHandlerHelper helper for getting of {@link Category} from Exchange.
	 * @param categoryUpdateProcessor   domain update service capability for processing {@link Category} update notifications.
	 */
	public CategoryCreatedEventHandler(final EventMessageHandlerHelper<Category> eventMessageHandlerHelper,
									   final CategoryUpdateProcessor categoryUpdateProcessor) {
		this.eventMessageHandlerHelper = eventMessageHandlerHelper;
		this.categoryUpdateProcessor = categoryUpdateProcessor;
	}

	@Override
	public void handleMessage(final EventMessage eventMessage) {
		final Category category = eventMessageHandlerHelper.getExchangedEntity(eventMessage);

		LOGGER.debug("Processing CATEGORY_CREATED event for Category with guid: " + category.getGuid());

		categoryUpdateProcessor.processCategoryCreated(category);
	}

}
