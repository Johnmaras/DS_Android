package com.example.john.testing_the_maps.PointAdapter;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Arrays;

public class PolylineAdapterDeserializer implements JsonDeserializer{

    @Override
    public PolylineAdapter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonContext) throws JsonParseException {
        //JsonArray array = jsonElement.getAsJsonArray();
        JsonObject object = jsonElement.getAsJsonObject();

        /*PointAdapter.LatLngAdapter origin = jsonContext.deserialize(object.get("origin"), PointAdapter.LatLngAdapter.class);
        PointAdapter.LatLngAdapter destination = jsonContext.deserialize(object.get("destination"), PointAdapter.LatLngAdapter.class);*/

        LatLngAdapter[] points = jsonContext.deserialize(object.get("points"), LatLngAdapter[].class);

        PolylineAdapter pl = new PolylineAdapter();
        pl.addAllPoint(Arrays.asList(points));

        return pl;
    }
}
