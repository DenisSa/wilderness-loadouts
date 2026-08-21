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

import java.util.Objects;

/**
 * Player-facing replacement liability for one physical item in the plugin's
 * deep-Wilderness planning scenario.
 */
public final class LossProfile
{
	public enum ReacquisitionMethod
	{
		MARKET("market"),
		QUEST_SHOP("quest shop"),
		PERDU("Perdu"),
		NPC_REPLACEMENT("NPC replacement"),
		TROUVER_REPAIR("Trouver relock"),
		SPECIAL_RULE("special rule"),
		UNKNOWN("unknown");

		private final String displayName;

		ReacquisitionMethod(String displayName)
		{
			this.displayName = displayName;
		}

		public String getDisplayName()
		{
			return displayName;
		}
	}

	public enum NonMonetaryBurden
	{
		NONE(""),
		CHARGES("charges must be restored"),
		REACQUISITION_TIME("requires reacquisition time"),
		MINIGAME_PROGRESS("requires minigame progress");

		private final String displayName;

		NonMonetaryBurden(String displayName)
		{
			this.displayName = displayName;
		}

		public String getDisplayName()
		{
			return displayName;
		}
	}

	public enum Confidence
	{
		EXACT,
		MARKET_ESTIMATE,
		UNKNOWN
	}

	public enum EligibilityPolicy
	{
		AUTO(null),
		REPLACEMENT_UNKNOWN("replacement value unknown"),
		TIME_ONLY("reacquisition time is not valued"),
		UNLOCKED_TROUVER("apply a Trouver parchment first"),
		LEGACY_TROUVER("legacy locked variant is not supported");

		private final String exclusionReason;

		EligibilityPolicy(String exclusionReason)
		{
			this.exclusionReason = exclusionReason;
		}

		public String getExclusionReason()
		{
			return exclusionReason;
		}
	}

	private final long costIfUnprotected;
	private final long costIfProtected;
	private final boolean canBeProtected;
	private final ReacquisitionMethod reacquisitionMethod;
	private final NonMonetaryBurden nonMonetaryBurden;
	private final Confidence confidence;
	private final EligibilityPolicy eligibilityPolicy;
	private final String source;

	private LossProfile(
		long costIfUnprotected,
		long costIfProtected,
		boolean canBeProtected,
		ReacquisitionMethod reacquisitionMethod,
		NonMonetaryBurden nonMonetaryBurden,
		Confidence confidence,
		EligibilityPolicy eligibilityPolicy,
		String source)
	{
		this.costIfUnprotected = Math.max(0, costIfUnprotected);
		this.costIfProtected = Math.max(0, costIfProtected);
		this.canBeProtected = canBeProtected;
		this.reacquisitionMethod = Objects.requireNonNull(reacquisitionMethod);
		this.nonMonetaryBurden = Objects.requireNonNull(nonMonetaryBurden);
		this.confidence = Objects.requireNonNull(confidence);
		this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy);
		this.source = Objects.requireNonNull(source);
	}

	public static LossProfile market(long marketPrice)
	{
		if (marketPrice <= 0)
		{
			return unknown();
		}
		return estimated(
			marketPrice,
			0,
			true,
			ReacquisitionMethod.MARKET,
			NonMonetaryBurden.NONE,
			"RuneLite market price");
	}

	static LossProfile estimate(long riskValue)
	{
		return estimated(
			Math.max(0, riskValue),
			0,
			true,
			ReacquisitionMethod.MARKET,
			NonMonetaryBurden.NONE,
			"Estimated replacement value");
	}

	public static LossProfile estimated(
		long costIfUnprotected,
		long costIfProtected,
		boolean canBeProtected,
		ReacquisitionMethod method,
		NonMonetaryBurden burden,
		String source)
	{
		return new LossProfile(
			costIfUnprotected,
			costIfProtected,
			canBeProtected,
			method,
			burden,
			Confidence.MARKET_ESTIMATE,
			EligibilityPolicy.AUTO,
			source);
	}

	public static LossProfile exact(
		long costIfUnprotected,
		long costIfProtected,
		boolean canBeProtected,
		ReacquisitionMethod method,
		NonMonetaryBurden burden,
		String source)
	{
		return new LossProfile(
			costIfUnprotected,
			costIfProtected,
			canBeProtected,
			method,
			burden,
			Confidence.EXACT,
			EligibilityPolicy.AUTO,
			source);
	}

	public static LossProfile excluded(
		EligibilityPolicy policy,
		NonMonetaryBurden burden,
		String source)
	{
		if (policy == EligibilityPolicy.AUTO)
		{
			throw new IllegalArgumentException("An excluded loss profile requires an exclusion policy");
		}
		return new LossProfile(
			0,
			0,
			false,
			ReacquisitionMethod.UNKNOWN,
			burden,
			Confidence.UNKNOWN,
			policy,
			source);
	}

	public static LossProfile unknown()
	{
		return excluded(
			EligibilityPolicy.REPLACEMENT_UNKNOWN,
			NonMonetaryBurden.NONE,
			"No authoritative replacement value");
	}

	public long getCostIfUnprotected()
	{
		return costIfUnprotected;
	}

	public long getCostIfProtected()
	{
		return costIfProtected;
	}

	public long getCost(boolean protectedItem)
	{
		return protectedItem ? costIfProtected : costIfUnprotected;
	}

	public boolean canBeProtected()
	{
		return canBeProtected;
	}

	public ReacquisitionMethod getReacquisitionMethod()
	{
		return reacquisitionMethod;
	}

	public NonMonetaryBurden getNonMonetaryBurden()
	{
		return nonMonetaryBurden;
	}

	public Confidence getConfidence()
	{
		return confidence;
	}

	public EligibilityPolicy getEligibilityPolicy()
	{
		return eligibilityPolicy;
	}

	public String getSource()
	{
		return source;
	}

	public boolean isAutoEligible()
	{
		return eligibilityPolicy == EligibilityPolicy.AUTO;
	}

	public boolean isMonetaryValueKnown()
	{
		return confidence != Confidence.UNKNOWN;
	}

	public boolean hasNonMonetaryBurden()
	{
		return nonMonetaryBurden != NonMonetaryBurden.NONE;
	}
}
