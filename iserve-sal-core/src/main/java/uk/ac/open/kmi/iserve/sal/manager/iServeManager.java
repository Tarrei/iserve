/*
   Copyright 2012  Knowledge Media Institute - The Open University

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package uk.ac.open.kmi.iserve.sal.manager;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.ontoware.rdf2go.model.Syntax;

import uk.ac.open.kmi.iserve.sal.ServiceFormat;
import uk.ac.open.kmi.iserve.sal.ServiceImporter;
import uk.ac.open.kmi.iserve.sal.SystemConfiguration;
import uk.ac.open.kmi.iserve.sal.exception.SalException;

/**
 * General Purpose Manager for iServe. 
 * Higher Level Access to Data management within iServe. Only a controlled subset
 * of the inner methods for data management are exposed at this level
 * 
 * TODO: Registering import mechanisms should also be defined somewhere
 * 
 * @author Carlos Pedrinaci (Knowledge Media Institute - The Open University)
 */
//public interface iServeManager extends LogManager, KeyManager,
//		ReviewManager, TaxonomyManager, UserManager {

public interface iServeManager {

	public abstract SystemConfiguration getConfiguration();
	
	/**
	 * Register a new importer for a given file format
	 * 
	 * @param format
	 * @param importer
	 * @return
	 */
	public abstract ServiceImporter registerImporter(ServiceFormat format, 
			ServiceImporter importer);
	
	/**
	 * Unregister a new importer for a given file format
	 * 
	 * @param format
	 * @return
	 */
	public abstract ServiceImporter unregisterImporter(ServiceFormat format);
	
	/*
	 * Service Management Methods
	 */

	// Create
	/**
	 * Register a new service. This operation takes care of performing the 
	 * appropriate format transformation. The source document is not stored on 
	 * the server. For keeping a copy of the source document within the server 
	 * use @see importService instead.
	 * 
	 * @param sourceDocumentUri
	 * @param format
	 * @return
	 * @throws SalException
	 */
	public abstract URI registerService(URI sourceDocumentUri, 
			ServiceFormat format) throws SalException;
	
	/**
	 * Imports a new service within iServe. The original document is stored 
	 * in the server and the transformed version registered within iServe.
	 * 
	 * @param serviceContent
	 * @param format
	 * @return
	 * @throws SalException
	 */
	public abstract URI importService(InputStream serviceContent, 
			ServiceFormat format) throws SalException;
	
	// Read
	/**
	 * Obtains the textual representation of a Service in a particular syntax
	 * 
	 * @param serviceUri
	 * @param format the format to use for the serialisation
	 * @return
	 * @throws SalException
	 */
	public abstract String getService(URI serviceUri, ServiceFormat format) throws SalException;
	
	/**
	 * List all the known services
	 * 
	 * @return
	 * @throws SalException
	 */
	public abstract List<URI> listServices() throws SalException;
	
	// Delete
	/**
	 * Unregisters the service from iServe. Any related documents are also 
	 * deleted in the process.
	 * 
	 * @param serviceUri
	 * @return
	 * @throws SalException
	 */
	public abstract boolean unregisterService(URI serviceUri) throws SalException;
	
	// Update Operations	
	/**
	 * Relates a given document to a service. The relationship is just a generic one. 
	 * 
	 * @param serviceUri the service URI
	 * @param relatedDocument the related document URI
	 * @return True if successful or False otherwise
	 * @throws SalException
	 */
	public abstract boolean addRelatedDocumentToService(URI serviceUri, URI relatedDocument) throws SalException;
	

	/*
	 *  Document Management Operations
	 */
	
	// Create
	/**
	 * Adds a document to the system. Documents are resources on their own
	 * but are expected to be linked to services.
	 * @see  
	 * 
	 * @param fileName name of the file to use
	 * @param docContent content of the document
	 * @param serviceUri the URI of service this document relates to
	 * @return
	 * @throws SalException
	 */
	public URI createDocument(InputStream docContent) throws SalException;
	
	// Read
	
	/**
	 * Lists all known documents
	 * 
	 * @return the list of the URIs of all the documents known to this server
	 * @throws SalException
	 */
	public abstract List<URI> listDocuments() throws SalException;
	
	/**
	 * Lists all documents related to a given service
	 * 
	 * @param serviceUri the service URI
	 * @return the List of the URIs of all the documents related to the service
	 * @throws SalException
	 */
	public abstract List<URI> listDocumentsForService(URI serviceUri) throws SalException;

	/**
	 * Obtains a particular document
	 * 
	 * @param documentUri the URI of the document to get
	 * @return the content of the document
	 * @throws SalException
	 */
	public abstract String getDocument(URI documentUri) throws SalException;
	
	// Delete
	/**
	 * Deletes a document from the server
	 * 
	 * @param documentUri the URI of the document to delete
	 * @return true if it was successful or null otherwise
	 * @throws SalException
	 */
	public abstract boolean deleteDocument(URI documentUri) throws SalException;
	
}

