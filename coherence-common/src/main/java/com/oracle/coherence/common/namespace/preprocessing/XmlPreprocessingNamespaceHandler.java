/*
 * File: XmlPreprocessingNamespaceHandler.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.common.namespace.preprocessing;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ExpressionParser;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.QualifiedName;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;
import com.tangosol.util.Base;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The {@link XmlPreprocessingNamespaceHandler} provides specialized {@link XmlElement}
 * pre and post processing capabilities for configurations files, useful for
 * transforming {@link XmlElement}s on the fly.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @author Jonathan Knight
 */
public class XmlPreprocessingNamespaceHandler extends AbstractNamespaceHandler
{
    /**
     * The 'service-name' {@link XmlElement}.
     */
    private static final String SERVICE_NAME = "service-name";

    /**
     * The 'scheme-name' {@link XmlElement}.
     */
    private static final String SCHEME_NAME = "scheme-name";

    /**
     * The 'caching-schemes' {@link XmlElement}.
     */
    private static final String CACHING_SCHEMES = "caching-schemes";

    /**
     * The 'cache-name' {@link XmlElement}.
     */
    private static final String CACHE_NAME = "cache-name";

    /**
     * The 'caching-scheme-mapping' {@link XmlElement}.
     */
    private static final String CACHING_SCHEME_MAPPING = "caching-scheme-mapping";

    /**
     * The 'defaults' {@link XmlElement}.
     */
    private static final String DEFAULTS = "defaults";

    /**
     * The 'interceptors' {@link XmlElement}.
     */
    private static final String INTERCEPTORS = "interceptors";

    /**
     * The 'name' {@link XmlElement} (for interceptors).
     */
    private static final String NAME = "name";

    /**
     * The 'originated-from' {@link XmlElement}.
     */
    private static final String ORIGINATED_FROM = "originated-from";

    /**
     * The 'replace-with-file' {@link XmlElement}.
     */
    private static final String REPLACE_WITH_FILE = "replace-with-file";

    /**
     * The 'introduce-cache-config' {@link XmlElement}.
     */
    private static final String INTRODUCE_CACHE_CONFIG = "introduce-cache-config";

    /**
     * The set of currently introduced cache configuration file uris.
     * (we keep track of this so we don't re-introduce previously introduced urs).
     */
    private HashSet<String> introducedURIs;

    /**
     * The prefix that was used to register the namespace.
     */
    private String prefix;

    /**
     * The {@link QualifiedName} representing the "introduce-cache-config" attribute.
     */
    private QualifiedName qnIntroduceCacheConfig;

    /**
     * The {@link QualifiedName} representing the "replace-with-file" attribute.
     */
    private QualifiedName qnReplaceWithFile;

    /**
     * The {@link QualifiedName} representing the "originated-from" attribute.
     */
    private QualifiedName qnOriginatedFrom;


    /**
     * Constructs an {@link XmlPreprocessingNamespaceHandler}.
     */
    public XmlPreprocessingNamespaceHandler()
    {
        // remember the uri's we've already introduced
        introducedURIs = new HashSet<String>();

        // establish a document preprocessor for the namespace
        DocumentElementPreprocessor dpp = new DocumentElementPreprocessor();

        // register an element preprocessor for "introduce-cache-config" directives
        dpp.addElementPreprocessor(new IntroduceCacheConfigPreprocessor());

        // register an element preprocessor for "replace-with-file" directives
        dpp.addElementPreprocessor(new ReplaceWithFilePreprocessor());

        setDocumentPreprocessor(dpp);
    }


    @Override
    public void onStartNamespace(ProcessingContext context,
                                 XmlElement        element,
                                 String            prefix,
                                 URI               uri)
    {
        super.onStartNamespace(context, element, prefix, uri);

        // we need to remember the prefix that was used to declare the namespace
        // so that we can use it later when pre-processing
        this.prefix = prefix;

        // establish some constants for the elements/attributes for this namespace
        qnIntroduceCacheConfig = new QualifiedName(this.prefix, INTRODUCE_CACHE_CONFIG);
        qnReplaceWithFile      = new QualifiedName(this.prefix, REPLACE_WITH_FILE);
        qnOriginatedFrom       = new QualifiedName(this.prefix, ORIGINATED_FROM);
    }


