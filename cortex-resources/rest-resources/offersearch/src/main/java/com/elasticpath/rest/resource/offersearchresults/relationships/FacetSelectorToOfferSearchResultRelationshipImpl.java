/*
 * Copyright © 2018 Elastic Path Software Inc. All rights reserved.
 */
package com.elasticpath.rest.resource.offersearchresults.relationships;

import javax.inject.Inject;

import io.reactivex.Observable;

import com.elasticpath.rest.definition.offersearches.FacetSelectorIdentifier;
import com.elasticpath.rest.definition.offersearches.FacetSelectorToOfferSearchResultRelationship;
import com.elasticpath.rest.definition.offersearches.OfferSearchResultIdentifier;
import com.elasticpath.rest.helix.data.annotation.RequestIdentifier;
import com.elasticpath.rest.id.type.IntegerIdentifier;

/**
 * Link from facet selector to offer search result.
 */
public class FacetSelectorToOfferSearchResultRelationshipImpl implements FacetSelectorToOfferSearchResultRelationship.LinkTo {

	private final FacetSelectorIdentifier facetSelectorIdentifier;

	/**
	 * Constructor.
	 * @param facetSelectorIdentifier identifier
	 */
	@Inject
	public FacetSelectorToOfferSearchResultRelationshipImpl(@RequestIdentifier final FacetSelectorIdentifier facetSelectorIdentifier) {
		this.facetSelectorIdentifier = facetSelectorIdentifier;
	}

	@Override
	public Observable<OfferSearchResultIdentifier> onLinkTo() {
		return Observable.just(OfferSearchResultIdentifier.builder()
				.withAppliedFacets(facetSelectorIdentifier.getFacet().getFacets().getAppliedFacets())
				.withPageId(IntegerIdentifier.of(1))
				.withScope(facetSelectorIdentifier.getFacet().getFacets().getScope())
				.withSearchId(facetSelectorIdentifier.getFacet().getFacets().getSearchId())
				.build());
	}
}
