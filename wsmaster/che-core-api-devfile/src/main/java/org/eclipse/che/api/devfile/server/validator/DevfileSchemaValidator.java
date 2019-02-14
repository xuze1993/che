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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.devfile.server.DevfileFormatException;
import org.eclipse.che.api.devfile.server.schema.DevfileSchemaProvider;
import org.everit.json.schema.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

/**
 * Validates YAML devfile content against given JSON schema.
 */
@Singleton
public class DevfileSchemaValidator {

  private Yaml yaml = new Yaml();
  private DevfileSchemaProvider schemaProvider;

  @Inject
  public DevfileSchemaValidator(DevfileSchemaProvider schemaProvider) {
    this.schemaProvider = schemaProvider;
  }

  public JSONObject validateBySchema(String yamlContent)
      throws DevfileFormatException {
    JSONObject data;
    try {
      Object obj = yaml.load(yamlContent);
      data = (JSONObject) wrapJsonObject(obj);
      schemaProvider.getSchema().validate(data);
    } catch (IOException e) {
      throw new DevfileFormatException("Unable to validate Devfile. Error: " + e.getMessage());
    } catch (ValidationException ve) {
      throw new DevfileFormatException(prepareErrorMessage(ve));
    }
    return data;
  }

  private String prepareErrorMessage(ValidationException e) {
    StringBuilder sb = new StringBuilder(
        "Devfile schema validation failed. Root error: [");
    sb.append(e.getMessage());
    sb.append("].");
    if (!e.getCausingExceptions().isEmpty()) {
      sb.append("Nested errors: ");
      List<String> messages = new ArrayList<>();
      flatternExceptions(e, messages);
      String msg = messages.stream().collect(Collectors.joining(",","[", "]"));
      sb.append(msg);
    }
    return sb.toString();
  }

  private void flatternExceptions(ValidationException exception, List<String> messages) {
    if (!exception.getCausingExceptions().isEmpty()) {
      for (ValidationException ex : exception.getCausingExceptions()) {
        flatternExceptions(ex, messages);
      }
    } else {
      messages.add(exception.getMessage());
    }

  }

  private Object wrapJsonObject(Object o) {

    //NULL => JSONObject.NULL
    if (o == null) {
      return JSONObject.NULL;
    }

    // Collection => JSONArray
    if (o instanceof Collection) {
      JSONArray array = new JSONArray();
      for (Object childObj : (Collection<?>) o) {
        array.put(wrapJsonObject(childObj));
      }
      return array;
    }

    // Array => JSONArray
    if (o.getClass().isArray()) {
      JSONArray array = new JSONArray();

      int length = Array.getLength(array);
      for (int i = 0; i < length; i++) {
        array.put(wrapJsonObject(Array.get(array, i)));
      }

      return array;
    }

    // Map => JSONObject
    if (o instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) o;

      JSONObject jsonObject = new JSONObject();
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        final String name = String.valueOf(entry.getKey());
        final Object value = entry.getValue();
        jsonObject.put(name, wrapJsonObject(value));
      }

      return jsonObject;
    }

    // everything else
    return o;
  }

}
