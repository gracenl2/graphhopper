package com.graphhopper.api;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.util.shapes.GHPoint;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import  org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class GraphHopperWebMockTest{
    @Mock
    OkHttpClient okHttpClient; //cas d'utilisation de mockito pour simuler les requete HTTP


    @Mock
    Call call; //cas d'utilisation de mockito pour simuler objetde l'appel HTTP

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

        assertTrue(rsp.hasErrors(), "Response should have errors");
        assertFalse(req.getHints().has("turn_description"), "turn_description hint should be removed from request hints");
    }

    /*Cas le serveur retourne un JSON valide */

    @Test
    void route_success_no_errors_copies_headers_and_hints_with_empty_paths_hints() throws IOException{
        GraphHopperWeb gh = createClient();

        GHRequest req= new GHRequest()
                .addPoint(new GHPoint(45.0, -73.0))
                .addPoint(new GHPoint(45.1, -73.1))
                .setProfile("car");

        String json = "{\"paths\":[],\"hints\":{\"abcd\":\"val-42\"}}";
        Response fakResponse = new Response.Builder()
                .request(new Request.Builder().url("https://localhost:8080/route").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(json, MediaType.get("application/json")))
                .addHeader("X-Rate-Limit-Remaining", "123")
                .build();

        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(fakResponse);

        GHResponse rsp = gh.route(req);
        assertFalse(rsp.hasErrors(), "Response should not have errors");

        var hintsMap= rsp.getHints().toMap();

        Object abcdHint = hintsMap.get("abcd");
        assertNotNull(abcdHint, "header should be copied into hints");
        assertEquals("val-42",abcdHint, "JSON response hints merge into GHResponse hints");
    }
}
