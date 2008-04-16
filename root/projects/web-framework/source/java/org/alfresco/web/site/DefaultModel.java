/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.web.site;

import java.util.HashMap;

import org.alfresco.tools.XMLUtil;
import org.alfresco.web.site.cache.CacheFactory;
import org.alfresco.web.site.cache.IContentCache;
import org.alfresco.web.site.filesystem.IFile;
import org.alfresco.web.site.filesystem.IFileSystem;
import org.alfresco.web.site.model.Component;
import org.alfresco.web.site.model.ComponentType;
import org.alfresco.web.site.model.Configuration;
import org.alfresco.web.site.model.ContentAssociation;
import org.alfresco.web.site.model.Endpoint;
import org.alfresco.web.site.model.ModelObject;
import org.alfresco.web.site.model.Page;
import org.alfresco.web.site.model.PageAssociation;
import org.alfresco.web.site.model.Template;
import org.alfresco.web.site.model.TemplateType;
import org.dom4j.Document;

/**
 * @author muzquiano
 */
public class DefaultModel extends AbstractModel implements IModel
{
    public DefaultModel(IFileSystem fileSystem)
    {
        super(fileSystem);
    }

    // load

    public Component loadComponent(RequestContext context, String id)
    {
        return (Component) loadObject(context, Component.TYPE_NAME, id);
    }

    public ComponentType loadComponentType(RequestContext context, String id)
    {
        return (ComponentType) loadObject(context, ComponentType.TYPE_NAME, id);
    }

    public Configuration loadConfiguration(RequestContext context, String id)
    {
        return (Configuration) loadObject(context, Configuration.TYPE_NAME, id);
    }

    public ContentAssociation loadContentAssociation(RequestContext context,
            String id)
    {
        return (ContentAssociation) loadObject(context, ContentAssociation.TYPE_NAME, id);
    }

    public Endpoint loadEndpoint(RequestContext context, String id)
    {
        return (Endpoint) loadObject(context, Endpoint.TYPE_NAME, id);
    }

    public Page loadPage(RequestContext context, String id)
    {
        return (Page) loadObject(context, Page.TYPE_NAME, id);
    }

    public PageAssociation loadPageAssociation(RequestContext context, String id)
    {
        return (PageAssociation) loadObject(context, PageAssociation.TYPE_NAME, id);
    }

    public Template loadTemplate(RequestContext context, String id)
    {
        return (Template) loadObject(context, Template.TYPE_NAME, id);
    }

    public TemplateType loadTemplateType(RequestContext context, String id)
    {
        return (TemplateType) loadObject(context, TemplateType.TYPE_NAME, id);
    }

    // instantiation

    public Component newComponent(RequestContext context)
    {
        return (Component) newObject(context, Component.TYPE_NAME);
    }

    public ComponentType newComponentType(RequestContext context)
    {
        return (ComponentType) newObject(context, ComponentType.TYPE_NAME);
    }

    public Configuration newConfiguration(RequestContext context)
    {
        return (Configuration) newObject(context, Configuration.TYPE_NAME);
    }

    public ContentAssociation newContentAssociation(RequestContext context)
    {
        return (ContentAssociation) newObject(context, ContentAssociation.TYPE_NAME);
    }

    public Endpoint newEndpoint(RequestContext context)
    {
        return (Endpoint) newObject(context, Endpoint.TYPE_NAME);
    }

    public Page newPage(RequestContext context)
    {
        return (Page) newObject(context, Page.TYPE_NAME);
    }

    public PageAssociation newPageAssociation(RequestContext context)
    {
        return (PageAssociation) newObject(context, PageAssociation.TYPE_NAME);
    }

    public Template newTemplate(RequestContext context)
    {
        return (Template) newObject(context, Template.TYPE_NAME);
    }

    public TemplateType newTemplateType(RequestContext context)
    {
        return (TemplateType) newObject(context, TemplateType.TYPE_NAME);
    }

    // generics

    public void saveObject(RequestContext context, ModelObject obj)
    {
        Document xmlDocument = obj.getDocument();
        if (xmlDocument != null)
        {
            String modelRelativePath = obj.getRelativePath();
            String modelFileName = obj.getFileName();
            
            // write the document to the model's file system
            ModelUtil.writeDocument(getFileSystem(), modelRelativePath,
                    modelFileName, xmlDocument);

            // make sure that the cache is in sync
            obj.touch();
            cachePut(context, obj);
        }
    }

    public ModelObject loadObject(RequestContext context, IFile file)
    {
        String modelRelativeFilePath = file.getPath();
        return _loadObject(context, modelRelativeFilePath);
    }
    
