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

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmpp.packet.JID;

import java.util.Set;

/**
 * Representation of a browse result as defined in XEP-0011: Jabber Browsing.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class BrowseResult
{
    public static final String NAMESPACE = "jabber:iq:browse";

    /**
     * The full JabberID of the entity described.
     */
    private final JID jid;

    /**
     * One of the categories from the list above, or a non-standard category prefixed with the string "x-".
     */
    private final String category;

    /**
     * One of the official types from the specified category, or a non-standard type prefixed with the string "x-".
     */
    private final String type;

    /**
     * A friendly name that may be used in a user interface.
     */
    private final String name;

    /**
     * A string containing the version of the node, equivalent to the response provided to a query in the 'jabber:iq:version' namespace. This is useful for servers, especially for lists of services (see the 'service/serverlist' category/type above).
     */
    private final String version;

    /**
     * A collection of namespaces that are used to advertise a feature.
     */
    private final Set<String> namespaces;

    /**
     * Children of the node that is being browsed. Browse results usually only contain the direct children of a node, not the grandchildren
     */
    private final Set<BrowseResult> children;

    public BrowseResult(JID jid, String category, String type, String name, String version, Set<String> namespaces, Set<BrowseResult> children)
    {
        this.jid = jid;
        this.category = category;
        this.type = type;
        this.name = name;
        this.version = version;
        this.namespaces = namespaces;
        this.children = children;
    }

    public JID getJid()
    {
        return jid;
    }

    public String getCategory()
    {
        return category;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }

    public Set<String> getNamespaces()
    {
        return namespaces;
    }

    public Set<BrowseResult> getChildren()
    {
        return children;
    }

    /**
     * Returns an XML element that represents the browse result.
     *
     * @return an XML element.
     */
    public Element asElement()
    {
        final Element result = DocumentHelper.createElement(QName.get("query", NAMESPACE));
        result.addAttribute("jid", jid.toString());
        if (category != null) {
            result.addAttribute("category", category);
        }
        if (type != null) {
            result.addAttribute("type", type);
        }
        if (name != null) {
            result.addAttribute("name", name);
        }
        if (version != null) {
            result.addAttribute("version", version);
        }
        for (final String namespace : namespaces) {
            result.addElement("ns").setText(namespace);
        }

        for(final BrowseResult child : children) {
            final Element childElement = result.addElement("item");
            if (child.getJid() != null) {
                childElement.addAttribute("jid", child.getJid().toString());
            }
            if (child.getCategory() != null) {
                childElement.addAttribute("category", child.getCategory());
            }
            if (child.getType() != null) {
                childElement.addAttribute("type", child.getType());
            }
            if (child.getName() != null) {
                childElement.addAttribute("name", child.getName());
            }
            if (child.getVersion() != null) {
                childElement.addAttribute("version", child.getVersion());
            }
            for (final String namespace : child.getNamespaces()) {
                childElement.addElement("ns").setText(namespace);
            }
        }

        return result;
    }
}
