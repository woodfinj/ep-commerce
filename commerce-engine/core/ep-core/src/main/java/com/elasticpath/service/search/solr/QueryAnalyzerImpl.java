/**
 * Copyright (c) Elastic Path Software Inc., 2014
 */
package com.elasticpath.service.search.solr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;

import com.elasticpath.commons.util.impl.StringEscapeUtilityImpl;

/**
 * Provides analyzing tools for <code>QueryComposer</code>s. Provides a bug-fix for Solr
 * thinking a space is a delimiter. I.e. the query "productName:canon camera" will be parsed as
 * "productName:canon &lt;defaultField&gt;:camera".
 */
public class QueryAnalyzerImpl extends AnalyzerImpl {
	/**
	 * Quote char.
	 */
	protected static final char QUOTE_CHAR = '"';

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s", Pattern.MULTILINE);
	private static final Pattern UNESCAPED_QUOTE_PATTERN = Pattern.compile("(^\")|([^\\\\][\"])", Pattern.MULTILINE);
	private static final char[] ILLEGAL_END_CHARS = new char[] { '\\', ';' };

	private static final String[] ILLEGAL_CHARACTERS = new String[] {
			";",
			"-",
			"(",
			")",
			"[",
			"]",
			"{",
			"}",
			"^",
			"\"",
			"~",
			":"

	};
	private static final Analyzer ANALYZER = new SimpleAnalyzer();

	private static final ThreadLocal<QueryParser> QUERY_PARSER = new ThreadLocal<>();

	@Override
	public String analyze(final String value) {
		return analyze(value, false);
	}

	@Override
	public String analyze(final String value, final boolean isEscapeAllQuotes) {
		String result = super.analyze(value, isEscapeAllQuotes);
		if ("".equals(result)) {
			return result;
		}
		return parseResult(result);
	}

	/**
	 * Analyze and parse the given string. Returns a trimmed instance of the string if not null, otherwise
	 * the empty string.  Also escapes all quotes in the string.
	 *
	 * @param string the string value to analyze
	 * @return the analyzed parsed text
	 */
	private String parseResult(final String string) {
		String workingString = string;
		for (int i = 0; i < ILLEGAL_END_CHARS.length; ++i) {
			while (workingString.endsWith(String.valueOf(ILLEGAL_END_CHARS[i]))) {
				workingString = workingString.substring(0, workingString.length() - 1);

				// we need to reset to beginning because we may have passed an illegal character
				// (by the order of the illegal chars array)
				i = 0;
			}
		}

		if (StringUtils.isBlank(workingString)) {
			// hack to not fail searching when all illegal chars have been removed
			return String.valueOf(QUOTE_CHAR) + ' ' + QUOTE_CHAR;
		}

		boolean hasEscapedcharacters = checkForEscapeCharacters(workingString);

		// special case for unbalanced unescaped quotes since we quote the string
		final Matcher matcher = UNESCAPED_QUOTE_PATTERN.matcher(workingString);
		int count = 0;
		while (matcher.find()) {
			++count;
		}

		if (count % 2 != 0) {
			workingString = StringEscapeUtilityImpl.escapeUnescapedQuotes(workingString);
		}

		// wire optimization to not use quotes if we don't need to,
		// only need quotes if we have whitespace
		if (!hasEscapedcharacters && !WHITESPACE_PATTERN.matcher(workingString).find()) {
			return workingString;
		}

		return QUOTE_CHAR + workingString + QUOTE_CHAR;

	}

	private boolean checkForEscapeCharacters(final String workingString) {
		for (String illegalCharacter: ILLEGAL_CHARACTERS) {
			if (workingString.contains(illegalCharacter)) {
				int indexOfIllegalCharacter = workingString.indexOf(illegalCharacter);
				if (indexOfIllegalCharacter == 0) {
					return true;
				}
				if (workingString.charAt(indexOfIllegalCharacter - 1) != '\\') {
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * Get parser object.
	 * @return parser object
	 */
	protected QueryParser getParser() {
		if (QUERY_PARSER.get() == null) {
			QUERY_PARSER.set(new QueryParser("text", ANALYZER));
		}
		return QUERY_PARSER.get();
	}
}
