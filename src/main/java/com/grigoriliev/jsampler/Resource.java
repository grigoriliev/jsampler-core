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


/**
 *
 * @author Grigor Iliev
 */
public class Resource {
	private String name = "Untitled";
	private String description = "";
	
	private final Vector<GenericListener> listeners = new Vector<GenericListener>();
	
	/**
	 * Creates a new instance of Resource 
	 */
	public
	Resource() { }
	
	/**
	 * Registers the specified listener to be notified when the resource info is changed.
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
	
	/**
	 * Gets the name of this resource.
	 * @return The name of this resource.
	 */
	public String
	getName() { return name; }
	
	/**
	 * Sets the name of this resource.
	 * @param name The new name of this resource.
	 */
	public void
	setName(String name) {
		this.name = name;
		fireChangeEvent();
	}
	
	/**
	 * Gets a brief description about this resource.
	 * @return A brief description about this resource.
	 */
	public String
	getDescription() { return description; }
	
	/**
	 * Sets a description about this resource.
	 * @param desc A brief description about this resource.
	 */
	public void
	setDescription(String desc) {
		description = desc;
		fireChangeEvent();
	}
	
	/** Notifies listeners that the recourse properties has changed. */
	protected void
	fireChangeEvent() {
		GenericEvent e = new GenericEvent(this);
		for(GenericListener l : listeners) l.jobDone(e);
	}
}
