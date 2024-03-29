/**
 * Copyright (c) Elastic Path Software Inc., 2016
 */
package com.elasticpath.importexport.common.adapters.products.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.elasticpath.common.dto.DisplayValue;
import com.elasticpath.domain.attribute.Attribute;
import com.elasticpath.domain.attribute.AttributeType;
import com.elasticpath.domain.attribute.AttributeValue;
import com.elasticpath.domain.attribute.AttributeValueFactory;
import com.elasticpath.domain.attribute.AttributeValueGroup;
import com.elasticpath.domain.attribute.impl.ProductAttributeValueImpl;
import com.elasticpath.domain.catalog.Catalog;
import com.elasticpath.importexport.common.caching.CachingService;
import com.elasticpath.importexport.common.dto.products.AttributeValuesDTO;
import com.elasticpath.validation.service.ValidatorUtils;

/**
 * Test class for {@link AttributeValuesAdapter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AttributeValuesAdapterTest {

	@Mock
	private CachingService cachingService;

	@Mock
	private ValidatorUtils validatorUtils;

	@Mock
	private AttributeValueGroup attributeValueGroup;

	@Mock
	private AttributeValueFactory attributeValueFactory;

	@InjectMocks
	private AttributeValuesAdapter adapterUnderTest;

	private static final String[] SAMPLE_LANGUAGE_TAGS = new String[]{
			"sr-Latn-RS",
			"en-CA",
			"fr-CA",
			"fr",
			"en"
	};

	@Before
	public void setUp() {
		when(attributeValueGroup.getAttributeValueFactory()).thenReturn(attributeValueFactory);
	}

	@Test
	public void verifyLocaleDependentAttributeValuesAddedToDto() {
		final String attributeName = "attribute_name";

		final Attribute attribute = createAttribute(attributeName);
		givenAttributeIsLocaleDependent(attribute);

		final Collection<AttributeValue> attributeValues = createAttributeValuesForLanguageTags(attribute, attributeName, SAMPLE_LANGUAGE_TAGS);

		final AttributeValuesDTO dto = new AttributeValuesDTO();

		adapterUnderTest.populateDTO(attributeValues, dto);

		final List<DisplayValue> displayValues = dto.getValues();

		for (final String languageTag : SAMPLE_LANGUAGE_TAGS) {
			assertThat(displayValues.stream())
					.as("No display language found for supported language tag")
					.anyMatch(displayValue -> languageTag.equals(displayValue.getLanguage()));
		}

		verifyZeroInteractions(attributeValueGroup);
		verifyZeroInteractions(cachingService);
		verifyZeroInteractions(attributeValueFactory);
		verifyZeroInteractions(validatorUtils);
	}

	@Test
	public void verifyLocaleDependentAttributeValuesNotAddedToDtoWhenAttributeNotLocaleDependent() {
		final String attributeName = "attribute_name";
		final Attribute attribute = createAttribute(attributeName);

		givenAttributeIsNotLocaleDependent(attribute);

		final Collection<AttributeValue> attributeValues = createAttributeValuesForLanguageTags(attribute, attributeName);

		final AttributeValuesDTO dto = new AttributeValuesDTO();

		adapterUnderTest.populateDTO(attributeValues, dto);

		final List<DisplayValue> displayValues = dto.getValues();

		assertThat(displayValues).as("Expected a single, locale-less value when the attribute key does not contain locale info").hasSize(1);

		final Optional<DisplayValue> displayValue = displayValues.stream().findFirst();

		assertThat(displayValue).isPresent();
		assertThat(displayValue.get().getLanguage())
				.as("Expected the attribute value not to have a language property when the attribute key does not contain locale info")
				.isNull();

		verifyZeroInteractions(cachingService);
		verifyZeroInteractions(attributeValueFactory);
		verifyZeroInteractions(validatorUtils);
		verifyZeroInteractions(attributeValueGroup);
	}

	@Test
	public void verifyNonLocaleDependentAttributeValuesPopulatedToDtoWithoutLanguage() {
		final String[] unlocalisedAttributeKeys = {
			"I'm not a locale",
			"Neither am I.",
			"foooo-baaar",
			"attribute-name",
			"attributename",
			"attributenameen",
			"attributenameen-CA",
			"attribute-en-CA-name"
		};

		for (final String key : unlocalisedAttributeKeys) {
			final Attribute attribute = createAttribute(key);
			givenAttributeIsNotLocaleDependent(attribute);

			final Collection<AttributeValue> attributeValues = createAttributeValuesForLanguageTags(attribute, key);

			final AttributeValuesDTO dto = new AttributeValuesDTO();

			adapterUnderTest.populateDTO(attributeValues, dto);

			final List<DisplayValue> displayValues = dto.getValues();

			assertThat(displayValues)
					.as("Expected a single, locale-less value when the attribute key does not contain locale info")
					.hasSize(1);

			final Optional<DisplayValue> displayValue = displayValues.stream().findFirst();

			assertThat(displayValue).isPresent();
			assertThat(displayValue.get().getLanguage())
					.as("Expected the attribute value not to have a language property when the attribute key does not contain locale info")
					.isNull();

			verifyZeroInteractions(cachingService);
			verifyZeroInteractions(attributeValueFactory);
			verifyZeroInteractions(validatorUtils);
			verifyZeroInteractions(attributeValueGroup);
		}


	}

	@Test
	public void verifyDtoWithoutDisplayLanguagePopulatedToDomainWithoutLanguage() {
		final String attributeKey = "attribute.name";

		final List<DisplayValue> displayValues = Collections.singletonList(new DisplayValue(null, "val1"));

		final AttributeValuesDTO dto = new AttributeValuesDTO();
		dto.setKey(attributeKey);
		dto.setValues(displayValues);

		final Attribute attribute = createAttribute(attributeKey);

		when(attributeValueGroup.getAttributeValue(attributeKey, null)).thenReturn(null);
		when(attributeValueFactory.createAttributeValue(attribute, attributeKey))
			.thenReturn(createAttributeValuePrototype(attribute, attributeKey));

		final Collection<AttributeValue> actualAttributeValues = new ArrayList<>();
		adapterUnderTest.populateDomain(dto, actualAttributeValues);

		for (final DisplayValue displayValue : displayValues) {
			assertThat(actualAttributeValues.stream())
					.as("No attribute value found")
					.anyMatch(attributeValue -> displayValue.getValue().equals(attributeValue.getStringValue())
							&& attributeKey.equals(attributeValue.getLocalizedAttributeKey()));
		}

		verify(cachingService).findAttribiteByKey(attributeKey);
		verify(attributeValueFactory).createAttributeValue(attribute, attributeKey);
		verify(validatorUtils).validateAttributeValue(any(AttributeValue.class));
		verify(attributeValueGroup).getAttributeValue(attributeKey, null);
	}

	@Test
	public void verifyLocalisedAttributeValueDomainObjsAreCreatedUsingLanguageTag() {
		final AttributeValuesDTO dto = new AttributeValuesDTO();

		final String attributeKey = "attribute.name";
		final int numOfTestLocaleStrings = SAMPLE_LANGUAGE_TAGS.length;

		dto.setKey(attributeKey);

		final Collection<Locale> catalogSupportedLocales = new ArrayList<>();
		final List<DisplayValue> displayValues = new ArrayList<>();
		final Collection<String> expectedLocalisedAttributeKeys = new ArrayList<>();
		final Attribute attribute = createAttribute(attributeKey);

		for (final String languageTag : SAMPLE_LANGUAGE_TAGS) {
			final Locale locale = Locale.forLanguageTag(languageTag);

			catalogSupportedLocales.add(locale);

			final String localisedAttributeKey = attributeKey + "_" + locale;
			expectedLocalisedAttributeKeys.add(localisedAttributeKey);

			displayValues.add(new DisplayValue(languageTag, languageTag + ".value"));

			when(attributeValueGroup.getAttributeValue(attributeKey, locale)).thenReturn(null);
			when(attributeValueFactory.createAttributeValue(attribute, localisedAttributeKey))
					.thenReturn(createAttributeValuePrototype(attribute, localisedAttributeKey));
		}

		givenAttributeHasCatalogWithSupportedLocales(attribute, catalogSupportedLocales);

		dto.setValues(displayValues);

		final Collection<AttributeValue> actualAttributeValues = new ArrayList<>();

		adapterUnderTest.populateDomain(dto, actualAttributeValues);

		for (final String expectedLocalisedAttributeKey : expectedLocalisedAttributeKeys) {
			assertThat(actualAttributeValues.stream())
					.as("No attribute value found for locale")
					.anyMatch(attributeValue -> expectedLocalisedAttributeKey.equals(attributeValue.getLocalizedAttributeKey()));
		}

		verify(cachingService).findAttribiteByKey(attributeKey);
		verify(attributeValueFactory, times(numOfTestLocaleStrings)).createAttributeValue(eq(attribute), startsWith(attributeKey));
		verify(validatorUtils, times(numOfTestLocaleStrings)).validateAttributeValue(any(AttributeValue.class));
		verify(attributeValueGroup, times(numOfTestLocaleStrings)).getAttributeValue(eq(attributeKey), any(Locale.class));
	}

	@Test
	public void verifyLocalesNotSupportedByCatalogAreSkippedWhenCreatingDomainObjs() {
		final AttributeValuesDTO dto = new AttributeValuesDTO();

		final String attributeKey = "attribute.name";

		dto.setKey(attributeKey);

		final List<DisplayValue> displayValues = new ArrayList<>();
		final Attribute attribute = createAttribute(attributeKey);
		final String testLocaleString = "en-CA";

		displayValues.add(new DisplayValue(testLocaleString, testLocaleString + ".value"));

		givenAttributeHasCatalogWithSupportedLocales(attribute, Collections.emptyList());

		dto.setValues(displayValues);

		final Collection<AttributeValue> actualAttributeValues = new ArrayList<>();

		adapterUnderTest.populateDomain(dto, actualAttributeValues);

		assertThat(actualAttributeValues)
				.as("Expected no attribute values to be produced when the locale is not supported by the catalog")
				.isEmpty();

		verify(cachingService).findAttribiteByKey(attributeKey);
		verifyZeroInteractions(attributeValueFactory);
		verifyZeroInteractions(validatorUtils);
		verifyZeroInteractions(attributeValueGroup);
	}

	private Attribute createAttribute(final String attributeName) {
		final Attribute attribute = Mockito.mock(Attribute.class, "Attr." + UUID.randomUUID());

		when(attribute.getKey()).thenReturn(attributeName);
		when(attribute.isMultiValueEnabled()).thenReturn(false);
		when(cachingService.findAttribiteByKey(attributeName)).thenReturn(attribute);

		return attribute;
	}

	private Collection<AttributeValue> createAttributeValuesForLanguageTags(final Attribute attribute, final String attributeName,
																			final String... localeLanguageTags) {
		if (localeLanguageTags.length == 0) {
			return Collections.singleton(createAttributeMock(attribute, attributeName, null));
		}

		final Collection<AttributeValue> attributeValues = new ArrayList<>();

		for (final String localeLanguageTag : localeLanguageTags) {
			final AttributeValue attributeValue = createAttributeMock(attribute, attributeName, localeLanguageTag);

			attributeValues.add(attributeValue);
		}

		return attributeValues;
	}

	private AttributeValue createAttributeMock(final Attribute attribute, final String attributeName, final String localeLanguageTag) {
		final AttributeValue attributeValue = Mockito.mock(AttributeValue.class, "AttrVal." + UUID.randomUUID());

		if (localeLanguageTag != null) {
			when(attributeValue.getLocalizedAttributeKey()).thenReturn(attributeName + "_" + localeLanguageTag);
		}

		final String stringValue = UUID.randomUUID().toString();

		when(attributeValue.getAttribute()).thenReturn(attribute);
		when(attributeValue.getValue()).thenReturn(stringValue);
		when(attributeValue.getStringValue()).thenReturn(stringValue);

		return attributeValue;
	}

	private void givenAttributeHasCatalogWithSupportedLocales(final Attribute attribute, final Collection<Locale> catalogSupportedLocales) {
		final Catalog catalog = Mockito.mock(Catalog.class, UUID.randomUUID().toString());

		when(attribute.getCatalog()).thenReturn(catalog);
		when(catalog.getSupportedLocales()).thenReturn(catalogSupportedLocales);
	}

	private void givenAttributeIsLocaleDependent(final Attribute attribute) {
		when(attribute.isLocaleDependant()).thenReturn(true);
	}

	private void givenAttributeIsNotLocaleDependent(final Attribute attribute) {
		when(attribute.isLocaleDependant()).thenReturn(false);
	}

	private AttributeValue createAttributeValuePrototype(final Attribute attribute, final String localisedKey) {
		final ProductAttributeValueImpl productAttributeValue = new ProductAttributeValueImpl();
		productAttributeValue.setAttribute(attribute);
		productAttributeValue.setLocalizedAttributeKey(localisedKey);
		productAttributeValue.setAttributeType(AttributeType.SHORT_TEXT);

		return productAttributeValue;
	}

}