package org.jetbrains.plugins.javaFX.fxml;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.javaFX.fxml.codeInsight.inspections.JavaFxEventHandlerInspection;

/**
 * @author Pavel.Dolgov
 */
public class JavaFxEventHandlerInspectionTest extends AbstractJavaFXTestCase {

  @Override
  protected void enableInspections() {
    myFixture.enableInspections(new JavaFxEventHandlerInspection());
  }


  public void testHighlightExact() throws Exception {
    doHighlightingTest();
  }

  public void testHighlightNonVoid() throws Exception {
    final JavaFxEventHandlerInspection inspection = new JavaFxEventHandlerInspection();
    inspection.myDetectNonVoidReturnType = true;
    myFixture.enableInspections(inspection);
    doHighlightingTest();
  }

  public void testHighlightAmbiguous() throws Exception {
    doHighlightingTest();
  }

  public void testHighlightGeneric() throws Exception {
    doHighlightingTest();
  }

  public void testHighlightRaw() throws Exception {
    doHighlightingTest();
  }

  public void testHighlightHalfRaw() throws Exception {
    doHighlightingTest();
  }

  public void testHighlightSpecific() throws Exception {
    doHighlightingTest();
  }

  public void testHighlightSuper() throws Exception {
    doHighlightingTest();
  }

  public void testHighlightWildcard() throws Exception {
    doHighlightingTest();
  }

  public void testQuickfixRaw() throws Exception {
    doQuickfixTest("Create method 'void onSort(SortEvent)'");
  }

  public void testQuickfixHalfRaw() throws Exception {
    doQuickfixTest("Create method 'void onSort(SortEvent)'");
  }

  public void testQuickfixSpecific() throws Exception {
    doQuickfixTest("Create method 'void onSort(SortEvent)'");
  }

  public void testQuickfixNoField() throws Exception {
    doQuickfixTest("Create method 'void onSort(SortEvent)'");
  }

  public void testQuickfixNoFieldNested() throws Exception {
    doQuickfixTest("Create method 'void onColumnEditStart(CellEditEvent)'");
  }

  public void testQuickfixSuper() throws Exception {
    doQuickfixTest("Create method 'void click(MouseEvent)'");
  }

  private void doHighlightingTest() throws Exception {
    myFixture.configureByFiles(getTestName(true) + ".fxml", getTestName(false) + ".java");
    myFixture.checkHighlighting();
  }

  private void doQuickfixTest(final String actionName) throws Exception {
    String path = getTestName(true) + ".fxml";
    final IntentionAction intention = myFixture.getAvailableIntention(actionName, path, getTestName(false) + ".java");
    assertNotNull(intention);
    myFixture.launchAction(intention);
    myFixture.checkResultByFile(getTestName(false) + ".java", getTestName(false) + "_after.java", true);
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return PluginPathManager.getPluginHomePath("javaFX") + "/testData/inspections/eventHandler/";
  }
}
