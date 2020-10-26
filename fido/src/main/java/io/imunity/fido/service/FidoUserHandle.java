/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

/**
 * Randomly generated byte array, uniquely identifying user.
 * According to Fido recommendations it should be 64 bytes.
 *
 * @author R. Ledzinski
 */
class FidoUserHandle
{
	final byte[] value;

	public FidoUserHandle(final byte[] value)
	{
		this.value = copyArray(value);
	}

	private FidoUserHandle()
	{
		this.value = new byte[64];
		new Random().nextBytes(value);
	}

	public static FidoUserHandle fromString(final String stringValue)
	{
		return new FidoUserHandle(Base64.getDecoder().decode(stringValue));
	}

	public String asString()
	{
		return Base64.getEncoder().encodeToString(value);
	}

	public byte[] getBytes()
	{
		return copyArray(value);
	}

	public static FidoUserHandle create()
	{
		return new FidoUserHandle();
	}

	private byte[] copyArray(final byte[] value)
	{
		return Arrays.copyOf(value, value.length);
	}
}