/*
 * Copyright © 2018 Elastic Path Software Inc. All rights reserved.
 */
package com.elasticpath.rest.resource.coupons.prototype;

import io.reactivex.Single;

import com.elasticpath.rest.ResourceInfo;
import com.elasticpath.rest.definition.coupons.PurchaseCouponListResource;
import com.elasticpath.rest.resource.coupons.constants.CouponsResourceConstants;

/**
 * Purchase Coupon List prototype for Info operation.
 */
public class InfoPurchaseCouponListPrototype implements PurchaseCouponListResource.Info {

	private static final Single<ResourceInfo> INFO_SINGLE = Single.just(ResourceInfo.builder()
			.withMaxAge(CouponsResourceConstants.MAX_AGE)
			.build());

	@Override
	public Single<ResourceInfo> onInfo() {
		return INFO_SINGLE;
	}
}
