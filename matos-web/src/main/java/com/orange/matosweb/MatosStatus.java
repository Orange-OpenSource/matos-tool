package com.orange.matosweb;

/*
 * #%L
 * Matos
 * $Id:$
 * $HeadURL:$
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
 * @author Pierre Cregut
 *
 */
public enum MatosStatus {
    /**
     * Analysis has not been done yet. 
     */
    TODO, 
    /**
     * Analysis is scheduled in the work list.
     */
    SCHEDULED,
    /**
     * Analysis engine is working on it.
     */
    COMPUTING,
    /**
     * Analysis aborted due to an internal failure of the analyzer.
     */
    SKIPPED, 
    
    /**
     * Analysis done and application follows the guidelines.
     */
    PASSED, 
    
    /**
     * Analysis done and application did not pass the criteria.
     */
    FAILED,
    /**
     * Deleted by the user. 
     */
     DELETED
}
