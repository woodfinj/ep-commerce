/**
 * Copyright (c) Elastic Path Software Inc., 2008
 */
package com.elasticpath.test.integration.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.elasticpath.common.dto.ShoppingItemDto;
import com.elasticpath.commons.constants.ContextIdNames;
import com.elasticpath.domain.catalog.Product;
import com.elasticpath.domain.customer.Customer;
import com.elasticpath.domain.customer.CustomerSession;
import com.elasticpath.domain.customer.impl.SessionCleanupJob;
import com.elasticpath.domain.factory.TestCustomerSessionFactoryForTestApplication;
import com.elasticpath.domain.factory.TestShoppingCartFactoryForTestApplication;
import com.elasticpath.domain.shopper.Shopper;
import com.elasticpath.domain.shoppingcart.ShoppingCart;
import com.elasticpath.domain.store.Store;
import com.elasticpath.sellingchannel.director.CartDirector;
import com.elasticpath.service.customer.CustomerService;
import com.elasticpath.service.customer.CustomerSessionCleanupService;
import com.elasticpath.service.customer.CustomerSessionService;
import com.elasticpath.service.misc.TimeService;
import com.elasticpath.service.shopper.ShopperCleanupService;
import com.elasticpath.service.shopper.ShopperService;
import com.elasticpath.service.shoppingcart.ShoppingCartService;
import com.elasticpath.service.shoppingcart.WishListService;
import com.elasticpath.settings.test.support.SimpleSettingValueProvider;
import com.elasticpath.test.integration.DirtiesDatabase;
import com.elasticpath.test.integration.cart.AbstractCartIntegrationTestParent;
import com.elasticpath.test.persister.testscenarios.SimpleStoreScenario;

/**
 * Test that the session cleanup job cleans up as expected.
 */
@SuppressWarnings("PMD.GodClass")
public class SessionCleanupJobIntegrationTest extends AbstractCartIntegrationTestParent {

	private static final String SHOULD_HAVE_BEEN_DELETED = "should have been deleted!";

	private static final String SHOULD_NOT_BE_DELETED = "should not be deleted!";

	private static final String WAS_NOT_PERSISTED = "was not persisted!";

	private static final int CUSTOMER_SESSION_CREATION_DAYS_AGO = 50;

	private static final int OLD_CUSTOMER_SESSION_ACCESSED_DAYS_AGO = 30;

	private static final int RECENT_CUSTOMER_SESSION_ACCESSED_DAYS_AGO = 10;

	private static final int DELETE_BEFORE_NUMBER_OF_DAYS = 20;

	private static final Integer DEFAULT_SESSION_CLEANUP_BATCH_SIZE = 1000;

	@Autowired
	private CartDirector cartDirector;

	@Autowired
	private ShoppingCartService shoppingCartService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private CustomerSessionService customerSessionService;

	@Autowired
	private CustomerSessionCleanupService customerSessionCleanupService;

	@Autowired
	private ShopperService shopperService;

	@Autowired
	private ShopperCleanupService shopperCleanupService;

	@Autowired
	private WishListService wishListService;

	private SessionCleanupJob sessionCleanupJob;

	private Product defaultProduct;

	/**
	 * Representing age of access date.
	 */
	private enum AccessAge {
		OLD, RECENT, MULTIAGED
	}

	/**
	 * Different types of carts.
	 */
	private enum CartType {
		NONE, EMPTY, NON_EMPTY
	}

	/**
	 * Different types of users.
	 */
	private enum CustomerType {
		ANONYMOUS, REGISTERED
	}


	/**
	 * Just a DTO that holds all the Sets we want to track for verification.
	 */
	private class TrackingSets {
		/** deletedSessions. */
		public final Set<CustomerSession> deletedSessions = new HashSet<>();
		/** keptSessions. */
		public final Set<CustomerSession> keptSessions = new HashSet<>();
		/** deletedShoppers. */
		public final Set<Shopper> deletedShoppers = new HashSet<>();
		/** keptShoppers. */
		public final Set<Shopper> keptShoppers = new HashSet<>();
	}

