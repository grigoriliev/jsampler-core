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
import com.grigoriliev.jsampler.JSPrefs;
import com.grigoriliev.jsampler.SamplerChannelModel;
import com.grigoriliev.jsampler.SamplerModel;
import com.grigoriliev.jsampler.juife.Task;

import com.grigoriliev.jsampler.CC;

import com.grigoriliev.jsampler.jlscp.FxSend;
import com.grigoriliev.jsampler.jlscp.event.MidiDataEvent;


/**
 * Provides the sampler channel's specific tasks.
 * @author Grigor Iliev
 */
public class Channel {
	
	/** Forbits the instantiation of this class. */
	private Channel() { }
	
	
	/**
	 * This task creates a new sampler channel.\
	 */
	public static class Add extends EnhancedTask<Integer> {
		/** Creates a new instance of <code>Add</code>. */
		public
		Add() {
			setTitle("Channel.Add_task");
			setDescription(JSI18n.i18n.getMessage("Channel.Add.desc"));
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			setResult(CC.getClient().addSamplerChannel());
			int chnId = getResult();
			
			JSPrefs p = CC.getViewConfig().preferences();
			if(!p.getBoolProperty(JSPrefs.USE_CHANNEL_DEFAULTS)) return;
			
			String s = p.getStringProperty(JSPrefs.DEFAULT_ENGINE);
			if(s != null && s.length() > 0) {
				CC.getClient().loadSamplerEngine(s, chnId);
			}
			
			s = p.getStringProperty(JSPrefs.DEFAULT_MIDI_INPUT);
			if(s != null && s.equals("firstDevice")) {
				assignFirstMidiDevice();
			} else if(s != null && s.equals("firstDeviceNextChannel")) {
				assignFirstMidiDeviceNextChannel();
			}
			
			s = p.getStringProperty(JSPrefs.DEFAULT_AUDIO_OUTPUT);
			if(s != null && s.equals("firstDevice")) {
				assignFirstAudioDevice();
			}
			
			s = p.getStringProperty(JSPrefs.DEFAULT_MIDI_INSTRUMENT_MAP);
			if(s != null && s.equals("midiInstrumentMap.none")) {
				CC.getClient().setChannelMidiInstrumentMap(chnId, -1);
			} else if(s != null && s.equals("midiInstrumentMap.default")) {
				CC.getClient().setChannelMidiInstrumentMap(chnId, -2);
			}
			
			float volume = p.getIntProperty(JSPrefs.DEFAULT_CHANNEL_VOLUME);
			volume /= 100;
			CC.getClient().setChannelVolume(chnId, volume);
		}
		
		private void
		assignFirstMidiDevice() throws Exception {
			if(CC.getSamplerModel().getMidiDeviceCount() < 1) return;
			int id = CC.getSamplerModel().getMidiDevices()[0].getDeviceId();
			CC.getClient().setChannelMidiInputDevice(getResult(), id);
		}
		
		private void
		assignFirstMidiDeviceNextChannel() throws Exception {
			int channelId = getResult();
			if(CC.getSamplerModel().getMidiDeviceCount() < 1) return;
			int id = CC.getSamplerModel().getMidiDevices()[0].getDeviceId();
			CC.getClient().setChannelMidiInputDevice(channelId, id);
			
			boolean[] usedChannels = new boolean[16];
			for(int i = 0; i < usedChannels.length; i++) usedChannels[i] = false;
			
			for(SamplerChannelModel m : CC.getSamplerModel().getChannels()) {
				if(m.getChannelId() == channelId) continue;
				if(m.getChannelInfo().getMidiInputDevice() != id) continue;
				if(m.getChannelInfo().getMidiInputPort() != 0) continue;
				int chn = m.getChannelInfo().getMidiInputChannel();
				if(chn >= 0 && chn < 16) usedChannels[chn] = true;
			}
			
			int lastUsed = -1;
			for(int i = 0; i < usedChannels.length; i++) {
				if(usedChannels[i]) lastUsed = i;
			}
			
			if(lastUsed == -1) {
				CC.getClient().setChannelMidiInputChannel(channelId, 0);
				return;
			}
			
			if(lastUsed < 15) {
				CC.getClient().setChannelMidiInputChannel(channelId, lastUsed + 1);
				return;
			}
			
			int firstUnused = -1;
			for(int i = 0; i < usedChannels.length; i++) {
				if(!usedChannels[i]) {
					firstUnused = i;
					break;
				}
			}
			
			if(firstUnused == -1) {
				CC.getClient().setChannelMidiInputChannel(channelId, 0);
				return;
			}
			
			CC.getClient().setChannelMidiInputChannel(channelId, firstUnused);
		}
		
