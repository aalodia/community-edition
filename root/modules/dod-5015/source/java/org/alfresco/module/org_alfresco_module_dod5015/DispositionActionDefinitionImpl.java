/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.module.org_alfresco_module_dod5015;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementAction;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEvent;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEventService;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEventType;
import org.alfresco.module.org_alfresco_module_dod5015.event.SimpleRecordsManagementEventTypeImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Disposition action implementation
 * 
 * @author Roy Wetherall
 */
public class DispositionActionDefinitionImpl implements DispositionActionDefinition, RecordsManagementModel
{
	/** Logger */
    private static Log logger = LogFactory.getLog(DispositionActionDefinitionImpl.class);
	
    /** Name */
    private String name;
    
    /** Description */
    private String description;
    
    /** Label */
    private String label;
    
    /** Node service */
    private NodeService nodeService;

    /** Records management action service */
    private RecordsManagementActionService recordsManagementActionService;
    
    /** Records management event service */
    private RecordsManagementEventService recordsManagementEventService;
    
    /** Disposition action node reference */
    private NodeRef dispositionActionNodeRef;
    
    /** Action index */
    private int index;
    
    /**
     * Constructor
     * 
     * @param services  service registry
     * @param nodeRef   disposition action node reference
     * @param index     index of disposition action
     */
    public DispositionActionDefinitionImpl(
    		RecordsManagementEventService recordsManagementEventService, 
    		RecordsManagementActionService recordsManagementActionService, 
    		NodeService nodeService, 
    		NodeRef nodeRef, 
    		int index)
    {
        this.recordsManagementEventService = recordsManagementEventService;
        this.recordsManagementActionService = recordsManagementActionService;
        this.nodeService = nodeService;
        this.dispositionActionNodeRef = nodeRef;
        this.index = index;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getNodeRef()
     */
    public NodeRef getNodeRef()
    {
        return this.dispositionActionNodeRef;
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getIndex()
     */
    public int getIndex()
    {
        return this.index;
    }
    
    /**
     *  @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getId()
     */
    public String getId()
    {
        return this.dispositionActionNodeRef.getId();
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getDescription()
     */
    public String getDescription()
    {
        if (description == null)
        {
            description = (String)nodeService.getProperty(this.dispositionActionNodeRef, PROP_DISPOSITION_DESCRIPTION);
        }
        return description;
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getName()
     */
    public String getName()
    {
        if (name == null)
        {
            name = (String)nodeService.getProperty(this.dispositionActionNodeRef, PROP_DISPOSITION_ACTION_NAME);
        }
        return name;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getLabel()
     */
    public String getLabel()
    {
        if (label == null)
        {
            String name = getName();
            label = name;
            
            // get the disposition action from the RM action service
            RecordsManagementAction action = recordsManagementActionService.getDispositionAction(name);
            if (action != null)
            {
                label = action.getLabel();
            }
        }
        
        return label;
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getPeriod()
     */
    public Period getPeriod()
    {
        return (Period)nodeService.getProperty(this.dispositionActionNodeRef, PROP_DISPOSITION_PERIOD);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getPeriodProperty()
     */
    public QName getPeriodProperty()
    {
        QName result = null;
        String value = (String)nodeService.getProperty(this.dispositionActionNodeRef, PROP_DISPOSITION_PERIOD_PROPERTY);
        if (value != null)
        {
            result = QName.createQName(value);
        }
        return result;        
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getEvents()
     */
    @SuppressWarnings("unchecked")
    public List<RecordsManagementEvent> getEvents()
    {
        List<RecordsManagementEvent> events = null;
        Collection<String> eventNames = (Collection<String>)nodeService.getProperty(this.dispositionActionNodeRef, PROP_DISPOSITION_EVENT);
        if (eventNames != null)
        {
            events = new ArrayList<RecordsManagementEvent>(eventNames.size());
            for (final String eventName : eventNames)
            {
            	RecordsManagementEvent event = null;            
            	if (recordsManagementEventService.existsEvent(eventName) == true)
            	{
            		event = recordsManagementEventService.getEvent(eventName);
            	}
            	else
            	{	            		            		
            		event = createEvent(eventName);           		            	
            	}
                events.add(event);
            }
        }
        else
        {
            events = java.util.Collections.EMPTY_LIST;
        }
        return events;
    }
    
    /**
     * Create event based on event name
     * 
     * @param  eventName				event name
     * @return RecordsManagementEvent	created records management event
     */
    private RecordsManagementEvent createEvent(final String eventName)
    {
			return AuthenticationUtil.runAs(new RunAsWork<RecordsManagementEvent>()
    	    {
				@Override
				public RecordsManagementEvent doWork() throws Exception 
				{
					// Create a place holder event 
            		// @see http://issues.alfresco.com/jira/browse/ALF-5369
					RecordsManagementEvent event = recordsManagementEventService.addEvent(SimpleRecordsManagementEventTypeImpl.NAME, eventName, eventName);
            		
            		if (logger.isWarnEnabled() == true)
            		{
            			logger.warn("The event (id=" + eventName + ") could not be found so an instance has " +
            					    "been created to represent it.  The Records Manager should be contacted " +
            					    "to enter the correct details of the event in the administration console." );
            		}
            		
            		return event;
				}
    			
    	    }, AuthenticationUtil.getSystemUserName()); 
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#eligibleOnFirstCompleteEvent()
     */
    public boolean eligibleOnFirstCompleteEvent()
    {
        boolean result = true;        
        String value = (String)nodeService.getProperty(this.dispositionActionNodeRef, PROP_DISPOSITION_EVENT_COMBINATION);
        if (value != null && value.equals("and") == true)
        {
            result = false;
        }
        return result;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition#getLocation()
     */
    public String getLocation()
    {
        return (String)nodeService.getProperty(this.dispositionActionNodeRef, PROP_DISPOSITION_LOCATION);
    }
}
