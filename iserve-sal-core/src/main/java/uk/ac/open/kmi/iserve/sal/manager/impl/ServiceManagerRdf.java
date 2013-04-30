/*
   Copyright ${year}  Knowledge Media Institute - The Open University

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
package uk.ac.open.kmi.iserve.sal.manager.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.QueryResultTable;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.openrdf.rdf2go.RepositoryModel;
import org.openrdf.repository.RepositoryException;

import uk.ac.open.kmi.iserve.commons.io.RDFRepositoryConnector;
import uk.ac.open.kmi.iserve.commons.vocabulary.DC;
import uk.ac.open.kmi.iserve.commons.vocabulary.MSM;

import uk.ac.open.kmi.iserve.imatcher.IServeIMatcher;
import uk.ac.open.kmi.iserve.sal.ServiceFormat;
import uk.ac.open.kmi.iserve.sal.ServiceFormatDetector;
import uk.ac.open.kmi.iserve.sal.SystemConfiguration;
import uk.ac.open.kmi.iserve.sal.exception.ServiceException;
import uk.ac.open.kmi.iserve.sal.manager.ServiceManager;
import uk.ac.open.kmi.iserve.sal.util.UriUtil;

public class ServiceManagerRdf implements ServiceManager {

	private ServiceFormatDetector formatDetector;

	private RDFRepositoryConnector repoConnector;

	private SystemConfiguration configuration;

	/**
	 * Constructor for the Service Manager. Protected to avoid external access.
	 * Any access to this should take place through the iServeManager
	 * 
	 * @param configuration
	 * @throws RepositoryException
	 * @throws TransformerConfigurationException
	 * @throws WSDLException
	 * @throws ParserConfigurationException
	 */
	protected ServiceManagerRdf(SystemConfiguration configuration) throws RepositoryException {
		this.configuration = configuration;
		repoConnector = new RDFRepositoryConnector(configuration.getServicesRepositoryUrl().toString(),
				configuration.getServicesRepositoryName());		
	}

	/**
	 * @return the repoConnector
	 */
	public RDFRepositoryConnector getRepoConnector() {
		return this.repoConnector;
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.iserve.sal.manager.ServiceManager#listService()
	 */
	@Override
	public List<URI> listServices() {
		List<URI> result = new ArrayList<URI>();
		String queryString = "select DISTINCT ?s where { \n" +
				"?s " + RDF.type.toSPARQL() + " " + MSM.NS_PREFIX + ":" + MSM.SERVICE +
				" . }";

		RepositoryModel model = repoConnector.openRepositoryModel();
		QueryResultTable qrt = model.sparqlSelect(queryString);
		if ( qrt != null ) {
			ClosableIterator<org.ontoware.rdf2go.model.QueryRow> iter = qrt.iterator();
			if ( iter != null ) {
				while ( iter.hasNext() ) {
					org.ontoware.rdf2go.model.QueryRow qr = iter.next();
					String valueString = qr.getValue("s").toString();
					try {
						result.add(new URI(valueString));
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				iter.close();
				iter = null;
			}
		}
		repoConnector.closeRepositoryModel(model);
		model = null;
		return result;
	}


	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.iserve.sal.manager.ServiceManager#addService(java.io.InputStream, java.net.URI)
	 */
	public URI addService(InputStream msmInputStream, URI sourceDocumentUri) throws ServiceException {

		if (msmInputStream == null || !isValid(msmInputStream)) {
			throw new ServiceException("The service is not valid MSM.");
		}

		if (sourceDocumentUri == null) {
			throw new ServiceException("The URI of the source document is null. Unable to store the service.");
		}

		URI newServiceUri = UriUtil.generateUniqueResourceUri(configuration.getServicesUri());

		// save RDF to OWLim
		RepositoryModel model = repoConnector.openRepositoryModel(sourceDocumentUri.toString());
		try {
			model.readFrom(msmInputStream, Syntax.RdfXml, newServiceUri.toString());
			ClosableIterator<Statement> serviceURIs = model.findStatements(Variable.ANY, RDF.type, model.createURI(MSM.SERVICE));
			if ( null == serviceURIs ) {
				throw new ServiceException("Errors in transformation results: msm:Service cannot be found");
			}
			Statement stmt = serviceURIs.next();
			if ( null == stmt || null == stmt.getSubject() ) {
				throw new ServiceException("Errors in transformation results: msm:Service cannot be found");
			}
			serviceURIs.close();

			// Store the source for the document is given
			model.addStatement(stmt.getSubject(), model.createURI(DC.SOURCE), model.createURI(sourceDocumentUri.toString()));

		} catch (ModelRuntimeException e) {
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		} finally {
			repoConnector.closeRepositoryModel(model);
		}	

		// add to iServe iMatcher
		// Replace with an observer mechanism so that anybody can register 
		// push this up to iServeManager though
		//		Model model = getServiceAsModel(serviceUri);
		//		model.open();
		//		String serviceInRdf = model.serialize(Syntax.RdfXml);
		//		IServeIMatcher.getInstance().addRdf(serviceInRdf, serviceUri);
		//		model.close();
		//		model = null;

		return newServiceUri;
	}

	/**
	 * Returns the URI of the document defining the service, i.e., without the 
	 * fragment.
	 * 
	 * @param serviceId the unique id of the service
	 * @return the URI of the service document
	 */
	private URI getServiceBaseUri(String serviceId) {
		return configuration.getServicesUri().resolve(serviceId);
	}

	/**
	 * @param msmServiceDescriptionStream
	 * @return
	 */
	private boolean isValid(InputStream msmServiceDescriptionStream) {
		// TODO Auto-generated method stub
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.iserve.sal.manager.ServiceManager#deleteService(java.lang.String)
	 */
	@Override
	public boolean deleteService(URI serviceUri) throws ServiceException {
		String contextUri = getContextUri(serviceUri);
		if ( null == contextUri ) {
			throw new ServiceException("Cannot find service identified by " + serviceUri);
		}

		// delete from OWLim
		RepositoryModel model = repoConnector.openRepositoryModel(contextUri);
		ClosableIterator<Statement> iter = model.iterator();
		while ( iter.hasNext() ) {
			Statement stmt = iter.next();
			model.removeStatement(stmt);
			// Remove from iServe iMatcher
			IServeIMatcher.getInstance().removeStatement(stmt);
		}
		iter.close();
		iter = null;
		repoConnector.closeRepositoryModel(model);
		model = null;

		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.iserve.sal.manager.ServiceManager#getService(java.lang.String, org.ontoware.rdf2go.model.Syntax)
	 */
	@Override
	public String getServiceSerialisation(URI serviceUri, Syntax syntax) throws ServiceException {
		String contextUri = getContextUri(serviceUri);
		if ( null == contextUri ) {
			throw new ServiceException("Cannot find service identified by " + serviceUri);
		}
		RepositoryModel model = repoConnector.openRepositoryModel(contextUri);
		String result = model.serialize(syntax);
		repoConnector.closeRepositoryModel(model);
		model = null;
		return result;
	}

	private ServiceFormat detectFormat(String serviceDescription) throws IOException {
		return formatDetector.detect(serviceDescription);
	}


	/**
	 * Get the context URI given a service
	 * 
	 * @param serviceUri
	 * @return
	 */
	private String getContextUri(URI serviceUri) {
		String queryString = "select ?c where { \n" +
				"graph ?c { \n" +
				"<" + serviceUri + "> " + RDF.type.toSPARQL() + " " + MSM.NS_PREFIX + ":" + MSM.SERVICE +
				" . } }";

		RepositoryModel model = repoConnector.openRepositoryModel();
		QueryResultTable qrt = model.sparqlSelect(queryString);
		String defUri = null;
		if ( qrt != null ) {
			ClosableIterator<org.ontoware.rdf2go.model.QueryRow> iter = qrt.iterator();
			if ( iter != null ) {
				while ( iter.hasNext() ) {
					org.ontoware.rdf2go.model.QueryRow qr = iter.next();
					String valueString = qr.getValue("c").toString();
					if ( valueString.toLowerCase().contains(configuration.getIserveUri().toString()) ) {
						defUri = valueString;
					}
				}
				iter.close();
				iter = null;
			}
		}
		repoConnector.closeRepositoryModel(model);
		model = null;
		return defUri;
	}


	/**
	 * Get the context URI given a service ID
	 * 
	 * @param serviceId
	 * @return
	 */
	private String getContextUriById(String serviceId) {
		String queryString = "select ?c where { \n" +
				"graph ?c { \n" +
				"?s " + RDF.type.toSPARQL() + " " + MSM.NS_PREFIX + ":" + MSM.SERVICE + "\n" +
				"FILTER regex(str(?s), \"" + serviceId + "\", \"i\")}\n}";

		RepositoryModel model = repoConnector.openRepositoryModel();
		QueryResultTable qrt = model.sparqlSelect(queryString);
		String defUri = null;
		if ( qrt != null ) {
			ClosableIterator<org.ontoware.rdf2go.model.QueryRow> iter = qrt.iterator();
			if ( iter != null ) {
				while ( iter.hasNext() ) {
					org.ontoware.rdf2go.model.QueryRow qr = iter.next();
					String valueString = qr.getValue("c").toString();
					if ( valueString.toLowerCase().contains(configuration.getIserveUri().toString()) ) {
						defUri = valueString;
					}
				}
				iter.close();
				iter = null;
			}
		}
		repoConnector.closeRepositoryModel(model);
		model = null;
		return defUri;
	}

//	// TODO: Id generation should not require a SPARQL Query
//	private URI getServiceUri(String serviceId) {
//		if (serviceId == null) {
//			return null;
//		}
//
//		String queryString = "select ?s where { \n" +
//				"?s " + RDF.type.toSPARQL() + " " + MSM.NS_PREFIX + ":" + MSM.SERVICE + "\n" +
//				"FILTER regex(str(?s), \"" + serviceId + "\", \"i\")\n}";
//
//		URI defUri = null;
//		RepositoryModel model = repoConnector.openRepositoryModel();
//		try {
//			QueryResultTable qrt = model.sparqlSelect(queryString);
//
//			if ( qrt != null ) {
//				ClosableIterator<org.ontoware.rdf2go.model.QueryRow> iter = qrt.iterator();
//				if ( iter != null ) {
//					while ( iter.hasNext() ) {
//						org.ontoware.rdf2go.model.QueryRow qr = iter.next();
//						String valueString = qr.getValue("s").toString();
//						if ( valueString.toLowerCase().contains(configuration.getIserveUri().toString()) ) {
//							defUri = new URI(valueString);
//						}
//					}
//					iter.close();
//					iter = null;
//				}
//			}
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			repoConnector.closeRepositoryModel(model);
//		}
//		return defUri;
//	}

	/* (non-Javadoc)
	 * @see uk.ac.open.kmi.iserve.sal.manager.ServiceManager#serviceExists(java.net.URI)
	 */
	@Override
	public boolean serviceExists(URI serviceUri) throws ServiceException {
		// TODO Auto-generated method stub
		return false;
	}

}