		private void
		assignFirstAudioDevice() throws Exception {
			if(CC.getSamplerModel().getAudioDeviceCount() < 1) return;
			int id = CC.getSamplerModel().getAudioDevices()[0].getDeviceId();
			CC.getClient().setChannelAudioOutputDevice(getResult(), id);
		}
	}

	/**
	 * This task removes the specified sampler channel.
	 */
	public static class Remove extends EnhancedTask {
		private int channel;
	
		/**
		 * Creates new instance of <code>Remove</code>.
		 * @param channel The numerical ID of the channel to remove.
		 */
		public
		Remove(int channel) {
			setTitle("Channel.Remove_task");
			setDescription(JSI18n.i18n.getMessage("Channel.Remove.desc", channel));
		
			this.channel = channel;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().removeSamplerChannel(channel); }
	}

	/**
	 * This task resets the specified sampler channel.
	 */
	public static class Reset extends EnhancedTask {
		private int channel;
	
		/**
		 * Creates new instance of <code>Reset</code>.
		 * @param channel The numerical ID of the channel to reset.
		 */
		public
		Reset(int channel) {
			setTitle("Channel.Reset_task");
			setDescription(JSI18n.i18n.getMessage("Channel.Reset.desc", channel));
		
			this.channel = channel;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().resetChannel(channel); }
	}

	/**
	 * This task sets an audio output channel of a specific sampler channel.
	 */
	public static class SetAudioOutputChannel extends EnhancedTask {
		private int chn;
		private int audioOut;
		private int audioIn;
	
		/**
		 * Creates new instance of <code>SetAudioOutputChannel</code>.
		 * @param channel The sampler channel number.
		 * @param audioOut The sampler channel's audio output
		 * channel which should be rerouted.
		 * @param audioIn The audio channel of the selected audio output device where
		 * <code>audioOut</code> should be routed to.
		 */
		public
		SetAudioOutputChannel(int channel, int audioOut, int audioIn) {
			setTitle("Channel.SetAudioOutputChannel_task");
			String s = JSI18n.i18n.getMessage("Channel.SetAudioOutputChannel.desc", channel);
			setDescription(s);
			
			this.chn = channel;
			this.audioOut = audioOut;
			this.audioIn = audioIn;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setChannelAudioOutputChannel(chn, audioOut, audioIn);
		}
	}

	/**
	 * This task sets the audio output device of a specific sampler channel.
	 */
	public static class SetAudioOutputDevice extends EnhancedTask {
		private int channel;
		private int deviceID;
	
		/**
		 * Creates new instance of <code>SetAudioOutputDevice</code>.
		 * @param channel The sampler channel number.
		 * @param deviceID The numerical ID of the audio output device.
		 */
		public
		SetAudioOutputDevice(int channel, int deviceID) {
			setTitle("Channel.SetAudioOutputDevice_task");
			String s = JSI18n.i18n.getMessage("Channel.SetAudioOutputDevice.desc", channel);
			setDescription(s);
		
			this.channel = channel;
			this.deviceID = deviceID;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setChannelAudioOutputDevice(channel, deviceID);
		}
	}
	
	/**
	 * This task sets the MIDI input channel the specified sampler channel should listen to.
	 */
	public static class SetMidiInputChannel extends EnhancedTask {
		private int channel;
		private int midiChannel;
	
		/**
		 * Creates new instance of <code>SetMidiInputChannel</code>.
		 * @param channel The sampler channel number.
		 * @param midiChannel The number of the new MIDI input channel where
		 * <code>channel</code> should listen to or -1 to listen on all 16 MIDI channels.
		 */
		public
		SetMidiInputChannel(int channel, int midiChannel) {
			setTitle("Channel.SetMidiInputChannel_task");
			String s = JSI18n.i18n.getMessage("Channel.SetMidiInputChannel.desc", channel);
			setDescription(s);
			
			this.channel = channel;
			this.midiChannel = midiChannel;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setChannelMidiInputChannel(channel, midiChannel);
		}
	}

