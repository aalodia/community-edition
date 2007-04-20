/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.processor;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TemplateException;
import org.alfresco.service.cmr.repository.TemplateImageResolver;
import org.alfresco.service.cmr.repository.TemplateProcessor;
import org.alfresco.service.cmr.repository.TemplateService;

/**
 * Implementation of the TemplateService using Spring configured script engines.
 * 
 * @author Kevin Roast
 */
public class TemplateServiceImpl implements TemplateService
{    
    /** Default Template processor engine to use */
    private String defaultTemplateEngine;    
    
    /** List of available template processors */
    private Map<String, TemplateProcessor> processors = new HashMap<String, TemplateProcessor>(5);
    private Map<String, String> processorNamesByExtension = new HashMap<String, String>(5);
    
    /** The node service */
    private NodeService nodeService;

    /**
     * @param defaultTemplateEngine The default Template Engine name to set.
     */
    public void setDefaultTemplateEngine(String defaultTemplateEngine)
    {
        this.defaultTemplateEngine = defaultTemplateEngine;
    }
    
    /**
     * Set the node service
     * 
     * @param nodeService   the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#getTemplateProcessor(java.lang.String)
     */
    public TemplateProcessor getTemplateProcessor(String engine)
    {        
        return this.processors.get(engine);
    }
    
    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#registerTemplateProcessor(org.alfresco.service.cmr.repository.TemplateProcessor)
     */
    public void registerTemplateProcessor(TemplateProcessor templateProcessor)
    {
        this.processors.put(templateProcessor.getName(), templateProcessor);
        this.processorNamesByExtension.put(templateProcessor.getExtension(), templateProcessor.getName());
    }

    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#processTemplate(java.lang.String, java.lang.Object)
     */
    public String processTemplate(String template, Object model) throws TemplateException
    {
        return processTemplate(getTemplateProcessorName(template), template, model);
    }

    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#processTemplate(java.lang.String, java.lang.Object, java.io.Writer)
     */
    public void processTemplate(String template, Object model, Writer out) throws TemplateException
    {
        processTemplate(getTemplateProcessorName(template), template, model, out);
    }
    
    /**
     * Gets the template processor name from the template string
     * 
     * @param template  the template string location
     * @return  the template processor string
     */
    private String getTemplateProcessorName(String template)
    {
        String engine = null;
        
        try
        {
            // Try and create the nodeRef
            NodeRef templateNodeRef = new NodeRef(template);
            String templateName = (String)this.nodeService.getProperty(templateNodeRef, ContentModel.PROP_NAME);                
            String extension = getFileExtension(templateName);
            if (extension != null)
            {
                engine = this.processorNamesByExtension.get(extension);
            }              
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Presume that the provided template is a classpath
            String extension = getFileExtension(template);
            if (extension != null)
            {
                engine = this.processorNamesByExtension.get(extension);
            }
        }
        
        // Set the default engine if none found
        if (engine == null)
        {
            engine = this.defaultTemplateEngine;
        }
        
        return engine;
    }
    
    /**
     * Gets the file extension of a file
     * 
     * @param fileName  the file name
     * @return  the file extension
     */
    private String getFileExtension(String fileName)
    {
        String extension = null;
        int index = fileName.lastIndexOf('.');
        if (index > -1 && (index < fileName.length() - 1))
        {
            extension = fileName.substring(index + 1);
        }
        return extension;
    }

    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#processTemplate(java.lang.String, java.lang.String, java.lang.Object, java.io.Writer)
     */
    public void processTemplate(String engine, String template, Object model, Writer out)
        throws TemplateException
    {
        try
        {
           // execute template processor
           TemplateProcessor processor = getTemplateProcessor(engine);
           processor.process(template, model, out);
        }
        catch (TemplateException terr)
        {
           throw terr;
        }
        catch (Throwable err)
        {
           throw new TemplateException(err.getMessage(), err);
        }
    }

    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#processTemplate(java.lang.String, java.lang.String, java.lang.Object)
     */
    public String processTemplate(String engine, String template, Object model)
        throws TemplateException
    {
        Writer out = new StringWriter(1024);
        processTemplate(engine, template, model, out);
        return out.toString();
    }

    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#processTemplateString(java.lang.String, java.lang.String, java.lang.Object, java.io.Writer)
     */
    public void processTemplateString(String engine, String template, Object model, Writer out)
        throws TemplateException
    {
        try
        {
            // execute template processor
            TemplateProcessor processor = getTemplateProcessor(engine);
            processor.processString(template, model, out);
        }
        catch (TemplateException terr)
        {
            throw terr;
        }
        catch (Throwable err)
        {
            throw new TemplateException(err.getMessage(), err);
        }
    }
    
    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#processTemplateString(java.lang.String, java.lang.String, java.lang.Object)
     */
    public String processTemplateString(String engine, String template, Object model)
        throws TemplateException
    {
        Writer out = new StringWriter(1024);
        processTemplateString(engine, template, model, out);
        return out.toString();
    }

    /**
     * @see org.alfresco.service.cmr.repository.TemplateService#buildDefaultModel(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.TemplateImageResolver)
     */
    public Map<String, Object> buildDefaultModel(NodeRef person, NodeRef companyHome, NodeRef userHome, NodeRef template, TemplateImageResolver imageResolver)
    {
        Map<String, Object> model = new HashMap<String, Object>(16, 1.0f);
        
        // Place the image resolver into the model
        if (imageResolver != null)
        {
            model.put(KEY_IMAGE_RESOLVER, imageResolver);
        }
        
        // Put the common node reference into the model
        model.put(KEY_COMPANY_HOME, companyHome);
        model.put(KEY_USER_HOME, userHome);
        model.put(KEY_PERSON, person);
        
        // add the template itself as "template" if it comes from content on a node
        if (template != null)
        {
            model.put(KEY_TEMPLATE, template);
        }
        
        // current date/time is useful to have and isn't supplied by FreeMarker by default
        model.put(KEY_DATE, new Date());
        
        return model;
    }
}