    /**
     * Merges the cache config elements from on cache config into another
     * cache config (mutating the "into" cache config).
     *
     * @param sFromURI            the URI to which the from element belongs
     * @param xmlFromCacheConfig  the element from which the merge should occur
     * @param xmlIntoCacheConfig  the element into which the merge should occur
     */
    @SuppressWarnings("unchecked")
    public void mergeCacheConfig(ProcessingContext context,
                                 String            sFromURI,
                                 XmlElement        xmlFromCacheConfig,
                                 XmlElement        xmlIntoCacheConfig)
    {
        Base.azzert(xmlFromCacheConfig != null);
        Base.azzert(xmlIntoCacheConfig != null);
        Base.azzert(xmlFromCacheConfig.getName().equals(xmlIntoCacheConfig.getName()));

        CacheConfig cacheConfig = context.getCookie(CacheConfig.class);

        Base.azzert(cacheConfig != null);

        // merge the attributes
        Map<String, XmlValue> mapFromAttributes = xmlFromCacheConfig.getAttributeMap();
        Map<String, XmlValue> mapIntoAttributes = xmlIntoCacheConfig.getAttributeMap();

        for (String sFromAttribute : mapFromAttributes.keySet())
        {
            if (!mapIntoAttributes.containsKey(sFromAttribute))
            {
                mapIntoAttributes.put(sFromAttribute, mapFromAttributes.get(sFromAttribute));

                QualifiedName qualifiedName = new QualifiedName(sFromAttribute);

                if (qualifiedName.getLocalName().equals("xmlns"))
                {
                    String sURI = mapFromAttributes.get(sFromAttribute).getString();

                    try
                    {
                        context.ensureNamespaceHandler(qualifiedName.getPrefix(), new URI(sURI));
                    }
                    catch (URISyntaxException uriSyntaxException)
                    {
                        throw new ConfigurationException(String.format("Invalid URI '%s' specified for Xml Namespace '%s'",
                                                                       sURI,
                                                                       qualifiedName.getPrefix()),
                                                         "You must specify a valid URI for the Xml Namespace.",
                                                         uriSyntaxException);
                    }
                }
            }
        }

        for (XmlElement element : (List<XmlElement>) xmlFromCacheConfig.getElementList())
        {
            if (element.getName().equals(CACHING_SCHEME_MAPPING))
            {
                mergeCacheMappings(context, sFromURI, element, xmlIntoCacheConfig);
            }
            else if (element.getName().equals(CACHING_SCHEMES))
            {
                mergeCachingSchemes(context, sFromURI, element, xmlIntoCacheConfig);
            }
            else if (element.getName().equals(DEFAULTS))
            {
                mergeDefaults(context, sFromURI, element, xmlIntoCacheConfig);
            }
            else if (element.getName().equals(INTERCEPTORS))
            {
                mergeInterceptors(context, sFromURI, element, xmlIntoCacheConfig);
            }
            else
            {
                mergeForeignElement(context, sFromURI, element, xmlIntoCacheConfig);
            }
        }
    }


