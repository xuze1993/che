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
package org.eclipse.che.selenium.dashboard;

import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.selenium.pageobject.dashboard.factories.CreateFactoryPage.TabNames.CONFIG_TAB_ID;
import static org.eclipse.che.selenium.pageobject.dashboard.factories.CreateFactoryPage.TabNames.GIT_TAB_ID;
import static org.eclipse.che.selenium.pageobject.dashboard.factories.CreateFactoryPage.TabNames.TEMPLATE_TAB_ID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.eclipse.che.selenium.core.client.TestFactoryServiceClient;
import org.eclipse.che.selenium.core.client.TestGitHubRepository;
import org.eclipse.che.selenium.core.client.TestWorkspaceServiceClient;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.workspace.InjectTestWorkspace;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.DashboardFactories;
import org.eclipse.che.selenium.pageobject.dashboard.factories.CreateFactoryPage;
import org.eclipse.che.selenium.pageobject.dashboard.factories.FactoryDetails;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CreateFactoryTest {
  private static final String MINIMAL_TEMPLATE_FACTORY_NAME = generate("factoryMin", 4);
  private static final String COMPLETE_TEMPLATE_FACTORY_NAME = generate("factoryComplete", 4);
  private static final String FACTORY_CREATED_FROM_WORKSPACE_NAME = generate("factoryWs", 4);
  private static final String FACTORY_CREATED_FROM_GIT_NAME = generate("factoryGit", 4);
  private static final String FACTORY_CREATED_FROM_CONFIG_NAME = generate("factoryConfig", 4);
  private static final String FACTORY_CREATED_FROM_USER_JSON_NAME = generate("factoryUser", 4);
  private static final String MIN_FACTORY_NAME = generate("", 3);
  private static final String MAX_FACTORY_NAME = generate("", 20);
  private static final String RESOURCES_CONFIG_FILE =
      "/org/eclipse/che/selenium/dashboard/config-ws.json";
  private static final String WS_HAS_NO_PROJECT_ERROR_MESSAGE =
      "Factory can't be created. The selected workspace has no projects defined. Project sources must be available from an external storage.";
  private static final String TOO_SHORT_NAME_MESAAGE =
      "The name has to be more than 3 characters long.";
  private static final String TOO_LONG_NAME_MESSAGE =
      "The name has to be less than 20 characters long.";
  private static final String SPECIAL_SYMBOLS_NAME = "***";
  private static final String SPECIAL_SYMBOLS_ERROR_MESSAGE =
      "Factory name may contain digits, latin letters, spaces, _ , . , - and should start only with digits, latin letters or underscores";
  private static final String EXIST_NAME_ERROR_MESSAGE = "This factory name is already used.";
  private static final String LOAD_FILE_CONFIGURATION_MESSAGE =
      "Successfully loaded file's configuration config-ws.json.";

  private String workspaceName1;
  private String workspaceName2;

  @Inject private TestWorkspaceServiceClient workspaceServiceClient;
  @Inject private TestFactoryServiceClient factoryServiceClient;
  @Inject private DashboardFactories dashboardFactories;
  @Inject private FactoryDetails factoryDetails;
  @Inject private DefaultTestUser defaultTestUser;
  @Inject private TestGitHubRepository testRepo;
  @Inject private CreateFactoryPage createFactoryPage;
  @Inject private Dashboard dashboard;

  // it is used to read workspace logs on test failure
  @InjectTestWorkspace(startAfterCreation = false)
  private TestWorkspace testWorkspace1;

  @InjectTestWorkspace(startAfterCreation = false)
  private TestWorkspace testWorkspace2;

  public CreateFactoryTest() {}

  @BeforeClass
  public void setUp() throws Exception {
    workspaceName1 = testWorkspace1.getName();
    workspaceName2 = testWorkspace2.getName();

    dashboard.open();
  }

  @AfterClass
  public void tearDown() throws Exception {
    workspaceServiceClient.delete(testWorkspace1.getName(), defaultTestUser.getName());
    workspaceServiceClient.delete(testWorkspace1.getName(), defaultTestUser.getName());

    List<String> factoryList =
        Arrays.asList(
            MINIMAL_TEMPLATE_FACTORY_NAME,
            COMPLETE_TEMPLATE_FACTORY_NAME,
            FACTORY_CREATED_FROM_WORKSPACE_NAME,
            FACTORY_CREATED_FROM_CONFIG_NAME,
            FACTORY_CREATED_FROM_GIT_NAME,
            FACTORY_CREATED_FROM_USER_JSON_NAME);

    for (String factory : factoryList) {
      factoryServiceClient.deleteFactory(factory);
    }
  }

  @BeforeMethod
  private void openNewFactoryPage() {
    // open the New Factory page before starting each test method
    dashboardFactories.selectFactoriesOnNavBar();
    dashboardFactories.waitAllFactoriesPage();
    dashboardFactories.clickOnAddFactoryBtn();
    createFactoryPage.waitToolbarTitle();
  }

  @Test
  public void checkCreateFactoryFromGitTab() throws IOException {
    // create the test repository and get url
    Path entryPath =
        Paths.get(getClass().getResource("/projects/default-spring-project").getPath());
    testRepo.addContent(entryPath);

    String repositoryUrl = testRepo.getHttpsTransportUrl();

    // open the  'Git' tab and fill the fields url and name
    createFactoryPage.clickOnSourceTab(GIT_TAB_ID);
    createFactoryPage.typeGitRepositoryUrl(repositoryUrl);
    createFactoryPage.typeFactoryName(FACTORY_CREATED_FROM_GIT_NAME);

    assertTrue(createFactoryPage.isCreateFactoryButtonEnabled());

    // create factory
    createFactoryPage.clickOnCreateFactoryButton();
    factoryDetails.waitFactoryName(FACTORY_CREATED_FROM_GIT_NAME);

    // check present the id url and named url of the factory
    dashboardFactories.waitFactoryIdUrl();
    dashboardFactories.waitFactoryNamedUrl(FACTORY_CREATED_FROM_GIT_NAME);
  }

  @Test
  public void checkCreateFactoryFromConfigTab() throws IOException, URISyntaxException {
    URL resourcesUploadFile = getClass().getResource(RESOURCES_CONFIG_FILE);

    // select the 'Config' tab
    createFactoryPage.clickOnSourceTab(CONFIG_TAB_ID);
    createFactoryPage.typeFactoryName(FACTORY_CREATED_FROM_CONFIG_NAME);

    assertTrue(createFactoryPage.isUploadFileButtonEnabled());

    assertFalse(createFactoryPage.isCreateFactoryButtonEnabled());

    // upload the configuration file from resources
    createFactoryPage.uploadSelectedConfigFile(Paths.get(resourcesUploadFile.toURI()));

    dashboard.waitNotificationMessage(LOAD_FILE_CONFIGURATION_MESSAGE);
    dashboard.waitNotificationIsClosed();

    assertTrue(createFactoryPage.isCreateFactoryButtonEnabled());

    // create factory
    createFactoryPage.clickOnCreateFactoryButton();
    factoryDetails.waitFactoryName(FACTORY_CREATED_FROM_CONFIG_NAME);

    // check present the id url and named url of the factory
    dashboardFactories.waitFactoryNamedUrl(FACTORY_CREATED_FROM_CONFIG_NAME);
    dashboardFactories.waitFactoryIdUrl();
  }

  @Test
  public void checkHandlingOfFactoryNames() {
    // create a factory from template
    createFactoryPage.waitToolbarTitle();
    createFactoryPage.clickOnSourceTab(TEMPLATE_TAB_ID);
    createFactoryPage.waitTemplateButtons();
    createFactoryPage.clickOnMinimalTemplateButton();

    // enter empty factory name
    createFactoryPage.typeFactoryName("");
    createFactoryPage.waitErrorMessageNotVisible();
    assertTrue(createFactoryPage.isCreateFactoryButtonEnabled());

    // enter factory name with a less than 3 symbols
    createFactoryPage.typeFactoryName(generate("", 2));
    createFactoryPage.waitErrorMessage(TOO_SHORT_NAME_MESAAGE);
    assertFalse(createFactoryPage.isCreateFactoryButtonEnabled());

    // enter factory name with exactly 3 symbols
    createFactoryPage.typeFactoryName(MIN_FACTORY_NAME);
    createFactoryPage.waitErrorMessageNotVisible();
    assertTrue(createFactoryPage.isCreateFactoryButtonEnabled());

    // enter factory name with special symbols
    createFactoryPage.typeFactoryName(SPECIAL_SYMBOLS_NAME);
    createFactoryPage.waitErrorMessage(SPECIAL_SYMBOLS_ERROR_MESSAGE);
    assertFalse(createFactoryPage.isCreateFactoryButtonEnabled());

    // enter factory name with more than 20 symbols
    createFactoryPage.typeFactoryName(generate("", 21));
    createFactoryPage.waitErrorMessage(TOO_LONG_NAME_MESSAGE);
    assertFalse(createFactoryPage.isCreateFactoryButtonEnabled());

    // enter factory name with exactly 20 symbols
    createFactoryPage.typeFactoryName(MAX_FACTORY_NAME);
    createFactoryPage.waitErrorMessageNotVisible();
    assertTrue(createFactoryPage.isCreateFactoryButtonEnabled());

    // enter an already existing factory name
    createFactoryPage.typeFactoryName(FACTORY_CREATED_FROM_CONFIG_NAME);
    createFactoryPage.waitErrorMessage(EXIST_NAME_ERROR_MESSAGE);

    assertFalse(createFactoryPage.isCreateFactoryButtonEnabled());
  }

  @Test
  public void shouldCreateFactoryFromTemplate() {
    // select the 'Template' tab
    createFactoryPage.waitToolbarTitle();
    createFactoryPage.clickOnSourceTab(TEMPLATE_TAB_ID);
    createFactoryPage.typeFactoryName(MINIMAL_TEMPLATE_FACTORY_NAME);
    createFactoryPage.waitTemplateButtons();
    createFactoryPage.clickOnMinimalTemplateButton();

    // create a factory from minimal template
    createFactoryPage.clickOnCreateFactoryButton();
    factoryDetails.waitFactoryName(MINIMAL_TEMPLATE_FACTORY_NAME);

    // check present the factory url
    dashboardFactories.waitFactoryNamedUrl(MINIMAL_TEMPLATE_FACTORY_NAME);
    dashboardFactories.waitFactoryIdUrl();

    // go to the factory list
    factoryDetails.clickOnBackToFactoriesListButton();
    dashboardFactories.waitAllFactoriesPage();
    dashboardFactories.waitFactoryName(MINIMAL_TEMPLATE_FACTORY_NAME);

    assertEquals(dashboardFactories.getFactoryRamLimit(MINIMAL_TEMPLATE_FACTORY_NAME), "2048 MB");

    // go to the 'Template' tab
    dashboardFactories.waitAllFactoriesPage();
    dashboardFactories.clickOnAddFactoryBtn();
    createFactoryPage.waitToolbarTitle();
    createFactoryPage.typeFactoryName(COMPLETE_TEMPLATE_FACTORY_NAME);
    createFactoryPage.clickOnSourceTab(TEMPLATE_TAB_ID);

    // create a factory from complete template
    createFactoryPage.waitTemplateButtons();
    createFactoryPage.clickOnCompleteTemplateButton();
    createFactoryPage.clickOnCreateFactoryButton();
    factoryDetails.waitFactoryName(COMPLETE_TEMPLATE_FACTORY_NAME);

    // check present the factory url
    dashboardFactories.waitFactoryNamedUrl(COMPLETE_TEMPLATE_FACTORY_NAME);
    dashboardFactories.waitFactoryIdUrl();

    // go to the factory list
    factoryDetails.clickOnBackToFactoriesListButton();

    dashboardFactories.waitAllFactoriesPage();
    dashboardFactories.waitFactoryName(COMPLETE_TEMPLATE_FACTORY_NAME);
    assertEquals(dashboardFactories.getFactoryRamLimit(COMPLETE_TEMPLATE_FACTORY_NAME), "2048 MB");
  }

  @Test
  public void checkEditorOfTemplateJson() throws Exception {
    // select the minimal template
    createFactoryPage.waitToolbarTitle();
    createFactoryPage.clickOnSourceTab(TEMPLATE_TAB_ID);
    createFactoryPage.typeFactoryName(FACTORY_CREATED_FROM_USER_JSON_NAME);
    createFactoryPage.waitTemplateButtons();
    createFactoryPage.clickOnMinimalTemplateButton();

    // delete content of the template
    createFactoryPage.setFocusInEditorTemplate();
    createFactoryPage.deleteAllContentFromTemplateEditor();

    assertFalse(createFactoryPage.isCreateFactoryButtonEnabled());

    // type the user json workspace
    createFactoryPage.typeConfigFileToTemplateEditor(RESOURCES_CONFIG_FILE);

    assertTrue(createFactoryPage.isCreateFactoryButtonEnabled());

    createFactoryPage.clickOnCreateFactoryButton();
    factoryDetails.waitFactoryName(FACTORY_CREATED_FROM_USER_JSON_NAME);

    // check present the factory url
    dashboardFactories.waitFactoryNamedUrl(FACTORY_CREATED_FROM_USER_JSON_NAME);
    dashboardFactories.waitFactoryIdUrl();
  }

  @Test
  public void checkWorkspaceFiltering() {
    // click on the search button and wait search field visible
    createFactoryPage.clickOnSearchFactoryButton();
    createFactoryPage.waitSearchFactoryField();

    // filter by full workspace name
    createFactoryPage.typeTextToSearchFactoryField(workspaceName1);
    createFactoryPage.waitWorkspaceNameInList(workspaceName1);

    // filter by a part of workspace name
    createFactoryPage.typeTextToSearchFactoryField(
        workspaceName1.substring(workspaceName1.length() / 2));
    createFactoryPage.waitWorkspaceNameInList(workspaceName1);

    // filter by a nonexistent workspace name
    createFactoryPage.typeTextToSearchFactoryField(generate("", 8));
    createFactoryPage.waitWorkspacesListIsEmpty();
  }
}
