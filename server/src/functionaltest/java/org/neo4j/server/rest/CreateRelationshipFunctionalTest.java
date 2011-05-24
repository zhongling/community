/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.server.WebTestUtils.CLIENT;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.NeoServerWithEmbeddedWebServer;
import org.neo4j.server.ServerBuilder;
import org.neo4j.server.database.DatabaseBlockedException;
import org.neo4j.server.modules.RESTApiModule;
import org.neo4j.server.rest.domain.GraphDbHelper;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.repr.RelationshipRepresentationTest;

import com.sun.jersey.api.client.ClientResponse;

public class CreateRelationshipFunctionalTest
{
    private String RELATIONSHIP_URI_PATTERN;

    private NeoServerWithEmbeddedWebServer server;
    private FunctionalTestHelper functionalTestHelper;
    private GraphDbHelper helper;
    
    public @Rule
    DocumentationGenerator gen = new DocumentationGenerator();

    @SuppressWarnings( "unchecked" )
    @Before
    public void setupServer() throws IOException {
        server = ServerBuilder.server().withRandomDatabaseDir().withSpecificServerModules(RESTApiModule.class).withPassingStartupHealthcheck().build();
        server.start();
        functionalTestHelper = new FunctionalTestHelper(server);
        helper = functionalTestHelper.getGraphDbHelper();

        RELATIONSHIP_URI_PATTERN = functionalTestHelper.dataUri() + "relationship/[0-9]+";
    }

    @After
    public void stopServer() {
        server.stop();
        server = null;
    }

    @Test
    public void shouldRespondWith201WhenSuccessfullyCreatedRelationshipWithProperties() throws Exception
    {
        long startNode = helper.createNode();
        long endNode = helper.createNode();
        String jsonString = "{\"to\" : \"" + functionalTestHelper.dataUri() + "node/" + endNode
                + "\", \"type\" : \"LOVES\", \"data\" : {\"foo\" : \"bar\"}}";
        ClientResponse response = CLIENT.resource( functionalTestHelper.dataUri() + "node/" + startNode + "/relationships" ).type(
                MediaType.APPLICATION_JSON ).accept( MediaType.APPLICATION_JSON ).entity( jsonString ).post( ClientResponse.class );
        assertEquals( 201, response.getStatus() );
        assertTrue( response.getLocation().toString().matches( RELATIONSHIP_URI_PATTERN ) );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getType() );
        String relationshipUri = response.getLocation().toString();
        long relationshipId = Long.parseLong( relationshipUri.substring( relationshipUri.lastIndexOf( '/' ) + 1 ) );
        Map<String, Object> properties = helper.getRelationshipProperties( relationshipId );
        assertEquals( MapUtil.map( "foo", "bar" ), properties );
        assertProperRelationshipRepresentation( JsonHelper.jsonToMap( response.getEntity( String.class ) ) );
        response.close();
    }

    /**
     * Create relationship.
     */
    @Documented
    @Test
    public void shouldRespondWith201WhenSuccessfullyCreatedRelationship() throws Exception
    {
        long startNode = helper.createNode();
        long endNode = helper.createNode();
        String jsonString = "{\"to\" : \"" + functionalTestHelper.dataUri() + "node/" + endNode + "\", \"type\" : \"LOVES\"}";
        String uri = functionalTestHelper.dataUri() + "node/" + startNode + "/relationships";
        ClientResponse response = CLIENT.resource( uri ).type(
                MediaType.APPLICATION_JSON ).accept( MediaType.APPLICATION_JSON ).entity( jsonString ).post( ClientResponse.class );
        assertEquals( 201, response.getStatus() );
        assertTrue( response.getLocation().toString().matches( RELATIONSHIP_URI_PATTERN ) );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getType() );
        assertProperRelationshipRepresentation( JsonHelper.jsonToMap( response.getEntity( String.class ) ) );
        gen.create()
                .payload( jsonString )
                .expectedStatus( Response.Status.CREATED )
                .post( uri );
        response.close();
    }

    @Test
    public void shouldRespondWith404WhenStartNodeDoesNotExist() throws DatabaseBlockedException
    {
        long endNode = helper.createNode();
        String jsonString = "{\"to\" : \"" + functionalTestHelper.dataUri() + "node/" + endNode
                + "\", \"type\" : \"LOVES\", \"data\" : {\"foo\" : \"bar\"}}";
        ClientResponse response = CLIENT.resource( functionalTestHelper.dataUri() + "node/999999/relationships" ).type(
                MediaType.APPLICATION_JSON ).accept( MediaType.APPLICATION_JSON ).entity( jsonString ).post( ClientResponse.class );
        assertEquals( 404, response.getStatus() );
        response.close();
    }

    @Test
    public void shouldRespondWith400WhenEndNodeDoesNotExist() throws DatabaseBlockedException
    {
        long startNode = helper.createNode();
        String jsonString = "{\"to\" : \"" + functionalTestHelper.dataUri() + "node/"
                + "999999\", \"type\" : \"LOVES\", \"data\" : {\"foo\" : \"bar\"}}";
        ClientResponse response = CLIENT.resource( functionalTestHelper.dataUri() + "node/" + startNode + "/relationships" ).type(
                MediaType.APPLICATION_JSON ).accept( MediaType.APPLICATION_JSON ).entity( jsonString ).post( ClientResponse.class );
        assertEquals( 400, response.getStatus() );
        response.close();
    }

    @Test
    public void shouldRespondWith400WhenBadJsonProvided() throws DatabaseBlockedException
    {
        long startNode = helper.createNode();
        long endNode = helper.createNode();
        String jsonString = "{\"to\" : \"" + functionalTestHelper.dataUri() + "node/" + endNode
                + "\", \"type\" : \"LOVES\", \"data\" : {\"foo\" : **BAD JSON HERE*** \"bar\"}}";
        ClientResponse response = CLIENT.resource( functionalTestHelper.dataUri() + "node/" + startNode + "/relationships" ).type(
                MediaType.APPLICATION_JSON ).accept( MediaType.APPLICATION_JSON ).entity( jsonString ).post( ClientResponse.class );

        assertEquals( 400, response.getStatus() );
        response.close();
    }

    private void assertProperRelationshipRepresentation(
            Map<String, Object> relrep )
    {
        RelationshipRepresentationTest.verifySerialisation( relrep );
    }
}
