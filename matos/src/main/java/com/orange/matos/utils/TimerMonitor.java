/*
 * $Id: TimerMonitor.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
 */
package com.orange.matos.utils;

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


/**
 * This interface defines the interface for a thread that must abort
 * after a timeout.
 *
 */
public interface TimerMonitor {

	/**
	 * allow abortion of a session upon timeout
	 * @param t the timer
	 */
	public void timeout(Timer t);

}