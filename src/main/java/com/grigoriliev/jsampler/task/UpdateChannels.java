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
import com.grigoriliev.jsampler.SamplerChannelModel;
import com.grigoriliev.jsampler.SamplerModel;
import com.grigoriliev.jsampler.CC;

import com.grigoriliev.jsampler.jlscp.SamplerChannel;

import com.grigoriliev.jsampler.juife.PDUtils;
import com.grigoriliev.jsampler.juife.Task;


/**
 * This task updates the sampler channel list.
 * @author Grigor Iliev
 */
public class UpdateChannels extends EnhancedTask {
	/** Creates a new instance of <code>UpdateChannels</code>. */
	public
	UpdateChannels() {
		setTitle("UpdateChannels_task");
		setDescription(JSI18n.i18n.getMessage("UpdateChannels.description"));
		//setCalculateElapsedTime(true);
	}
	
	/** The entry point of the task. */
	@Override
	public void
	exec() throws Exception {
		SamplerModel sm = CC.getSamplerModel();
		Integer[] chnIDs = CC.getClient().getSamplerChannelIDs();
		
		boolean found = false, changed = false;
		
		boolean isAdjustingOld = CC.getSamplerModel().getChannelListIsAdjusting();
		
		PDUtils.runOnUiThreadAndWait(new Runnable() {
			public void
			run() {
				CC.getSamplerModel().setChannelListIsAdjusting(true);
				CC.getMainFrame().setAutoUpdateChannelListUI(false);
			}
		});
		
		for(SamplerChannelModel m : sm.getChannels()) {
			for(int i = 0; i < chnIDs.length; i++) {
				if(m.getChannelId() == chnIDs[i]) {
					chnIDs[i] = -1;
					found = true;
				}
			}
			
			if(!found) {
				sm.removeChannelById(m.getChannelId());
				changed = true;
			}
			found = false;
		}

		SamplerChannel[] chns = CC.getClient().getSamplerChannels(chnIDs);
		
		for(int i = 0; i < chns.length - 1; i++) sm.addChannel(chns[i]);
		
		manageAutoUpdate(false);
		
		if(chns.length > 0) sm.addChannel(chns[chns.length - 1]);
		else if(!CC.getSamplerModel().getChannelListIsAdjusting()) {
			if(!isAdjustingOld && !changed); // do nothing if nothing is changed
			else sm.addChannel(null); // fire dummy event to end an adjusting sequence
		}
	}

	@Override
	public void
	onError(Exception e) {
		manageAutoUpdate(true);
	}
	
	private void
	manageAutoUpdate(boolean force) {
		if(!force) {
			Task[] tasks = CC.getTaskQueue().getPendingTasks();
			for(Task t : tasks) if(t.equals(this)) return;
		}
		
		try {
			PDUtils.runOnUiThreadAndWait(new Runnable() {
				public void
				run() {
					CC.getSamplerModel().setChannelListIsAdjusting(false);
					CC.getMainFrame().setAutoUpdateChannelListUI(true);
					CC.getMainFrame().updateChannelListUI();
				}
			});
		} catch(Exception x) {
			x.printStackTrace();
		}
	}
		
	/**
	 * Used to decrease the traffic. All task in the queue
	 * equal to this are removed if added using {@link CC#scheduleTask}.
	 * @see CC#addTask
	 */
	@Override
	public boolean
	equals(Object obj) {
		if(obj == null) return false;
		if(!(obj instanceof UpdateChannels)) return false;
		
		return true;
	}
}
