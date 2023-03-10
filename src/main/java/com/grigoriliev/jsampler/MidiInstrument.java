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

import com.grigoriliev.jsampler.event.MidiInstrumentEvent;
import com.grigoriliev.jsampler.event.MidiInstrumentListener;
import com.grigoriliev.jsampler.juife.PDUtils;

import com.grigoriliev.jsampler.jlscp.MidiInstrumentInfo;


/**
 * Represents a MIDI instrument.
 * @author Grigor Iliev
 */
public class MidiInstrument {
	private MidiInstrumentInfo info;
	private final Vector<MidiInstrumentListener> listeners =
		new Vector<MidiInstrumentListener>();
	
	private static int firstProgramNumber = 0;
	
	
	/** Creates a new instance of MidiInstrument */
	public
	MidiInstrument() { }
	
	/**
	 * Creates a new instance of <code>MidiInstrument</code>.
	 * @param info Provides mapping information about this instrument.
	 */
	public
	MidiInstrument(MidiInstrumentInfo info) { this.info = info; }
	
	/**
	 * Registers the specified listener for receiving event messages.
	 * @param l The <code>MidiInstrumentListener</code> to register.
	 */
	public void
	addMidiInstrumentListener(MidiInstrumentListener l) {
		listeners.add(l);
	}
	
	/**
	 * Removes the specified listener.
	 * @param l The <code>MidiInstrumentListener</code> to remove.
	 */
	public void
	removeMidiInstrumentListener(MidiInstrumentListener l) {
		listeners.remove(l);
	}
	
	
	/**
	 * Gets the name of this MIDI instrument.
	 * @return The name of this MIDI instrument.
	 */
	public String
	getName() { return info.getName(); }
	
	/**
	 * Sets the name of this MIDI instrument.
	 * @param name The new name of this MIDI instrument.
	 */
	public void
	setName(String name) {
		info.setName(name);
		fireInfoChanged();
	}
	
	/**
	 * Gets the information about this instrument.
	 * @return The information about this instrument.
	 */
	public MidiInstrumentInfo
	getInfo() { return info; }
	
	/**
	 * Sets the information about this MIDI instrument.
	 * @param info The new information about this MIDI instrument.
	 */
	public void
	setInfo(MidiInstrumentInfo info) {
		this.info = info;
		fireInfoChanged();
	}
	
	/**
	 * Gets the MIDI program numbering, whether
	 * the index of the first MIDI program is 0 or 1.
	 */
	public static int
	getFirstProgramNumber() { return firstProgramNumber; }
	
	/**
	 * Sets the MIDI program numbering, whether
	 * the index of the first MIDI program is 0 or 1.
	 */
	public static void
	setFirstProgramNumber(int idx) { firstProgramNumber = idx; }
	
	/**
	 * Determines whether the specified object is of type
	 * <code>MidiInstrument</code> and has equal map ID, MIDI bank and MIDI program.
	 * @param obj The reference object with which to compare.
	 * @return <code>true</code> if the specified object is of type
	 * <code>MidiInstrument</code> and has equal map ID, MIDI bank and MIDI program.
	 */
	public boolean
	equals(Object obj) {
		if(obj == null || getInfo() == null) return false;
		if(!(obj instanceof MidiInstrument)) return false;
		MidiInstrument i = (MidiInstrument)obj;
		return getInfo().equals(i.getInfo());
	}
	
	/**
	 * Returns the program number and name of this instrument.
	 * @return The program number and name of this instrument.
	 */
	public String
	toString() {
		int i = getFirstProgramNumber() + getInfo().getMidiProgram();
		return String.valueOf(i) + ". " + getName();
	}
	
	/**
	 * Notifies listeners that the the MIDI instrument settings are changed.
	 * Note that this method can be invoked outside the event-dispatching thread.
	 */
	private void
	fireInfoChanged() {
		final MidiInstrumentEvent e = new MidiInstrumentEvent(this);
		
		PDUtils.runOnUiThread(new Runnable() {
			public void
			run() { fireInfoChanged(e); }
		});
	}
	
	
	/** Notifies listeners that the MIDI instrument settings are changed. */
	private void
	fireInfoChanged(MidiInstrumentEvent e) {
		for(MidiInstrumentListener l : listeners) l.instrumentInfoChanged(e);
	}
}
