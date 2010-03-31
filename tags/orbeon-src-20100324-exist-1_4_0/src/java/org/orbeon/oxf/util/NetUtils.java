/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.util;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.StaticExternalContext;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.generator.RequestGenerator;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.servlet.ServletExternalContext;
import org.orbeon.oxf.xml.ContentHandlerAdapter;
import org.orbeon.oxf.xml.XMLUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {

    private static Logger logger = LoggerFactory.createLogger(NetUtils.class);

    public static final String DYNAMIC_RESOURCES_SESSION_KEY = "orbeon.resources.dynamic.";
    // Resources are served by the XForms server. It is not ideal to refer to XForms-related functionality from here.
    public static final String DYNAMIC_RESOURCES_PATH = "/xforms-server/dynamic/";

    private static final Pattern PATTERN_NO_AMP;
    private static final Pattern PATTERN_AMP;
//    private static final Pattern PATTERN_AMP_AMP;

    public static final String STANDARD_PARAMETER_ENCODING = "utf-8";

    private static final SimpleDateFormat dateHeaderFormats[] = {
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
        new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
        new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };

    private static final TimeZone gmtZone = TimeZone.getTimeZone("GMT");
    private static FileItemFactory fileItemFactory;

    public static final int REQUEST_SCOPE = 0;
    public static final int SESSION_SCOPE = 1;
    public static final int APPLICATION_SCOPE = 2;

    // Default HTTP 1.1 charset for text/* mediatype
    public static final String DEFAULT_HTTP_TEXT_READING_ENCODING = "iso-8859-1";
    // Default RFC 3023 default charset for txt/xml mediatype
    public static final String DEFAULT_TEXT_XML_READING_ENCODING = "us-ascii";
    public static final String APPLICATION_SOAP_XML = "application/soap+xml";

    static {
        // Set timezone to GMT as required for HTTP headers
        for (SimpleDateFormat dateHeaderFormat: dateHeaderFormats)
            dateHeaderFormat.setTimeZone(gmtZone);

        final String notEqNorAmpChar = "[^=&]";
        final String token = notEqNorAmpChar+ "+";
        PATTERN_NO_AMP = Pattern.compile( "(" + token + ")=(" + token + ")(?:&|(?<!&)\\z)" );
        PATTERN_AMP = Pattern.compile( "(" + token + ")=(" + token + ")(?:&amp;|&|(?<!&amp;|&)\\z)" );
//        PATTERN_AMP_AMP = Pattern.compile( "(" + token + ")=(" + token + ")(?:&amp;amp;|&|(?<!&amp;amp;|&)\\z)" );
    }

    public static long getDateHeader(String stringValue) throws ParseException {
        for (SimpleDateFormat dateHeaderFormat: dateHeaderFormats) {
            try {
                Date date = dateHeaderFormat.parse(stringValue);
                return date.getTime();
            } catch (Exception e) {// used to be ParseException, but NumberFormatException may be thrown as well
                // Ignore and try next
            }
        }
        throw new ParseException(stringValue, 0);
    }

    /**
     * Return true if the document was modified since the given date, based on the If-Modified-Since
     * header. If the request method was not "GET", or if no valid lastModified value was provided,
     * consider the document modified.
     */
    public static boolean checkIfModifiedSince(HttpServletRequest request, long lastModified) {
        // Do the check only for the GET method
        if (!"GET".equals(request.getMethod()) || lastModified <= 0)
            return true;
        // Check dates
        String ifModifiedHeader = request.getHeader("If-Modified-Since");
        if (logger.isDebugEnabled())
            logger.debug("Found If-Modified-Since header");
        if (ifModifiedHeader != null) {
            try {
                long dateTime = getDateHeader(ifModifiedHeader);
                if (lastModified <= (dateTime + 1000)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Sending SC_NOT_MODIFIED response");
                    return false;
                }
            } catch (Exception e) {// used to be ParseException, but NumberFormatException may be thrown as well
                // Ignore
            }
        }
        return true;
    }

    /**
     * Return a request path info that looks like what one would expect. The path starts with a "/", relative to the
     * servlet context. If the servlet was included or forwarded to, return the path by which the *current* servlet was
     * invoked, NOT the path of the calling servlet.
     *
     * Request path = servlet path + path info.
     *
     * @param request   servlet HTTP request
     * @return          path
     */
    public static String getRequestPathInfo(HttpServletRequest request) {

        // NOTE: Servlet 2.4 spec says: "These attributes [javax.servlet.include.*] are accessible from the included
        // servlet via the getAttribute method on the request object and their values must be equal to the request URI,
        // context path, servlet path, path info, and query string of the included servlet, respectively."
        // NOTE: This is very different from the similarly-named forward attributes!

        // Get servlet path
        String servletPath = (String) request.getAttribute("javax.servlet.include.servlet_path");
        if (servletPath == null) {
            servletPath = request.getServletPath();
            if (servletPath == null)
                servletPath = "";
        }
        
        // Get path info
        String pathInfo = (String) request.getAttribute("javax.servlet.include.path_info");
        if (pathInfo == null) {
            pathInfo = request.getPathInfo();
            if (pathInfo == null)
                pathInfo = "";
        }

        // Concatenate servlet path and path info, avoiding a double slash
        String requestPath = servletPath.endsWith("/") && pathInfo.startsWith("/")
                ? servletPath + pathInfo.substring(1)
                : servletPath + pathInfo;

        // Add starting slash if missing
        if (!requestPath.startsWith("/"))
            requestPath = "/" + requestPath;

        return requestPath;
    }

    /**
     * Return the last modification date of the given absolute URL if it is "fast" to do so, i.e. if it is an "oxf:" or
     * a "file:" protocol.
     *
     * @param absoluteURL   absolute URL to check
     * @return              last modification date if "fast" or 0 if not fast or if an error occurred
     */
    public static long getLastModifiedIfFast(String absoluteURL) {
        final long lastModified;
        if (absoluteURL.startsWith("oxf:") || absoluteURL.startsWith("file:")) {
            try {
                lastModified = getLastModified(URLFactory.createURL(absoluteURL));
            } catch (IOException e) {
                throw new OXFException(e);
            }
        } else {
            // Value of 0 for lastModified will cause XFormsResourceServer to set Last-Modified and Expires properly to "now".
            lastModified = 0;
        }
        return lastModified;
    }

    /**
     * Get the last modification date of a URL.
     *
     * @return last modified timestamp, null if le 0
     */
    public static Long getLastModifiedAsLong(URL url) throws IOException {
        final long connectionLastModified = getLastModified(url);
        // Zero and negative values often have a special meaning, make sure to normalize here
        return connectionLastModified <= 0 ? null : connectionLastModified;
    }

    /**
     * Get the last modification date of a URL.
     *
     * @return last modified timestamp "as is"
     */
    public static long getLastModified(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            // Optimize file: access. Also, this prevents throwing an exception if the file doesn't exist as we try to close the stream below.
            return new File(URLDecoder.decode(url.getFile(), STANDARD_PARAMETER_ENCODING)).lastModified();
        } else {
            // Use URLConnection
            final URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof HttpURLConnection)
                ((HttpURLConnection) urlConnection).setRequestMethod("HEAD");
            try {
                return getLastModified(urlConnection);
            } finally {
                urlConnection.getInputStream().close();
            }
        }
    }

    /**
     * Get the last modification date of an open URLConnection.
     *
     * This handles the (broken at some point in the Java libraries) case of the file: protocol.
     *
     * @return last modified timestamp, null if le 0
     */
    public static Long getLastModifiedAsLong(URLConnection urlConnection) {
        final long connectionLastModified = getLastModified(urlConnection);
        // Zero and negative values often have a special meaning, make sure to normalize here
        return connectionLastModified <= 0 ? null : connectionLastModified;
    }

    /**
     * Get the last modification date of an open URLConnection.
     *
     * This handles the (broken at some point in the Java libraries) case of the file: protocol.
     *
     * @return last modified timestamp "as is"
     */
    public static long getLastModified(URLConnection urlConnection) {
        try {
            long lastModified = urlConnection.getLastModified();
            if (lastModified == 0 && "file".equals(urlConnection.getURL().getProtocol()))
                lastModified = new File(URLDecoder.decode(urlConnection.getURL().getFile(), STANDARD_PARAMETER_ENCODING)).lastModified();
            return lastModified;
        } catch (UnsupportedEncodingException e) {
            // Should not happen as we are using a required encoding
            throw new OXFException(e);
        }
    }

    /**
     * Check if an URL is relative to another URL.
     */
    public static boolean relativeURL(URL url1, URL url2) {
        return ((url1.getProtocol() == null && url2.getProtocol() == null) || url1.getProtocol().equals(url2.getProtocol()))
                && ((url1.getAuthority() == null && url2.getAuthority() == null) || url1.getAuthority().equals(url2.getAuthority()))
                && ((url1.getPath() == null && url2.getPath() == null) || url2.getPath().startsWith(url1.getPath()));
    }

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        int count;
        byte[] buffer = new byte[1024];
        while ((count = is.read(buffer)) > 0)
            os.write(buffer, 0, count);
    }

    public static void copyStream(Reader reader, Writer writer) throws IOException {
        int count;
        char[] buffer = new char[1024];
        while ((count = reader.read(buffer)) > 0)
            writer.write(buffer, 0, count);
    }

    public static String readStreamAsString(Reader reader) throws IOException {
        final StringBuilderWriter writer = new StringBuilderWriter();
        copyStream(reader, writer);
        return writer.toString();
    }

    public static String getContentTypeCharset(String contentType) {
        final Map<String, String> parameters = getContentTypeParameters(contentType);
        return (parameters == null) ? null : parameters.get("charset");
    }

    public static Map<String, String> getContentTypeParameters(String contentType) {
        if (contentType == null)
            return null;

        // Check whether there may be parameters
        final int semicolonIndex = contentType.indexOf(";");
        if (semicolonIndex == -1)
            return null;

        // Tokenize
        final StringTokenizer st = new StringTokenizer(contentType, ";");

        if (!st.hasMoreTokens())
            return null; // should not happen as there should be at least the content type    

        st.nextToken();

        // No parameters
        if (!st.hasMoreTokens())
            return null;

        // Parse parameters
        final Map<String, String> parameters = new HashMap<String, String>();
        while (st.hasMoreTokens()) {
            final String parameter = st.nextToken().trim();
            final int equalIndex = parameter.indexOf('=');
            if (equalIndex == -1)
                continue;
            final String name = parameter.substring(0, equalIndex).trim();
            final String value = parameter.substring(equalIndex + 1).trim();
            parameters.put(name, value);
        }
        return parameters;
    }

    public static Map<String, String> getCharsetHeaderCharsets(String header) {
        if (header == null)
            return null;
        int semicolonIndex = header.indexOf(";");
        final String charsets;
        if (semicolonIndex == -1)
            charsets = header.trim();
        else
            charsets = header.substring(0, semicolonIndex).trim();

        final StringTokenizer st = new StringTokenizer(charsets, ",");
        final Map<String, String> charsetsMap = new HashMap<String, String>();
        while (st.hasMoreTokens()) {
            charsetsMap.put(st.nextToken(), "");
        }

        return charsetsMap;
    }

    public static String getContentTypeMediaType(String contentType) {
        if (contentType == null || contentType.equalsIgnoreCase("content/unknown"))
            return null;
        int semicolonIndex = contentType.indexOf(";");
        if (semicolonIndex == -1)
            return contentType;
        return contentType.substring(0, semicolonIndex).trim();
    }

    /**
     * @param queryString a query string of the form n1=v1&n2=v2&... to decode.  May be null.
     * @param acceptAmp -> "&amp;" if true, "&" if false
     *
     * @return a Map of String[] indexed by name, an empty Map if the query string was null
     */
    public static Map<String, String[]> decodeQueryString(final CharSequence queryString, final boolean acceptAmp) {

        final Map<String, String[]> result = new TreeMap<String, String[]>();
        if (queryString != null) {
            final Matcher matcher = acceptAmp ? PATTERN_AMP.matcher(queryString) : PATTERN_NO_AMP.matcher(queryString);
            int matcherEnd = 0;
            while (matcher.find()) {
                matcherEnd = matcher.end();
                try {
                    // Group 0 is the whole match, e.g. a=b, while group 1 is the first group
                    // denoted ( with parens ) in the expression.  Hence we start with group 1.
                    final String name = URLDecoder.decode(matcher.group(1), NetUtils.STANDARD_PARAMETER_ENCODING);
                    final String value = URLDecoder.decode(matcher.group(2), NetUtils.STANDARD_PARAMETER_ENCODING);

                    StringUtils.addValueToStringArrayMap(result, name, value);
                } catch (UnsupportedEncodingException e) {
                    // Should not happen as we are using a required encoding
                    throw new OXFException(e);
                }
            }
            if (queryString.length() != matcherEnd) {
                // There was garbage at the end of the query.
                throw new OXFException("Malformed URL: " + queryString);
            }
        }
        return result;
    }

    /**
     * Encode a query string. The input Map contains names indexing Object[].
     */
    public static String encodeQueryString(Map parameters) {
        final StringBuilder sb = new StringBuilder(100);
        boolean first = true;
        try {
            for (Object o: parameters.keySet()) {
                final String name = (String) o;
                final Object[] values = (Object[]) parameters.get(name);
                for (final Object currentValue: values) {
                    if (currentValue instanceof String) {
                        if (!first)
                            sb.append('&');

                        sb.append(URLEncoder.encode(name, NetUtils.STANDARD_PARAMETER_ENCODING));
                        sb.append('=');
                        sb.append(URLEncoder.encode((String) currentValue, NetUtils.STANDARD_PARAMETER_ENCODING));

                        first = false;
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            // Should not happen as we are using a required encoding
            throw new OXFException(e);
        }
        return sb.toString();
    }

    /**
     * Combine a path info and a parameters map to form a path info with a query string.
     */
    public static String pathInfoParametersToPathInfoQueryString(String pathInfo, Map parameters) throws IOException {
        final StringBuilder redirectURL = new StringBuilder(pathInfo);
        if (parameters != null) {
            boolean first = true;
            for (Object o: parameters.keySet()) {
                final String name = (String) o;
                final Object[] values = (Object[]) parameters.get(name);
                for (final Object currentValue: values) {
                    if (currentValue instanceof String) {
                        redirectURL.append(first ? "?" : "&");
                        redirectURL.append(URLEncoder.encode(name, NetUtils.STANDARD_PARAMETER_ENCODING));
                        redirectURL.append("=");
                        redirectURL.append(URLEncoder.encode((String) currentValue, NetUtils.STANDARD_PARAMETER_ENCODING));
                        first = false;
                    }
                }
            }
        }
        return redirectURL.toString();
    }

    /**
     * Append a query string to an URL. This adds a '?' or a '&' or nothing, as needed.
     *
     * @param urlString     existing URL string
     * @param queryString   query string, or null
     * @return              resulting URL
     */
    public static String appendQueryString(String urlString, String queryString) {
        if (org.apache.commons.lang.StringUtils.isBlank(queryString)) {
            return urlString;
        } else {
            final StringBuilder updatedActionStringBuilder = new StringBuilder(urlString);
            updatedActionStringBuilder.append((urlString.indexOf('?') == -1) ? '?' : '&');
            updatedActionStringBuilder.append(queryString);
            return updatedActionStringBuilder.toString();
        }
    }

    /**
     * Check whether a URL starts with a protocol.
     *
     * We consider that a protocol consists only of ASCII letters and must be at least two
     * characters long, to avoid confusion with Windows drive letters.
     */
    public static boolean urlHasProtocol(String urlString) {
        int colonIndex = urlString.indexOf(":");

        // No protocol is there is no colon or if there is only one character in the protocol
        if (colonIndex == -1 || colonIndex == 1)
            return false;

        // Check that there is a protocol
        boolean allChar = true;
        for (int i = 0; i < colonIndex; i++) {
            char c = urlString.charAt(i);
            if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) {
                allChar = false;
                break;
            }
        }
        return allChar;
    }

    /**
     * Resolve a URI against a base URI. (Be sure to pay attention to the order or parameters.)
     *
     * @param href  URI to resolve
     * @param base  URI base
     * @return      resolved URI
     */
    public static String resolveURI(String href, String base) {
        final String resolvedURIString;
        if (base != null) {
            final URI baseURI;
            try {
                baseURI = new URI(base);
            } catch (URISyntaxException e) {
                throw new OXFException(e);
            }
            resolvedURIString = baseURI.resolve(href).normalize().toString();// normalize to remove "..", etc.
        } else {
            resolvedURIString = href;
        }
        return resolvedURIString;
    }

    public static String headersToString(HttpServletRequest request) {
        final StringBuffer sb = new StringBuffer();
        for (Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
            final String name = (String) e.nextElement();
            sb.append(name);
            sb.append("=");
            for (Enumeration f = request.getHeaders(name); f.hasMoreElements();) {
                final String value = (String) f.nextElement();
                sb.append(value);
                if (f.hasMoreElements())
                    sb.append(",");
            }
            if (e.hasMoreElements())
                    sb.append("|");
        }
        return sb.toString();
    }

   public static String readURIToLocalURI(String uri) throws URISyntaxException, IOException {
       final PipelineContext pipelineContext = StaticExternalContext.getStaticContext().getPipelineContext();
       final URLConnection urlConnection = new URI(uri).toURL().openConnection();
       InputStream inputStream = null;
       try {
           inputStream = urlConnection.getInputStream();
           return inputStreamToAnyURI(pipelineContext, inputStream, REQUEST_SCOPE);
       } finally {
           if (inputStream != null) inputStream.close();
       }
   }

    public static byte[] base64StringToByteArray(String base64String) {
        return Base64.decode(base64String);
    }

    /**
     * Convert a String in xs:base64Binary to an xs:anyURI.
     *
     * NOTE: The implementation creates a temporary file. The Pipeline Context is required so
     * that the file can be deleted when no longer used.
     */
    public static String base64BinaryToAnyURI(PipelineContext pipelineContext, String value, int scope) {
        // Convert Base64 to binary first
        final byte[] bytes = base64StringToByteArray(value);

        return inputStreamToAnyURI(pipelineContext, new ByteArrayInputStream(bytes), scope);
    }

    /**
     * Read an InputStream into a byte array.
     *
     * @param is    InputStream
     * @return      byte array
     */
    public static byte[] inputStreamToByteArray(InputStream is) {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            copyStream(new BufferedInputStream(is), os);
            os.close();
            return os.toByteArray();
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }

    /**
     * Read a URI into a byte array.
     *
     * @param uri   URI to read
     * @return      byte array
     */
    public static byte[] uriToByteArray(String uri) {
        InputStream is = null;
        try {
            is = new URI(uri).toURL().openStream();
            return inputStreamToByteArray(is);
        } catch (Exception e) {
            throw new OXFException(e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                throw new OXFException(e);
            }
        }
    }

    /**
     * Convert a URI to a FileItem.
     *
     * The implementation creates a temporary file. The PipelineContext is required so that the file can be deleted
     * when no longer used.
     */
    public static FileItem anyURIToFileItem(PipelineContext pipelineContext, String uri, int scope) {
        InputStream inputStream = null;
        try {
            inputStream = new URI(uri).toURL().openStream();

            // Get FileItem
            return prepareFileItemFromInputStream(pipelineContext, inputStream, scope);

        } catch (Exception e) {
            throw new OXFException(e);
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                throw new OXFException(e);
            }
        }
    }

    /**
     * Convert an InputStream to an xs:anyURI.
     *
     * The implementation creates a temporary file. The PipelineContext is required so that the file can be deleted
     * when no longer used.
     */
    public static String inputStreamToAnyURI(PipelineContext pipelineContext, InputStream inputStream, int scope) {
        // Get FileItem
        final FileItem fileItem = prepareFileItemFromInputStream(pipelineContext, inputStream, scope);

        // Return a file URL
        final File storeLocation = ((DiskFileItem) fileItem).getStoreLocation();
        return storeLocation.toURI().toString();
    }

    private static FileItem prepareFileItemFromInputStream(PipelineContext pipelineContext, InputStream inputStream, int scope) {
        // Get FileItem
        final FileItem fileItem = prepareFileItem(pipelineContext, scope);
        // Write to file
        OutputStream os = null;
        try {
            os = fileItem.getOutputStream();
            copyStream(inputStream, os);
        } catch (IOException e) {
            throw new OXFException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new OXFException(e);
                }
            }
        }
        // Create file if it doesn't exist (necessary when the file size is 0)
        final File storeLocation = ((DiskFileItem) fileItem).getStoreLocation();
        try {
            storeLocation.createNewFile();
        } catch (IOException e) {
            throw new OXFException(e);
        }

        return fileItem;
    }

    /**
     * Return a FileItem which is going to be automatically destroyed upon destruction of the request, session or
     * application.
     */
    public static FileItem prepareFileItem(PipelineContext pipelineContext, int scope) {
        // We use the commons file upload utilities to save a file
        if (fileItemFactory == null)
            fileItemFactory = new DiskFileItemFactory(0, SystemUtils.getTemporaryDirectory());
        final FileItem fileItem = fileItemFactory.createItem("dummy", "dummy", false, null);
        // Make sure the file is deleted appropriately
        if (scope == REQUEST_SCOPE) {
            deleteFileOnRequestEnd(pipelineContext, fileItem);
        } else if (scope == SESSION_SCOPE) {
            deleteFileOnSessionTermination(pipelineContext, fileItem);
        } else if (scope == APPLICATION_SCOPE) {
            deleteFileOnContextDestroyed(pipelineContext, fileItem);
        } else {
            throw new OXFException("Invalid context requested: " + scope);
        }
        // Return FileItem object
        return fileItem;
    }

    /**
     * Add listener to fileItem which is going to be automatically destroyed at the end of request
     *
     * @param pipelineContext PipelineContext
     * @param fileItem        FileItem
     */
    public static void deleteFileOnRequestEnd(PipelineContext pipelineContext, final FileItem fileItem) {
        // Make sure the file is deleted at the end of request
        pipelineContext.addContextListener(new PipelineContext.ContextListenerAdapter() {
            public void contextDestroyed(boolean success) {
                deleteFileItem(fileItem, REQUEST_SCOPE);
            }
        });
    }

    /**
     * Add listener to fileItem which is going to be automatically destroyed on session destruction
     *
     * @param pipelineContext PipelineContext
     * @param fileItem        FileItem
     */
    public static void deleteFileOnSessionTermination(PipelineContext pipelineContext, final FileItem fileItem) {
        // Try to delete the file on exit and on session termination
        final ExternalContext externalContext = (ExternalContext) pipelineContext.getAttribute(PipelineContext.EXTERNAL_CONTEXT);
        final ExternalContext.Session session = externalContext.getSession(false);
        if (session != null) {
            session.addListener(new ExternalContext.Session.SessionListener() {
                public void sessionDestroyed() {
                    deleteFileItem(fileItem, SESSION_SCOPE);
                }
            });
        } else {
            logger.debug("No existing session found so cannot register temporary file deletion upon session destruction: " + fileItem.getName());
        }
    }

    /**
     * Add listener to fileItem which is going to be automatically destroyed when the servlet is destroyed
     *
     * @param pipelineContext PipelineContext
     * @param fileItem        FileItem
     */
    public static void deleteFileOnContextDestroyed(PipelineContext pipelineContext, final FileItem fileItem) {
        // Try to delete the file on exit and on session termination
        final ExternalContext externalContext = (ExternalContext) pipelineContext.getAttribute(PipelineContext.EXTERNAL_CONTEXT);
        ExternalContext.Application application = externalContext.getApplication();
        if (application != null) {
            application.addListener(new ExternalContext.Application.ApplicationListener() {
                public void servletDestroyed() {
                    deleteFileItem(fileItem, APPLICATION_SCOPE);
                }
            });
        } else {
            logger.debug("No application object found so cannot register temporary file deletion upon session destruction: " + fileItem.getName());
        }
    }

    private static void deleteFileItem(FileItem fileItem, int scope) {
        if (logger.isDebugEnabled() && fileItem instanceof DiskFileItem) {
            final File storeLocation = ((DiskFileItem) fileItem).getStoreLocation();
            if (storeLocation != null) {
                final String temporaryFileName = storeLocation.getAbsolutePath();
                final String scopeString = (scope == REQUEST_SCOPE) ? "request" : (scope == SESSION_SCOPE) ? "session" : "application";
                logger.debug("Deleting temporary " + scopeString + "-scoped file: " + temporaryFileName);
            }
        }
        fileItem.delete();
    }

    /**
     * Convert a String in xs:anyURI to an xs:base64Binary.
     *
     * The URI has to be a URL. It is read entirely
     */
    public static String anyURIToBase64Binary(String value) {
        InputStream is = null;
        try {
            // Read from URL and convert to Base64
            is = URLFactory.createURL(value).openStream();
            final StringBuffer sb = new StringBuffer();
            XMLUtils.inputStreamToBase64Characters(is, new ContentHandlerAdapter() {
                public void characters(char ch[], int start, int length) {
                    sb.append(ch, start, length);
                }
            });
            // Return Base64 String
            return sb.toString();
        } catch (IOException e) {
            throw new OXFException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new OXFException(e);
                }
            }
        }
    }

    public static void anyURIToOutputStream(String value, OutputStream outputStream) {
        InputStream is = null;
        try {
            is = URLFactory.createURL(value).openStream();
            copyStream(is, outputStream);
        } catch (IOException e) {
            throw new OXFException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new OXFException(e);
                }
            }
        }
    }

    /**
     * Return the charset associated with a text/* Content-Type header. If a charset is present, return it. Otherwise,
     * guess depending on whether the mediatype is text/xml or not.
     *
     * @param contentType   Content-Type header value
     * @return              charset
     */
    public static String getTextCharsetFromContentType(String contentType) {
        final String charset;
        final String connectionCharset = getContentTypeCharset(contentType);
        if (connectionCharset != null) {
            charset = connectionCharset;
        } else {

            // RFC 3023: "Conformant with [RFC2046], if a text/xml entity is
            // received with the charset parameter omitted, MIME processors and
            // XML processors MUST use the default charset value of
            // "us-ascii"[ASCII]. In cases where the XML MIME entity is
            // transmitted via HTTP, the default charset value is still
            // "us-ascii". (Note: There is an inconsistency between this
            // specification and HTTP/1.1, which uses ISO-8859-1[ISO8859] as the
            // default for a historical reason. Since XML is a new format, a new
            // default should be chosen for better I18N. US-ASCII was chosen,
            // since it is the intersection of UTF-8 and ISO-8859-1 and since it
            // is already used by MIME.)"

            if (XMLUtils.isXMLMediatype(contentType))
                charset = DEFAULT_TEXT_XML_READING_ENCODING;
            else
                charset = DEFAULT_HTTP_TEXT_READING_ENCODING;
        }
        return charset;
    }

    /**
     * Remove the first path element of a path. Return null if there is only one path element
     *
     * E.g. /foo/bar => /bar?a=b
     *
     * @param path  path to modify
     * @return      modified path or null
     */
    public static String removeFirstPathElement(String path) {
        final int secondSlashIndex = path.indexOf('/', 1);
        if (secondSlashIndex == -1)
            return null;

        return path.substring(secondSlashIndex);
    }

    /**
     * Return the first path element of a path. If there is only one path element, return the entire path.
     *
     * E.g. /foo/bar => /foo
     *
     * @param path  path to analyze
     * @return      first path element
     */
    public static String getFirstPathElement(String path) {
        final int secondSlashIndex = path.indexOf('/', 1);
        if (secondSlashIndex == -1)
            return path;

        return path.substring(0, secondSlashIndex);
    }

    /**
     * Transform an URI accessible from the server into a URI accessible from the client. The mapping expires with the
     * session.
     *
     * @param propertyContext   context to obtain session
     * @param uri               server URI to transform
     * @param filename          file name
     * @param contentType       type of the content referred to by the URI, or null if unknown
     * @param lastModified      last modification timestamp
     * @return                  client URI
     */
    public static String proxyURI(PropertyContext propertyContext, String uri, String filename, String contentType, long lastModified) {

        // Create a digest, so that for a given URI we always get the same key
        final String digest = SecureUtils.digestString(uri, "MD5", "hex");

        // Get session
        final ExternalContext externalContext = (ExternalContext) propertyContext.getAttribute(PipelineContext.EXTERNAL_CONTEXT);
        final ExternalContext.Session session = externalContext.getSession(true);// NOTE: We force session creation here. Should we? What's the alternative?

        if (session != null) {
            // Store mapping into session
            session.getAttributesMap(ExternalContext.Session.APPLICATION_SCOPE).put(DYNAMIC_RESOURCES_SESSION_KEY + digest,
                    new DynamicResource(uri, filename, contentType, -1, lastModified));
        }

        // Rewrite new URI to absolute path without the context
        return DYNAMIC_RESOURCES_PATH + digest;
    }

    /**
     * Utility method to decode a multipart/fomr-data stream and return a Map of parameters of type Object[], each of
     * which can be a String or FileData.
     */
    public static Map<String, Object[]> getParameterMapMultipart(PipelineContext pipelineContext, final ExternalContext.Request request, String headerEncoding) {

        final Map<String, Object[]> uploadParameterMap = new HashMap<String, Object[]>();
        try {
            // Setup commons upload

            // Read properties
            // NOTE: We use properties scoped in the Request generator for historical reasons. Not too good.
            int maxSize = RequestGenerator.getMaxSizeProperty();
            int maxMemorySize = RequestGenerator.getMaxMemorySizeProperty();

            final DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory(maxMemorySize, SystemUtils.getTemporaryDirectory());

            final ServletFileUpload upload = new ServletFileUpload(diskFileItemFactory) {
                protected FileItem createItem(Map headers, boolean isFormField) throws FileUploadException {
                    if (isFormField) {
                        // Handle externalized values
                        final String externalizeFormValuesPrefix = org.orbeon.oxf.properties.Properties.instance().getPropertySet().getString(ServletExternalContext.EXTERNALIZE_FORM_VALUES_PREFIX_PROPERTY);
                        final String fieldName = getFieldName(headers);
                        if (externalizeFormValuesPrefix != null && fieldName.startsWith(externalizeFormValuesPrefix)) {
                            // In this case, we do as if the value content is an uploaded file so that it can be externalized
                            return super.createItem(headers, false);
                        } else {
                            // Just create the FileItem using the default way
                            return super.createItem(headers, isFormField);
                        }
                    } else {
                        // Just create the FileItem using the default way
                        return super.createItem(headers, isFormField);
                    }
                }
            };
            upload.setHeaderEncoding(headerEncoding);
            upload.setSizeMax(maxSize);

            // Add a listener to destroy file items when the pipeline context is destroyed
            pipelineContext.addContextListener(new PipelineContext.ContextListenerAdapter() {
                public void contextDestroyed(boolean success) {
                    for (final String name: uploadParameterMap.keySet()) {
                        final Object values[] = uploadParameterMap.get(name);
                        for (final Object currentValue: values) {
                            if (currentValue instanceof FileItem) {
                                final FileItem fileItem = (FileItem) currentValue;
                                fileItem.delete();
                            }
                        }
                    }
                }
            });

            // Wrap and implement just the required methods for the upload code
            final InputStream inputStream;
            try {
                inputStream = request.getInputStream();
            } catch (IOException e) {
                throw new OXFException(e);
            }

            final RequestContext requestContext = new RequestContext() {

                public int getContentLength() {
                    return request.getContentLength();
                }

                public InputStream getInputStream() {
                    // NOTE: The upload code does not actually check that it doesn't read more than the content-length
                    // sent by the client! Maybe here would be a good place to put an interceptor and make sure we
                    // don't read too much.
                    return new InputStream() {
                        public int read() throws IOException {
                            return inputStream.read();
                        }
                    };
                }

                public String getContentType() {
                    return request.getContentType();
                }

                public String getCharacterEncoding() {
                    return request.getCharacterEncoding();
                }
            };

            // Parse the request and add file information
            try {
                for (Object o: upload.parseRequest(requestContext)) {
                    final FileItem fileItem = (FileItem) o;
                    // Add value to existing values if any
                    if (fileItem.isFormField()) {
                        // Simple form field
                        // Assume that form fields are in UTF-8. Can they have another encoding? If so, how is it specified?
                        StringUtils.addValueToObjectArrayMap(uploadParameterMap, fileItem.getFieldName(), fileItem.getString(STANDARD_PARAMETER_ENCODING));
                    } else {
                        // File
                        StringUtils.addValueToObjectArrayMap(uploadParameterMap, fileItem.getFieldName(), fileItem);
                    }
                }
            } catch (FileUploadBase.SizeLimitExceededException e) {
                // Should we do something smart so we can use the Presentation
                // Server error page anyway? Right now, this is going to fail
                // miserably with an error.
                throw e;
            } catch (UnsupportedEncodingException e) {
                // Should not happen
                throw new OXFException(e);
            } finally {
                // Close the input stream; if we don't nobody does, and if this stream is
                // associated with a temporary file, that file may resist deletion
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw new OXFException(e);
                    }
                }
            }

            return uploadParameterMap;
        } catch (FileUploadException e) {
            throw new OXFException(e);
        }
    }

    public static class DynamicResource {
        private String uri;
        private String filename;
        private String contentType;
        private long size;
        private long lastModified;

        public DynamicResource(String uri, String filename, String contentType, long size, long lastModified) {
            this.uri = uri;
            this.filename = filename;
            this.contentType = contentType;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getURI() {
            return uri;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSize() {
            return size;
        }

        public long getLastModified() {
            return lastModified;
        }
    }
}
