/**
 * Copyright (c) Elastic Path Software Inc., 2017
 */
package com.elasticpath.cmclient.core.security;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.AuthenticationException;

import com.elasticpath.domain.cmuser.CmUser;
import com.elasticpath.service.cmuser.CmUserService;

/**
 * Listener that is responsible for handling spring security events.
 *
 */
public class SpringSecurityListener implements ApplicationListener<ApplicationEvent> {
	
	private CmUserService cmUserService;

	@Override
	public void onApplicationEvent(final ApplicationEvent event) {
		if (event instanceof AuthenticationFailureBadCredentialsEvent) {
			final AuthenticationFailureBadCredentialsEvent badCredentialsEvent = (AuthenticationFailureBadCredentialsEvent) event;

			handleBadCredentialsEvent(badCredentialsEvent);
		}

	}

	private void handleBadCredentialsEvent(final AuthenticationFailureBadCredentialsEvent badCredentialsEvent) {
		final AuthenticationException exception = badCredentialsEvent.getException();
		final String userName = badCredentialsEvent.getAuthentication().getPrincipal().toString();
		if (exception instanceof BadCredentialsException && userName != null) {

			handleCmUserFailedLoginAttempt(userName);
			final CmUser cmUser = (CmUser) badCredentialsEvent.getAuthentication().getDetails();
			if (cmUser != null) {
				handleLastAttempt(cmUser);
			}
		}
	}

	private void handleCmUserFailedLoginAttempt(final String userName) {
		if (userName != null) {
			cmUserService.addFailedLoginAttempt(userName);
		}
	}

	private void handleLastAttempt(final CmUser cmUser) {
		if (!cmUser.isAccountNonLocked()) {
			throw new LockedException("Account has been locked.");
		}
	}

	/**
	 * Gets the cmUser service.
	 * 
	 * @return the cmUserService
	 */
	public CmUserService getCmUserService() {
		return cmUserService;
	}

	/**
	 * Sets the cmUser service.
	 * 
	 * @param cmUserService the cmUserService
	 */
	public void setCmUserService(final CmUserService cmUserService) {
		this.cmUserService = cmUserService;
	}

}