    /**
     * Merges the cache mappings from one cache config into another cache config
     * (mutating the "into" cache config).
     *
     * @param sFromURI                 the URI to which the from element belongs
     * @param xmlCachingSchemeMapping  the cache mapping element from which the merge should occur
     * @param xmlIntoCacheConfig       the cache config into which the merge should occur
     */
    @SuppressWarnings("unchecked")
    private void mergeCacheMappings(ProcessingContext context,
                                    String            sFromURI,
                                    XmlElement        xmlCachingSchemeMapping,
                                    XmlElement        xmlIntoCacheConfig)
    {
        if (xmlCachingSchemeMapping != null)
        {
            for (XmlElement xmlCacheMapping : (List<XmlElement>) xmlCachingSchemeMapping.getElementList())
            {
                QualifiedName qualifiedName = xmlCacheMapping.getQualifiedName();

                if (qualifiedName.hasPrefix())
                {
                    mergeForeignElement(context,
                                        sFromURI,
                                        xmlCacheMapping,
                                        xmlIntoCacheConfig.ensureElement(CACHING_SCHEME_MAPPING));
                }
                else
                {
                    XmlElement xmlCacheName = xmlCacheMapping.getElement(CACHE_NAME);

                    if (xmlCacheName != null)
                    {
                        String sCacheName = xmlCacheName.getString().trim();

                        if (isCacheMappingDefinedFor(sCacheName, xmlIntoCacheConfig))
                        {
                            // SKIP: we won't merge in the cache mapping as it
                            // already exists in the destination cache config
                        }
                        else
                        {
                            // clone the element to merge
                            XmlElement xmlMergeElement = (XmlElement) xmlCacheMapping.clone();

                            // annotate the origin of the merging element
                            xmlMergeElement.addAttribute(qnOriginatedFrom.getName()).setString(sFromURI);

                            xmlIntoCacheConfig.ensureElement(CACHING_SCHEME_MAPPING).getElementList()
                            .add(xmlMergeElement);
                        }
                    }
                }
            }
        }
    }


    /**
     * Merges the cache scheme definitions from one cache config into another
     * cache config (mutating the "into" cache config).
     *
     * @param sFromURI            the URI to which the from element belongs
     * @param xmlSchemes          the cache schemes element from which the merge should occur
     * @param xmlIntoCacheConfig  the cache config into which the merge should occur
     */
    @SuppressWarnings("unchecked")
    private void mergeCachingSchemes(ProcessingContext context,
                                     String            sFromURI,
                                     XmlElement        xmlSchemes,
                                     XmlElement        xmlIntoCacheConfig)
    {
        if (xmlSchemes != null)
        {
            for (XmlElement xmlScheme : (List<XmlElement>) xmlSchemes.getElementList())
            {
                QualifiedName qualifiedName = xmlScheme.getQualifiedName();

                if (qualifiedName.hasPrefix())
                {
                    mergeForeignElement(context,
                                        sFromURI,
                                        xmlScheme,
                                        xmlIntoCacheConfig.ensureElement(CACHING_SCHEMES));
                }
                else
                {
                    String sSchemeName = getSchemeNameFor(xmlScheme);

                    if (sSchemeName != null)
                    {
                        if (isSchemeDefinedFor(sSchemeName, xmlIntoCacheConfig))
                        {
                            // SKIP: we won't merge the scheme as one is already
                            // defined in the destination cache config
                        }
                        else
                        {
                            // clone the element to merge
                            XmlElement xmlMergeElement = (XmlElement) xmlScheme.clone();

                            // annotate the origin of the merging element
                            xmlMergeElement.addAttribute(qnOriginatedFrom.getName()).setString(sFromURI);

                            xmlIntoCacheConfig.ensureElement(CACHING_SCHEMES).getElementList().add(xmlMergeElement);
                        }
                    }
                }
            }
        }
    }


