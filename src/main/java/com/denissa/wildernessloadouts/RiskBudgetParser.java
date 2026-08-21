/*
 * Copyright (c) 2026, DenisSa
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.denissa.wildernessloadouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RiskBudgetParser
{
	private static final Pattern VALUE_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([km]?)");
	private static final BigDecimal LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);

	private RiskBudgetParser()
	{
	}

	public static long parse(String input)
	{
		if (input == null)
		{
			throw new IllegalArgumentException("Enter a filler-risk budget");
		}

		String normalized = input.trim().toLowerCase(Locale.ENGLISH);
		Matcher matcher = VALUE_PATTERN.matcher(normalized);
		if (!matcher.matches())
		{
			throw new IllegalArgumentException("Use a value such as 500k or 1.5m");
		}

		String suffix = matcher.group(2);
		if (matcher.group(1).contains(".") && suffix.isEmpty())
		{
			throw new IllegalArgumentException("Decimals require a k or m suffix");
		}

		BigDecimal multiplier = BigDecimal.ONE;
		if ("k".equals(suffix))
		{
			multiplier = BigDecimal.valueOf(1_000L);
		}
		else if ("m".equals(suffix))
		{
			multiplier = BigDecimal.valueOf(1_000_000L);
		}

		try
		{
			BigDecimal value = new BigDecimal(matcher.group(1)).multiply(multiplier)
				.setScale(0, RoundingMode.UNNECESSARY);
			if (value.compareTo(LONG_MAX) > 0)
			{
				throw new IllegalArgumentException("Budget is too large");
			}
			return value.longValueExact();
		}
		catch (ArithmeticException exception)
		{
			throw new IllegalArgumentException("Budget must resolve to whole GP", exception);
		}
	}
}
