package com.orange.analysis.anasoot.main;

/*
 * #%L
 * Matos
 * %%
 * Copyright (C) 2004 - 2014 Orange SA
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.orange.matos.core.Alert;

/**
 * This runtime exception is a hack so that a Soot phase can stop Soot and goes through the
 * Transform boundary back to the MatosPhase level without changing the exceptions exported
 * by soot. It should only be used at two places :
 * <ul>
 * <li> it can be raised in a transform where regular alerts are exchanged with RuntimeAlert
 * <li> it must be caught inside AnasootPhase and changed back to an alert.
 * </ul>
 * @author piac6784
 *
 */

public class AlertRuntimeException extends RuntimeException {
	/**
	 * Serial number nonsense 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Wrapped alert.
	 */
	private final Alert alert;
	
	/**
	 * Private constructor not to be used outside.
	 * @param a The wrapped alert
	 */
	private AlertRuntimeException(Alert a) {
		alert = a;
	}
	
	/**
	 * Used by soot transforms to wrap an Alert.
	 * @param a The alert.
	 */
	public static void wrap(Alert a) {
		throw new AlertRuntimeException(a);
	}
	
	/**
	 * Used by the AnasootPhase to give back the alert to the upper level.
	 * @param a Runtime exception.
	 * @throws Alert always raised.
	 */
	public static void unwrap(AlertRuntimeException a) throws Alert {
		throw a.alert;
	}
}
