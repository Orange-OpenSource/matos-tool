package com.orange.analysis.anasoot.loop;

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
import soot.tagkit.Tag;

/** This tag is used as a marker on method to indicate that the loop analysis
    was performed and on invoke statement to indicate that they are part of a
    local loop */
public class LoopTag implements Tag {
    /**
     * Name of the tag.
     */
    public static final String name = "LoopTag";
    private byte [] key;

    LoopTag() {	key = new byte [0]; }

    @Override
	public String getName() { return name; }
    @Override
	public byte [] getValue() { return key; }
}
    
