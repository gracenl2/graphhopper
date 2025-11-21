package com.graphhopper.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graphhopper.util.PMap;

/* Tests mockito sur GHMatrixAbstractRequester et MatrixResponse */

@ExtendWith(MockitoExtension.class)
class GraphHopperMatrixWebMockTest {

    @Mock
    GHMatrixAbstractRequester requester;

    @Mock
    GHMRequest request;

    @Mock
    PMap hints;

    /* Tests sur les setKey */
    @Test
    void setKey_null_ThrowsIllegalSTateException(){
        GraphHopperMatrixWeb client = new GraphHopperMatrixWeb(requester);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> client.setKey(null),
            "Expected setKey(null) to throw, but it didn't"
        );
        assertEquals("Key cannot be empty", ex.getMessage());
    }

    @Test
    void setKey_empty_ThrowsIllegalStateException(){
        GraphHopperMatrixWeb client = new GraphHopperMatrixWeb(requester);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> client.setKey(""),
            "Expected setKey(\"\") to throw, but it didn't"
        );
        assertEquals("Key cannot be empty", ex.getMessage());
    }

    /* route() avec clé */
    @Test
    void route_withKey_addKeyHint_toRequester(){

        //client avec requester mock et une clé
        GraphHopperMatrixWeb client = new GraphHopperMatrixWeb(requester).setKey("test-key");
        
        //la requete renvoie une map de hints simulée
        when(request.getHints()).thenReturn(hints);

        //reponse simulée du backend
        MatrixResponse expected = new MatrixResponse();
        when(requester.route(request)).thenReturn(expected);

        //test appel
        MatrixResponse actual = client.route(request);

        //reponse: objet
        assertSame(expected, actual, "route response should be the same as returned by requester");

        //verifications clé est ajouté dans les hints
        verify(request).getHints();
        verify(hints).putObject(GraphHopperMatrixWeb.KEY, "test-key");
        
        //route vers requester
        verify(requester).route(request);

        //aucune autre interaction
        verifyNoMoreInteractions(request, hints, requester);
    }

    /* route() sans clé */
    @Test
    void route_withoutKey_doesNotAddKeyHint_toRequester(){
        GraphHopperMatrixWeb client = new GraphHopperMatrixWeb(requester);

        MatrixResponse expected = new MatrixResponse();
        when(requester.route(request)).thenReturn(expected);

        MatrixResponse actual = client.route(request);

        //reponse doit etre celle du requester
        assertSame(expected, actual, "route response should be the same as returned by requester");

        //verification appel requester
        verify(requester).route(request);

        //la requete ne doit pas appeler getHints car pas de clé à ajouter
        verify(request, never()).getHints();

        //aucun appel sur pmap
        verifyNoInteractions(hints);

        //pas d'autre requetes
        verifyNoMoreInteractions(requester, request);
    }

}
