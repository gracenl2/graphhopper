package com.graphhopper.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.jackson.Jackson;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.JsonFeatureCollection;
import com.graphhopper.util.shapes.GHPoint;

import com.github.javafaker.Faker;
import okhttp3.*;

import org.hibernate.validator.constraints.ModCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.Map;

import static com.graphhopper.json.Statement.If;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mock;
import static org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraphHopperWebMockTest{
    @Mock
    OkHttpClientWrapper okHttpClient;


    @Mock
    Call call;

    private GraphHopperWeb createClient(){
        GraphHopperWeb gh = new GraphHopperWeb("https://unused/route");
        gh.setPostRequest(true);
        gh.setKey("test-key");
        gh.setDownloader(okHttpClient);
        return gh;
    }

    /*Cas où le serveur retourne un JSON d'erreur
     * 
     * 
     */
    @Test
    void route_error_json_triggers_hasErrors_and_removes_turn_description_hints() throws IOException{
        GraphHopperWeb gh = createClient();

        GHRequest req= new GHRequest()
                .addPoint(new GHPoint(45.5, -73.6))
                .addPoint(new GHPoint(45.4, -73.7))
                .setProfile("car");

        req.getHints().putObject("turn_description", true);

        String body = "{\"message\":\"An error occurred\",\"hints\":[]}";

        Response fakResponse = new Response.Builder()
                .request(new Request.Builder().url("https://localhost:8080/route").build())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .build();

        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(fakResponse);

        GHResponse rsp = gh.route(req);

        assertTrue(rsp.hasErrors());
        assertFalse(req.getHints().has("turn_description"));
    }
}
