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

import java.util.Date;
import java.util.logging.Level;

import com.grigoriliev.jsampler.HF;
import com.grigoriliev.jsampler.JSI18n;
import com.grigoriliev.jsampler.juife.AbstractTask;

import com.grigoriliev.jsampler.CC;

import com.grigoriliev.jsampler.jlscp.LSException;


/**
 * This class extends <code>AbstractTask</code> to add new features.
 * @author Grigor Iliev
 */
public abstract class EnhancedTask<R> extends AbstractTask<R> {
	public static final int SOCKET_ERROR = 1;
	private boolean stopped = false;
	private boolean silent = false;
	private boolean showErrorDetails;
	private boolean calculateElapsedTime = false;
	private long elapsedTime = -1;
	
	public
	EnhancedTask() { this(false); }

	public
	EnhancedTask(boolean showErrorDetails) {
		this.showErrorDetails = showErrorDetails;
	}

	public void
	run() {
		try {
			if(getCalculateElapsedTime()) {
				elapsedTime = new Date().getTime();
			}

			exec();

			if(getCalculateElapsedTime()) {
				elapsedTime = new Date().getTime() - elapsedTime;
			}
			if(getCalculateElapsedTime()) System.out.println("time: " + getElapsedTime());
		} catch(java.net.SocketException x) {
			setErrorCode(SOCKET_ERROR);
			setErrorMessage(JSI18n.i18n.getError("SOCKET_ERROR"));
			CC.getLogger().log(Level.FINE, getErrorMessage(), x);
		} catch(Exception x) {
			setErrorMessage(getDescription() + ": " + HF.getErrorMessage(x));
			if(showErrorDetails) setErrorDetails(x);
			CC.getLogger().log(Level.FINE, getErrorMessage(), x);
			onError(x);
		}
	}

	public void
	exec() throws Exception { }

	public void
	onError(Exception e) { e.printStackTrace(); }
	
	/**
	 * Marks that the execution of this task was interrupted.
	 */
	public void
	stop() { stopped = true; }
	
	/**
	 * Determines whether the execution of this task was interrupted.
	 * @return <code>true</code> if the execution of this task was interrupted,
	 * <code>false</code> otherwise.
	 */
	public boolean
	isStopped() { return stopped; }
	
	/**
	 * Determines whether an error message should be shown
	 * if the execution of the task fail.
	 */
	public boolean
	isSilent() { return silent; }
	
	/**
	 * Sets whether an error message should be shown
	 * if the execution of the task fail.
	 */
	public void
	setSilent(boolean b) { silent = b; }
	
	/**
	 * Sets the error details provided by the given exception (if the given
	 * exception is <code>LSException</code> instance and contains error details).
	 */
	public void
	setErrorDetails(Exception e) {
		if(e == null) return;
		
		if(e instanceof LSException) {
			LSException x = (LSException)e;
			if(x.getDetails() != null && x.getDetails().length() > 0) {
				setErrorDetails(x.getDetails());
			}
		}
	}

	/** Determines whether to calculate the elapsed time for this task. */
	public boolean
	getCalculateElapsedTime() { return calculateElapsedTime; }

	/** Sets whether to calculate the elapsed time for this task. */
	public void
	setCalculateElapsedTime(boolean b) { calculateElapsedTime = b; }

	/** Gets the elapsed time for this task. */
	public long
	getElapsedTime() { return elapsedTime; }
}
