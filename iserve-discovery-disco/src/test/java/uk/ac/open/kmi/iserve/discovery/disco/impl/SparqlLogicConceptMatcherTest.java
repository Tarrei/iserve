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

package uk.ac.open.kmi.iserve.discovery.disco.impl;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Table;
import junit.extensions.TestSetup;
import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.iserve.commons.io.MediaType;
import uk.ac.open.kmi.iserve.commons.io.Syntax;
import uk.ac.open.kmi.iserve.commons.io.util.FilenameFilterBySyntax;
import uk.ac.open.kmi.iserve.discovery.api.MatchResult;
import uk.ac.open.kmi.iserve.discovery.disco.LogicConceptMatchType;
import uk.ac.open.kmi.iserve.sal.manager.impl.iServeFacade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * LogicConceptMatcherTest
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @since 01/08/2013
 */
public class SparqlLogicConceptMatcherTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(SparqlLogicConceptMatcherTest.class);

    private static final String OWLS_TC_SERVICES = "/OWLS-TC3-MSM";
    private static final String WSC08_01_SERVICES = "/wsc08-dataset01/services";
    private static final String JGD_SERVICES = "/jgd-services";
    private static final String SOA4RE_SERVICES = "/soa4re";
    private static final String SPARQL_ENDPOINT = "http://localhost:8080/openrdf-sesame/repositories/Test";

    private static SparqlLogicConceptMatcher conceptMatcher;
    private static iServeFacade manager;

    public static TestSetup suite() {

        TestSetup setup = new TestSetup(new TestSuite(SparqlLogicConceptMatcherTest.class)) {
            protected void setUp() throws Exception {
                // do your one-time setup here
                manager = iServeFacade.getInstance();

                // Clean the whole thing before testing
                manager.clearRegistry();

                conceptMatcher = new SparqlLogicConceptMatcher(SPARQL_ENDPOINT);

                // Obtain service documents
                URI testFolder = SparqlLogicConceptMatcherTest.class.getResource(OWLS_TC_SERVICES).toURI();
                FilenameFilter ttlFilter = new FilenameFilterBySyntax(Syntax.TTL);
                File dir = new File(testFolder);
                File[] msmTtlTcFiles = dir.listFiles(ttlFilter);

                FileInputStream in;
                // Upload every document and obtain their URLs
                for (File ttlFile : msmTtlTcFiles) {
                    log.debug("Importing {}", ttlFile.getAbsolutePath());
                    in = new FileInputStream(ttlFile);
                    manager.importServices(in, MediaType.TEXT_TURTLE.getMediaType());
                }
                log.debug("Ready");
            }

            protected void tearDown() throws Exception {
                // do your one-time tear down here!
            }
        };
        return setup;
    }

    public void testMatch() throws Exception {

        URI origin = URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar");
        URI destination = URI.create("http://127.0.0.1/ontology/SUMO.owl#Quantity");

        // Obtain matches
        Stopwatch stopwatch = new Stopwatch().start();
        MatchResult match = conceptMatcher.match(origin, destination);
        stopwatch.stop();

        log.info("Obtained match in {} \n {}", stopwatch, match);
        Assert.assertEquals(match.getMatchType(), LogicConceptMatchType.Plugin);
    }

    @Test
    public void testMatchBySets() throws Exception {

        Set<URI> origins = new HashSet<URI>();
        origins.add(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"));
        origins.add(URI.create("http://127.0.0.1/ontology/SUMO.owl#Abstract"));
        origins.add(URI.create("http://127.0.0.1/ontology/SUMO.owl#TherapeuticProcess"));

        Set<URI> destinations = new HashSet<URI>();
        destinations.add(URI.create("http://127.0.0.1/ontology/SUMO.owl#Quantity"));
        destinations.add(URI.create("http://127.0.0.1/ontology/SUMO.owl#IntentionalProcess"));
        destinations.add(URI.create("http://127.0.0.1/ontology/SUMO.owl#Entity"));

        // Obtain cross-matches
        Stopwatch stopwatch = new Stopwatch().start();
        Table<URI, URI, MatchResult> matches =
                conceptMatcher.match(origins, destinations);
        stopwatch.stop();

        log.info("Obtained all cross matches ({}) in {}", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 4);
        stopwatch.reset();

    }

    @Test
    public void testListMatchesOfType() throws Exception {
        // Obtain only exact
        Stopwatch stopwatch = new Stopwatch().start();
        Map<URI, MatchResult> matches =
                conceptMatcher.listMatchesOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Exact);
        stopwatch.stop();

        log.info("Obtained all exact matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 1);
        stopwatch.reset();

        // Obtain only plugin
        stopwatch.start();
        matches = conceptMatcher.listMatchesOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Plugin);
        stopwatch.stop();

        log.info("Obtained all plugin matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 5);
        stopwatch.reset();

        // Obtain only plugin
        stopwatch.start();
        matches = conceptMatcher.listMatchesOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Subsume);
        stopwatch.stop();

        log.info("Obtained all plugin matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 0);
        stopwatch.reset();
    }

    @Test
    public void testListMatchesAtLeastOfType() throws Exception {
        // Obtain at least exact
        Stopwatch stopwatch = new Stopwatch().start();
        Map<URI, MatchResult> matches =
                conceptMatcher.listMatchesAtLeastOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Exact);
        stopwatch.stop();

        log.info("Obtained at least Exact matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 1);
        stopwatch.reset();

        // Obtain at least plugin
        stopwatch.start();
        matches = conceptMatcher.listMatchesAtLeastOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Plugin);
        stopwatch.stop();

        log.info("Obtained at least Plugin matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 6);
        stopwatch.reset();

        // Obtain at least subsumes
        stopwatch.start();
        matches = conceptMatcher.listMatchesAtLeastOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Subsume);
        stopwatch.stop();

        log.info("Obtained at least Subsumes matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 6);
        stopwatch.reset();

        // Obtain at least fail
        stopwatch.start();
        matches = conceptMatcher.listMatchesAtLeastOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Fail);
        stopwatch.stop();

        log.info("Obtained at least Fail matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 6);
        stopwatch.reset();
    }

    @Test
    public void testListMatchesAtMostOfType() throws Exception {
        // Obtain at least exact
        Stopwatch stopwatch = new Stopwatch().start();
        Map<URI, MatchResult> matches =
                conceptMatcher.listMatchesAtMostOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Exact);
        stopwatch.stop();

        log.info("Obtained at most Exact matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 6);
        stopwatch.reset();

        // Obtain at least plugin
        stopwatch.start();
        matches = conceptMatcher.listMatchesAtMostOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Plugin);
        stopwatch.stop();

        log.info("Obtained at most Plugin matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 5);
        stopwatch.reset();

        // Obtain at least subsumes
        stopwatch.start();
        matches = conceptMatcher.listMatchesAtMostOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Subsume);
        stopwatch.stop();

        log.info("Obtained at most Subsumes matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 0);
        stopwatch.reset();

        // Obtain at least fail
        stopwatch.start();
        matches = conceptMatcher.listMatchesAtMostOfType(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Fail);
        stopwatch.stop();

        log.info("Obtained at most Fail matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 0);
        stopwatch.reset();
    }

    @Test
    public void testListMatchesWithinRange() throws Exception {

        // Obtain all matches
        Stopwatch stopwatch = new Stopwatch().start();
        Map<URI, MatchResult> matches =
                conceptMatcher.listMatchesWithinRange(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Fail, LogicConceptMatchType.Exact);
        stopwatch.stop();

        log.info("Obtained all matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 6);
        stopwatch.reset();

        // Obtain only exact
        stopwatch.start();
        matches = conceptMatcher.listMatchesWithinRange(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Exact, LogicConceptMatchType.Exact);
        stopwatch.stop();

        log.info("Obtained exact matches ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 1);
        stopwatch.reset();

        // Obtain from Plugin up
        stopwatch.start();
        matches = conceptMatcher.listMatchesWithinRange(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Plugin, LogicConceptMatchType.Exact);
        stopwatch.stop();

        log.info("Obtained matches >= Plugin ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 6);
        stopwatch.reset();

        // Obtain Plugin and subsumes
        stopwatch.start();
        matches = conceptMatcher.listMatchesWithinRange(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Subsume, LogicConceptMatchType.Plugin);
        stopwatch.stop();

        log.info("Obtained Subsumes >= matches >= Plugin ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 5);
        stopwatch.reset();

        // Invert limits
        stopwatch.start();
        matches = conceptMatcher.listMatchesWithinRange(URI.create("http://127.0.0.1/ontology/SUMO.owl#EuroDollar"), LogicConceptMatchType.Exact, LogicConceptMatchType.Plugin);
        stopwatch.stop();

        log.info("Obtained Exact >= matches >= Plugin ({}) in {} \n", matches.size(), stopwatch);
        Assert.assertEquals(matches.size(), 0);
        stopwatch.reset();

    }
}