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

import com.grigoriliev.jsampler.SamplerChannelModel;
import com.grigoriliev.jsampler.SamplerModel;


/**
 * A semantic event which indicates changes of a sampler channel list.
 * @author Grigor Iliev
 */
public class SamplerChannelListEvent extends java.util.EventObject {
	private SamplerChannelModel channelModel;
	
	/**
	 * Constructs a <code>SamplerChannelListEvent</code> object.
	 *
	 * @param source The object that originated the event.
	 * @param channelModel The model of the sampler channel for which this event occurs.
	 */
	public
	SamplerChannelListEvent(Object source, SamplerChannelModel channelModel) {
		super(source);
		this.channelModel = channelModel;
	}
	
	/**
	 * Gets the sampler channel model for which this event occurs.
	 * @return The sampler channel model for which this event occurs.
	 * Note that the return value can be <code>null</code> in case
	 * of a dummy event that can be used to end an adjusting sequence.
	 * @see SamplerModel#getChannelListIsAdjusting
	 */
	public SamplerChannelModel
	getChannelModel() { return channelModel; }
}
