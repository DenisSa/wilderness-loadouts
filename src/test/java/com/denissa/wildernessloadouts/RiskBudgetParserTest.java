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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RiskBudgetParserTest
{
	@Test
	public void parsesRuneScapeStyleValues()
	{
		assertEquals(0L, RiskBudgetParser.parse("0"));
		assertEquals(500L, RiskBudgetParser.parse("500"));
		assertEquals(50_000L, RiskBudgetParser.parse("50k"));
		assertEquals(500_000L, RiskBudgetParser.parse("500K"));
		assertEquals(1_000_000L, RiskBudgetParser.parse("1m"));
		assertEquals(1_500_000L, RiskBudgetParser.parse("1.5m"));
	}

	@Test
	public void rejectsInvalidValues()
	{
		assertInvalid("-1");
		assertInvalid("1.5");
		assertInvalid("1.0001k");
		assertInvalid("500 thousand");
		assertInvalid("");
		assertInvalid("9223372036854775808");
	}

	private static void assertInvalid(String value)
	{
		try
		{
			RiskBudgetParser.parse(value);
			fail("Expected invalid budget: " + value);
		}
		catch (IllegalArgumentException expected)
		{
			// Expected.
		}
	}
}