	/**
	 * Used as a verifying interface.
	 */
	private interface Verifier<T> {
		boolean verify(T test);
	}

	/**
	 *
	 * @throws java.lang.Exception Exception.
	 */
	@Before
	public void setUp() throws Exception {
		sessionCleanupJob = createSessionCleanupJob();
		defaultProduct = persistProductWithSku();
	}

	private SessionCleanupJob createSessionCleanupJob() {
		TimeService timeService = getBeanFactory().getBean(ContextIdNames.TIME_SERVICE);

		SessionCleanupJob sessionCleanupJob = new SessionCleanupJob();
		sessionCleanupJob.setTimeService(timeService);
		sessionCleanupJob.setCustomerSessionCleanupService(customerSessionCleanupService);
		sessionCleanupJob.setShoppingCartService(shoppingCartService);
		sessionCleanupJob.setWishlistService(wishListService);
		sessionCleanupJob.setShopperCleanupService(shopperCleanupService);
		sessionCleanupJob.setBatchSizeProvider(new SimpleSettingValueProvider<>(DEFAULT_SESSION_CLEANUP_BATCH_SIZE));
		sessionCleanupJob.setMaxDaysHistoryProvider(new SimpleSettingValueProvider<>(DELETE_BEFORE_NUMBER_OF_DAYS));

		return sessionCleanupJob;
	}

	/**
	 * Tests setting up 8 different customer sessions where some sessions qualify for deletion.
	 *
	 * These are the target sessions:
	 * 1. Old Anonymous Customer Session w/ no Shopping Cart
	 * 2. Old Anonymous Customer Session w/ empty Shopping Cart
	 * 3. Recent Anonymous Customer Session w/ no Shopping Cart.
	 *
	 * Expected to delete customer sessions 1-3, 5, 8 but to keep 4, 6 and 7.
	 * Expected to keep shoppers 4, 6 - 8.  (8 because the registered customer cart kept, but not the session).
	 */
	@DirtiesDatabase
	@Test
	public void testStandard8Cases() {

		final TrackingSets track = new TrackingSets();

		// 1
		makeScenario(AccessAge.OLD, CustomerType.ANONYMOUS, CartType.NONE, "1", "A", track.deletedSessions, track.deletedShoppers);
		// 2
		makeScenario(AccessAge.OLD, CustomerType.ANONYMOUS, CartType.EMPTY, "2", "B", track.deletedSessions, track.deletedShoppers);
		// 3
		makeScenario(AccessAge.RECENT, CustomerType.ANONYMOUS, CartType.NONE, "4", "D", track.keptSessions, track.keptShoppers);

		// Verify that I can find all created customer sessions and shoppers.
		verifyCreations(track, 0);

		// Test
		sessionCleanupJob.purgeSessionHistory();

		// Verify
		verifyDeletions(track, 1);
	}

	/**
	 * Test to see that the job adheres to the Cleanup BatchSize setting.
	 *
	 * Idea is to create 15 customer sessions and to run the cleaner twice.  It should
	 * return 10 and 5 respectively.
	 *
	 * These are the target type of sessions:
	 * No. |   CS   | Shopper | Cart Empty | Recent
	 * ----+--------+---------+------------+--------
	 *  1. |    1   |    A    |    Empty   |  old
	 *
	 * Expected:
	 * Delete 10 first.
	 * Then Delete 5.
	 */
	@DirtiesDatabase
	@Test
	public void testCleanUpAdheresToBatchSize() {

		final TrackingSets track = new TrackingSets();
		final int initialSessionBatchSize = 27;
		final int cleanupBatchSize = 10;
		final int expectedLastPassSize = initialSessionBatchSize % cleanupBatchSize;

		sessionCleanupJob.setBatchSizeProvider(new SimpleSettingValueProvider<>(cleanupBatchSize));

		for (int i = 0; i < initialSessionBatchSize; i++) {
			makeScenario(AccessAge.OLD, CustomerType.ANONYMOUS, CartType.NONE, String.valueOf(i), "A", track.deletedSessions, track.deletedShoppers);
		}

		// Verify that I can find all created customer sessions and shoppers.
		verifyCreations(track, 0);

		// First delete should delete 10 items.
		int leftOverSessions = initialSessionBatchSize;
		int checkDeleteSize = cleanupBatchSize;
		int loopCount = 0;
		while (leftOverSessions > 0) {
			assertThat(cleanupBatchSize)
				.as("The delete size (%d) did not match the batch size (%d) on loop %d.", checkDeleteSize, cleanupBatchSize, loopCount)
				.isEqualTo(checkDeleteSize);
			checkDeleteSize = sessionCleanupJob.purgeSessionHistory();
			leftOverSessions -= checkDeleteSize;
			loopCount++;
		}

		// Verify
		assertThat(expectedLastPassSize)
			.as("The delete size (%d) does not match expected size (%d) on last pass.", checkDeleteSize, expectedLastPassSize)
			.isEqualTo(checkDeleteSize);
		verifyDeletions(track, 1);
	}

