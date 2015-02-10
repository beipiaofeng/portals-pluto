/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pluto.driver.services.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.portlet.Event;
import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.pluto.container.EventProvider;
import org.apache.pluto.container.PortletContainer;
import org.apache.pluto.container.PortletStateAwareResponseContext;
import org.apache.pluto.container.PortletURLProvider;
import org.apache.pluto.container.PortletWindow;
import org.apache.pluto.container.driver.PlutoServices;
import org.apache.pluto.container.impl.PortletURLImpl;
import org.apache.pluto.driver.core.PortalRequestContext;
import org.apache.pluto.driver.url.PortalURL;
import org.apache.pluto.driver.url.PortalURLPublicParameter;

/**
 * @version $Id$
 *
 */
public abstract class PortletStateAwareResponseContextImpl extends PortletResponseContextImpl implements
                PortletStateAwareResponseContext
{
    private final Logger LOGGER = LoggerFactory.getLogger(PortletStateAwareResponseContextImpl.class);
    private final boolean isDebug = LOGGER.isDebugEnabled();

    private List<Event> events;
    private PortletURLProviderImpl portletURLProvider;
    
    public PortletStateAwareResponseContextImpl(PortletContainer container, HttpServletRequest containerRequest,
                                                HttpServletResponse containerResponse, PortletWindow window)
    {
        super(container, containerRequest, containerResponse, window);
        if (isDebug) {
           LOGGER.debug("Creating PortletURLProviderImpl.");
        }
        
        this.portletURLProvider = new PortletURLProviderImpl(getPortalURL(), PortletURLProvider.TYPE.RENDER, window);
        
        if (isDebug) {
           LOGGER.debug("Done! created PortletURLProviderImpl.");
           String txt = portletURLProvider.logSomething();
           LOGGER.debug("URL Provider said: " + txt);
        }
    }
    
    protected PortletURLProvider getPortletURLProvider()
    {
        return portletURLProvider;
    }
    
    @Override
    public void close()
    {
        if (!isClosed())
        {
            super.close();
            if (isDebug) {
               LOGGER.debug("Filtering the URL.");
            }
            new PortletURLImpl(this, portletURLProvider).filterURL();
            if (isDebug) {
               LOGGER.debug("Applying the changes.");
            }
           PortalURL url = portletURLProvider.apply();
           if (isDebug) {
              LOGGER.debug("Merging.");
           }
           PortalRequestContext.getContext(getServletRequest()).mergePortalURL(url, getPortletWindow().getId().getStringId());
           if (isDebug) {
              LOGGER.debug("exiting.");
           }
       }
    }
    
    @Override
    public void release()
    {
        events = null;
        portletURLProvider = null;
        super.release();
    }
    
    public List<Event> getEvents()
    {
        if (isReleased())
        {
            return null;
        }
        if (events == null)
        {
            events = new ArrayList<Event>();
        }
        return events;
    }

    public PortletMode getPortletMode()
    {
        return isClosed() ? null : portletURLProvider.getPortletMode();
    }

    public Map<String, String[]> getRenderParameters()
    {
        return isClosed() ? null : portletURLProvider.getRenderParameters();
    }

    public WindowState getWindowState()
    {
        return isClosed() ? null : portletURLProvider.getWindowState();
    }

    public void setPortletMode(PortletMode portletMode)
    {
        if (!isClosed())
        {
            portletURLProvider.setPortletMode(portletMode);
        }
    }

    public void setWindowState(WindowState windowState)
    {
        if (!isClosed())
        {
            portletURLProvider.setWindowState(windowState);
        }
    }

    public EventProvider getEventProvider()
    {
        return isClosed() ? null : new EventProviderImpl(getPortletWindow(), PlutoServices.getServices().getPortletRegistryService());
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.PortletStateAwareResponseContext#addPublicRenderParameter(javax.xml.namespace.QName, java.lang.String, java.lang.String[])
     */
    public void addPublicRenderParameter(QName qn, String identifier, String[] values) {
       if (isDebug) {
          StringBuilder txt = new StringBuilder("Add PRP. QName = ");
          txt.append(qn.toString())
             .append(", ID = ").append(identifier)
             .append(", values = ").append(Arrays.toString(values));
          LOGGER.debug(txt.toString());
       }
       if (!isClosed()) {
          portletURLProvider.addPublicRenderParameter(qn, identifier, values);
       }
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.PortletStateAwareResponseContext#removePublicRenderParameter(javax.xml.namespace.QName, java.lang.String)
     */
    public void removePublicRenderParameter(QName qn, String id) {
       if (isDebug) {
          LOGGER.debug("Remove PRP. QName = " + qn.toString());
       }
       if (!isClosed()) {
          portletURLProvider.removePublicRenderParameter(qn, id);
       }
    }
    
    /**
     * Add a public render parameter for given window ID and parameter name
     *  
     * @param qn           QName
     * @param identifier   Identifier for PRP
     * @param values       values array
     */
    public void addPublicRenderParameter(String windowId, String name, String[] values) {
       if (!isClosed()) {
          portletURLProvider.addPublicRenderParameter(windowId, name, values);
       }
    }
    
    /**
     * Remove the PRP for the given window ID and parameter name
     * 
     * @param windowId
     * @param name
     */
    public void removePublicRenderParameter(String windowId, String name) {
       if (!isClosed()) {
          portletURLProvider.removeParameter(windowId, name);
       }
    }
    
    /**
     * Returns <code>true</code> if the given name representa a public render
     * parameter for the given window.
     * 
     * @param windowId
     * @param name
     * @return
     */
    public boolean isPublicRenderParameter(String windowId, String name) {
       boolean ret = false;
       if (!isClosed()) {
          ret = portletURLProvider.isPublicRenderParameter(windowId, name);
       }
       return ret;
    }
    
    /**
     * Retrieves the available private parameter names for the given window ID
     * @param windowId
     * @return
     */
    public Set<String> getParameterNames(String windowId) {
       Set<String> pns = new HashSet<String>();
       if (!isClosed()) {
          pns = portletURLProvider.getParameterNames(windowId);
       }
       return pns;
    }
    
    /**
     * Gets the values array for the given window ID and private parameter name
     * @param windowId
     * @param name
     * @return
     */
    public String[] getParameterValues(String windowId, String name) {
       String[] vals = new String[0];
       if (!isClosed()) {
          vals = portletURLProvider.getParameterValues(windowId, name);
       }
       return vals;
    }

    /**
     * Adds the specified private parameter if not already present, or updates the
     * values for the parameter if it is already present.
     * @param windowId
     * @param name
     * @param values
     */
    public void setParameter(String windowId, String name, String[] values) {
       if (!isClosed()) {
          portletURLProvider.setParameter(windowId, name, values);
       }
    }
    
    /** 
     * Removes the private parameter for the given window and name. Does nothing if the
     * given parameter is not present.
     * @param windowId
     * @param name
     */
    public void removeParameter(String windowId, String name) {
       if (!isClosed()) {
          portletURLProvider.removeParameter(windowId, name);
       }
    }

}