    public ModelObject loadObject(RequestContext context, String typeId, String id)
    {
        String modelRelativeFilePath = this.convertIDToRelativeFilePath(typeId, id);
        return _loadObject(context, modelRelativeFilePath);
    }
    
    public ModelObject loadObject(RequestContext context, String id)
    {
        String modelRelativeFilePath = this.convertIDToRelativeFilePath(id);
        return _loadObject(context, modelRelativeFilePath);
        
    }
        
    protected ModelObject _loadObject(RequestContext context, String modelRelativeFilePath)
    {
        // check the cache to see if we already have it
        ModelObject obj = cacheGetByPath(context, modelRelativeFilePath);
        if (obj != null)
        {
            return obj;
        }

        try
        {
            // Read the document from the model's file system
            IFile file = getFileSystem().getFile(modelRelativeFilePath);
            if (file != null)
            {
                Document document = ModelUtil.readDocument(file);
                obj = convertDocumentToModelObject(document,
                        file.getModificationDate());

                // get the relative path for this type
                int u = modelRelativeFilePath.lastIndexOf("/");
                String relativePath = modelRelativeFilePath.substring(0, u);

                obj.setRelativePath(relativePath);

                // make sure that the cache is in sync
                cachePut(context, obj);
            }
            else
            {
                // we didn't find the file, we'll return null
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return (ModelObject) obj;
    }

    

    public void removeObject(RequestContext context, ModelObject obj)
    {
        Document xmlDocument = obj.getDocument();
        if (xmlDocument != null)
        {
            String modelRelativePath = obj.getRelativePath();
            String modelFileName = obj.getFileName();

            // delete the file from the model's file system
            getFileSystem().deleteFile(modelRelativePath, modelFileName);

            // make sure that the cache is in sync
            cacheRemove(context, obj);
        }
    }

    public ModelObject newObject(RequestContext context, String typeName)
    {
        // construct the xml
        String xml = "<" + typeName + "></" + typeName + ">";

        // constructs a new GUID (with prefix if available)
        String id = newGUID(typeName);

        // build the object
        ModelObject obj = null;
        try
        {
            Document d = XMLUtil.parse(xml);
            XMLUtil.addChildValue(d.getRootElement(), "id", id);
            XMLUtil.addChildValue(d.getRootElement(), "name", id);
            XMLUtil.addChildValue(d.getRootElement(), "description", id);

            obj = (ModelObject) convertDocumentToModelObject(d,
                    System.currentTimeMillis());

            // get the relative path for this type
            String modelRelativePath = getConfiguration().getModelTypePath(
                    typeName);
            obj.setRelativePath(modelRelativePath);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return obj;
    }

    public ModelObject[] loadObjects(RequestContext context, String typeName)
    {
        ModelObject[] array = new ModelObject[] {};

        String modelRelativeDirectoryPath = context.getConfig().getModelTypePath(
                typeName);
        
        // read files from the model's file system
        IFile[] files = getFileSystem().getFiles(modelRelativeDirectoryPath);
        if (files != null)
        {
            array = new ModelObject[files.length];
            for (int i = 0; i < files.length; i++)
            {
                array[i] = loadObject(context, files[i]);
            }
        }

        return array;
    }

    
    
    
    // Internal Cache for Objects
    protected HashMap cacheMap = null;

    public void cacheInvalidateAll(RequestContext context)
    {
        if (cacheMap != null)
            cacheMap.clear();
    }

    protected IContentCache getCache(RequestContext context)
    {
        if (cacheMap == null)
            cacheMap = new HashMap();

        String cacheMapKey = context.getStoreId();
        IContentCache cache = (IContentCache) cacheMap.get(cacheMapKey);
        if (cache == null)
        {
            long timeout = 30 * 60 * 1000; // 30 minutes
            cache = CacheFactory.createADSCache(getFileSystem(), timeout);
            cacheMap.put(cacheMapKey, cache);
        }
        return cache;
    }

    protected void cachePut(RequestContext context, ModelObject obj)
    {
        String cacheKey = obj.getRelativeFilePath();
        getCache(context).put(cacheKey, obj);
    }

    protected void cacheRemove(RequestContext context, ModelObject obj)
    {
        String cacheKey = obj.getRelativeFilePath();
        getCache(context).remove(cacheKey);
    }

    protected ModelObject cacheGetByPath(RequestContext context,
            String relativePath)
    {
        ModelObject obj = (ModelObject) getCache(context).get(relativePath);
        return obj;
    }
}
