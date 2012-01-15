package play.template2;


import play.template2.exceptions.GTCompilationException;

import java.io.*;
import java.net.URL;

public abstract class IO {
    /**
     * Read file content to a String
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file, String encoding) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            StringWriter result = new StringWriter();
            PrintWriter out = new PrintWriter(result);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            return result.toString();
        } catch(IOException e) {
            throw new GTCompilationException("Error reading the file " + file, e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception e) {
                    //
                }
            }
        }
    }

    /**
     * Read fileURL content to a String
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(URL fileURL, String encoding) {
        InputStream is = null;
        try {
            is = fileURL.openStream();
            StringWriter result = new StringWriter();
            PrintWriter out = new PrintWriter(result);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            return result.toString();
        } catch(IOException e) {
            throw new GTCompilationException("Error reading the file " + fileURL, e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception e) {
                    //
                }
            }
        }
    }

    /**
     * Read file content to a String (always use utf-8)
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file) {
        return readContentAsString(file, "utf-8");
    }

    /**
     * Read fileURL content to a String (always use utf-8)
     * @param fileURL The file to read
     * @return The String content
     */
    public static String readContentAsString(URL fileURL) {
        return readContentAsString(fileURL, "utf-8");
    }

    /**
     * Write binay data to a file
     * @param data The binary data to write
     * @param file The file to write
     */
    public static void write(byte[] data, File file) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.flush();
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if(os != null) os.close();
            } catch(Exception e) {
                //
            }
        }
    }

    /**
     * Read binary content of a file (warning does not use on large file !)
     * @param file The file te read
     * @return The binary data
     */
    public static byte[] readContent(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] result = new byte[(int) file.length()];
            is.read(result);
            return result;
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception e) {
                    //
                }
            }
        }
    }


    /**
     * If url points to a real file on disk, we return the File-object pointing to this file.
     * if not, we return null
     * @param urlFile url to file
     * @return
     */
    public static File getFileFromURL(URL urlFile) {
        if ( urlFile.getProtocol().equals("FILE")) {
            return new File(urlFile.getFile());
        } else {
            return null;
        }
    }
    
    public static class FileInfo {
        public final long lastModified;
        public final long size;

        public FileInfo(long lastModified, long size) {
            this.lastModified = lastModified;
            this.size = size;
        }
    }

    /**
     * Returns fileInfo for the file pointed to by the url..
     * If file is inside a jar, then lastModified is set to the date of the jar..
     * @param fileURL
     * @return
     */
    public static FileInfo getFileInfo(URL fileURL) {
        File file = getFileFromURL(fileURL);
        if ( file == null) {
            return new FileInfo(0,0);
        }
        
        return new FileInfo(file.lastModified(), file.length());
    }



}
