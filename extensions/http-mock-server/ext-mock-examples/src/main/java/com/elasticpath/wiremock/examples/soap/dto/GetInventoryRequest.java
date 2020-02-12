/*
 * Copyright © 2019 Elastic Path Software Inc. All rights reserved.
 */
package com.elasticpath.wiremock.examples.soap.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A top level DTO that models a request to {@link com.elasticpath.wiremock.examples.soap.DynamicSoapResponseDefinitionTransformer}.
 *
 */
@XmlRootElement(name = "GetInventoryRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetInventoryRequest {

	@XmlElement(name = "InventoryLine")
	private List<InventoryLine> inventoryLine;

	public List<InventoryLine> getInventoryLine() {
		return inventoryLine;
	}

	public void setInventoryLine(final List<InventoryLine> inventoryLine) {
		this.inventoryLine = inventoryLine;
	}
}