    /**
     * Merges the cache config &lt;defaults&gt; elements from one cache configuration
     * into another cache defaults element (mutating the "into" cache config).
     * <p>
     * NOTE: Merging will only occur if the 'into' cache doesn't already contain a
     * defaults element.
     *
     * @param context             the {@link ProcessingContext} in which the merge is occurring
     * @param sFromURI            the URI to which the 'from' element belongs
     * @param xmlFromDefaults     the defaults element from which the merge should occur
     * @param xmlIntoCacheConfig  the element into which the merge should occur
     */
    @SuppressWarnings("unchecked")
    private void mergeDefaults(ProcessingContext context,
                               String            sFromURI,
                               XmlElement        xmlFromDefaults,
                               XmlElement        xmlIntoCacheConfig)
    {
        // we only attempt to merge 'from' <defaults> when it isn't already
        // defined in the 'into' cache configuration
        if (xmlIntoCacheConfig.getElement(DEFAULTS) == null && xmlFromDefaults != null)
        {
            // create the <defaults> element in the 'into' cache config
            XmlElement xmlIntoDefaults = xmlIntoCacheConfig.ensureElement(DEFAULTS);

            // merge each of the 'from' <defaults> into the 'into' <defaults>
            for (XmlElement xmlDefault : (List<XmlElement>) xmlFromDefaults.getElementList())
            {
                QualifiedName qualifiedName = xmlDefault.getQualifiedName();

                if (qualifiedName.hasPrefix())
                {
                    mergeForeignElement(context, sFromURI, xmlDefault, xmlIntoDefaults);
                }
                else
                {
                    // clone the element to merge
                    XmlElement xmlMergeElement = (XmlElement) xmlDefault.clone();

                    // annotate the origin of the merging element
                    xmlMergeElement.addAttribute(qnOriginatedFrom.getName()).setString(sFromURI);

                    // add the merged element
                    xmlIntoDefaults.getElementList().add(xmlMergeElement);
                }
            }
        }
    }


    /**
     * Merges the interceptors from one cache config into another cache config
     * (mutating the "into" cache config).
     *
     * @param sFromURI             the URI to which the from element belongs
     * @param xmlFromInterceptors  the interceptors element from which the merge should occur
     * @param xmlIntoCacheConfig   the cache config into which the merge should occur
     */
    @SuppressWarnings("unchecked")
    private void mergeInterceptors(ProcessingContext context,
                                   String            sFromURI,
                                   XmlElement        xmlFromInterceptors,
                                   XmlElement        xmlIntoCacheConfig)
    {
        if (xmlFromInterceptors != null)
        {
            for (XmlElement xmlInterceptor : (List<XmlElement>) xmlFromInterceptors.getElementList())
            {
                QualifiedName qualifiedName = xmlInterceptor.getQualifiedName();

                if (qualifiedName.hasPrefix())
                {
                    mergeForeignElement(context,
                                        sFromURI,
                                        xmlInterceptor,
                                        xmlIntoCacheConfig.ensureElement(INTERCEPTORS));
                }
                else
                {
                    XmlElement xmlInterceptorName = xmlInterceptor.getElement(NAME);

                    // assume we can merge the interceptor
                    boolean mergeInterceptor = true;

                    // check that the interceptor name is unique
                    // (we can only merge uniquely named interceptors)
                    if (xmlInterceptorName != null)
                    {
                        String sInterceptorName = xmlInterceptorName.getString().trim();

                        mergeInterceptor = !isInterceptorDefinedFor(sInterceptorName, xmlIntoCacheConfig);
                    }

                    if (mergeInterceptor)
                    {
                        // clone the element to merge
                        XmlElement xmlMergeElement = (XmlElement) xmlInterceptor.clone();

                        // annotate the origin of the merging element
                        xmlMergeElement.addAttribute(qnOriginatedFrom.getName()).setString(sFromURI);

                        xmlIntoCacheConfig.ensureElement(INTERCEPTORS).getElementList().add(xmlMergeElement);
                    }

                }
            }
        }
    }


    /**
     * Merges the foreign configuration element from one cache config into another
     * cache config (mutating the "into" cache config).
     *
     * @param sFromURI            the URI to which the from element belongs
     * @param xmlFromElement      the foreign element from which the merge should occur
     * @param xmlIntoCacheConfig  the cache config into which the merge should occur
     */
    private void mergeForeignElement(ProcessingContext context,
                                     String            sFromURI,
                                     XmlElement        xmlFromElement,
                                     XmlElement        xmlIntoCacheConfig)
    {
        QualifiedName    qualifiedName = xmlFromElement.getQualifiedName();
        NamespaceHandler handler       = context.getNamespaceHandler(qualifiedName.getPrefix());

        if (handler instanceof IntroduceCacheConfigSupport)
        {
            ((IntroduceCacheConfigSupport) handler).mergeConfiguration(context,
                                                                       sFromURI,
                                                                       xmlFromElement,
                                                                       xmlIntoCacheConfig,
                                                                       qnOriginatedFrom);
        }
    }


