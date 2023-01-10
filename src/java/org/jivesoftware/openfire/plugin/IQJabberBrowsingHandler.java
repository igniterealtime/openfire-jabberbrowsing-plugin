/*
 * Copyright (C) 2023 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.openfire.plugin;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.disco.IQDiscoInfoHandler;
import org.jivesoftware.openfire.disco.IQDiscoItemsHandler;
import org.jivesoftware.openfire.disco.ServerFeaturesProvider;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.handler.IQVersionHandler;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.util.*;

/**
 * An IQ Handler that processes IQ requests sent to the server that contain queries related to the protocol described
 * in XEP-0011: Jabber Browsing.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0011.html">XEP-0011: Jabber Browsing</a>
 */
public class IQJabberBrowsingHandler extends IQHandler implements ServerFeaturesProvider
{
    private static final Logger Log = LoggerFactory.getLogger(IQJabberBrowsingHandler.class);

    /**
     * Category and types defined in XEP-11: Jabber Browsing (as of version 1.3.1)
     */
    public static Map<String, Set<String>> XEP_CATEGORIES_AND_TYPES = new HashMap<>();
    static {
        XEP_CATEGORIES_AND_TYPES.put("application", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("application").add("bot");
        XEP_CATEGORIES_AND_TYPES.get("application").add("calendar");
        XEP_CATEGORIES_AND_TYPES.get("application").add("editor");
        XEP_CATEGORIES_AND_TYPES.get("application").add("fileserver");
        XEP_CATEGORIES_AND_TYPES.get("application").add("game");
        XEP_CATEGORIES_AND_TYPES.get("application").add("whiteboard");

        XEP_CATEGORIES_AND_TYPES.put("conference", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("conference").add("irc");
        XEP_CATEGORIES_AND_TYPES.get("conference").add("list");
        XEP_CATEGORIES_AND_TYPES.get("conference").add("private");
        XEP_CATEGORIES_AND_TYPES.get("conference").add("public");
        XEP_CATEGORIES_AND_TYPES.get("conference").add("topic");
        XEP_CATEGORIES_AND_TYPES.get("conference").add("url");

        XEP_CATEGORIES_AND_TYPES.put("headline", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("headline").add("logger");
        XEP_CATEGORIES_AND_TYPES.get("headline").add("notice");
        XEP_CATEGORIES_AND_TYPES.get("headline").add("rss");
        XEP_CATEGORIES_AND_TYPES.get("headline").add("stock");

        XEP_CATEGORIES_AND_TYPES.put("keyword", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("keyword").add("dictionary");
        XEP_CATEGORIES_AND_TYPES.get("keyword").add("dns");
        XEP_CATEGORIES_AND_TYPES.get("keyword").add("software");
        XEP_CATEGORIES_AND_TYPES.get("keyword").add("thesaurus");
        XEP_CATEGORIES_AND_TYPES.get("keyword").add("web");
        XEP_CATEGORIES_AND_TYPES.get("keyword").add("whois");

        XEP_CATEGORIES_AND_TYPES.put("render", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("render").add("en2fr");
        XEP_CATEGORIES_AND_TYPES.get("render").add("*2*");
        XEP_CATEGORIES_AND_TYPES.get("render").add("tts");

        XEP_CATEGORIES_AND_TYPES.put("service", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("service").add("aim");
        XEP_CATEGORIES_AND_TYPES.get("service").add("icq");
        XEP_CATEGORIES_AND_TYPES.get("service").add("irc");
        XEP_CATEGORIES_AND_TYPES.get("service").add("jabber");
        XEP_CATEGORIES_AND_TYPES.get("service").add("jud");
        XEP_CATEGORIES_AND_TYPES.get("service").add("msn");
        XEP_CATEGORIES_AND_TYPES.get("service").add("pager");
        XEP_CATEGORIES_AND_TYPES.get("service").add("serverlist");
        XEP_CATEGORIES_AND_TYPES.get("service").add("sms");
        XEP_CATEGORIES_AND_TYPES.get("service").add("smtp");
        XEP_CATEGORIES_AND_TYPES.get("service").add("yahoo");

        XEP_CATEGORIES_AND_TYPES.put("user", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("user").add("client");
        XEP_CATEGORIES_AND_TYPES.get("user").add("forward");
        XEP_CATEGORIES_AND_TYPES.get("user").add("inbox");
        XEP_CATEGORIES_AND_TYPES.get("user").add("portable");
        XEP_CATEGORIES_AND_TYPES.get("user").add("voice");

        XEP_CATEGORIES_AND_TYPES.put("validate", new HashSet<>());
        XEP_CATEGORIES_AND_TYPES.get("validate").add("grammar");
        XEP_CATEGORIES_AND_TYPES.get("validate").add("spell");
        XEP_CATEGORIES_AND_TYPES.get("validate").add("xml");

    }
    private final IQHandlerInfo info;

    public IQJabberBrowsingHandler()
    {
        super("Jabber Browsing handler");
        this.info = new IQHandlerInfo("query", "jabber:iq:browse");
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException
    {
        Log.trace("Processing Jabber Browsing request from {}", packet.getFrom());
        if (packet.isResponse()) {
            Log.debug("Silently ignoring IQ response stanza from {}", packet.getFrom());
            return null;
        }

        final IQ reply = IQ.createResultIQ(packet);
        reply.setChildElement(packet.getChildElement().createCopy());

        if (IQ.Type.set == packet.getType()) {
            Log.debug("Returning error to {}: request is of incorrect IQ type.", packet.getFrom());
            reply.setError(PacketError.Condition.feature_not_implemented);
            return reply;
        }

        final BrowseResult browseResult = browse(packet.getTo(), packet.getFrom());
        reply.getChildElement().add(browseResult.asElement());
        return reply;
    }

    @Override
    public IQHandlerInfo getInfo()
    {
        return info;
    }

    @Override
    public Iterator<String> getFeatures()
    {
        return Collections.singleton(BrowseResult.NAMESPACE).iterator();
    }

    /**
     * Finds XEP-0011-defined browse results of a target entity, by performing XEP-0030-based Service Discovery requests
     * on behalf of the requester on the target entity. The requester address is provided to allow the Service Discovery
     * service to authorize the request, where applicable.
     *
     * @param target The entity for which to return a browse result.
     * @param requester The entity that requests a browse result of an entity.
     * @return A Browse Result entity.
     */
    public BrowseResult browse(final JID target, final JID requester)
    {
        Log.trace("Browse Jabber entity {} for {}", target, requester);

        // Use Service Discovery to identify the identity and features that are part of the browse result of the target.
        final Element infoElement = getDiscoInfo(target, requester);
        final String category = parseCategory(infoElement);
        final String type = parseType(category, infoElement);
        final String name = parseName(infoElement);
        final String version = getVersion(target, requester);
        final Set<String> namespaces = parseNamespaces(infoElement);
        final Set<BrowseResult> children = new HashSet<>(); // To be filled later.

        final BrowseResult result = new BrowseResult(target, category, type, name, version, namespaces, children);

        // Use Service Discovery to identify all items that are potential children.
        final Collection<Element> itemElements = getDiscoItems(target, requester);

        for (final Element itemElement : itemElements) {
            final String jidValue = itemElement.attributeValue("jid");
            final JID jid;
            try {
                jid = new JID(jidValue);
            } catch (IllegalArgumentException e) {
                Log.debug("Silently ignoring a service discovery item of entity '{}' that has an invalid JID value: {}", target, jidValue);
                continue;
            }

            // Use an #info request again to identify identity and features of each child.
            final Element childInfoElement = getDiscoInfo(jid, requester);

            final String childCategory = parseCategory(childInfoElement);
            final String childType = parseType(childCategory, childInfoElement);
            final String childName = parseName(childInfoElement);
            final String childVersion = getVersion(jid, requester);
            final Set<String> childNamespaces = parseNamespaces(childInfoElement);
            final Set<BrowseResult> grandchildren = Collections.emptySet(); // Do not recurse.

            final BrowseResult child = new BrowseResult(jid, childCategory, childType, childName, childVersion, childNamespaces, grandchildren);

            result.getChildren().add(child);
        }
        return result;
    }

    public Collection<Element> getDiscoItems(final JID target, final JID requester)
    {
        Log.trace("Perform disco#items request on {} on behalf of {}", target, requester);

        final IQ itemsRequest = new IQ(IQ.Type.get);
        itemsRequest.setTo(target);
        itemsRequest.setFrom(requester);
        itemsRequest.setChildElement("query", IQDiscoItemsHandler.NAMESPACE_DISCO_ITEMS);

        final IQ itemsResponse = XMPPServer.getInstance().getIQDiscoItemsHandler().handleIQ(itemsRequest);
        if (itemsResponse.getError() != null) {
            return Collections.emptySet();
        }
        final Element childElement = itemsResponse.getChildElement();
        if (childElement == null || !"query".equals(childElement.getName()) || !IQDiscoItemsHandler.NAMESPACE_DISCO_ITEMS.equals(childElement.getNamespaceURI())) {
            return Collections.emptySet();
        }

        return childElement.elements("item");
    }

    public Element getDiscoInfo(final JID target, final JID requester)
    {
        Log.trace("Perform disco#info request on {} on behalf of {}", target, requester);

        final IQ infoRequest = new IQ(IQ.Type.get);
        infoRequest.setTo(target);
        infoRequest.setFrom(requester);
        infoRequest.setChildElement("query", IQDiscoInfoHandler.NAMESPACE_DISCO_INFO);

        final IQ infoResponse = XMPPServer.getInstance().getIQDiscoInfoHandler().handleIQ(infoRequest);
        if (infoResponse.getError() != null) {
            return null;
        }

        final Element childElement = infoResponse.getChildElement();
        if (childElement == null || !"query".equals(childElement.getName()) || !IQDiscoInfoHandler.NAMESPACE_DISCO_INFO.equals(childElement.getNamespaceURI())) {
            return null;
        }
        return infoResponse.getChildElement();
    }

    public String getVersion(final JID target, final JID requester)
    {
        Log.trace("Perform version request on {} on behalf of {}", target, requester);

        final IQ versionRequest = new IQ(IQ.Type.get);
        versionRequest.setTo(target);
        versionRequest.setFrom(requester);
        versionRequest.setChildElement("query", "jabber:iq:version");

        final IQ versionResponse = new IQVersionHandler().handleIQ(versionRequest);
        if (versionResponse.getError() != null) {
            return null;
        }

        final Element childElement = versionResponse.getChildElement();
        if (childElement == null || !"query".equals(childElement.getName()) || !"jabber:iq:version".equals(childElement.getNamespaceURI())) {
            return null;
        }

        final Element version = childElement.element("version");
        if (version == null) {
            return null;
        }
        final String versionValue = version.getTextTrim();
        return versionValue == null || versionValue.isEmpty() ? null : versionValue;
    }

    public static String parseCategory(final Element discoInfoElement)
    {
        if (discoInfoElement == null) {
            return null;
        }

        // Disco#info can have multiple identities with each a category.
        final Set<String> categories = new HashSet<>();
        final Set<String> types = new HashSet<>();

        for (final Element identityElement : discoInfoElement.elements("identity")) {
            final String category = identityElement.attributeValue("category");
            if (category != null && !category.trim().isEmpty()) {
                categories.add(category.trim());
            }
            final String type = identityElement.attributeValue("type");
            if (type != null && !type.trim().isEmpty()) {
                types.add(type.trim());
            }

            if (!categories.isEmpty() && !JiveGlobals.getBooleanProperty("plugin.jabberbrowsing.concat-identities", false)) {
                break;
            }
        }

        switch (categories.size()) {
            case 0: return null;
            default: return "x-" + String.join("_and_", categories);
            case 1:
                final String category = categories.iterator().next();
                if ("gateway".equals(category)) {
                    return "service";
                }
                if ("server".equals(category) && "im".equals(String.join("_and_", types))) {
                    return "service";
                }
                if ("collaboration".equals(category) && "whiteboard".equals(String.join("_and_", types))) {
                    return "application";
                }
                if (XEP_CATEGORIES_AND_TYPES.containsKey(category)) {
                    return category;
                } else {
                    return "x-"+category;
                }
        }
    }

    public static String parseType(final String category, final Element discoInfoElement) {
        if (discoInfoElement == null) {
            return null;
        }

        final Set<String> types = new HashSet<>();

        for (final Element identityElement : discoInfoElement.elements("identity")) {
            final String type = identityElement.attributeValue("type");
            if (type != null && !type.trim().isEmpty()) {
                types.add(type.trim());

                if (!JiveGlobals.getBooleanProperty("plugin.jabberbrowsing.concat-identities", false)) {
                    break;
                }
            }
        }

        if (category.startsWith("x-")) {
            return String.join("_and_", types);
        }

        switch (types.size()) {
            case 0: return null;
            default: return "x-" + String.join("_and_", types);
            case 1:
                final String type = types.iterator().next();
                if ("service".equals(category) && "im".equals(type)) {
                    return "jabber";
                }
                if (XEP_CATEGORIES_AND_TYPES.containsKey(category) && XEP_CATEGORIES_AND_TYPES.get(category).contains(type)) {
                    return type;
                } else {
                    return "x-"+type;
                }
        }
    }

    public static String parseName(final Element discoInfoElement)
    {
        if (discoInfoElement == null) {
            return null;
        }

        // Disco#info can have multiple identities with each a name.
        final Set<String> names = new HashSet<>();
        for (final Element identityElement : discoInfoElement.elements("identity")) {
            final String name = identityElement.attributeValue("name");
            if (name != null && !name.trim().isEmpty()) {
                names.add(name.trim());
            }
        }

        return names.isEmpty() ? null : String.join(", ", names);
    }

    public static Set<String> parseNamespaces(final Element discoInfoElement)
    {
        final Set<String> results = new HashSet<>();
        if (discoInfoElement == null) {
            return results;
        }

        for (final Element identityElement : discoInfoElement.elements("feature")) {
            final String var = identityElement.attributeValue("var");
            if (var != null && !var.trim().isEmpty()) {
                results.add(var.trim());
            }
        }
        return results;
    }
}
