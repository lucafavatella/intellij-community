/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Nov 4, 2001
 * Time: 5:19:35 PM
 */
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.*;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeComparator;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

public class InspectionTree extends Tree {
  @NotNull private final GlobalInspectionContextImpl myContext;
  @NotNull private final ExcludedInspectionTreeNodesManager myExcludedManager;
  @NotNull private InspectionTreeState myState = new InspectionTreeState();
  private boolean myQueueUpdate;

  public InspectionTree(@NotNull Project project,
                        @NotNull GlobalInspectionContextImpl context, InspectionResultsView view) {
    setModel(new DefaultTreeModel(new InspectionRootNode(project, new InspectionTreeUpdater(view))));
    myContext = context;
    myExcludedManager = view.getExcludedManager();

    setCellRenderer(new CellRenderer());
    setRootVisible(!myContext.isSingleInspectionRun());
    setShowsRootHandles(true);
    UIUtil.setLineStyleAngled(this);
    addTreeWillExpandListener(new ExpandListener());

    myState.getExpandedUserObjects().add(project);

    TreeUtil.installActions(this);
    new TreeSpeedSearch(this, new Convertor<TreePath, String>() {
      @Override
      public String convert(TreePath o) {
        return InspectionsConfigTreeComparator.getDisplayTextToSort(o.getLastPathComponent().toString());
      }
    });

    addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        TreePath newSelection = e.getNewLeadSelectionPath();
        if (newSelection != null && !isUnderQueueUpdate()) {
          myState.setSelectionPath(newSelection);
        }
      }
    });
  }

  public void setQueueUpdate(boolean queueUpdate) {
    myQueueUpdate = queueUpdate;
  }

  public boolean isUnderQueueUpdate() {
    return myQueueUpdate;
  }

  public void removeAllNodes() {
    getRoot().removeAllChildren();
    nodeStructureChanged(getRoot());
  }

  public InspectionTreeNode getRoot() {
    return (InspectionTreeNode)getModel().getRoot();
  }

  @Nullable
  public InspectionToolWrapper getSelectedToolWrapper() {
    final TreePath[] paths = getSelectionPaths();
    if (paths == null) return null;
    InspectionToolWrapper toolWrapper = null;
    for (TreePath path : paths) {
      Object[] nodes = path.getPath();
      for (int j = nodes.length - 1; j >= 0; j--) {
        Object node = nodes[j];
        if (node instanceof InspectionGroupNode) {
          return null;
        }
        if (node instanceof InspectionNode) {
          InspectionToolWrapper wrapper = ((InspectionNode)node).getToolWrapper();
          if (toolWrapper == null) {
            toolWrapper = wrapper;
          }
          else if (toolWrapper != wrapper) {
            return null;
          }
          break;
        }
      }
    }

    return toolWrapper;
  }

  @Nullable
  public RefEntity getCommonSelectedElement() {
    final Object node = getCommonSelectedNode();
    return node instanceof RefElementNode ? ((RefElementNode)node).getElement() : null;
  }

  @Nullable
  private Object getCommonSelectedNode() {
    final TreePath[] paths = getSelectionPaths();
    if (paths == null) return null;
    final Object[][] resolvedPaths = new Object[paths.length][];
    for (int i = 0; i < paths.length; i++) {
      TreePath path = paths[i];
      resolvedPaths[i] = path.getPath();
    }

    Object currentCommonNode = null;
    for (int i = 0; i < resolvedPaths[0].length; i++) {
      final Object currentNode = resolvedPaths[0][i];
      for (int j = 1; j < resolvedPaths.length; j++) {
        final Object o = resolvedPaths[j][i];
        if (!o.equals(currentNode)) {
          return currentCommonNode;
        }
      }
      currentCommonNode = currentNode;
    }
    return currentCommonNode;
  }

  @NotNull
  public RefEntity[] getSelectedElements() {
    TreePath[] selectionPaths = getSelectionPaths();
    if (selectionPaths != null) {
      InspectionToolWrapper toolWrapper = getSelectedToolWrapper();
      if (toolWrapper == null) return RefEntity.EMPTY_ELEMENTS_ARRAY;

      List<RefEntity> result = new ArrayList<RefEntity>();
      for (TreePath selectionPath : selectionPaths) {
        final InspectionTreeNode node = (InspectionTreeNode)selectionPath.getLastPathComponent();
        addElementsInNode(node, result);
      }
      return result.toArray(new RefEntity[result.size()]);
    }
    return RefEntity.EMPTY_ELEMENTS_ARRAY;
  }

  private static void addElementsInNode(InspectionTreeNode node, List<RefEntity> out) {
    if (!node.isValid()) return;
    if (node instanceof RefElementNode) {
      final RefEntity element = ((RefElementNode)node).getElement();
      if (!out.contains(element)) {
        out.add(0, element);
      }
    }
    if (node instanceof ProblemDescriptionNode) {
      final RefEntity element = ((ProblemDescriptionNode)node).getElement();
      if (!out.contains(element)) {
        out.add(0, element);
      }
    }
    final Enumeration children = node.children();
    while (children.hasMoreElements()) {
      InspectionTreeNode child = (InspectionTreeNode)children.nextElement();
      addElementsInNode(child, out);
    }
  }

  public CommonProblemDescriptor[] getSelectedDescriptors() {
    if (getSelectionCount() == 0) return CommonProblemDescriptor.EMPTY_ARRAY;
    final TreePath[] paths = getSelectionPaths();
    final LinkedHashSet<CommonProblemDescriptor> descriptors = new LinkedHashSet<CommonProblemDescriptor>();
    for (TreePath path : paths) {
      Object node = path.getLastPathComponent();
      traverseDescriptors((InspectionTreeNode)node, descriptors, myExcludedManager);
    }
    return descriptors.toArray(new CommonProblemDescriptor[descriptors.size()]);
  }

  public int getSelectedProblemCount() {
    if (getSelectionCount() == 0) return 0;
    final TreePath[] paths = getSelectionPaths();

    Set<InspectionTreeNode> result = new HashSet<>();
    MultiMap<InspectionTreeNode, InspectionTreeNode> rootDependencies = new MultiMap<>();
    for (TreePath path : paths) {

      final InspectionTreeNode node = (InspectionTreeNode)path.getLastPathComponent();
      final Collection<InspectionTreeNode> visitedChildren = rootDependencies.get(node);
      for (InspectionTreeNode child : visitedChildren) {
        result.remove(child);
      }

      boolean needToAdd = true;
      for (int i = 0; i < path.getPathCount() - 1; i++) {
        final InspectionTreeNode parent = (InspectionTreeNode) path.getPathComponent(i);
        rootDependencies.putValue(parent, node);
        if (result.contains(parent)) {
          needToAdd = false;
          break;
        }
      }

      if (needToAdd) {
        result.add(node);
      }
    }

    int count = 0;
    for (InspectionTreeNode node : result) {
      count += node.getProblemCount();
    }
    return count;
  }

  private static void traverseDescriptors(InspectionTreeNode node,
                                          LinkedHashSet<CommonProblemDescriptor> descriptors,
                                          ExcludedInspectionTreeNodesManager manager){
    if (node instanceof ProblemDescriptionNode) {
      if (node.isValid() && !node.isResolved(manager)) {
        final CommonProblemDescriptor descriptor = ((ProblemDescriptionNode)node).getDescriptor();
        if (descriptor != null) {
          descriptors.add(descriptor);
        }
      }
    }
    for(int i = node.getChildCount() - 1; i >= 0; i--){
      traverseDescriptors((InspectionTreeNode)node.getChildAt(i), descriptors, manager);
    }
  }

  private void nodeStructureChanged(InspectionTreeNode node) {
    ((DefaultTreeModel)getModel()).nodeStructureChanged(node);
  }

  public void queueUpdate() {
    ((InspectionRootNode) getRoot()).getUpdater().update(null, true);
  }

  public void restoreExpansionAndSelection(@Nullable InspectionTreeNode reloadedNode) {
    myState.restoreExpansionAndSelection(this, reloadedNode);
  }

  public void setState(@NotNull InspectionTreeState state) {
    myState = state;
  }

  public InspectionTreeState getTreeState() {
    return myState;
  }

  public void setTreeState(@NotNull InspectionTreeState treeState) {
    myState = treeState;
  }

  private class ExpandListener implements TreeWillExpandListener {
    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
      final InspectionTreeNode node = (InspectionTreeNode)event.getPath().getLastPathComponent();
      final Object userObject = node.getUserObject();
      //TODO: never re-sort
      if (node.isValid() && !myState.getExpandedUserObjects().contains(userObject)) {
        sortChildren(node);
        nodeStructureChanged(node);
      }
      myState.getExpandedUserObjects().add(userObject);
      // Smart expand
      if (node.getChildCount() == 1) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            expandPath(new TreePath(node.getPath()));
          }
        });
      }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      InspectionTreeNode node = (InspectionTreeNode)event.getPath().getLastPathComponent();
      myState.getExpandedUserObjects().remove(node.getUserObject());
    }
  }

  private class CellRenderer extends ColoredTreeCellRenderer {
    /*  private Project myProject;
      InspectionManagerEx myManager;
      public CellRenderer(Project project) {
        myProject = project;
        myManager = (InspectionManagerEx)InspectionManager.getInstance(myProject);
      }*/

    @Override
    public void customizeCellRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      InspectionTreeNode node = (InspectionTreeNode)value;

      append(node.toString(),
             patchAttr(node, appearsBold(node) ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : getMainForegroundAttributes(node)));

      int problemCount = node.getProblemCount();
      if (!leaf) {
        append(" " + InspectionsBundle.message("inspection.problem.descriptor.count", problemCount), patchAttr(node, SimpleTextAttributes.GRAYED_ATTRIBUTES));
      }

      if (!node.isValid()) {
        append(" " + InspectionsBundle.message("inspection.invalid.node.text"), patchAttr(node, SimpleTextAttributes.ERROR_ATTRIBUTES));
      } else {
        setIcon(node.getIcon(expanded));
      }
      // do not need reset model (for recalculation of prefered size) when digit number of problemCount is growth
      // or INVALID marker appears
      final String tail = StringUtil.repeat(" ", Math.max(0, 5- - String.valueOf(problemCount).length()));
      append(tail);
    }

    public SimpleTextAttributes patchAttr(InspectionTreeNode node, SimpleTextAttributes attributes) {
      if (node.isResolved(myExcludedManager)) {
        return new SimpleTextAttributes(attributes.getBgColor(), attributes.getFgColor(), attributes.getWaveColor(), attributes.getStyle() | SimpleTextAttributes.STYLE_STRIKEOUT);
      }
      return attributes;
    }

    private SimpleTextAttributes getMainForegroundAttributes(InspectionTreeNode node) {
      SimpleTextAttributes foreground = SimpleTextAttributes.REGULAR_ATTRIBUTES;
      if (node instanceof RefElementNode) {
        RefEntity refElement = ((RefElementNode)node).getElement();

        if (refElement instanceof RefElement) {
          refElement = ((RefElement)refElement).getContainingEntry();
          if (((RefElement)refElement).isEntry() && ((RefElement)refElement).isPermanentEntry()) {
            foreground = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.blue);
          }
        }

      }
      final FileStatus nodeStatus = node.getNodeStatus();
      if (nodeStatus != FileStatus.NOT_CHANGED){
        foreground = new SimpleTextAttributes(foreground.getBgColor(), nodeStatus.getColor(), foreground.getWaveColor(), foreground.getStyle());
      }
      return foreground;
    }

    private boolean appearsBold(Object node) {
      return ((InspectionTreeNode)node).appearsBold();
    }
  }

  private void sortChildren(InspectionTreeNode node) {
    final List<TreeNode> children = TreeUtil.childrenToArray(node);
    Collections.sort(children, InspectionResultsViewComparator.getInstance());
    node.removeAllChildren();
    TreeUtil.addChildrenTo(node, children);
    ((DefaultTreeModel)getModel()).reload(node);
  }

  @NotNull
  public GlobalInspectionContextImpl getContext() {
    return myContext;
  }
}
