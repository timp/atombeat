/**
 * Copyright (C) 2009 Orbeon, Inc.
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
package org.orbeon.oxf.processor.test;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.EmailProcessor;
import org.orbeon.oxf.processor.ProcessorUtils;
import org.orbeon.oxf.util.LoggerFactory;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.util.StringUtils;
import org.orbeon.oxf.util.URLRewriterUtils;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.XPathUtils;
import org.orbeon.oxf.xml.dom4j.LocationData;

import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.security.Principal;
import java.util.*;

/**
 * ExternalContext used by the TestScriptProcessor. It is configurable with an XML document representing
 * the request.
 */
public class TestExternalContext implements ExternalContext  {

    private static final Logger logger = LoggerFactory.createLogger(TestExternalContext.class);

    private PipelineContext pipelineContext;
    private Document requestDocument;

    private Request request;
    private Response response;

    private Map<String, Object> attributesMap;

    public TestExternalContext(PipelineContext pipelineContext, Document requestDocument) {
        this.pipelineContext = pipelineContext;
        this.requestDocument = requestDocument;
    }

    private class Request implements ExternalContext.Request {

        private Map<String, Object> attributesMap;
        private Map<String, Object[]> parameterMap;
        private Map<String, String> headerMap;
        private Map<String, String[]> headerValuesMap;

        private InputStream bodyInputStream;
        private String bodyContentType;
        private String bodyEncoding;
        private long bodyContentLength;
        private Reader bodyReader;
        private boolean getInputStreamCalled;
        private boolean getReaderCalled;

        public Map<String, Object> getAttributesMap() {
            if (attributesMap == null) {
                attributesMap = new LinkedHashMap<String, Object>();
                for (Iterator i = XPathUtils.selectIterator(requestDocument, "/*/attributes/attribute"); i.hasNext();) {
                    final Element e = (Element) i.next();
                    final String name = XPathUtils.selectStringValueNormalize(e, "name");
                    final String value = XPathUtils.selectStringValueNormalize(e, "value[1]");
                    attributesMap.put(name, value);
                }
            }
            return attributesMap;
        }

        public String getAuthType() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/auth-type");
        }

        public String getCharacterEncoding() {
            if (bodyInputStream == null)
                setupBody();
            return bodyEncoding;
        }

