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

package com.grigoriliev.jsampler.task;

import com.grigoriliev.jsampler.JSI18n;
import com.grigoriliev.jsampler.CC;

import com.grigoriliev.jsampler.view.JSChannel;

import com.grigoriliev.jsampler.jlscp.Client;
import com.grigoriliev.jsampler.jlscp.SamplerChannel;


/**
 * This task duplicates the specified sampler channels.
 * @author Grigor Iliev
 */
public class DuplicateChannels extends EnhancedTask {
	SamplerChannel[] chnS;
	
	/**
	 * Creates a new instance of <code>DuplicateChannels</code>.
	 * @param channel The channel to duplicate.
	 */
	public
	DuplicateChannels(SamplerChannel channel) {
		initTask();
		
		chnS = new SamplerChannel[1];
		chnS[0] = channel;
	}
	
	/**
	 * Creates a new instance of <code>DuplicateChannels</code>.
	 * @param channels The channels to duplicate.
	 */
	public
	DuplicateChannels(JSChannel[] channels) {
		initTask();
		
		chnS = new SamplerChannel[channels.length];
		
		for(int i = 0; i < channels.length; i++) chnS[i] = channels[i].getChannelInfo();
	}
	
	private void
	initTask() {
		setTitle("DuplicateChannels_task");
		setDescription(JSI18n.i18n.getMessage("DuplicateChannels.description"));
	}
	
	/** The entry point of the task. */
	@Override
	public void
	exec() throws Exception {
		for(SamplerChannel c : chnS) {
			duplicateSettings(c, CC.getClient().addSamplerChannel());
		}
	}
	
	private void
	duplicateSettings(SamplerChannel sc, int c) throws Exception {
		Client client = CC.getClient();
		
		if(sc.getMidiInputDevice() >= 0) {
			client.setChannelMidiInputDevice(c, sc.getMidiInputDevice());
			client.setChannelMidiInputPort(c, sc.getMidiInputPort());
			client.setChannelMidiInputChannel(c, sc.getMidiInputChannel());
		}
		
		if(sc.getAudioOutputDevice() >= 0) {
			client.setChannelAudioOutputDevice(c, sc.getAudioOutputDevice());
		}
		
		if(sc.getEngine() != null) {
			client.loadSamplerEngine(sc.getEngine().getName(), c);
			
			client.setChannelVolume(c, sc.getVolume());
			
			if(sc.isSoloChannel()) client.setChannelSolo(c, true);
			
			if(sc.isMuted() && !sc.isMutedBySolo())
				client.setChannelMute(c, true);
		
			if(sc.getInstrumentFile() != null) client.loadInstrument (
				sc.getInstrumentFile(), sc.getInstrumentIndex(), c, true
			);
		}
	}
}
