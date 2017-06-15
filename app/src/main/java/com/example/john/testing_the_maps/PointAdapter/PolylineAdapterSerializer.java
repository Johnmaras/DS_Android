package com.example.john.testing_the_maps.PointAdapter;

import com.google.gson.*;

import java.lang.reflect.Type;

public class PolylineAdapterSerializer implements JsonSerializer{

    @Override
    public JsonElement serialize(Object o, Type type, JsonSerializationContext context){
        JsonObject jsonObject = new JsonObject();

        JsonElement origin = context.serialize(((PolylineAdapter)o).getOrigin());
        JsonElement destination = context.serialize(((PolylineAdapter)o).getDestination());

        jsonObject.add("origin", origin);
        jsonObject.add("destination", destination);

        JsonElement points = context.serialize(((PolylineAdapter)o).getPoints());

        jsonObject.add("points", points);

        return jsonObject;

    }
}
