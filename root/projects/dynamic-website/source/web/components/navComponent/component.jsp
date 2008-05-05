<%@ page import="org.alfresco.web.site.*" %>
<%@ page import="org.alfresco.web.site.model.*" %>
<%@ page buffer="0kb" autoFlush="true" contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="/WEB-INF/tlds/alf.tld" prefix="alf" %>
<%@ taglib uri="/WEB-INF/tlds/adw.tld" prefix="adw" %>
<%@ page isELIgnored="false" %>
<alf:require script="/yui/yahoo-dom-event/yahoo-dom-event.js"/>
<alf:require script="/yui/container/container-min.js"/>
<alf:require script="/yui/menu/menu.js"/>
<%
	// get the request context
	RequestContext context = RequestUtil.getRequestContext(request);
	
	String orientation = (String) context.getRenderContext().get("orientation");
	if(orientation == null || "".equals(orientation))
	{
		String currentThemeId = ThemeUtil.getCurrentThemeId(context);
		String unconfiguredImageUrl = URLUtil.browser(context, "/themes/builder/images/" + currentThemeId + "/icons/unconfigured_component_large.gif");
		String renderString = "<img src='" + unconfiguredImageUrl + "' border='0' alt='Unconfigured Nav Component'/>";	
		out.println(renderString);
		return;	
	}
	
	// determine the renderer to use
	String renderer = (String) context.getRenderContext().get("renderer");
	if(renderer == null)
	{
		renderer = "renderers/horizontalNav1/horizontalNav1.jsp";
		if("vertical".equalsIgnoreCase(orientation))
			renderer = "renderers/verticalNav1/verticalNav1.jsp";
	}
	
	// dispatch
	if(renderer.endsWith(".jsp"))
	{
		RequestUtil.include(request, response, renderer);
	}
	else if(renderer.endsWith(".xsl"))
	{
		// TODO: add support for XSL renderers
	}
	else if(renderer.endsWith(".ftl"))
	{
		// TODO: add support for FTL renderers
	}
%>