	/**
	 * This task sets the MIDI input device on a specific sampler channel.
	 */
	public static class SetMidiInputDevice extends EnhancedTask {
		private int channel;
		private int deviceID;
	
		/**
		 * Creates new instance of <code>SetMidiInputDevice</code>.
		 * @param channel The sampler channel number.
		 * @param deviceID The numerical ID of the MIDI input device.
		 */
		public
		SetMidiInputDevice(int channel, int deviceID) {
			setTitle("Channel.SetMidiInputDevice_task");
			String s = JSI18n.i18n.getMessage("Channel.SetMidiInputDevice.desc", channel);
			setDescription(s);
			
			this.channel = channel;
			this.deviceID = deviceID;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setChannelMidiInputDevice(channel, deviceID);
		}
	}

	/**
	 * This task sets the MIDI input port of a specific sampler channel.
	 */
	public static class SetMidiInputPort extends EnhancedTask {
		private int channel;
		private int port;
	
		/**
		 * Creates new instance of <code>SetMidiInputPort</code>.
		 * @param channel The sampler channel number.
		 * @param port The MIDI input port number
		 * of the MIDI input device connected to the specified sampler channel.
		 */
		public
		SetMidiInputPort(int channel, int port) {
			setTitle("Channel.SetMidiInputPort_task");
			setDescription(JSI18n.i18n.getMessage("Channel.SetMidiInputPort.desc", channel));
			
			this.channel = channel;
			this.port = port;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setChannelMidiInputPort(channel, port);
		}
	}
	
	/**
	 * This task loads a sampler engine in a specific sampler channel.
	 */
	public static class LoadEngine extends EnhancedTask {
		private String engine;
		private int channel;
		
