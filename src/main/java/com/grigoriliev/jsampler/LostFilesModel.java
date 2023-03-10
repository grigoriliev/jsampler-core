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

package com.grigoriliev.jsampler;

import java.util.Vector;

import com.grigoriliev.jsampler.juife.event.GenericEvent;
import com.grigoriliev.jsampler.juife.event.GenericListener;
import com.grigoriliev.jsampler.juife.event.TaskEvent;
import com.grigoriliev.jsampler.juife.event.TaskListener;

import com.grigoriliev.jsampler.task.InstrumentsDb.FindLostInstrumentFiles;

/**
 * A data model providing information about the lost instrument files in the instruments database.
 * @author Grigor Iliev
 */
public class LostFilesModel {
	private final Vector<String> lostFiles = new Vector<String>();
	private final Vector<GenericListener> listeners = new Vector<GenericListener>();
	
	/** Creates a new instance of <code>LostFilesModel</code> */
	public
	LostFilesModel() { }
	
	/**
	 * Registers the specified listener to be notified when the list
	 * of lost files is updated.
	 * @param l The <code>GenericListener</code> to register.
	 */
	public void
	addChangeListener(GenericListener l) { listeners.add(l); }
	
	/**
	 * Removes the specified listener.
	 * @param l The <code>GenericListener</code> to remove.
	 */
	public void
	removeChangeListener(GenericListener l) { listeners.remove(l); }
	
	/** Returns a list of all lost files. */
	public String[]
	getLostFiles() {
		return lostFiles.toArray(new String[lostFiles.size()]);
	}
	
	/**
	 * Gets the absolute pathname of the lost file at the specified position.
	 * @param index The position of the lost file to return.
	 * @return The absolute pathname of the lost file at the specified position.
	 */
	public String
	getLostFile(int index) { return lostFiles.get(index); }
	
	/**
	 * Gets the number of lost files in the instruments database.
	 * @return The number of lost files in the instruments database.
	 */
	public int
	getLostFileCount() { return lostFiles.size(); }
	
	/** Updates the list of lost instrument files. */
	public void
	update() {
		final FindLostInstrumentFiles t = new FindLostInstrumentFiles();
		t.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(t.doneWithErrors()) return;
				lostFiles.removeAllElements();
				for(String s : t.getResult()) lostFiles.add(s);
				fireLostFileListUpdated();
			}
		});
		
		lostFiles.removeAllElements();
		fireLostFileListUpdated();
		
		CC.getTaskQueue().add(t);
	}
	
	/** 
	 * Notifies listeners that the list of lost files is updated.
	 */
	private void
	fireLostFileListUpdated() {
		GenericEvent e = new GenericEvent(this);
		for(GenericListener l : listeners) l.jobDone(e);
	}
}
