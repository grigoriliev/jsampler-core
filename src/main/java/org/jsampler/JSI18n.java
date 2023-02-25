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

package org.jsampler;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.grigoriliev.jsampler.juife.I18n;


/**
 * The <code>JSI18n</code> class manages the locale-specific data of the JSampler core.
 * @author Grigor Iliev
 */
public class JSI18n extends I18n {
	/** Provides the locale-specific data of this library. */
	public static JSI18n i18n = new JSI18n();
	
	private static Locale[] locales = {  new Locale("en", "US") };
	
	private
	JSI18n() {
		setButtonsBundle("org.jsampler.langprops.ButtonsLabelsBundle");
		setErrorsBundle("org.jsampler.langprops.ErrorsBundle");
		setLabelsBundle("org.jsampler.langprops.LabelsBundle");
		setLogsBundle("org.jsampler.langprops.LogsBundle");
		//setMenusBundle("org.jsampler.langprops.MenuLabelsBundle");
		setMessagesBundle("org.jsampler.langprops.MessagesBundle");
	}
	
	/**
	 * Gets all available locales.
	 * @return All available locales.
	 */
	public static Locale[]
	getAvailableLocales() { return locales; }
}
