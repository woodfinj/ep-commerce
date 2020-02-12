/**
 * Copyright (c) Elastic Path Software Inc., 2013
 */
package com.elasticpath.search.solr;

import org.apache.lucene.search.SortField;
import org.apache.solr.schema.DoublePointField;
import org.apache.solr.schema.SchemaField;


/**
 * Custom sort field for sorting by price with price lists. Necessary to allow
 * PriceComparatorSource to do the sorting. 
 */
public class ProductPriceSortField extends DoublePointField {
	@Override
	public SortField getSortField(final SchemaField field, final boolean reverse) {
		return new SortField(field.getName(), new PriceComparatorSource(), reverse);
	}
	
}
