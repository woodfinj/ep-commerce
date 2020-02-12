/**
 * Copyright (c) Elastic Path Software Inc., 2013
 */
package com.elasticpath.service.search.solr;

import java.util.Collection;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;

import com.elasticpath.persistence.api.EpPersistenceException;
import com.elasticpath.service.search.IndexType;

/**
 * Service to handle SOLR object delegation.
 */
public interface SolrManager extends SolrProvider {

	/**
	 * Adds the given document to index.
	 *
	 * @param client the client to operate on
	 * @param document the document to add into/update index
	 * @throws EpPersistenceException in case of any errors
	 */
	void addUpdateDocument(SolrClient client,
			SolrInputDocument document) throws EpPersistenceException;

	/**
	 * Adds the given collection of documents to the index.
	 *
	 * @param client the client to operate on
	 * @param documents the collection of documents to add into/update the index
	 * @throws EpPersistenceException in case of any errors
	 */
	void addUpdateDocument(SolrClient client,
			Collection<SolrInputDocument> documents)
			throws EpPersistenceException;

	/**
	 * Deletes the document specified by the UID.
	 *
	 * @param client the client to operate on
	 * @param uid the UID of the object
	 * @throws EpPersistenceException in case of any errors
	 */
	void deleteDocument(SolrClient client, long uid)
			throws EpPersistenceException;

	/**
	 * Flushes (writes) changes to the index.
	 *
	 * @param client the client to operate on
	 * @param optimize whether to optimize the index changes when closing
	 * @throws EpPersistenceException in case of any errors
	 */
	void flushChanges(SolrClient client,
			boolean optimize) throws EpPersistenceException;

	/**
	 * Rebuilds the spelling index.
	 *
	 * @param client the client to operate on
	 * @throws EpPersistenceException in case of any errors
	 */
	void rebuildSpelling(SolrClient client) throws EpPersistenceException;

	/**
	 * Obtain the {@code SolrDocumentPublisher} which will be used to publish {@code SolrInputDocument}s
	 * to the appropriate Solr client.
	 * @param indexType {@link IndexType} of the publisher you want to obtain
	 * @return the associated publisher.
	 */
	SolrDocumentPublisher getDocumentPublisher(IndexType indexType);
}