		/**
		 * Creates new instance of <code>LoadEngine</code>.
		 * @param engine The name of the engine to load.
		 * @param channel The number of the sampler channel
		 * the deployed engine should be assigned to.
		 */
		public
		LoadEngine(String engine, int channel) {
			this.engine = engine;
			this.channel = channel;
			
			setTitle("Channel.LoadEngine_task");
			
			Object[] objs = { engine, new Integer(channel) };
			setDescription(JSI18n.i18n.getMessage("Channel.LoadEngine.desc", objs));
		}
		
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().loadSamplerEngine(engine, channel); }
	}
	
	/**
	 * This task loads and assigns an instrument to a sampler channel.
	 */
	public static class LoadInstrument extends EnhancedTask {
		private String filename;
		private int instrIndex;
		private int channel;
		
		/**
		 * Creates new instance of <code>LoadInstrument</code>.
		 * @param filename The name of the instrument file
		 * on the LinuxSampler instance's host system.
		 * @param instrIndex The index of the instrument in the instrument file.
		 * @param channel The number of the sampler channel the
		 * instrument should be assigned to.
		 */
		public
		LoadInstrument(String filename, int instrIndex, int channel) {
			this.filename = filename;
			this.instrIndex = instrIndex;
			this.channel = channel;
			
			setTitle("Channel.LoadInstrument_task");
			setDescription(JSI18n.i18n.getMessage("Channel.LoadInstrument.desc"));
		}
		
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().loadInstrument(filename, instrIndex, channel, true);
		}
	}
	
	/**
	 * This task assigns the specifed MIDI instrument map to the specified sampler channel.
	 */
	public static class SetMidiInstrumentMap extends EnhancedTask {
		private int channel;
		private int mapId;
	
		/**
		 * Creates new instance of <code>SetMidiInstrumentMap</code>.
		 * @param channel The sampler channel number.
		 * @param mapId The numerical ID of the MIDI instrument
		 * map that should be assigned to the specified sampler channel.
		 * To remove the current map binding use <code>-1</code>.
		 * To set the current map to be the default map use <code>-2</code>.
		 */
		public
		SetMidiInstrumentMap(int channel, int mapId) {
			setTitle("Channel.SetMidiInstrumentMap_task");
			String s = JSI18n.i18n.getMessage("Channel.SetMidiInstrumentMap.desc", channel);
			setDescription(s);
			
			this.channel = channel;
			this.mapId = mapId;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setChannelMidiInstrumentMap(channel, mapId);
		}
	}
	
	/**
	 * This task mutes/unmutes a specific sampler channel.
	 */
	public static class SetMute extends EnhancedTask {
		private int channel;
		private boolean mute;
	
		/**
		 * Creates new instance of <code>SetMute</code>.
		 * @param channel The sampler channel to be muted/unmuted.
		 * @param mute If <code>true</code> the specified channel is muted,
		 * else the channel is unmuted.
		 */
		public
		SetMute(int channel, boolean mute) {
			setTitle("Channel.SetMute_task");
			setDescription(JSI18n.i18n.getMessage("Channel.SetMute.desc", channel));
		
			this.channel = channel;
			this.mute = mute;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().setChannelMute(channel, mute); }
	}
	
	/**
	 * This task solos/unsolos a specific sampler channel.
	 */
	public static class SetSolo extends EnhancedTask {
		private int channel;
		private boolean solo;
	
		/**
		 * Creates new instance of <code>SetSolo</code>.
		 * @param channel The sampler channel number.
		 * @param solo Specify <code>true</code> to solo the specified channel,
		 * <code>false</code> otherwise.
		 */
		public
		SetSolo(int channel, boolean solo) {
			setTitle("Channel.SetSolo_task");
			setDescription(JSI18n.i18n.getMessage("Channel.SetSolo.desc", channel));
		
			this.channel = channel;
			this.solo = solo;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().setChannelSolo(channel, solo); }
	}

	/**
	 * This task sets the volume of a specific sampler channel.
	 */
	public static class SetVolume extends EnhancedTask {
		private int channel;
		private float volume;
	
		/**
		 * Creates new instance of <code>SetVolume</code>.
		 * @param channel The sampler channel number.
		 * @param volume The new volume value.
		 */
		public
		SetVolume(int channel, float volume) {
			setTitle("Channel.SetVolume_task");
			setDescription(JSI18n.i18n.getMessage("Channel.SetVolume.desc", channel));
		
			this.channel = channel;
			this.volume = volume;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			/*
			 * Because of the rapid flow of volume change tasks in some cases
			 * we need to do some optimization to decrease the traffic.
			 */
			boolean b = true;
			Task[] tS = CC.getTaskQueue().getPendingTasks();
			
			for(int i = tS.length - 1; i >= 0; i--) {
				Task t = tS[i];
			
				if(t instanceof SetVolume) {
					SetVolume scv = (SetVolume)t;
					if(scv.getChannelId() == channel) {
						CC.getTaskQueue().removeTask(scv);
					}
				}
			}
		
			CC.getClient().setChannelVolume(channel, volume);
		}
	
		/**
		 * Gets the ID of the channel whose volume should be changed.
		 * @return The ID of the channel whose volume should be changed.
		 */
		public int
		getChannelId() { return channel; }
	}

	/**
	 * This task updates the settings of a specific sampler channel.
	 */
	public static class UpdateInfo extends EnhancedTask {
		private int channel;
		
		/**
		 * Creates new instance of <code>UpdateInfo</code>.
		 * @param channel The sampler channel to be updated.
		 */
		public
		UpdateInfo(int channel) {
			setTitle("Channel.UpdateInfo_task");
			setDescription(JSI18n.i18n.getMessage("Channel.UpdateInfo.desc"));
			
			this.channel = channel;
		}
		
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			/*
			 * We don't want to bother the user if error occurs when updating
			 * a channel because in most cases this happens due to a race
			 * condition between delete/update events. So we just log this
			 * error instead to indicate the failure of this task.
			 */
			setSilent(true);
			SamplerModel sm = CC.getSamplerModel();
			sm.updateChannel(CC.getClient().getSamplerChannelInfo(channel));
		}
		
		/**
		 * Gets the ID of the channel for which information should be obtained.
		 * @return The ID of the channel for which information should be obtained.
		 */
		public int
		getChannelId() { return channel; }
	}

	/**
	 * This task creates an additional effect send on the specified sampler channel.
	 */
	public static class AddFxSend extends EnhancedTask<Integer> {
		private int channel;
		private int midiCtrl;
		private String name;
		
		/**
		 * Creates a new instance of <code>AddFxSend</code>.
		 * @param channel The sampler channel, on which a new effect send should be added.
		 * @param midiCtrl Defines the MIDI controller, which
		 * will be able alter the effect send level.
		 */
		public
		AddFxSend(int channel, int midiCtrl) {
			this(channel, midiCtrl, null);
		}
	
		/**
		 * Creates a new instance of <code>AddFxSend</code>.
		 * @param channel The sampler channel, on which a new effect send should be added.
		 * @param midiCtrl Defines the MIDI controller, which
		 * will be able alter the effect send level.
		 * @param name The name of the effect send entity.
		 * The name does not have to be unique.
		 */
		public
		AddFxSend(int channel, int midiCtrl, String name) {
			setTitle("Channel.AddFxSend_task");
			setDescription(JSI18n.i18n.getMessage("Channel.AddFxSend.desc"));
			this.channel = channel;
			this.midiCtrl = midiCtrl;
			this.name = name;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			setResult(CC.getClient().createFxSend(channel, midiCtrl, name));
		}
	}

	/**
	 * This task removes the specified effect send on the specified sampler channel.
	 */
	public static class RemoveFxSend extends EnhancedTask {
		private int channel;
		private int fxSend;
		
		/**
		 * Creates a new instance of <code>RemoveFxSend</code>.
		 * @param channel The sampler channel, from which an effect send should be removed.
		 * @param fxSend The ID of the effect send that should be removed.
		 */
		public
		RemoveFxSend(int channel, int fxSend) {
			setTitle("Channel.RemoveFxSend_task");
			String s = JSI18n.i18n.getMessage("Channel.RemoveFxSend.desc", channel, fxSend);
			setDescription(s);
			this.channel = channel;
			this.fxSend = fxSend;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().destroyFxSend(channel, fxSend); }
	}
	
	/**
	 * This task gets the list of effect sends on the specified sampler channel.
	 */
	public static class GetFxSends extends EnhancedTask<FxSend[]> {
		private int channel;
		
		/**
		 * Creates a new instance of <code>GetFxSends</code>.
		 */
		public
		GetFxSends() { this(-1); }
		
		/**
		 * Creates a new instance of <code>GetFxSends</code>.
		 */
		public
		GetFxSends(int channel) {
			setTitle("Channel.GetFxSends_task");
			setDescription(JSI18n.i18n.getMessage("Channel.GetFxSends.desc", channel));
			
			this.channel = channel;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { setResult(CC.getClient().getFxSends(channel)); }
		
		/**
		 * Gets the channel ID.
		 */
		public int
		getChannel() { return channel; }
		
		/**
		 * Sets the channel, for which effect sends should be obtained.
		 */
		public void
		setChannel(int channel) {
			this.channel = channel;
			setDescription(JSI18n.i18n.getMessage("Channel.GetFxSends.desc", channel));
		}
	}
	
	/**
	 * This task updates the list of effect sends on the specified sampler channel.
	 */
	public static class UpdateFxSends extends EnhancedTask {
		private int channel;
		
		/**
		 * Creates a new instance of <code>UpdateFxSends</code>.
		 * @param channel The numerical ID of the sampler channel
		 * whose effect send list should be updated.
		 */
		public
		UpdateFxSends(int channel) {
			setTitle("Channel.UpdateFxSends_task");
			setDescription(JSI18n.i18n.getMessage("Channel.UpdateFxSends.desc", channel));
			
			this.channel = channel;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			SamplerChannelModel scm;
			scm = CC.getSamplerModel().getChannelById(channel);
			Integer[] fxSendIDs = CC.getClient().getFxSendIDs(channel);
		
			boolean found = false;
			
			for(FxSend fxs : scm.getFxSends()) {
				for(int i = 0; i < fxSendIDs.length; i++) {
					if(fxSendIDs[i] == fxs.getFxSendId()) {
						fxSendIDs[i] = -1;
						found = true;
					}
				}
			
				if(!found) scm.removeFxSendById(fxs.getFxSendId());
				found = false;
			}
		
			FxSend fxs;
			
			for(int id : fxSendIDs) {
				if(id >= 0) {
					fxs = CC.getClient().getFxSendInfo(channel, id);
					scm.addFxSend(fxs);
				}
			}
		}
	}
	
	/**
	 * This task updates the settings of a specific effect send.
	 */
	public static class UpdateFxSendInfo extends EnhancedTask {
		private int channel;
		private int fxSend;
		
		/**
		 * Creates new instance of <code>UpdateFxSendInfo</code>.
		 * @param channel The numerical ID of the sampler channel
		 * containing the effect send entity that should be updated.
		 * @param fxSend The numerical ID of the effect send
		 * that should be updated.
		 */
		public
		UpdateFxSendInfo(int channel, int fxSend) {
			setTitle("Channel.UpdateFxSendInfo_task");
			String s = "Channel.UpdateFxSendInfo.desc";
			setDescription(JSI18n.i18n.getMessage(s, channel, fxSend));
			
			this.channel = channel;
			this.fxSend = fxSend;
		}
		
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			/*
			 * We don't want to bother the user if error occurs when updating
			 * an effect send because in most cases this happens due to a race
			 * condition between delete/update events. So we just log this
			 * error instead to indicate the failure of this task.
			 */
			setSilent(true);
			SamplerChannelModel scm;
			scm = CC.getSamplerModel().getChannelById(channel);
			scm.updateFxSend(CC.getClient().getFxSendInfo(channel, fxSend));
		}
	}

	/**
	 * This task changes the name of a specific effect send.
	 */
	public static class SetFxSendName extends EnhancedTask {
		private int channel;
		private int fxSend;
		private String name;
	
		/**
		 * Creates new instance of <code>SetFxSendName</code>.
		 * @param channel The sampler channel number.
		 * @param fxSend The numerical ID of the effect
		 * send, which name should be changed.
		 * @param name The new name of the effect send entity.
		 */
		public
		SetFxSendName(int channel, int fxSend, String name) {
			setTitle("Channel.SetFxSendName_task");
			String s = "Channel.SetFxSendName.desc";
			setDescription(JSI18n.i18n.getMessage(s, channel, fxSend));
		
			this.channel = channel;
			this.fxSend = fxSend;
			this.name = name;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().setFxSendName(channel, fxSend, name); }
	}
	
	/**
	 * This task sets the MIDI controller of a specific effect send.
	 */
	public static class SetFxSendAudioOutputChannel extends EnhancedTask {
		private int channel;
		private int fxSend;
		private int audioSrc;
		private int audioDst;
		
		/**
		 * Creates new instance of <code>SetFxSendAudioOutputChannel</code>.
		 * @param channel The sampler channel number.
		 * @param fxSend The numerical ID of the effect send entity to be rerouted.
		 * @param audioSrc The numerical ID of the effect send's audio output channel,
		 * which should be rerouted.
		 * @param audioDst The audio channel of the selected audio output device
		 * where <code>audioSrc</code> should be routed to.
		 */
		public
		SetFxSendAudioOutputChannel(int channel, int fxSend, int audioSrc, int audioDst) {
			setTitle("Channel.SetFxSendAudioOutputChannel_task");
			String s = "Channel.SetFxSendAudioOutputChannel.desc";
			setDescription(JSI18n.i18n.getMessage(s, channel, fxSend));
		
			this.channel = channel;
			this.fxSend = fxSend;
			this.audioSrc = audioSrc;
			this.audioDst = audioDst;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setFxSendAudioOutputChannel (
				channel, fxSend, audioSrc, audioDst
			);
		}
	}
	
	/**
	 * This task sets the volume of a specific effect send.
	 */
	public static class SetFxSendLevel extends EnhancedTask {
		private int channel;
		private int fxSend;
		private float volume;
	
		/**
		 * Creates new instance of <code>SetFxSendLevel</code>.
		 * @param channel The sampler channel number.
		 * @param fxSend The numerical ID of the effect send, which
		 * volume should be changed.
		 * @param volume The new volume value.
		 */
		public
		SetFxSendLevel(int channel, int fxSend, float volume) {
			setTitle("Channel.SetFxSendLevel_task");
			String s = JSI18n.i18n.getMessage("Channel.SetFxSendLevel.desc", channel, fxSend);
			setDescription(s);
		
			this.channel = channel;
			this.fxSend = fxSend;
			this.volume = volume;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setFxSendLevel(channel, fxSend, volume);
		}
	}
	
	/**
	 * This task (re)assigns the destination effect of a specific effect send.
	 */
	public static class SetFxSendEffect extends EnhancedTask {
		private int channel;
		private int fxSend;
		private int chainId;
		private int chainPos;
	
		/**
		 * Creates new instance of <code>SetFxSendEffect</code>.
		 * @param channel The sampler channel number.
		 * @param fxSend The numerical ID of the effect send.
		 * @param chainId The numerical ID of the effect chain. If -1 is
		 * specified the destination effect is removed.
		 */
		public
		SetFxSendEffect(int channel, int fxSend, int chainId, int chainPos) {
			setTitle("Channel.SetFxSendEffect_task");
			String s = JSI18n.i18n.getMessage("Channel.SetFxSendEffect.desc", channel, fxSend);
			setDescription(s);
		
			this.channel = channel;
			this.fxSend = fxSend;
			this.chainId = chainId;
			this.chainPos = chainPos;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			if(chainId == -1) {
				CC.getClient().removeFxSendEffect(channel, fxSend);
			} else {
				CC.getClient().setFxSendEffect(channel, fxSend, chainId, chainPos);
			}
		}
	}

	/**
	 * This task sets the MIDI controller of a specific effect send.
	 */
	public static class SetFxSendMidiController extends EnhancedTask {
		private int channel;
		private int fxSend;
		private int midiCtrl;
	
		/**
		 * Creates new instance of <code>SetFxSendMidiController</code>.
		 * @param channel The sampler channel number.
		 * @param fxSend The numerical ID of the effect
		 * send, which MIDI controller should be changed.
		 * @param midiCtrl The MIDI controller which shall be
		 * able to modify the effect send's send level.
		 */
		public
		SetFxSendMidiController(int channel, int fxSend, int midiCtrl) {
			setTitle("Channel.SetFxSendMidiController_task");
			String s = "Channel.SetFxSendMidiController.desc";
			setDescription(JSI18n.i18n.getMessage(s, channel, fxSend));
		
			this.channel = channel;
			this.fxSend = fxSend;
			this.midiCtrl = midiCtrl;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			CC.getClient().setFxSendMidiController(channel, fxSend, midiCtrl);
		}
	}

	/**
	 * This task starts an instrument editor for editing the loaded instrument
	 * on the specified sampler channel.
	 */
	public static class EditInstrument extends EnhancedTask {
		private int chn;
		
		/**
		 * Creates new instance of <code>EditInstrument</code>.
		 * @param channel The sampler channel number.
		 */
		public
		EditInstrument(int channel) {
			setTitle("Channel.EditInstrument_task");
			String s = JSI18n.i18n.getMessage("Channel.EditInstrument.desc");
			setDescription(s);
			
			this.chn = channel;
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception { CC.getClient().editChannelInstrument(chn); }
	}

	/**
	 * This task starts an instrument editor for editing the loaded instrument
	 * on the specified sampler channel.
	 */
	public static class SendMidiMsg extends EnhancedTask {
		private int chn;
		private MidiDataEvent.Type type;
		private int arg1;
		private int arg2;
		
		/**
		 * Creates new instance of <code>SendMidiMsg</code>.
		 * @param channel The sampler channel number.
		 */
		public
		SendMidiMsg(int channel, MidiDataEvent.Type type, int arg1, int arg2) {
			this.chn = channel;
			this.type = type;
			this.arg1 = arg1;
			this.arg2 = arg2;
			
			setTitle("Channel.SendMidiMsg_task");
			String s = JSI18n.i18n.getMessage("Channel.SendMidiMsg.desc", channel);
			setDescription(s);
		}
	
		/** The entry point of the task. */
		@Override
		public void
		exec() throws Exception {
			setSilent(true);
			CC.getClient().sendChannelMidiData(chn, type, arg1, arg2);
		}
	}
}
