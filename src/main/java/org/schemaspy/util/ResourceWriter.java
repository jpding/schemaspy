/*
 * This file is a part of the SchemaSpy project (http://schemaspy.org).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.schemaspy.util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class ResourceWriter {
    private static final Logger logger = Logger.getLogger(ResourceWriter.class.getName());
    private static ResourceWriter instance = new ResourceWriter();

    /**
     * Write the specified resource to the specified filename
     *
     * @param resourceName
     * @param writeTo
     * @throws IOException
     */
    public void writeResource(String resourceName, File writeTo) throws IOException {
        writeTo.getParentFile().mkdirs();
        InputStream in = getClass().getResourceAsStream(resourceName);
        if (in == null)
            throw new IOException("Resource \"" + resourceName + "\" not found");

        byte[] buf = new byte[4096];

        OutputStream out = new FileOutputStream(writeTo);
        int numBytes = 0;
        while ((numBytes = in.read(buf)) != -1) {
            out.write(buf, 0, numBytes);
        }
        in.close();
        out.close();
    }

    /**
     * Copies resources to target folder.
     *
     * @param resourceUrl
     * @param targetPath
     * @return
     */
    public static void copyResources(URL resourceUrl, File targetPath, FileFilter filter) throws IOException {
        if (resourceUrl == null) {
            return;
        }

        URLConnection urlConnection = resourceUrl.openConnection();

        /**
         * Copy resources either from inside jar or from project folder.
         */
        if (urlConnection instanceof JarURLConnection) {
            copyJarResourceToPath((JarURLConnection) urlConnection, targetPath, filter);
        } else {
            File file = new File(resourceUrl.getPath());
            if (file.isDirectory()) {
                FileUtils.copyDirectory(file, targetPath, filter);
            } else {
                FileUtils.copyFile(file, targetPath);
            }
        }
    }

    /**
     * Copies resources from the jar file of the current thread and extract it
     * to the destination path.
     *
     * @param jarConnection
     * @param destPath destination file or directory
     */
    static void copyJarResourceToPath(JarURLConnection jarConnection, File destPath, FileFilter filter) {
        try {
            JarFile jarFile = jarConnection.getJarFile();
            String jarConnectionEntryName = jarConnection.getEntryName();

            /**
             * Iterate all entries in the jar file.
             */
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                JarEntry jarEntry = e.nextElement();
                String jarEntryName = jarEntry.getName();

                /**
                 * Extract files only if they match the path.
                 */
                if (jarEntryName.startsWith(jarConnectionEntryName + "/")) {
                    String filename = jarEntryName.substring(jarConnectionEntryName.length());
                    File currentFile = new File(destPath, filename);


                    if (jarEntry.isDirectory()) {
                        currentFile.mkdirs();
                    } else {
                        if ((filter == null) || filter.accept(currentFile)) {
                            InputStream is = jarFile.getInputStream(jarEntry);
                            OutputStream out = FileUtils.openOutputStream(currentFile);
                            IOUtils.copy(is, out);
                            is.close();
                            out.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }
}