    /**
     * Obtains the name of the scheme as defined in the <scheme-name>
     * element that is a child of the specified scheme.
     * <p>
     * Note: If the <scheme-name> is not specified then the <service-name>
     * is used.
     *
     * @param xmlScheme
     *
     * @return the name of the scheme or <code>null</code> if one is not defined.
     */
    public String getSchemeNameFor(XmlElement xmlScheme)
    {
        XmlElement xmlSchemeName = xmlScheme.getElement(SCHEME_NAME);

        if (xmlSchemeName == null)
        {
            XmlElement xmlServiceName = xmlScheme.getElement(SERVICE_NAME);

            if (xmlServiceName == null)
            {
                // when there's no service name or scheme name, default to the parent name
                return "$" + xmlScheme.getName();
            }
            else
            {
                return xmlServiceName.getString().trim();
            }
        }
        else
        {
            return xmlSchemeName.getString().trim();
        }
    }


    /**
     * Determines if there is a <cache-mapping> defined with the specified name
     * in the {@link XmlElement} representing a <cache-config>
     *
     * @param sCacheName
     * @param xmlCacheConfig
     *
     * @return  <code>true</code> if the cache mapping is defined, <code>false</code> otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean isCacheMappingDefinedFor(String     sCacheName,
                                            XmlElement xmlCacheConfig)
    {
        XmlElement xmlCachingSchemeMapping = xmlCacheConfig.getElement(CACHING_SCHEME_MAPPING);

        if (xmlCachingSchemeMapping == null)
        {
            return false;
        }
        else
        {
            for (XmlElement xmlCacheMapping : (List<XmlElement>) xmlCachingSchemeMapping.getElementList())
            {
                XmlElement xmlCacheName = xmlCacheMapping.getElement(CACHE_NAME);

                if (xmlCacheName != null)
                {
                    String sDefinedCacheName = xmlCacheName.getString().trim();

                    if (sDefinedCacheName.trim().equals(sCacheName))
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }


    /**
     * Determines if there is a <*-scheme> defined with the specified name
     * in the {@link XmlElement} representing a <cache-config>
     *
     * @param sSchemeName
     * @param xmlCacheConfig
     *
     * @return <code>true</code> if the scheme is defined, <code>false</code> otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean isSchemeDefinedFor(String     sSchemeName,
                                      XmlElement xmlCacheConfig)
    {
        XmlElement xmlCachingSchemes = xmlCacheConfig.getElement(CACHING_SCHEMES);

        if (xmlCachingSchemes == null)
        {
            return false;
        }
        else
        {
            for (XmlElement xmlCachingScheme : (List<XmlElement>) xmlCachingSchemes.getElementList())
            {
                String sDefinedSchemeName = getSchemeNameFor(xmlCachingScheme);

                if (sDefinedSchemeName != null)
                {
                    if (sDefinedSchemeName.equals(sSchemeName))
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }


    /**
     * Determines if there is an <interceptor> defined with the specified name
     * in the {@link XmlElement} representing a <interceptor>
     *
     * @param sInterceptorName
     * @param xmlCacheConfig
     *
     * @return  <code>true</code> if the interceptor is defined, <code>false</code> otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean isInterceptorDefinedFor(String     sInterceptorName,
                                           XmlElement xmlCacheConfig)
    {
        XmlElement xmlInterceptors = xmlCacheConfig.getElement(INTERCEPTORS);

        if (xmlInterceptors == null)
        {
            return false;
        }
        else
        {
            for (XmlElement xmlInteceptor : (List<XmlElement>) xmlInterceptors.getElementList())
            {
                XmlElement xmlInterceptorName = xmlInteceptor.getElement(NAME);

                if (xmlInterceptorName != null)
                {
                    String sDefinedInteceptorName = xmlInterceptorName.getString().trim();

                    if (sDefinedInteceptorName.trim().equals(sInterceptorName))
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }


    /**
     * Provides support for custom pre-processing for foreign/non-Coherence-based
     * namespaces (ie: {@link NamespaceHandler}s) when using the 'introduce-cache-config'
     * pre-processor.
     */
    public interface IntroduceCacheConfigSupport
    {
        /**
         * Merges the non-Coherence-based element from one cache config into another
         * cache config (mutating the "into" cache config).
         *
         * @param sFromURI            the URI to which the from element belongs
         * @param xmlElement          the non-Coherence-based element from which
         *                            the merge should occur
         * @param xmlIntoCacheConfig  the cache config into which the merge should occur
         * @param qnOriginatedFrom    the QualifiedName to use for decorating
         *                            the element with the 'originated-from' attribute
         */
        void mergeConfiguration(ProcessingContext context,
                                String            sFromURI,
                                XmlElement        xmlElement,
                                XmlElement        xmlIntoCacheConfig,
                                QualifiedName     qnOriginatedFrom);
    }


