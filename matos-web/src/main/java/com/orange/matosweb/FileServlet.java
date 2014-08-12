
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves files that are stored in user sessions.
 * @author Pierre Cregut
 *
 */
public class FileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestedFile = request.getPathInfo();

        String user = request.getRemoteUser();

        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403
            return;
        }

        File sessionPath = new File(new File(System.getProperty("matos.tmpdir"), "matos"), user);

        if (requestedFile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
            return;
        }

        File file = new File(sessionPath, URLDecoder.decode(requestedFile, "UTF-8"));
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
            return;
        }

        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setContentType("text/html");
        response.setHeader("Content-Length", String.valueOf(file.length()));

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int length;
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file),
                DEFAULT_BUFFER_SIZE);
        try {
            ServletOutputStream output = response.getOutputStream();
            try {
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
    }
}