	private void verifyCreations(final TrackingSets tracking, final int passNumber) {
		final Set<CustomerSession> allSessions = unionSessions(tracking.deletedSessions, tracking.keptSessions);
		verifyCustomerSessionsPersistedState(getCustomerSessionExistsVerifier(), allSessions, WAS_NOT_PERSISTED, passNumber);

		final Set<Shopper> allShoppers = unionShoppers(tracking.deletedShoppers, tracking.keptShoppers);
		verifyShoppersPersistedState(getShopperExistsVerifier(), allShoppers, WAS_NOT_PERSISTED, passNumber);
	}

	/**
	 * Verifies that the right things are deleted, and the wrong things are not deleted.
	 *
	 * @param tracking tracking DTO.
	 * @param passNumber for test context.
	 */
	private void verifyDeletions(final TrackingSets tracking, final int passNumber) {
		verifyCustomerSessionsPersistedState(getCustomerSessionExistsVerifier(), tracking.keptSessions, SHOULD_NOT_BE_DELETED, passNumber);
		verifyCustomerSessionsPersistedState(getCustomerSessionNotExistsVerifier(), tracking.deletedSessions, SHOULD_HAVE_BEEN_DELETED, passNumber);
		verifyShoppersPersistedState(getShopperExistsVerifier(), tracking.keptShoppers, SHOULD_NOT_BE_DELETED, passNumber);
		verifyShoppersPersistedState(getShopperNotExistsVerifier(), tracking.deletedShoppers, SHOULD_HAVE_BEEN_DELETED, passNumber);
	}

	private Set<CustomerSession> unionSessions(final Set<CustomerSession> deletedSet, final Set<CustomerSession> keptSet) {
		final Set<CustomerSession> allSet = new HashSet<>();
		allSet.addAll(deletedSet);
		allSet.addAll(keptSet);
		return allSet;
	}

	private Set<Shopper> unionShoppers(final Set<Shopper> deletedSet, final Set<Shopper> keptSet) {
		final Set<Shopper> allSet = new HashSet<>();
		allSet.addAll(deletedSet);
		allSet.addAll(keptSet);
		return allSet;
	}

	private void verifyCustomerSessionsPersistedState(final Verifier<CustomerSession> customerSessionVerifier,
			final Set<CustomerSession> sessionSet, final String message, final int passNumber) {
		for (CustomerSession checkSession : sessionSet) {
			assertThat(customerSessionVerifier.verify(checkSession))
				.as("CustomerSession (guid:%s) (email:%s) %s - Pass %d",
					checkSession.getGuid(), checkSession.getShopper().getCustomer().getEmail(), message, passNumber)
				.isTrue();
		}
	}

	private void verifyShoppersPersistedState(final Verifier<Shopper> shopperVerifier, final Set<Shopper> shopperSet,
			final String message, final int passNumber) {
		for (Shopper shopper : shopperSet) {
			assertThat(shopperVerifier.verify(shopper))
				.as("Shopper (uid:%d) (guid:%s) %s - Pass %d", shopper.getUidPk(), shopper.getGuid(), message, passNumber)
				.isTrue();
		}
	}