    /**
     * The {@link IntroduceCacheConfigPreprocessor} is an {@link ElementProcessor}
     * that introduces and merges one or more cache config resources/files
     * specified by the "introduce-cache-config" attribute in a <cache-config> element.
     */
    public class IntroduceCacheConfigPreprocessor implements ElementPreprocessor
    {
        /**
         * The 'cache-config' {@link XmlElement}.
         */
        private static final String CACHE_CONFIG = "cache-config";


        @Override
        public boolean preprocess(ProcessingContext context,
                                  XmlElement        xmlElement) throws ConfigurationException
        {
            // "introduce-cache-config" may only be applied with in a cache-config
            QualifiedName qName = xmlElement.getQualifiedName();

            if (!qName.hasPrefix() && qName.getLocalName().equals(CACHE_CONFIG))
            {
                // obtain the cache config URIs (a comma separated list in
                // the "introduce-cache-config" attribute)
                XmlValue xmlValue = xmlElement.getAttribute(qnIntroduceCacheConfig.getName());

                if (xmlValue == null)
                {
                    // nothing to do... as the directive isn't specified
                    return false;
                }
                else
                {
                    // get and then remove the "introduce-cache-config" attribute
                    // (so we don't attempt to merge it again)
                    String sCacheConfigURIs = xmlValue.getString();

                    xmlElement.getAttributeMap().remove(qnIntroduceCacheConfig.getName());

                    // pre-process (merge) the specified URIs into the current element
                    String[] arrURIs = sCacheConfigURIs.split(",");

                    // assume we haven't changed the configuration Xml
                    boolean fChangedXml = false;

                    for (String sURI : arrURIs)
                    {
                        sURI = sURI == null ? "" : sURI.trim();

                        if (!sURI.isEmpty())
                        {
                            // as we allow the URI to be an expression,
                            // we need to parse it first
                            ExpressionParser   parser = context.getExpressionParser();
                            Expression<String> exprURI;

                            try
                            {
                                exprURI = parser.parse(sURI, String.class);
                            }
                            catch (ParseException e)
                            {
                                throw new ConfigurationException("Failed to parse the 'introduce-cache-config' expression ["
                                                                 + sURI + "]",
                                                                 "Please ensure that the URI is correctly formatted",
                                                                 e);
                            }

                            // now evaluate the expression to get the real URI!
                            sURI = exprURI.evaluate(context.getDefaultParameterResolver());

                            // only process the URI if we've not already processed it.
                            if (!introducedURIs.contains(sURI))
                            {
                                // attempt to load the specified URI
                                XmlElement xmlOtherElement = XmlHelper.loadFileOrResource(sURI,
                                                                                          INTRODUCE_CACHE_CONFIG,
                                                                                          context.getContextClassLoader());

                                // ensure that the root element and this element are the same type
                                if (xmlOtherElement == null)
                                {
                                    throw new ConfigurationException("Failed to load the 'introduce-cache-config' resource ["
                                                                     + sURI + "]",
                                                                     "Please ensure that is it available and has suitable permissions");
                                }
                                else
                                {
                                    if (xmlElement.getName().equals(xmlOtherElement.getName()))
                                    {
                                        // remember this URI as we want to avoid recursive pre-processing/introduction of it
                                        introducedURIs.add(sURI);

                                        // merge the otherElement into the this element
                                        mergeCacheConfig(context, sURI, xmlOtherElement, xmlElement);

                                        // as we've merged, it's highly likely we changed the document,
                                        // we have to let Coherence know the document
                                        // should be considered for pre-processing again
                                        fChangedXml = true;
                                    }
                                    else
                                    {
                                        throw new ConfigurationException("The 'introduce-cache-config' resource ["
                                                                         + sURI + "] is not a <cache-config>",
                                                                         "The root element of the resource must be a <cache-config>");
                                    }
                                }
                            }
                        }
                    }

                    // let Coherence know if we've changed the configuration
                    return fChangedXml;
                }
            }
            else
            {
                // no change has been made
                return false;
            }
        }
    }


