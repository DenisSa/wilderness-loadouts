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

public final class LoadoutSlotSelection
{
	private final SlotState state;
	private final Integer lockedItemId;

	private LoadoutSlotSelection(SlotState state, Integer lockedItemId)
	{
		this.state = Objects.requireNonNull(state);
		this.lockedItemId = lockedItemId;
	}

	public static LoadoutSlotSelection auto()
	{
		return new LoadoutSlotSelection(SlotState.AUTO, null);
	}

	public static LoadoutSlotSelection empty()
	{
		return new LoadoutSlotSelection(SlotState.EMPTY, null);
	}

	public static LoadoutSlotSelection locked(int itemId)
	{
		if (itemId < 0)
		{
			throw new IllegalArgumentException("A locked item ID must be non-negative");
		}
		return new LoadoutSlotSelection(SlotState.LOCKED, itemId);
	}

	public SlotState getState()
	{
		return state;
	}

	public Integer getLockedItemId()
	{
		return lockedItemId;
	}
}
