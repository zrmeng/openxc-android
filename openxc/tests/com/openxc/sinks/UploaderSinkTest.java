package com.openxc.sinks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.openxc.TestUtils;
import com.openxc.messages.SimpleVehicleMessage;
import com.openxc.messages.VehicleMessage;

@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class UploaderSinkTest {
    UploaderSink sink;
    VehicleMessage message = new SimpleVehicleMessage("foo", "bar");
    Gson gson;

    @Before
    public void setUp() throws IOException, DataSinkException {
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        Robolectric.setDefaultHttpResponse(200, "dummy");
        sink = new UploaderSink(Robolectric.application, "http://localhost");
        gson = new Gson();
    }

    @After
    public void tearDown() throws IOException, DataSinkException {
        sink.stop();
    }

    @Test
    public void testNothingUploadedYet() throws DataSinkException {
        assertFalse(Robolectric.httpRequestWasMade());
    }

    @Test
    public void testUploadBatch() throws DataSinkException, IOException {
        for(int i = 0; i < 25; i++) {
            sink.receive(message);
        }
        TestUtils.pause(1000);
        assertTrue(Robolectric.httpRequestWasMade());

        Type listType = new TypeToken<List<SimpleVehicleMessage>>() {}.getType();
        ArrayList<SimpleVehicleMessage> messages = new ArrayList<>();
        HttpPost request;
        while((request = (HttpPost) Robolectric.getNextSentHttpRequest()) != null) {
            InputStream payload = request.getEntity().getContent();
            int length = payload.available();
            byte[] buffer = new byte[length];
            payload.read(buffer);

            messages.addAll((List<SimpleVehicleMessage>)gson.fromJson(new String(buffer), listType));
        }
        assertThat(messages, hasSize(25));
        for(SimpleVehicleMessage deserializedMessage : messages) {
            assertThat(message, equalTo((VehicleMessage) deserializedMessage));
        }
    }
}
