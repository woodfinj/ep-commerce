/**
 * Copyright (c) Elastic Path Software Inc., 2017
 */
package com.elasticpath.cmclient.core.common;

import org.eclipse.jface.action.Action;

import com.elasticpath.cmclient.core.CoreImageRegistry;
import com.elasticpath.cmclient.core.CoreMessages;
import com.elasticpath.cmclient.core.event.NavigationEvent;
import com.elasticpath.cmclient.core.helpers.PaginationSupport;

/**
 * Go to the last page of results.
 */
public class PaginationControlResultLastAction extends Action {

	private final PaginationSupport paginationSupport;

	/**
	 * Constructor.
	 *
	 * @param paginationSupport the pagination support
	 */
	protected PaginationControlResultLastAction(final PaginationSupport paginationSupport) {
		super();
		setImageDescriptor(CoreImageRegistry.IMAGE_RESULTSET_LAST);
		setToolTipText(CoreMessages.get().navigation_LastPage);
		this.paginationSupport = paginationSupport;
	}

	@Override
	public void run() {
		paginationSupport.fireNavigationEvent(NavigationEvent.NavigationType.LAST, null);
	}
}
