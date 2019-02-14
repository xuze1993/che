/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.devfile.server.validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.devfile.server.DevfileFormatException;
import org.eclipse.che.api.devfile.server.schema.DevfileSchemaProvider;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.yaml.snakeyaml.Yaml;

/** Validates YAML devfile content against given JSON schema. */
@Singleton
public class DevfileSchemaValidator {

  private Yaml yaml = new Yaml();
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private DevfileSchemaProvider schemaProvider;

  @Inject
  public DevfileSchemaValidator(DevfileSchemaProvider schemaProvider) {
    this.schemaProvider = schemaProvider;
  }

  public JSONObject validateBySchema(String yamlContent, boolean verbose)
      throws DevfileFormatException {
    JSONObject data;
    try {
      Object obj = yaml.load(yamlContent);
      JsonElement jsonObject = wrapSnakeObject(obj);
      data = new JSONObject(new JSONTokener(gson.toJson(jsonObject)));
      schemaProvider.getSchema().validate(data);
    } catch (IOException e) {
      throw new DevfileFormatException("Unable to validate Devfile. Error: " + e.getMessage());
    } catch (ValidationException ve) {
      throw new DevfileFormatException("Devfile schema validation failed. Errors: " + ve.getMessage());
    }
    return data;
  }

  public static JsonElement wrapSnakeObject(Object o) {

    //NULL => JsonNull
    if (o == null)
      return JsonNull.INSTANCE;

    // Collection => JsonArray
    if (o instanceof Collection) {
      JsonArray array = new JsonArray();
      for (Object childObj : (Collection<?>)o)
        array.add(wrapSnakeObject(childObj));
      return array;
    }

    // Array => JsonArray
    if (o.getClass().isArray()) {
      JsonArray array = new JsonArray();

      int length = Array.getLength(array);
      for (int i=0; i<length; i++)
        array.add(wrapSnakeObject(Array.get(array, i)));

      return array;
    }

    // Map => JsonObject
    if (o instanceof Map) {
      Map<?, ?> map = (Map<?, ?>)o;

      JsonObject jsonObject = new JsonObject();
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        final String name = String.valueOf(entry.getKey());
        final Object value = entry.getValue();
        jsonObject.add(name, wrapSnakeObject(value));
      }

      return jsonObject;
    }

    // everything else => JsonPrimitive
    if (o instanceof String)
      return new JsonPrimitive((String)o);
    if (o instanceof Number)
      return new JsonPrimitive((Number)o);
    if (o instanceof Character)
      return new JsonPrimitive((Character)o);
    if (o instanceof Boolean)
      return new JsonPrimitive((Boolean)o);

    // otherwise.. string is a good guess
    return new JsonPrimitive(String.valueOf(o));
  }

}