        public String getContainerType() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/container-type");
        }

        public String getContainerNamespace() {
            return "";
        }

        public int getContentLength() {
            if (bodyInputStream == null)
                setupBody();
            return (int) bodyContentLength;
        }

        public String getContentType() {
            if (bodyInputStream == null)
                setupBody();
            return bodyContentType;
        }

        public InputStream getInputStream() throws IOException {
            if (getReaderCalled)
                throw new IllegalStateException("Cannot call getInputStream() after getReader() has been called.");
            if (bodyInputStream == null)
                setupBody();
            getInputStreamCalled = true;
            return bodyInputStream;
        }

        private void setupBody() {
            try {
                Element bodyNode = (Element) XPathUtils.selectSingleNode(requestDocument, "/*/body");
                if (bodyNode != null) {
                    String contentTypeAttribute = bodyNode.attributeValue("content-type");
                    final String contentType = NetUtils.getContentTypeMediaType(contentTypeAttribute);
                    final String charset = NetUtils.getContentTypeCharset(contentTypeAttribute);

                    String hrefAttribute = bodyNode.attributeValue("href");
                    // TODO: Support same scenarios as Email processor
                    if (hrefAttribute == null)
                        throw new OXFException("Inline content not implemented yet.");

                    LocationData locationData = (LocationData) bodyNode.getData();
                    String systemId = locationData == null ? null : locationData.getSystemID();

                    SAXSource saxSource = EmailProcessor.getSAXSource(null, pipelineContext, hrefAttribute, systemId, contentType);
                    final FileItem fileItem = EmailProcessor.handleStreamedPartContent(pipelineContext, saxSource);

                    if (!(XMLUtils.isTextOrJSONContentType(contentType) || XMLUtils.isXMLMediatype(contentType))) {
                        // This is binary content
                        if (fileItem != null) {

                            bodyInputStream = fileItem.getInputStream();
                            bodyContentType = contentType;
                            bodyContentLength = fileItem.getSize();
                        } else {
                            // TODO
                            throw new OXFException("Not implemented yet.");
//                            byte[] data = XMLUtils.base64StringToByteArray((String) content);
//
//                            bodyInputStream = new ByteArrayInputStream(data);
//                            bodyContentType = contentType;
//                            bodyContentLength = data.length;
                        }
                    } else {
                        // This is text content
                        if (fileItem != null) {
                            // The text content was encoded when written to the FileItem

                            bodyInputStream = fileItem.getInputStream();
                            bodyContentType = contentType;
                            bodyEncoding = charset;
                            bodyContentLength = fileItem.getSize();

                        } else {
                            // TODO
                            throw new OXFException("Not implemented yet.");

//                            final String s = (String) content
//                            byte[] bytes = s.getBytes(charset);
//
//                            bodyInputStream = new ByteArrayInputStream(bytes);
//                            bodyContentType = contentType;
//                            bodyEncoding = charset;
//                            bodyContentLength = bytes.length;
                        }
                    }
                }
            } catch (Exception e) {
                throw new OXFException(e);
            }
        }

        public Reader getReader() throws IOException {
            if (getInputStreamCalled)
                throw new IllegalStateException("Cannot call getReader() after getInputStream() has been called.");
            if (bodyInputStream == null)
                setupBody();
            if (bodyReader == null)
                bodyReader = new InputStreamReader(bodyInputStream, bodyEncoding);
            getReaderCalled = true;
            return bodyReader;
        }

        public String getContextPath() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/context-path");
        }

        public Map<String, String> getHeaderMap() {
            if (headerMap == null) {
                final Map<String, String> map = new LinkedHashMap<String, String>();
                for (Iterator i = XPathUtils.selectIterator(requestDocument, "/*/headers/header"); i.hasNext();) {
                    final Element e = (Element) i.next();
                    final String name = XPathUtils.selectStringValueNormalize(e, "name");
                    final String value = XPathUtils.selectStringValueNormalize(e, "value[1]");
                    map.put(name, value);
                }
                headerMap = Collections.unmodifiableMap(map);
            }
            return headerMap;
        }

        public Map<String, String[]> getHeaderValuesMap() {
            if (headerValuesMap == null) {
                final Map<String, String[]> map = new LinkedHashMap<String, String[]>();
                for (Iterator i = XPathUtils.selectIterator(requestDocument, "/*/headers/header"); i.hasNext();) {
                    final Element e = (Element) i.next();
                    final String name = XPathUtils.selectStringValueNormalize(e, "name");
                    for (Iterator j = XPathUtils.selectIterator(e, "value"); j.hasNext();) {
                        final Element valueElement = (Element) j.next();
                        final String value = XPathUtils.selectStringValueNormalize(valueElement, ".");
                        StringUtils.addValueToStringArrayMap(map, name, value);
                    }
                }
                headerValuesMap = Collections.unmodifiableMap(map);
            }
            return headerValuesMap;
        }

        public Locale getLocale() {
            // NIY
            return null;
        }

        public Enumeration getLocales() {
            // NIY
            return null;
        }

        public String getMethod() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/method");
        }

        public Map<String, Object[]> getParameterMap() {
            if (parameterMap == null) {
                final Map<String, Object[]> map = new LinkedHashMap<String, Object[]>();
                for (Iterator i = XPathUtils.selectIterator(requestDocument, "/*/parameters/parameter"); i.hasNext();) {
                    final Element e = (Element) i.next();
                    final String name = XPathUtils.selectStringValueNormalize(e, "name");
                    for (Iterator j = XPathUtils.selectIterator(e, "value"); j.hasNext();) {
                        final Element valueElement = (Element) j.next();
                        final String value = XPathUtils.selectStringValueNormalize(valueElement, ".");
                        StringUtils.addValueToObjectArrayMap(map, name, value);
                    }
                }
                parameterMap = Collections.unmodifiableMap(map);
            }
            return parameterMap;
        }

        public String getPathInfo() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/path-info");
        }

        public String getPathTranslated() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/path-translated");
        }

        public String getProtocol() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/protocol");
        }

        public String getQueryString() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/query-string");
        }

        public String getRemoteAddr() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/remote-addr");
        }

        public String getRemoteHost() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/remote-host");
        }

        public String getRemoteUser() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/remote-user");
        }

        public String getRequestedSessionId() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/requested-session-id");
        }

        public String getRequestPath() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/request-path");
        }

        public String getRequestURI() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/request-uri");
        }

        public String getRequestURL() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/request-url");
        }

        public String getScheme() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/scheme");
        }

        public String getServerName() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/server-name");
        }

        public int getServerPort() {
            return ProcessorUtils.selectIntValue(requestDocument, "/*/server-port", 80);
        }

        public String getServletPath() {
            return XPathUtils.selectStringValueNormalize(requestDocument, "/*/servlet-path");
        }

        public String getClientContextPath(String urlString) {
            return URLRewriterUtils.getClientContextPath(this, URLRewriterUtils.isPlatformPath(urlString));
        }

        public Principal getUserPrincipal() {
            // NIY
            return null;
        }

        public boolean isRequestedSessionIdValid() {
            // NIY
            return false;
        }

        public boolean isSecure() {
            return ProcessorUtils.selectBooleanValue(requestDocument, "/*/is-secure", false);
        }

        public boolean isUserInRole(String role) {
            // NIY
            return false;
        }

        public void sessionInvalidate() {
            // NIY
        }

        public String getPortletMode() {
            return null;
        }

        public String getWindowState() {
            return null;
        }

        public Object getNativeRequest() {
            return TestExternalContext.this.getNativeRequest();
        }
    }

    private class Response implements ExternalContext.Response {
        public void addHeader(String name, String value) {
        }

        public boolean checkIfModifiedSince(long lastModified, boolean allowOverride) {
            return true;
        }

        public String getCharacterEncoding() {
            return null;
        }

        public String getNamespacePrefix() {
            return null;
        }

        public OutputStream getOutputStream() throws IOException {
            return null;
        }

        public PrintWriter getWriter() throws IOException {
            return null;
        }

        public boolean isCommitted() {
            return false;
        }

        public void reset() {
        }

        public String rewriteActionURL(String urlString) {
            return rewriteResourceURL(urlString, false);
        }

        public String rewriteRenderURL(String urlString) {
            return rewriteResourceURL(urlString, false);
        }

        public String rewriteActionURL(String urlString, String portletMode, String windowState) {
            return rewriteResourceURL(urlString, false);
        }

        public String rewriteRenderURL(String urlString, String portletMode, String windowState) {
            return rewriteResourceURL(urlString, false);
        }

        public String rewriteResourceURL(String urlString, boolean absolute) {
            return rewriteResourceURL(urlString, absolute ? REWRITE_MODE_ABSOLUTE : REWRITE_MODE_ABSOLUTE_PATH_OR_RELATIVE);
        }

        public String rewriteResourceURL(String urlString, int rewriteMode) {
            return URLRewriterUtils.rewriteURL(getRequest(), urlString, rewriteMode);
        }

        public void sendError(int len) throws IOException {
        }

        public void sendRedirect(String pathInfo, Map parameters, boolean isServerSide, boolean isExitPortal, boolean isNoRewrite) throws IOException {
        }

        public void setCaching(long lastModified, boolean revalidate, boolean allowOverride) {
        }

        public void setResourceCaching(long lastModified, long expires) {
        }

        public void setContentLength(int len) {
        }

        public void setContentType(String contentType) {
        }

        public void setHeader(String name, String value) {
        }

        public void setStatus(int status) {
        }

        public void setTitle(String title) {
        }

        public Object getNativeResponse() {
            return TestExternalContext.this.getNativeResponse();
        }
    }

    public ExternalContext.Request getRequest() {
        if (request == null)
            request = new Request();
        return request;
    }

    public ExternalContext.Response getResponse() {
        if (response == null)
            response = new Response();
        return response;
    }

    private ExternalContext.Session session;

    public ExternalContext.Session getSession(boolean create) {
        if (session == null && create) {
            session = new ExternalContext.Session() {
                final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
                public long getCreationTime() {
                    return System.currentTimeMillis();
                }

                public String getId() {
                    // TODO
                    return null;
                }

                public long getLastAccessedTime() {
                    // TODO
                    return 0;
                }

                public int getMaxInactiveInterval() {
                    // TODO
                    return 0;
                }

                public void invalidate() {
                    // TODO
                }

                public boolean isNew() {
                    // TODO
                    return false;
                }

                public void setMaxInactiveInterval(int interval) {
                    // TODO
                }

                public Map<String, Object> getAttributesMap() {
                    return attributes;
                }

                public Map<String, Object> getAttributesMap(int scope) {
                    return attributes;
                }

                public void addListener(SessionListener sessionListener) {
                    // TODO
                }

                public void removeListener(SessionListener sessionListener) {
                    // TODO
                }
            };
        }
        return session;
    }

    public ExternalContext.Application getApplication() {
        // NIY
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String path, boolean isContextRelative) {
        // NIY
        return null;
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        // NIY
        return null;
    }

    public String getStartLoggerString() {
        return "Running test processor";
    }

    public String getEndLoggerString() {
        return "Done running test processor";
    }

    public Object getNativeRequest() {
        return null;
    }

    public Object getNativeResponse() {
        return null;
    }

    public Object getNativeSession(boolean flag) {
        return null;
    }

    public Map<String, Object> getAttributesMap() {
        if (attributesMap == null) {
            attributesMap = new LinkedHashMap<String, Object>();
        }
        return attributesMap;
    }

    public Map<String, String> getInitAttributesMap() {
        return Collections.emptyMap();
    }

    public Object getNativeContext() {
        return null;
    }

    public String getRealPath(String path) {
        // NIY
        return null;
    }

    public void log(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void log(String msg) {
        logger.info(msg);
    }

    public String rewriteServiceURL(String urlString, boolean forceAbsolute) {
        return URLRewriterUtils.rewriteServiceURL(getRequest(), urlString, forceAbsolute);
    }
}
