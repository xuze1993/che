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
package org.eclipse.che.selenium.dashboard.workspaces;

import static java.util.Arrays.asList;
import static org.eclipse.che.selenium.core.TestGroup.UNDER_REPAIR;
import static org.eclipse.che.selenium.pageobject.dashboard.NewWorkspace.Stack.BLANK;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.google.inject.Inject;
import java.util.List;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.client.TestWorkspaceServiceClient;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.eclipse.che.selenium.core.workspace.InjectTestWorkspace;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.DocumentationPage;
import org.eclipse.che.selenium.pageobject.dashboard.NewWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceConfig;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceOverview;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceProjects;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.Workspaces;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Sergey Skorik
 * @author Ihor Okhrimenko
 */
public class WorkspacesListTest {
  private static final int BLANK_WS_MB = 2048;
  private static final int JAVA_WS_MB = 3072;
  private static final String EXPECTED_DOCUMENTATION_PAGE_TITLE = "Eclipse Che Documentation";
  private static final String EXPECTED_JAVA_PROJECT_NAME = "web-java-spring";
  private static final String NEWEST_CREATED_WORKSPACE_NAME = "just-created-workspace";
  private static final int EXPECTED_SORTED_WORKSPACES_COUNT = 1;

  @Inject private Dashboard dashboard;
  @Inject private WorkspaceProjects workspaceProjects;
  @Inject private WorkspaceConfig workspaceConfig;
  @Inject private DefaultTestUser defaultTestUser;
  @Inject private Workspaces workspaces;
  @Inject private NewWorkspace newWorkspace;
  @Inject private TestWorkspaceServiceClient testWorkspaceServiceClient;
  @Inject private SeleniumWebDriverHelper seleniumWebDriverHelper;
  @Inject private SeleniumWebDriver seleniumWebDriver;
  @Inject private DocumentationPage documentationPage;
  @Inject private WorkspaceOverview workspaceOverview;

  @InjectTestWorkspace(memoryGb = 2, startAfterCreation = false)
  private TestWorkspace workspace1;

  @InjectTestWorkspace(memoryGb = 2, startAfterCreation = false)
  private TestWorkspace workspaceToDelete;

  @InjectTestWorkspace(memoryGb = 3, startAfterCreation = false)
  private TestWorkspace workspace2;

  private Workspaces.WorkspaceListItem expectedBlankItem;
  private Workspaces.WorkspaceListItem expectedJavaItem;
  private Workspaces.WorkspaceListItem expectedNewestWorkspaceItem;

  @BeforeClass
  public void setUp() throws Exception {
    expectedBlankItem =
        new Workspaces.WorkspaceListItem(
            defaultTestUser.getName(), workspace1.getName(), BLANK_WS_MB, 0);
    expectedJavaItem =
        new Workspaces.WorkspaceListItem(
            defaultTestUser.getName(), workspace2.getName(), JAVA_WS_MB, 0);

    expectedNewestWorkspaceItem =
        new Workspaces.WorkspaceListItem(
            defaultTestUser.getName(), NEWEST_CREATED_WORKSPACE_NAME, BLANK_WS_MB, 0);

    dashboard.open();
  }

  @BeforeMethod
  public void prepareToTestMethod() {
    dashboard.waitDashboardToolbarTitle();
    dashboard.selectWorkspacesItemOnDashboard();
  }

  @AfterClass
  public void tearDown() throws Exception {
    testWorkspaceServiceClient.delete(
        expectedNewestWorkspaceItem.getWorkspaceName(), defaultTestUser.getName());
  }

  @Test
  public void shouldDisplayElements() throws Exception {
    workspaces.waitPageLoading();
    dashboard.waitWorkspacesCountInWorkspacesItem(getWorkspacesCount());

    workspaces.waitWorkspaceIsPresent(workspace1.getName());
    workspaces.waitWorkspaceIsPresent(workspace2.getName());
  }