	private void makeScenario(final AccessAge accessAge, final CustomerType customerType, final CartType cartType,
			final String customerSessionId, final String shopperId,
			final Set<CustomerSession> sessionSet, final Set<Shopper> shopperSet) {

		final Shopper shopper = makeShopperAndCustomer(accessAge, customerType, cartType, shopperId, shopperSet);
		makeCustomerSessionAndCart(accessAge, cartType, customerSessionId, shopper, sessionSet);
	}

	private Shopper makeShopperAndCustomer(final AccessAge accessAge, final CustomerType customerType, final CartType cartType,
			final String shopperId, final Set<Shopper> shopperSet) {

		final String customerEmail = getCustomerEmail(accessAge, customerType, cartType, shopperId);
		final Customer customer = makeCustomer(customerType, customerEmail);
		final Shopper shopper = createPersistedShopper(customer, shopperId);

		// Update tracking set for later verification.
		shopperSet.add(shopper);

		return shopper;
	}

	private String getCustomerEmail(final AccessAge accessAge, final CustomerType customerType, final CartType cartType, final String suffix) {
		final List<String> emailChunks = new ArrayList<>();
		emailChunks.add(getEmailChunk(accessAge));
		emailChunks.add(getEmailChunk(customerType));
		emailChunks.add(getEmailChunk(cartType));
		emailChunks.add(suffix);
		return buildEmail(emailChunks);
	}

	private Customer makeCustomer(final CustomerType customerType, final String customerEmail) {
		if (customerType == CustomerType.ANONYMOUS) {
			return createAnonymousCustomer(customerEmail);
		}
		return createAndSaveCustomer(customerEmail);
	}


	private Customer createAnonymousCustomer(final String email) {
		final Customer customer = getBeanFactory().getBean(ContextIdNames.CUSTOMER);
		customer.setEmail(email);
		customer.setStoreCode(getScenarioStore().getCode());
		customer.setAnonymous(true);
		return customer;
	}

	private void makeCustomerSessionAndCart(final AccessAge accessAge, final CartType cartType,
			final String customerSessionId, final Shopper shopper, final Set<CustomerSession> sessionSet) {

		final int customerAccessAge = getAccessAgeInDays(accessAge);
		final CustomerSession customerSession = createPersistedCustomerSessionWithDaysOldAndCustomer(customerSessionId,
				customerAccessAge, shopper);

		makeShoppingCart(cartType, customerSession);

		sessionSet.add(customerSession);
	}

	private CustomerSession createPersistedCustomerSessionWithDaysOldAndCustomer(final String customerSessionGuidPrefix,
			final int customerSessionDaysOld, final Shopper shopper) {
    	final CustomerSession customerSession = buildCustomerSession(shopper, customerSessionDaysOld);

    	if (customerSessionGuidPrefix != null) {
	    	customerSession.setGuid(String.format("(%s)-%s", customerSessionGuidPrefix, customerSession.getGuid()));
    	}

		customerSessionService.add(customerSession);

		return customerSession;
	}

	private CustomerSession buildCustomerSession(final Shopper shopper, final int customerSessionDaysOld) {
		final Date lastAccessedDate = convertDaysToDate(customerSessionDaysOld);
		final Date creationDate = convertDaysToDate(CUSTOMER_SESSION_CREATION_DAYS_AGO);

		final CustomerSession customerSession =
			TestCustomerSessionFactoryForTestApplication.getInstance().createNewCustomerSessionWithContext(shopper);
    	customerSession.setCreationDate(creationDate);
		customerSession.setLastAccessedDate(lastAccessedDate);
		customerSession.setCurrency(getScenarioStore().getDefaultCurrency());

		return customerSession;
	}

    private Shopper createPersistedShopper(final Customer customer, final String shopperGuidId) {
    	Shopper shopper;
    	if (customer.isAnonymous()) {
    		shopper = shopperService.createAndSaveShopper(getScenarioStore().getCode());
    	} else {
    		shopper = shopperService.findOrCreateShopper(customer, getScenarioStore().getCode());
    	}

    	if (shopperGuidId != null) {
	    	shopper.setGuid(String.format("(%s)-%s", shopperGuidId, shopper.getGuid()));
	    	shopperService.save(shopper);
    	}

    	// re-added as anonymous customers are not persisted and therefore are set to null after the Shopper is saved.
    	if (customer.isAnonymous()) {
    		shopper.setCustomer(customer);
    	}

    	return shopper;
    }

