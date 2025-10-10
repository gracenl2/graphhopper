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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.Map;

import static com.graphhopper.json.Statement.If;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This class unit tests the class. For integration tests against a real server
 * see RouteResourceClientHCTest.
 */
public class GraphHopperWebTest {

        @ParameterizedTest(name = "POST={0}")
        @ValueSource(booleans = { true, false })
        public void testGetClientForRequest(boolean usePost) {
                GraphHopperWeb gh = new GraphHopperWeb(null).setPostRequest(usePost);
                GHRequest req = new GHRequest(new GHPoint(42.509225, 1.534728), new GHPoint(42.512602, 1.551558))
                                .setProfile("car");
                req.putHint(GraphHopperWeb.TIMEOUT, 5);

                assertEquals(5, gh.getClientForRequest(req).connectTimeoutMillis());
        }

        @Test
        public void profileIncludedAsGiven() {
                GraphHopperWeb hopper = new GraphHopperWeb("https://localhost:8000/route");
                // no vehicle -> no vehicle
                assertEquals("https://localhost:8000/route?profile=&type=json&instructions=true&points_encoded=true&points_encoded_multiplier=1000000"
                                +
                                "&calc_points=true&algorithm=&locale=en_US&elevation=false&optimize=false",
                                hopper.createGetRequest(new GHRequest()).url().toString());

                // vehicle given -> vehicle used in url
                assertEquals("https://localhost:8000/route?profile=my_car&type=json&instructions=true&points_encoded=true&points_encoded_multiplier=1000000"
                                +
                                "&calc_points=true&algorithm=&locale=en_US&elevation=false&optimize=false",
                                hopper.createGetRequest(new GHRequest().setProfile("my_car")).url().toString());
        }

        @Test
        public void headings() {
                GraphHopperWeb hopper = new GraphHopperWeb("http://localhost:8080/route");
                GHRequest req = new GHRequest(new GHPoint(42.509225, 1.534728), new GHPoint(42.512602, 1.551558))
                                .setHeadings(Arrays.asList(10.0, 90.0)).setProfile("car");
                assertEquals("http://localhost:8080/route?profile=car&point=42.509225,1.534728&point=42.512602,1.551558&type=json&instructions=true&points_encoded=true&points_encoded_multiplier=1000000"
                                +
                                "&calc_points=true&algorithm=&locale=en_US&elevation=false&optimize=false&heading=10.0&heading=90.0",
                                hopper.createGetRequest(req).url().toString());
        }

        @Test
        public void customModel() throws JsonProcessingException {
                GraphHopperWeb client = new GraphHopperWeb("http://localhost:8080/route");
                JsonFeatureCollection areas = new JsonFeatureCollection();
                Coordinate[] area_1_coordinates = new Coordinate[] {
                                new Coordinate(48.019324184801185, 11.28021240234375),
                                new Coordinate(48.019324184801185, 11.53564453125),
                                new Coordinate(48.11843396091691, 11.53564453125),
                                new Coordinate(48.11843396091691, 11.28021240234375),
                                new Coordinate(48.019324184801185, 11.28021240234375),
                };
                Coordinate[] area_2_coordinates = new Coordinate[] {
                                new Coordinate(48.15509285476017, 11.53289794921875),
                                new Coordinate(48.15509285476017, 11.8212890625),
                                new Coordinate(48.281365151571755, 11.8212890625),
                                new Coordinate(48.281365151571755, 11.53289794921875),
                                new Coordinate(48.15509285476017, 11.53289794921875),
                };
                areas.getFeatures().add(new JsonFeature("area_1",
                                "Feature",
                                null,
                                new GeometryFactory().createPolygon(area_1_coordinates),
                                new HashMap<>()));
                areas.getFeatures().add(new JsonFeature("area_2",
                                "Feature",
                                null,
                                new GeometryFactory().createPolygon(area_2_coordinates),
                                new HashMap<>()));
                CustomModel customModel = new CustomModel()
                                .addToSpeed(If("road_class == MOTORWAY", Statement.Op.LIMIT, "80"))
                                .addToPriority(If("surface == DIRT", Statement.Op.MULTIPLY, "0.7"))
                                .addToPriority(If("surface == SAND", Statement.Op.MULTIPLY, "0.6"))
                                .setDistanceInfluence(69d)
                                .setHeadingPenalty(22)
                                .setAreas(areas);
                GHRequest req = new GHRequest(new GHPoint(42.509225, 1.534728), new GHPoint(42.512602, 1.551558))
                                .setCustomModel(customModel)
                                .setProfile("car");

                IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                () -> client.createGetRequest(req));
                assertEquals("Custom models cannot be used for GET requests. Use setPostRequest(true)", e.getMessage());

                ObjectNode postRequest = client.requestToJson(req);
                JsonNode customModelJson = postRequest.get("custom_model");
                ObjectMapper objectMapper = Jackson.newObjectMapper();
                JsonNode expected = objectMapper
                                .readTree("{\"distance_influence\":69.0,\"heading_penalty\":22.0,\"areas\":{" +
                                                "\"type\":\"FeatureCollection\",\"features\":[" +
                                                "{\"id\":\"area_1\",\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[48.019324184801185,11.28021240234375],[48.019324184801185,11.53564453125],[48.11843396091691,11.53564453125],[48.11843396091691,11.28021240234375],[48.019324184801185,11.28021240234375]]]},\"properties\":{}},"
                                                +
                                                "{\"id\":\"area_2\",\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[48.15509285476017,11.53289794921875],[48.15509285476017,11.8212890625],[48.281365151571755,11.8212890625],[48.281365151571755,11.53289794921875],[48.15509285476017,11.53289794921875]]]},\"properties\":{}}]},"
                                                +
                                                "\"priority\":[{\"if\":\"surface == DIRT\",\"multiply_by\":\"0.7\"},{\"if\":\"surface == SAND\",\"multiply_by\":\"0.6\"}],"
                                                +
                                                "\"speed\":[{\"if\":\"road_class == MOTORWAY\",\"limit_to\":\"80\"}]}");
                assertEquals(expected, objectMapper.valueToTree(customModelJson));

