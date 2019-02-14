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

import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.regex.Pattern;
import org.eclipse.che.api.devfile.server.DevfileFormatException;
import org.eclipse.che.api.devfile.server.schema.DevfileSchemaProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

public class DevfileSchemaValidatorTest {

  private DevfileSchemaValidator schemaValidator;

  @BeforeClass
  public void setUp() throws Exception {
    schemaValidator = new DevfileSchemaValidator(new DevfileSchemaProvider());
  }

  @Test(dataProvider = "validDevfiles")
  public void shouldDoNotThrowExceptionOnValidationValidDevfile(String resourceFilePath)
      throws Exception {
    schemaValidator.validateBySchema(getResource(resourceFilePath));
  }

  @DataProvider
  public Object[][] validDevfiles() {
    return new Object[][] {
      {"editor_plugin_tool/devfile_editor_plugins.yaml"},
      {"kubernetes_openshift_tool/devfile_openshift_tool.yaml"},
      {"dockerimage_tool/devfile_dockerimage_tool.yaml"}
    };
  }

  @Test(dataProvider = "invalidDevfiles")
  public void shouldThrowExceptionOnValidationNonValidDevfile(
      String resourceFilePath, String expectedMessageRegexp) throws Exception {
    try {
      schemaValidator.validateBySchema(getResource(resourceFilePath));
    } catch (DevfileFormatException e) {
      if (!Pattern.matches(expectedMessageRegexp, e.getMessage())) {
        fail("DevfileFormatException with unexpected message is thrown: " + e.getMessage());
      }
      return;
    }
    fail("DevfileFormatException expected to be thrown but is was not");
  }

  @DataProvider
  public Object[][] invalidDevfiles() {
    return new Object[][] {
      // Devfile model testing
      {
        "devfile/devfile_missing_name.yaml",
        "Devfile schema validation failed. Root error: \\[#: 5 schema violations found\\].Nested errors: \\[#: required key \\[name\\] not found,#/tools/0: required key \\[local\\] not found,#/tools/0: required key \\[image\\] not found,#/tools/0: required key \\[memoryLimit\\] not found,#/tools/0: required key \\[type\\] not found\\]$"
      },
      {
        "devfile/devfile_missing_spec_version.yaml",
        "Devfile schema validation failed. Root error: \\[#: 5 schema violations found\\].Nested errors: \\[#: required key \\[specVersion\\] not found,#/tools/0: required key \\[local\\] not found,#/tools/0: required key \\[image\\] not found,#/tools/0: required key \\[memoryLimit\\] not found,#/tools/0: required key \\[type\\] not found\\]$"
      },
      {
        "devfile/devfile_with_undeclared_field.yaml",
        "Devfile schema validation failed. Root error: \\[#: extraneous key \\[unknown\\] is not permitted\\].$"
      },
      // Tool model testing
      {
        "tool/devfile_missing_tool_name.yaml",
        "Devfile schema validation failed. Root error: \\[#/tools/0: #: only 1 subschema matches out of 2\\].Nested errors: \\[#/tools/0: required key \\[name\\] not found\\]$"
      },
      {
        "tool/devfile_missing_tool_type.yaml",
        "Devfile schema validation failed. Root error: \\[#/tools/0: #: only 0 subschema matches out of 2\\].Nested errors: \\[#/tools/0: required key \\[local\\] not found,#/tools/0: required key \\[image\\] not found,#/tools/0: required key \\[memoryLimit\\] not found,#/tools/0: required key \\[type\\] not found]$"
      },
      {
        "tool/devfile_tool_with_undeclared_field.yaml",
        "Devfile schema validation failed. Root error: \\[#/tools/0: #: only 1 subschema matches out of 2\\].Nested errors: \\[#/tools/0: extraneous key \\[unknown\\] is not permitted\\]$"
      },
      // Command model testing
      {
        "command/devfile_missing_command_name.yaml",
        "Devfile schema validation failed. Root error: \\[#/commands/0: required key \\[name\\] not found\\].$"
      },
      {
        "command/devfile_missing_command_actions.yaml",
        "Devfile schema validation failed. Root error: \\[#/commands/0: required key \\[actions\\] not found\\].$"
      },
      {
        "command/devfile_multiple_commands_actions.yaml",
        "Devfile schema validation failed. Root error: \\[#/commands/0/actions: expected maximum item count: 1, found: 2\\].$"
      },
      // cheEditor/chePlugin tool model testing
      {
        "editor_plugin_tool/devfile_editor_tool_with_missing_id.yaml",
        "Devfile schema validation failed. Root error: \\[#/tools/0: #: only 1 subschema matches out of 2\\].Nested errors: \\[#/tools/0: required key \\[id\\] not found\\]$"
      },
      // kubernetes/openshift tool model testing
      {
        "kubernetes_openshift_tool/devfile_openshift_tool_with_missing_local.yaml",
        "Devfile schema validation failed. Root error: \\[#/tools/0: #: only 1 subschema matches out of 2\\].Nested errors: \\[#/tools/0: required key \\[local\\] not found\\]$"
      },
      // Dockerimage tool model testing
      {
        "dockerimage_tool/devfile_dockerimage_tool_with_missing_image.yaml",
        "Devfile schema validation failed. Root error: \\[#/tools/0: #: only 1 subschema matches out of 2\\].Nested errors: \\[#/tools/0: required key \\[image\\] not found\\]$"
      },
      {
        "dockerimage_tool/devfile_dockerimage_tool_with_missing_memory_limit.yaml",
        "Devfile schema validation failed. Root error: \\[#/tools/0: #: only 1 subschema matches out of 2\\].Nested errors: \\[#/tools/0: required key \\[memoryLimit\\] not found\\]$"
      },
    };
  }

  private String getResource(String name) throws IOException {
    return Files.readFile(getClass().getClassLoader().getResourceAsStream("schema_test/" + name));
  }
}