    /**
     * The {@link ReplaceWithFilePreprocessor} is an {@link ElementPreprocessor}
     * that replaces elements in a document with elements specified in
     * another file.
     */
    public class ReplaceWithFilePreprocessor implements ElementPreprocessor
    {
        @SuppressWarnings("unchecked")
        @Override
        public boolean preprocess(ProcessingContext context,
                                  XmlElement        xmlElement) throws ConfigurationException
        {
            // determine if the element as a 'replace-with-file' attribute (directive)
            XmlValue xmlReplaceWithFile = xmlElement.getAttribute(qnReplaceWithFile.getName());

            if (xmlReplaceWithFile == null)
            {
                // doesn't exist so nothing has changed
                return false;
            }
            else
            {
                // evaluate the expression that has been used to specify the file
                String             sURI   = xmlReplaceWithFile.getString().trim();

                ExpressionParser   parser = context.getExpressionParser();
                Expression<String> exprURI;

                try
                {
                    exprURI = parser.parse(sURI, String.class);
                }
                catch (ParseException e)
                {
                    throw new ConfigurationException("Failed to parse the 'replace-with-file' expression [" + sURI
                                                     + "]",
                                                     "Please ensure that the URI is correctly formatted",
                                                     e);
                }

                // now evaluate the expression to get the real URI!
                sURI = exprURI.evaluate(context.getDefaultParameterResolver());

                try
                {
                    // now attempt to load the resource
                    XmlElement xmlOtherElement = XmlHelper.loadFileOrResource(sURI,
                                                                              REPLACE_WITH_FILE,
                                                                              context.getContextClassLoader());

                    // ensure that the current element and the other element are from the same namespace
                    if (xmlElement.getName().equals(xmlOtherElement.getName()))
                    {
                        // remove all of the child elements of the element
                        xmlElement.getElementList().clear();

                        // remove all of the attributes of the element
                        xmlElement.getAttributeMap().clear();

                        // add all of the replace-with-file element children into the element
                        for (XmlElement xmlChildElement : (List<XmlElement>) xmlOtherElement.getElementList())
                        {
                            xmlElement.getElementList().add(xmlChildElement.clone());
                        }

                        // add all of the replace-with-file element attributes to the element
                        for (Entry<String, XmlValue> attribute :
                            (Set<Entry<String, XmlValue>>) xmlOtherElement.getAttributeMap().entrySet())
                        {
                            xmlElement.getAttributeMap().put(attribute.getKey(), attribute.getValue().clone());
                        }

                        // we changed the element so we may need to reprocess
                        return true;
                    }
                    else
                    {
                        throw new ConfigurationException("The root element [" + xmlOtherElement.getName()
                                                         + "] of the 'replace-with-file' resource [" + sURI
                                                         + "] is not the same as the root element ["
                                                         + xmlElement.getName() + "] that it is meant to replace.",
                                                         "Please ensure that the root element of the resource is the same as the element it's meant to replace.");
                    }
                }
                catch (Exception e)
                {
                    throw new ConfigurationException("Failed to load the 'replace-with-file' expression [" + sURI + "]",
                                                     "Please ensure that the resource is available",
                                                     e);
                }
            }
        }
    }
}
