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
package org.orbeon.oxf.xforms.processor;

import org.apache.log4j.Logger;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.common.Version;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.resources.ResourceManagerWrapper;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.util.*;
import org.orbeon.oxf.xforms.XFormsContainingDocument;
import org.orbeon.oxf.xforms.XFormsProperties;
import org.orbeon.oxf.xforms.XFormsUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Serve XForms engine JavaScript and CSS resources by combining them.
 */
public class XFormsResourceServer extends ProcessorImpl {

    public static final String LOGGING_CATEGORY = "resources";
    private static final Logger logger = LoggerFactory.createLogger(XFormsResourceServer.class);
    private static final long ONE_YEAR_IN_MILLISECONDS = 365L * 24 * 60 * 60 * 1000;

    public XFormsResourceServer() {
    }

    public static IndentedLogger getIndentedLogger() {
        return XFormsContainingDocument.getIndentedLogger(logger, XFormsServer.getLogger(), LOGGING_CATEGORY);
    }

    @Override
    public void start(PipelineContext pipelineContext) {
        final ExternalContext externalContext = XFormsUtils.getExternalContext(pipelineContext);
        final ExternalContext.Request request = externalContext.getRequest();
        final ExternalContext.Response response = externalContext.getResponse();

        final String requestPath = request.getRequestPath();
        final String filename = requestPath.substring(requestPath.lastIndexOf('/') + 1);
        
        final IndentedLogger indentedLogger = getIndentedLogger();

        if (requestPath.startsWith(NetUtils.DYNAMIC_RESOURCES_PATH)) {
            // Dynamic resource requested

            final ExternalContext.Session session = externalContext.getSession(false);
            if (session != null) {
                // Store mapping into session
                final String lookupKey = NetUtils.DYNAMIC_RESOURCES_SESSION_KEY + filename;
                NetUtils.DynamicResource resource = (NetUtils.DynamicResource) session.getAttributesMap().get(lookupKey);

                if (resource != null && resource.getURI() != null) {
                    // Found URI, stream it out

                    // Set caching headers

                    // NOTE: Algorithm is that XFOutputControl currently passes either -1 or the last modified of the
                    // resource if "fast" to obtain last modified ("oxf:" or "file:"). Would be nice to do better: pass
                    // whether resource is cacheable or not; here, when dereferencing the resource, we get the last
                    // modified (Last-Modified header from HTTP even) and store it. Then we can handle conditional get.
                    // This is some work though. Might have to proxy conditional GET as well. So for now we don't
                    // handle conditional GET and produce a non-now last modified only in a few cases.
                    
                    response.setCaching(resource.getLastModified(), false, false);

                    if (resource.getSize() > 0)
                        response.setContentLength((int) resource.getSize());// NOTE: Why does this API (and Servlet counterpart) take an int?
                    
                    // TODO: for Safari, try forcing application/octet-stream
                    // NOTE: IE 6/7 don't display a download box when detecting an HTML document (known IE bug)
                    if (resource.getContentType() != null)
                        response.setContentType(resource.getContentType());
                    else
                        response.setContentType("application/octet-stream");

                    // File name visible by the user
                    final String contentFilename = resource.getFilename() != null ? resource.getFilename() : filename;

                    // Handle as attachment
                    // TODO: should try to provide extension based on mediatype if file name is not provided?
                    // TODO: filename should be encoded somehow, as 1) spaces don't work and 2) non-ISO-8859-1 won't work
                    try {
//                        response.setHeader("Content-Disposition", "?utf-8?b?" + Base64.encode(("attachment; filename=" + contentFilename).getBytes("UTF-8")) +"?=");
                        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(contentFilename, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        // Will not happen
                        throw new OXFException(e);
                    }

                    // Copy stream out
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        // The resource URI may already be absolute, or may be relative to the server base. Make sure we work with an absolute URI.
                        final String absoluteResourceURI = externalContext.rewriteServiceURL(resource.getURI(), true);

                        is = URLFactory.createURL(absoluteResourceURI).openStream();
                        os = response.getOutputStream();
                        NetUtils.copyStream(is, os);
                        os.flush();
                    } catch (Exception e) {
                        indentedLogger.logWarning("", "exception copying stream", e);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                indentedLogger.logWarning("", "exception closing input stream", e);
                            }
                        }
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                indentedLogger.logWarning("", "exception closing output stream", e);
                            }
                        }
                    }
                } else {
                    // Not found
                    response.setStatus(ExternalContext.SC_NOT_FOUND);
                }
            }

        } else {
            // CSS or JavaScript resource requested
            final boolean isCSS = filename.endsWith(".css");
            final boolean isJS = filename.endsWith(".js");

            // Eliminate funny requests
            if (!isCSS && !isJS) {
                response.setStatus(ExternalContext.SC_NOT_FOUND);
                return;
            }

            // Find what features are requested
            // Assume a file name of the form: xforms-feature1-feature2-feature3-...[-min].[css|js]
            boolean isMinimal = false;
            final Map<String, XFormsFeatures.FeatureConfig> requestedFeaturesMap = new HashMap<String, XFormsFeatures.FeatureConfig>();
            {
                final StringTokenizer st = new StringTokenizer(filename.substring(0, filename.lastIndexOf(".")), "-");
                while (st.hasMoreTokens()) {
                    final String currentToken = st.nextToken();

                    if (currentToken.equals("min")) {
                        // Request for minimal resources
                        isMinimal = true;
                        continue;
                    } else if (currentToken.equals("xforms")) {
                        // Ignore this token
                        continue;
                    }

                    final XFormsFeatures.FeatureConfig currentFeature = XFormsFeatures.getFeatureById(currentToken);
                    if (currentFeature != null) {
                        // Add feature
                        requestedFeaturesMap.put(currentFeature.getName(), currentFeature);
                    } else {
                        // Don't allow unknown features
                        response.setStatus(ExternalContext.SC_NOT_FOUND);
                        return;
                    }
                }
            }

            // Determine list of resources to load
            final List<XFormsFeatures.ResourceConfig> resources;
            if (isCSS)
                resources = XFormsFeatures.getCSSResourcesByFeatureMap(requestedFeaturesMap);
            else
                resources = XFormsFeatures.getJavaScriptResourcesByFeatureMap(requestedFeaturesMap);

            // Get last modified date
            final long combinedLastModified = computeCombinedLastModified(resources, isMinimal);

            // If conditional get and date ok, send not modified

            // Set Last-Modified, required for caching and conditional get
            if (URLRewriterUtils.isResourcesVersioned()) {
                // Use expiration far in the future
                response.setResourceCaching(combinedLastModified, combinedLastModified + ONE_YEAR_IN_MILLISECONDS);
            } else {
                // Use standard expiration policy
                response.setResourceCaching(combinedLastModified, 0);
            }

            // Check If-Modified-Since and don't return content if condition is met
            if (!response.checkIfModifiedSince(combinedLastModified, false)) {
                response.setStatus(ExternalContext.SC_NOT_MODIFIED);
                return;
            }

            OutputStream os = null;
            try {
                os = response.getOutputStream();
                response.setContentType(isCSS ? "text/css" : "application/x-javascript");
                {
                    final boolean isDebugEnabled = indentedLogger.isDebugEnabled();
                    if (XFormsProperties.isCacheCombinedResources()) {
                        // Caching requested
                        final File resourceFile = cacheResources(indentedLogger, resources, pipelineContext, requestPath, combinedLastModified, isCSS, isMinimal);
                        if (resourceFile != null) {
                            // Caching could take place, send out cached result
                            if (isDebugEnabled)
                                indentedLogger.logDebug("resources", "serving from cache ", "request path", requestPath);
                            final FileInputStream fis = new FileInputStream(resourceFile);
                            NetUtils.copyStream(fis, os);
                            fis.close();
                            os.flush();
                        } else {
                            // Was unable to cache, just serve
                            if (isDebugEnabled)
                                indentedLogger.logDebug("resources", "caching requested but not possible, serving directly", "request path", requestPath);
                            generate(indentedLogger, resources, pipelineContext, os, isCSS, isMinimal);
                        }
                    } else {
                        // Should not cache, just serve
                        if (isDebugEnabled)
                            indentedLogger.logDebug("resources", "caching not requested, serving directly", "request path", requestPath);
                        generate(indentedLogger, resources, pipelineContext, os, isCSS, isMinimal);
                    }
                }
            } catch (OXFException e) {
                throw e;
            } catch (Exception e) {
                throw new OXFException(e);
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        indentedLogger.logWarning("", "exception closing output stream", e);
                    }
                }
            }
        }
    }

    /**
     * Compute the last modification date of the given resources.
     *
     * @param resources     list of XFormsFeatures.ResourceConfig to consider
     * @param isMinimal     whether to use minimal resources
     * @return              last modification date
     */
    public static long computeCombinedLastModified(List<XFormsFeatures.ResourceConfig> resources, boolean isMinimal) {
        long combinedLastModified = 0;
        for (final XFormsFeatures.ResourceConfig resource: resources) {
            final long lastModified = ResourceManagerWrapper.instance().lastModified(resource.getResourcePath(isMinimal), false);
            if (lastModified > combinedLastModified)
                combinedLastModified = lastModified;
        }
        return combinedLastModified;
    }

    /**
     * Try to cache the combined resources on disk.
     *
     * @param indentedLogger        logger
     * @param resources             list of XFormsFeatures.ResourceConfig to consider
     * @param propertyContext       current PipelineContext (used for rewriting and matchers)
     * @param resourcePath          path to store the cached resource to
     * @param combinedLastModified  last modification date of the resources to combine
     * @param isCSS                 whether to generate CSS or JavaScript resources
     * @param isMinimal             whether to use minimal resources
     * @return                      File pointing to the generated resource, null if caching could not take place
     */
    public static File cacheResources(IndentedLogger indentedLogger, List<XFormsFeatures.ResourceConfig> resources,
                                      PropertyContext propertyContext, String resourcePath, long combinedLastModified,
                                      boolean isCSS, boolean isMinimal) {
        try {
            final File resourceFile;
            final String realPath = ResourceManagerWrapper.instance().getRealPath(resourcePath);
            final boolean isDebugEnabled = indentedLogger.isDebugEnabled();
            if (realPath != null) {
                // We hope to be able to cache as a resource
                resourceFile = new File(realPath);
                if (resourceFile.exists()) {
                    // Resources exist, generate if needed
                    final long resourceLastModified = resourceFile.lastModified();
                    if (resourceLastModified < combinedLastModified) {
                        // Resource is out of date, generate
                        if (isDebugEnabled)
                            indentedLogger.logDebug("resources", "cached combined resources out of date, saving", "resource path", resourcePath);
                        final FileOutputStream fos = new FileOutputStream(resourceFile);
                        generate(indentedLogger, resources, propertyContext, fos, isCSS, isMinimal);
                    } else {
                        if (isDebugEnabled)
                            indentedLogger.logDebug("resources", "cached combined resources exist and are up-to-date", "resource path", resourcePath);
                    }
                } else {
                    // Resource doesn't exist, generate
                    if (isDebugEnabled)
                        indentedLogger.logDebug("resources", "cached combined resources don't exist, saving", "resource path", resourcePath);
                    resourceFile.getParentFile().mkdirs();
                    resourceFile.createNewFile();
                    final FileOutputStream fos = new FileOutputStream(resourceFile);
                    generate(indentedLogger, resources, propertyContext, fos, isCSS, isMinimal);
                }
            } else {
                if (isDebugEnabled)
                    indentedLogger.logDebug("resources", "unable to locate real path for cached combined resources, not saving", "resource path", resourcePath);
                resourceFile = null;
            }
            return resourceFile;
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }

    /**
     * Generate the resources into the given OutputStream. The stream is flushed and closed when done.
     *
     * @param indentedLogger        logger
     * @param resources             list of XFormsFeatures.ResourceConfig to consider
     * @param propertyContext       current PipelineContext (used for rewriting and matchers)
     * @param os                    OutputStream to write to
     * @param isCSS                 whether to generate CSS or JavaScript resources
     * @param isMinimal             whether to use minimal resources
     * @throws URISyntaxException
     * @throws IOException
     */
    private static void generate(IndentedLogger indentedLogger, List<XFormsFeatures.ResourceConfig> resources, PropertyContext propertyContext, OutputStream os, boolean isCSS, boolean isMinimal) throws URISyntaxException, IOException {
        if (isCSS) {
            // CSS: rewrite url() in content

            final ExternalContext externalContext = XFormsUtils.getExternalContext(propertyContext);

            final Writer outputWriter = new OutputStreamWriter(os, "utf-8");

            // Create matcher that matches all paths in case resources are versioned
            final List<URLRewriterUtils.PathMatcher> matchAllPathMatcher = URLRewriterUtils.getMatchAllPathMatcher();

            // Output Orbeon Forms version
            outputWriter.write("/* This file was produced by Orbeon Forms " + Version.getVersion() + " */\n");

            for (final XFormsFeatures.ResourceConfig resource: resources) {
                final String resourcePath = resource.getResourcePath(isMinimal);
                final InputStream is = ResourceManagerWrapper.instance().getContentAsStream(resourcePath);

                final String content;
                {
                    final Reader reader = new InputStreamReader(is, "utf-8");
                    final StringBuilderWriter StringBuilderWriter = new StringBuilderWriter();
                    NetUtils.copyStream(reader, StringBuilderWriter);
                    reader.close();
                    content = StringBuilderWriter.toString();
                }

                final URI unresolvedResourceURI = new URI(resourcePath);
                {
                    int index = 0;
                    while (true) {
                        final int newIndex = content.indexOf("url(", index);

                        if (newIndex == -1) {
                            // Output remainder
                            if (index == 0)
                                outputWriter.write(content);
                            else
                                outputWriter.write(content.substring(index));
                            break;
                        } else {
                            // output so far
                            outputWriter.write(content.substring(index, newIndex));
                        }

                        // Get URL
                        String uriString;
                        {
                            final int closingIndex = content.indexOf(")", newIndex + 4);
                            if (closingIndex == -1)
                                throw new OXFException("Missing closing parenthesis in url() in resource: " + resource.getResourcePath(isMinimal));

                            uriString = content.substring(newIndex + 4, closingIndex);

                            // Some URLs seem to start and end with quotes
                            if (uriString.startsWith("\""))
                                uriString = uriString.substring(1);

                            if (uriString.endsWith("\""))
                                uriString = uriString.substring(0, uriString.length() - 1);

                            index = closingIndex + 1;
                        }
                        // Rewrite URL and output it as an absolute path
                        try {
                            final URI resolvedResourceURI = unresolvedResourceURI.resolve(uriString.trim()).normalize();// normalize to remove "..", etc.

                            final String rewrittenURI = URLRewriterUtils.rewriteResourceURL(externalContext.getRequest(),
                                    resolvedResourceURI.toString(), matchAllPathMatcher, ExternalContext.Response.REWRITE_MODE_ABSOLUTE_PATH);

                            outputWriter.write("url(" + rewrittenURI + ")");
                        } catch (Exception e) {
                            indentedLogger.logWarning("resources", "found invalid URI in CSS file", "uri", uriString);
                            outputWriter.write("url(" + uriString + ")");
                        }
                    }
                }
            }
            outputWriter.flush();
        } else {
            // JavaScript: just send out

            // Output Orbeon Forms version
            final Writer outputWriter = new OutputStreamWriter(os, "utf-8");
            outputWriter.write("// This file was produced by Orbeon Forms " + Version.getVersion() + "\n");
            outputWriter.flush();

            // Output
            int index = 0;
            for (Iterator<XFormsFeatures.ResourceConfig> i = resources.iterator(); i.hasNext(); index++) {
                final XFormsFeatures.ResourceConfig resourceConfig = i.next();
                final InputStream is = ResourceManagerWrapper.instance().getContentAsStream(resourceConfig.getResourcePath(isMinimal));
                // Line break seems to help. We assume that the encoding is compatible with ASCII/UTF-8
                if (index > 0)
                    os.write((byte) '\n');
                NetUtils.copyStream(is, os);
                is.close();
            }
        }
        os.flush();
        os.close();
    }
}
