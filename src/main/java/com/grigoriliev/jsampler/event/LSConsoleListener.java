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

/**
 * The listener interface for receiving events about LS Console changes.
 * @author Grigor Iliev
 */
public interface LSConsoleListener {
	/** Invoked when the text in the command line is changed. */
	public void commandLineTextChanged(LSConsoleEvent e);
	
	/** Invoked when the command in the command line has been executed. */
	public void commandExecuted(LSConsoleEvent e);
	
	/** Invoked when response is received from LinuxSampler. */
	public void responseReceived(LSConsoleEvent e);
}
