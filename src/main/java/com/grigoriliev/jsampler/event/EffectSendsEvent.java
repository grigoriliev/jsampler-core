/*
 *   JSampler - a front-end for LinuxSampler
 *
 *   Copyright (C) 2005-2023 Grigor Iliev <grigor@grigoriliev.com>
 *
 *   This file is part of JSampler.
 *
 *   JSampler is free software: you can redistribute it and/or modify it under
 *   the terms of the GNU General Public License as published by the Free
 *   Software Foundation, either version 3 of the License, or (at your option)
 *   any later version.
 *
 *   JSampler is distributed in the hope that it will be useful, but WITHOUT
 *   ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *   FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *   more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with JSampler. If not, see <https://www.gnu.org/licenses/>.
 */

package com.grigoriliev.jsampler.event;

import com.grigoriliev.jsampler.jlscp.FxSend;

/**
 * A semantic event which indicates effect sends changes.
 * @author Grigor Iliev
 */
public class EffectSendsEvent extends java.util.EventObject {
	private FxSend fxSend;
	
	/**
	 * Constructs a <code>EffectSendsEvent</code> object.
	 * @param source The object that originated the event.
	 */
	public EffectSendsEvent(Object source, FxSend fxSend) {
		super(source);
		this.fxSend = fxSend;
	}
	
	/**
	 * Gets the effect send, for which the event is fired.
	 */
	public FxSend
	getFxSend() { return fxSend; }
}