  @Test
  public void checkWorkspaceSelectingByCheckbox() throws Exception {
    String workspaceName1 = workspace1.getName();
    String workspaceName2 = workspace2.getName();

    workspaces.waitPageLoading();

    // select all by bulk
    workspaces.selectAllWorkspacesByBulk();
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName1);
    workspaces.waitBulkCheckboxEnabled();
    workspaces.waitDeleteWorkspaceBtn();

    // unselect all by bulk
    workspaces.selectAllWorkspacesByBulk();
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName1);
    workspaces.waitBulkCheckboxDisabled();
    workspaces.waitDeleteWorkspaceBtnDisappearance();

    // select all by bulk
    workspaces.selectAllWorkspacesByBulk();
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName1);
    workspaces.waitBulkCheckboxEnabled();
    workspaces.waitDeleteWorkspaceBtn();

    // unselect one checkbox
    workspaces.selectWorkspaceByCheckbox(workspaceName1);
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName1);
    workspaces.waitBulkCheckboxDisabled();
    workspaces.waitDeleteWorkspaceBtn();

    // unselect all checkboxes
    workspaces.selectWorkspaceByCheckbox(workspaceName2);
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName1);
    workspaces.waitBulkCheckboxDisabled();

    // for avoid of failing in the multi-thread mode when unexpected workspaces can appear in the
    // workspaces list
    workspaces.clickOnUnexpectedWorkspacesCheckboxes(asList(workspaceName1, workspaceName2));

    workspaces.waitDeleteWorkspaceBtnDisappearance();

    // select one checkbox
    workspaces.selectWorkspaceByCheckbox(workspaceName1);
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName1);
    workspaces.waitBulkCheckboxDisabled();
    workspaces.waitDeleteWorkspaceBtn();

    // select all checkboxes
    workspaces.selectWorkspaceByCheckbox(workspaceName2);
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxEnabled(workspaceName1);

    // for avoid of failing in the multi-thread mode
    workspaces.clickOnUnexpectedWorkspacesCheckboxes(asList(workspaceName1, workspaceName2));

    workspaces.waitBulkCheckboxEnabled();
    workspaces.waitDeleteWorkspaceBtn();

    // unselect all by bulk
    workspaces.selectAllWorkspacesByBulk();
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName2);
    workspaces.waitWorkspaceCheckboxDisabled(workspaceName1);
    workspaces.waitBulkCheckboxDisabled();
    workspaces.waitDeleteWorkspaceBtnDisappearance();
  }

  @Test(groups = UNDER_REPAIR)
  public void checkSorting() {
    workspaces.waitPageLoading();
    workspaces.clickOnRamButton();

    List<Workspaces.WorkspaceListItem> items = workspaces.getVisibleWorkspaces();

    // items are sorted by name, check is present for ensuring of items order
    if (items.get(0).getRamAmount() != BLANK_WS_MB) {
      workspaces.clickOnRamButton();
      items = workspaces.getVisibleWorkspaces();
    }

    // check items order after "RAM" clicking
    try {
      assertEquals(items.get(0).getRamAmount(), BLANK_WS_MB);
      assertEquals(items.get(1).getRamAmount(), JAVA_WS_MB);
    } catch (AssertionError ex) {
      // remove try-catch block after issue has been resolved
      fail("Known permanent failure https://github.com/eclipse/che/issues/4242");
    }

    // check reverse order after "RAM" clicking
    workspaces.clickOnRamButton();
    items = workspaces.getVisibleWorkspaces();
    try {
      assertEquals(items.get(0).getRamAmount(), JAVA_WS_MB);
      assertEquals(items.get(1).getRamAmount(), BLANK_WS_MB);
    } catch (AssertionError ex) {
      // remove try-catch block after issue has been resolved
      fail("Known permanent failure https://github.com/eclipse/che/issues/4242");
    }
  }

  @Test
  public void checkSearchField() throws Exception {
    int nameLength = expectedBlankItem.getWorkspaceName().length();
    int existingWorkspacesCount = getWorkspacesCount();
    String sequenceForSearch =
        expectedBlankItem.getWorkspaceName().substring(nameLength - 5, nameLength);

    workspaces.waitVisibleWorkspacesCount(existingWorkspacesCount);

    workspaces.typeToSearchInput(sequenceForSearch);
    workspaces.waitVisibleWorkspacesCount(EXPECTED_SORTED_WORKSPACES_COUNT);
    List<Workspaces.WorkspaceListItem> items = workspaces.getVisibleWorkspaces();
    assertEquals(items.get(0).getWorkspaceName(), expectedBlankItem.getWorkspaceName());

    // check displaying list size
    workspaces.typeToSearchInput("");
    workspaces.waitVisibleWorkspacesCount(getWorkspacesCount());
  }

  @Test
  public void checkWorkspaceActions() throws Exception {
    workspaces.waitPageLoading();
    String mainWindow = seleniumWebDriver.getWindowHandle();

    // check documentation link
    workspaces.clickOnDocumentationLink();
    seleniumWebDriverHelper.waitOpenedSomeWin();
    seleniumWebDriverHelper.switchToNextWindow(mainWindow);

    documentationPage.waitTitle(EXPECTED_DOCUMENTATION_PAGE_TITLE);

    seleniumWebDriver.close();
    seleniumWebDriver.switchTo().window(mainWindow);

    // go to workspace details by clicking on item in workspaces list
    workspaces.clickOnAddWorkspaceBtn();
    newWorkspace.waitPageLoad();

    seleniumWebDriver.navigate().back();

    workspaces.waitPageLoading();

    workspaces.clickOnWorkspaceListItem(
        defaultTestUser.getName(), expectedBlankItem.getWorkspaceName());

    workspaceOverview.checkNameWorkspace(expectedBlankItem.getWorkspaceName());

    seleniumWebDriver.navigate().back();

    // check "Add project" button
    workspaces.waitPageLoading();

    workspaces.moveCursorToWorkspaceRamSection(expectedJavaItem.getWorkspaceName());
    workspaces.clickOnWorkspaceAddProjectButton(expectedJavaItem.getWorkspaceName());

    workspaceProjects.waitSearchField();

    seleniumWebDriver.navigate().back();

    // check "Workspace configuration" button
    workspaces.waitPageLoading();

    workspaces.moveCursorToWorkspaceRamSection(expectedJavaItem.getWorkspaceName());
    workspaces.clickOnWorkspaceConfigureButton(expectedJavaItem.getWorkspaceName());
    workspaceConfig.waitConfigForm();

    seleniumWebDriver.navigate().back();

    // check adding the workspace to list
    workspaces.waitPageLoading();
    workspaces.clickOnAddWorkspaceBtn();
    newWorkspace.waitToolbar();
    newWorkspace.typeWorkspaceName(NEWEST_CREATED_WORKSPACE_NAME);
    newWorkspace.selectStack(BLANK);
    newWorkspace.clickOnCreateButtonAndEditWorkspace();
    workspaceOverview.checkNameWorkspace(NEWEST_CREATED_WORKSPACE_NAME);

    dashboard.selectWorkspacesItemOnDashboard();

    workspaces.waitPageLoading();
    workspaces.waitVisibleWorkspacesCount(getWorkspacesCount());

    Workspaces.WorkspaceListItem newestCreatedWorkspaceItem =
        workspaces.getWorkspacesListItemByWorkspaceName(
            workspaces.getVisibleWorkspaces(), NEWEST_CREATED_WORKSPACE_NAME);

    assertTrue(newestCreatedWorkspaceItem.equals(expectedNewestWorkspaceItem));
  }

  @Test
  public void deleteWorkspacesByCheckboxes() throws Exception {
    workspaces.waitPageLoading();

    workspaces.selectWorkspaceByCheckbox(workspaceToDelete.getName());
    workspaces.clickOnDeleteWorkspacesBtn();
    workspaces.clickOnDeleteButtonInDialogWindow();

    workspaces.waitWorkspaceIsNotPresent(workspaceToDelete.getName());
  }

  private int getWorkspacesCount() throws Exception {
    return testWorkspaceServiceClient.getAll().size();
  }
}
