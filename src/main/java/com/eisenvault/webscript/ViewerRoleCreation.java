package com.eisenvault.webscript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class ViewerRoleCreation extends AbstractWebScript {
 
    protected ServiceRegistry serviceRegistry = null;
    protected SearchService searchService = null; 
    protected NodeService nodeService = null;
    protected AuthorityService authorityService = null;
    protected String authorityName = null;
    protected PermissionService permissionService = null;
    
    /**
     * Returns a list of sites in Alfresco
     *   
     * @return a list of site nodes 
     */  
    protected List<NodeRef> getSites(){
    
        //Get all sites
        String nodeQuery = "TYPE:\"st:site\"";

        SearchParameters params = new SearchParameters();
        params.setLanguage(SearchService.LANGUAGE_LUCENE);
        params.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        params.setQuery(nodeQuery);
        ResultSet results = null;
        List<NodeRef> siteNodeRefs = new ArrayList<NodeRef>();
        
        try {
            results = this.searchService.query(params);
            if (results == null) {
                return null;

            } else {

                Iterator<ResultSetRow> siteNodes = results.iterator();
                while(siteNodes.hasNext()){
                     
                     ResultSetRow siteRow = siteNodes.next();
                     NodeRef nodeRef = siteRow.getNodeRef();
                     
                     siteNodeRefs.add(nodeRef);
                }
                
                return siteNodeRefs;
            }
            
        } finally {
            if (results != null) {
                results.close();
            }
        } 
         
    }    
    
    /**
     *  Webscript execute method
     *  
     */
    public void execute(WebScriptRequest req, WebScriptResponse res) 
            throws IOException  {
         
        try
        { 
            
            // build a json object
            JSONObject obj = new JSONObject();
            
            List<NodeRef> sites = this.getSites();
            System.out.println("************* Sites *************" + sites);
              
            Iterator<NodeRef> siteNodes = sites.iterator();
            while(siteNodes.hasNext()){
                 
                NodeRef nodeRef = siteNodes.next();
                  
                 //Get the sitename
                 String siteid = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);  
                 String siteGroup = new StringBuilder().append("GROUP_site_").append(siteid).toString();
                       
                 if(this.authorityService.authorityExists(siteGroup)){
                    
                    Set<String> authorities = this.authorityService.getContainedAuthorities(AuthorityType.GROUP, siteGroup, true);
                    String customRoleAuthorityName = this.authorityName.replace(":SITE", siteid);
                    
                    System.out.println("********* SiteID, SiteGroup, Authority and CustomRoleAuthorityName *********" + siteid + siteGroup + authorities + customRoleAuthorityName);
                    if (authorities.contains(customRoleAuthorityName) == false)
                     { 
                        obj.put(siteGroup, customRoleAuthorityName);
                        
                        //create the authority
                        String newAuthName = this.authorityService.createAuthority(AuthorityType.GROUP, customRoleAuthorityName.replace("GROUP_", "")); 
                        this.authorityService.addAuthority(siteGroup, newAuthName);
                         
                        // Assign the group the relevant permission on the site and document library
                        String[] bits = customRoleAuthorityName.split("_");
                        String role = bits[bits.length-1];
                        
                        System.out.println("******* NewAuthName, Bits, Role *******" + newAuthName + bits + role);
                        
                        permissionService.setPermission(nodeRef, customRoleAuthorityName, role, true);                            
                        NodeRef libraryNodeRef = nodeService.getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, "documentLibrary");
                        permissionService.setPermission(libraryNodeRef, customRoleAuthorityName, role, true);
                     }
                 }
            }
             
            // build a JSON string and send it back
            String jsonString = obj.toString();
            res.getWriter().write(jsonString);
        }
        catch(JSONException e)
        {
            throw new WebScriptException("Unable to serialize JSON");
        } 
    }
    
     
    
    /* ------------------------------------------------------------ */
    /* Getter/Setter methods */
    /* ------------------------------------------------------------ */
    
    /**
     * @return the serviceRegistry
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * @param serviceRegistry the serviceRegistry to set
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.searchService = serviceRegistry.getSearchService(); 
        this.nodeService = serviceRegistry.getNodeService();
        this.permissionService = serviceRegistry.getPermissionService();
    }

    /**
     * @return the searchService
     */
    public SearchService getSearchService() {
        return searchService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
    
    /**
     * @return the authorityService
     */
    public AuthorityService getAuthorityService() {
        return authorityService;
    }

    /**
     * @param authorityService the searchService to set
     */
    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }
      

    /**
     * @return the nodeService
     */
    public NodeService getNodeService() {
        return nodeService;
    }

    /**
     * @param nodeService the nodeService to set
     */
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }


    public String getAuthorityName() {
        return authorityName;
    }


    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }
    
    /**
     * Set permission service
     */
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }  
    public PermissionService getPermissionService()
    {
        return permissionService;
    }
}