	private void makeShoppingCart(final CartType cartType, final CustomerSession customerSession) {
		if (cartType == CartType.NONE) {
			return;
		}

		final ShoppingCart shoppingCart = TestShoppingCartFactoryForTestApplication.getInstance().createNewCartWithMemento(
				customerSession.getShopper(), getScenario().getStore());
		shoppingCart.setCustomerSession(customerSession);
		if (cartType == CartType.NON_EMPTY) {
			addProductToShoppingCart(shoppingCart);
		}
		shoppingCart.setDefault(true);
		shoppingCartService.saveOrUpdate(shoppingCart);
	}

	private Customer createAndSaveCustomer(final String email) {
		final Customer customer = getBeanFactory().getBean(ContextIdNames.CUSTOMER);
		customer.setUserId(email);
		customer.setEmail(email);
		customer.setStoreCode(getScenarioStore().getCode());
		customer.setAnonymous(false);

		customerService.add(customer);

		return customer;
	}

	private Store getScenarioStore() {
		SimpleStoreScenario scenario = (SimpleStoreScenario) getTac().getScenario(SimpleStoreScenario.class);
		return scenario.getStore();
	}

	private void addProductToShoppingCart(final ShoppingCart shoppingCart) {
		// Add a default Product.
		ShoppingItemDto dto = new ShoppingItemDto(defaultProduct.getDefaultSku().getSkuCode(), 1);
		cartDirector.addItemToCart(shoppingCart, dto);
	}

    private boolean verifyCustomerSessionExists(final CustomerSession customerSession) {
		return customerSessionCleanupService.checkPersistedCustomerSessionGuidExists(customerSession.getGuid());
    }

    private boolean verifyShopperExists(final Shopper shopper) {
		return shopper.isPersisted() && shopperService.get(shopper.getUidPk()) != null;
	}

    // I wish Java had delegates. =(
    private Verifier<CustomerSession> getCustomerSessionExistsVerifier() {
		return this::verifyCustomerSessionExists;
    }

    private Verifier<CustomerSession> getCustomerSessionNotExistsVerifier() {
		return customerSession -> !verifyCustomerSessionExists(customerSession);
    }

    private Verifier<Shopper> getShopperExistsVerifier() {
		return shopper -> verifyShopperExists(shopper);
    }

    private Verifier<Shopper> getShopperNotExistsVerifier() {
		return shopper -> !verifyShopperExists(shopper);
    }

    private int getAccessAgeInDays(final AccessAge age) {
    	switch(age) {
	    	case OLD:
	    		return OLD_CUSTOMER_SESSION_ACCESSED_DAYS_AGO;
	    	case RECENT:
	    		return RECENT_CUSTOMER_SESSION_ACCESSED_DAYS_AGO;
	    	default:
	    		return 0;
    	}
    }

    private String getEmailChunk(final AccessAge age) {
    	switch(age) {
	    	case OLD:
	    		return "Old";
	    	case RECENT:
	    		return "Recent";
	    	case MULTIAGED:
	    		return "Multiaged";
	    	default:
	    		return null;
    	}
    }

    private String getEmailChunk(final CartType cartType) {
    	switch(cartType) {
	    	case NONE:
	    		return "No-Shopping-Cart";
	    	case EMPTY:
	    		return "Empty-Shopping-Cart";
	    	case NON_EMPTY:
	    		return "Non-Empty-Shopping-Cart";
	    	default:
	    		return null;
    	}
    }

    private String getEmailChunk(final CustomerType customerType) {
    	switch(customerType) {
	    	case ANONYMOUS:
	    		return "Anon";
	    	case REGISTERED:
	    		return "Reg";
	    	default:
	    		return null;
    	}
    }

    private String buildEmail(final List<String> emailChunks) {
    	return String.format("%s@test.com", StringUtils.join(emailChunks, '-'));
    }

	private Date convertDaysToDate(final int days) {
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, -days);
		return calendar.getTime();
	}
}

