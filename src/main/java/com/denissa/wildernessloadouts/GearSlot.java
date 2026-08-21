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

public enum GearSlot
{
	HEAD("Head", 1),
	CAPE("Cape", 8),
	NECK("Neck", 9),
	AMMO("Ammo", 10),
	WEAPON("Weapon", 16),
	BODY("Body", 17),
	SHIELD("Shield", 18),
	LEGS("Legs", 25),
	GLOVES("Gloves", 32),
	BOOTS("Boots", 33),
	RING("Ring", 34);

	private final String displayName;
	private final int bankLayoutPosition;

	GearSlot(String displayName, int bankLayoutPosition)
	{
		this.displayName = displayName;
		this.bankLayoutPosition = bankLayoutPosition;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getBankLayoutPosition()
	{
		return bankLayoutPosition;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