                CustomModel cm = objectMapper.readValue("{\"distance_influence\":null}", CustomModel.class);
                assertNull(cm.getDistanceInfluence());
        }

        @Test
        public void testSetKeyValidation() {
                GraphHopperWeb gh = new GraphHopperWeb();

                // 1. null key should throw NullPointerException
                NullPointerException npe = assertThrows(NullPointerException.class, () -> gh.setKey(null));
                assertEquals("Key must not be null", npe.getMessage());

                // 2. empty key should throw IllegalArgumentException
                IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> gh.setKey(""));
                assertEquals("Key must not be empty", iae.getMessage());

                // 3. valid key should work and be returned in the chain
                GraphHopperWeb returned = gh.setKey("my-api-key");
                assertSame(gh, returned, "setKey should return this for chaining");
        }

        @Test
        void route_error_json_triggers_hasErrors_and_removes_turn_description() {
                Faker faker = new Faker(new Random(7)); // deterministic fake strings

                final StringBuilder seenBody = new StringBuilder(); // capture the outbound POST body for inspection

                Interceptor fake = chain -> { // OkHttp interceptor to short-circuit the network
                        Request req = chain.request(); // the request built by route(...) (POST in this test)
                        if (req.body() != null) { // read the body graphhopper sends
                                var buf = new okio.Buffer();
                                req.body().writeTo(buf);
                                seenBody.append(buf.readString(StandardCharsets.UTF_8)); // stash for assertions
                        }
                        String body = "{\"message\":\"" // craft an *error-shaped* JSON payload
                                        + faker.lorem().word() + "\",\"hints\":[]}"; // so readErrors(...) returns
                                                                                     // something
                        return new Response.Builder()
                                        .request(req) // tie response to the incoming request
                                        .protocol(Protocol.HTTP_1_1)
                                        .code(400) // non-2xx is fine; we only care about message/hints JSON
                                        .message("Bad Request")
                                        .body(ResponseBody.create(body, // body the route(...) will parse
                                                        MediaType.get("application/json")))
                                        .build();
                };

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(fake) // install the fake responder
                                .build();

                GraphHopperWeb gh = new GraphHopperWeb("https://unused/route")
                                .setPostRequest(true) // force the POST code path in route(...)
                                .setKey(faker.internet().password(8, 12)) // any non-empty key to pass validation
                                .setDownloader(client); // use our intercepted OkHttp client

                GHRequest req = new GHRequest()
                                .addPoint(new GHPoint(45.5, -73.6)) // minimal 2-point route
                                .addPoint(new GHPoint(45.4, -73.7))
                                .setProfile("car");
                req.getHints().putObject("turn_description", true); // this hint is *read then removed* inside
                                                                    // route(...)

                GHResponse resp = gh.route(req); // === executes route(...) ===

                assertTrue(resp.hasErrors(), // error JSON => readErrors(...) populated => early return
                                "error JSON should produce hasErrors() == true");

                assertFalse(seenBody.toString().contains("turn_description"),
                                "turn_description must be removed from the POST body"); // proves route(...) removed it
                                                                                        // before building body
        }

        @Test
        void route_success_no_errors_copies_headers_and_hints_with_empty_paths() {
                Faker faker = new Faker(new Random(13)); // deterministic

                Interceptor fake = chain -> { // fake a successful server answer
                        Request req = chain.request();
                        String body = "{\"paths\":[],\"hints\":{\"" // no 'message' => no errors
                                        + faker.letterify("abcd") + "\":\"" // include one JSON hint to be merged
                                        + faker.bothify("val-##") + "\"}}";
                        return new Response.Builder()
                                        .request(req)
                                        .protocol(Protocol.HTTP_1_1)
                                        .code(200).message("OK")
                                        .addHeader("X-Rate-Limit-Remaining", "123") // headers will be copied into
                                                                                    // res.hints as List<String>
                                        .addHeader("X-Trace", faker.bothify("trace-####"))
                                        .body(ResponseBody.create(body, MediaType.get("application/json")))
                                        .build();
                };

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(fake) // use fake response
                                .build();

                GraphHopperWeb gh = new GraphHopperWeb("https://unused/route")
                                .setPostRequest(true) // POST path again
                                .setKey("k")
                                .setDownloader(client);

                GHRequest req = new GHRequest()
                                .addPoint(new GHPoint(45.0, -73.0)) // minimal valid request
                                .addPoint(new GHPoint(45.1, -73.1))
                                .setProfile("car");

                GHResponse resp = gh.route(req); // === executes route(...) ===

                assertFalse(resp.hasErrors(), "no 'message' -> no errors");

                Map<String, Object> hints = resp.getHints().toMap();

                // find the header regardless of case
                Object rateObj = null;
                for (String k : hints.keySet()) {
                        if ("X-Rate-Limit-Remaining".equalsIgnoreCase(k)) {
                                rateObj = hints.get(k);
                                break;
                        }
                }

                assertNotNull(rateObj, "X-Rate-Limit-Remaining header should be copied into hints");
                assertTrue(rateObj instanceof List, "header should be stored as a List");
                List<?> rateList = (List<?>) rateObj;
                assertEquals(Collections.singletonList("123"), rateList);

                assertTrue(hints.containsKey("abcd"), "JSON 'hints' should be merged into resp.hints");
        }

}
