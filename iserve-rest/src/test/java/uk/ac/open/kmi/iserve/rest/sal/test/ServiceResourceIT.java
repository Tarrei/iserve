/*
 * Copyright (c) 2013. Knowledge Media Institute - The Open University
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

package uk.ac.open.kmi.iserve.rest.sal.test;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jayway.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.iserve.core.ConfigurationProperty;
import uk.ac.open.kmi.iserve.core.SystemConfiguration;
import uk.ac.open.kmi.iserve.sal.exception.SalException;
import uk.ac.open.kmi.iserve.sal.manager.RegistryManager;
import uk.ac.open.kmi.iserve.sal.manager.impl.RegistryManagementModule;
import uk.ac.open.kmi.msm4j.io.MediaType;
import uk.ac.open.kmi.msm4j.io.Syntax;
import uk.ac.open.kmi.msm4j.io.util.FilenameFilterBySyntax;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;

/**
 * Class Description
 *
 * @author Carlos Pedrinaci (Knowledge Media Institute - The Open University)
 * @version $Rev$
 *          $LastChangedDate$
 *          $LastChangedBy$
 */
public class ServiceResourceIT {

    private static final Logger log = LoggerFactory.getLogger(ServiceResourceIT.class);

    private static final String OWLS_TC_SERVICES = "/services/OWLS-1.1-MSM";

    // Ensure that these are the ones listed in shiro.ini
    private static final String ROOT_USER = "root";
    private static final String ROOT_PASSWD = "secret";

    // Limit the number of documents to upload to the registry
    private static final int MAX_DOCS = 25;
    // For HTML testing (logging via forms, etc)
    private static final WebClient webClient = new WebClient();
    private static URI testFolder;
    private static File[] msmTtlTcFiles;
    private static RegistryManager manager;
    private static URL iserveUrl;

    /**
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {

        testFolder = ServiceResourceIT.class.getResource(OWLS_TC_SERVICES).toURI();
        FilenameFilter ttlFilter = new FilenameFilterBySyntax(Syntax.TTL);
        File dir = new File(testFolder);
        msmTtlTcFiles = dir.listFiles(ttlFilter);
        int numServices = msmTtlTcFiles.length;

        // Setup Web client
        webClient.setThrowExceptionOnFailingStatusCode(true);
        CookieManager cookieMan = new CookieManager();
        cookieMan = webClient.getCookieManager();
        cookieMan.setCookiesEnabled(true);

        Injector injector = Guice.createInjector(new RegistryManagementModule());
        manager = injector.getInstance(RegistryManager.class);

        SystemConfiguration config = injector.getInstance(SystemConfiguration.class);
        iserveUrl = new URL(config.getString(ConfigurationProperty.ISERVE_URL));

        // Set default values for rest assured
        RestAssured.baseURI = new StringBuilder()
                .append(iserveUrl.getProtocol()).append("://").append(iserveUrl.getHost()).toString();

        RestAssured.port = iserveUrl.getPort();
        RestAssured.basePath = iserveUrl.getPath();

        // Logout
//        logOut();
    }

    private static void logOut() throws IOException {
        log.info("Logging out...");
        // Make sure we are logged out
        final HtmlPage homePage = webClient.getPage(iserveUrl);
        try {
            homePage.getAnchorByHref("/jsp/logout.jsp").click();
        } catch (ElementNotFoundException e) {
            //Ignore
        }
    }

    @Ignore("Ignore until Shiro REST is fixed")
    @Test
    public void logIn() throws FailingHttpStatusCodeException, IOException, InterruptedException {

        HtmlPage page = webClient.getPage(iserveUrl.toString() + "/jsp/login.jsp");
        HtmlForm form = page.getFormByName("loginform");
        form.getInputByName("username").setValueAttribute(ROOT_USER);
        form.getInputByName("password").setValueAttribute(ROOT_PASSWD);
        page = form.getInputByName("submit").click();
        // This'll throw an expection if not logged in
        page.getAnchorByHref("/iserve/jsp/logout.jsp");
    }

    /**
     * Test method for AddService.
     */
    @Ignore("Ignore until Shiro REST, and Guice + Jersey are fixed")
    @Test
    public final void testAddService() throws IOException {
        // Clean the whole thing before testing
        try {
            manager.clearRegistry();
        } catch (SalException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Add all the test collections
        log.info("Uploading services");
        log.info("Test collection: " + testFolder);

 /*
        // Disable until Shiro REST is sorted out

        log.info("Trying without logging in");
        given().log().all().                                                                // Log requests
                redirects().follow(true).                                                       // Follow redirects introduced by Shiro
                multiPart("file", msmTtlTcFiles[0], MediaType.TEXT_TURTLE.getMediaType()).      // Submit file
                expect().log().all().                                                           // Log responses
                response().statusCode(302).                                                     // We should get a 302 that takes us to the login page
                when().post("/services");

        log.info("Correctly redirected");


        // Now log into Shiro with "rememberMe" set and try again
        log.info("Now logging in");

        HtmlPage page = webClient.getPage(iserveUrl.toString() + "/login.jsp");
        HtmlForm form = page.getFormByName("loginform");
        form.<HtmlInput>getInputByName("username").setValueAttribute(ROOT_USER);
        form.<HtmlInput>getInputByName("password").setValueAttribute(ROOT_PASSWD);
//        HtmlCheckBoxInput checkbox = form.getInputByName("rememberMe");
//        checkbox.setChecked(true);
        page = form.<HtmlInput>getInputByName("submit").click();

        Cookie cookie = webClient.getCookieManager().getCookie("JSESSIONID");
        log.info("Cookie data: " + cookie.toString());
  */


        // Test oriented towards individuals.
        // To be updated with application oriented logging.

        log.info("Uploading services");
        for (int i = 0; i < MAX_DOCS && i < msmTtlTcFiles.length; i++) {
            // rest assured
            given().log().all().                                                // Log requests
//                    sessionId(cookie.getValue()).                                   // Keep the session along
                    redirects().follow(true).                                       // Follow redirects introduced by Shiro
                    multiPart("file", msmTtlTcFiles[i], MediaType.TEXT_TURTLE.getMediaType()).  // Submit file
                    expect().log().all().                                           // Log responses
                    response().statusCode(201).                                     // We should get a 201 created (if we have the rights)
                    when().post("/id/services");
        }
    }

    @Ignore("Ignore until Shiro REST, and Guice + Jersey are fixed")
    @Test
    public void testDeleteService() throws Exception {

        Set<URI> existingServices = manager.getServiceManager().listServices();

        log.info("Deleting services");
        // Disable until Shiro for REST is sorted out
        // Try to delete endpoint
//        given().expect().response().statusCode(405).
//                when().delete("/services");

        // Try to delete non existing svcs (directly at the graph level)
        for (int i = 0; i < 10; i++) {
            given().expect().response().statusCode(404).
                    when().delete("/id/services/" + i);
        }

        // Try to delete non existing svcs
        for (int i = 0; i < 10; i++) {
            given().expect().response().statusCode(404).
                    when().delete("/id/services/" + i + "/serviceName");
        }

        // Try to delete services using their entire URIs
        for (URI uri : existingServices) {
            String relativeUri = iserveUrl.toURI().resolve("services").relativize(uri)
                    .toASCIIString();
            log.info("Deleting service id: " + uri);
            given().expect().response().statusCode(410).
                    when().delete("/id/services/" + relativeUri);
        }

    }
}
