package me.kitakeyos.j2me.presentation.script.component;

import me.kitakeyos.j2me.domain.script.model.LuaScript;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Map;

/**
 * Script list component using JTree for displaying Lua scripts in a tree structure.
 * Supports nested folders and provides script selection functionality.
 */
public class ScriptList extends JPanel {

    public interface ScriptSelectionListener {
        void onScriptSelected(String scriptPath);
    }

    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JTree scriptTree;
    private ScriptSelectionListener listener;

    public ScriptList(ScriptSelectionListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Scripts"));

        rootNode = new DefaultMutableTreeNode("Scripts");
        treeModel = new DefaultTreeModel(rootNode);
        scriptTree = new JTree(treeModel);

        scriptTree.setRootVisible(false);
        scriptTree.setShowsRootHandles(true);
        scriptTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Custom cell renderer for icons
        scriptTree.setCellRenderer(new ScriptTreeCellRenderer());

        // Selection listener
        scriptTree.addTreeSelectionListener(e -> {
            if (listener != null) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) scriptTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.isLeaf() && selectedNode.getUserObject() instanceof ScriptNode) {
                    ScriptNode scriptNode = (ScriptNode) selectedNode.getUserObject();
                    listener.onScriptSelected(scriptNode.getFullPath());
                } else {
                    listener.onScriptSelected(null);
                }
            }
        });

        add(new JScrollPane(scriptTree), BorderLayout.CENTER);
    }

    /**
     * Load scripts into tree structure
     */
    public void loadScripts(Map<String, LuaScript> scripts) {
        rootNode.removeAllChildren();

        for (String scriptPath : scripts.keySet()) {
            addScriptToTree(scriptPath);
        }

        treeModel.reload();
        expandAllNodes();
    }

    /**
     * Add a script to the tree, creating folder nodes as needed
     */
    public void addScript(String scriptPath) {
        addScriptToTree(scriptPath);
        treeModel.reload();
        expandAllNodes();
    }

    private void addScriptToTree(String scriptPath) {
        String[] parts = scriptPath.split("/");
        DefaultMutableTreeNode currentNode = rootNode;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean isLast = (i == parts.length - 1);

            DefaultMutableTreeNode childNode = findChild(currentNode, part);

            if (childNode == null) {
                if (isLast) {
                    // Leaf node - script file
                    ScriptNode scriptNode = new ScriptNode(part, scriptPath);
                    childNode = new DefaultMutableTreeNode(scriptNode);
                } else {
                    // Folder node
                    FolderNode folderNode = new FolderNode(part);
                    childNode = new DefaultMutableTreeNode(folderNode);
                }
                currentNode.add(childNode);
            }

            currentNode = childNode;
        }
    }

    /**
     * Remove a script from the tree
     */
    public void removeScript(String scriptPath) {
        removeScriptFromTree(rootNode, scriptPath);
        cleanupEmptyFolders(rootNode);
        treeModel.reload();
        expandAllNodes();
    }

    private boolean removeScriptFromTree(DefaultMutableTreeNode node, String scriptPath) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object userObject = child.getUserObject();

            if (userObject instanceof ScriptNode) {
                ScriptNode scriptNode = (ScriptNode) userObject;
                if (scriptNode.getFullPath().equals(scriptPath)) {
                    node.remove(i);
                    return true;
                }
            } else if (removeScriptFromTree(child, scriptPath)) {
                return true;
            }
        }
        return false;
    }

    private void cleanupEmptyFolders(DefaultMutableTreeNode node) {
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child.getUserObject() instanceof FolderNode) {
                cleanupEmptyFolders(child);
                if (child.getChildCount() == 0) {
                    node.remove(i);
                }
            }
        }
    }

    private DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObject = child.getUserObject();

            String nodeName = null;
            if (userObject instanceof ScriptNode) {
                nodeName = ((ScriptNode) userObject).getName();
            } else if (userObject instanceof FolderNode) {
                nodeName = ((FolderNode) userObject).getName();
            }

            if (name.equals(nodeName)) {
                return child;
            }
        }
        return null;
    }

    public String getSelectedScriptName() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) scriptTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof ScriptNode) {
            return ((ScriptNode) selectedNode.getUserObject()).getFullPath();
        }
        return null;
    }

    public void selectFirstScript() {
        DefaultMutableTreeNode firstLeaf = findFirstLeaf(rootNode);
        if (firstLeaf != null) {
            TreePath path = new TreePath(firstLeaf.getPath());
            scriptTree.setSelectionPath(path);
            scriptTree.scrollPathToVisible(path);
        }
    }

    private DefaultMutableTreeNode findFirstLeaf(DefaultMutableTreeNode node) {
        if (node.isLeaf() && node.getUserObject() instanceof ScriptNode) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode result = findFirstLeaf((DefaultMutableTreeNode) node.getChildAt(i));
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private void expandAllNodes() {
        for (int i = 0; i < scriptTree.getRowCount(); i++) {
            scriptTree.expandRow(i);
        }
    }

    // ==================== Inner Classes ====================

    /**
     * Represents a script file in the tree
     */
    public static class ScriptNode {
        private final String name;
        private final String fullPath;

        public ScriptNode(String name, String fullPath) {
            this.name = name;
            this.fullPath = fullPath;
        }

        public String getName() {
            return name;
        }

        public String getFullPath() {
            return fullPath;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Represents a folder in the tree
     */
    public static class FolderNode {
        private final String name;

        public FolderNode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Custom cell renderer for script tree
     */
    private static class ScriptTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Icon folderIcon;
        private final Icon scriptIcon;

        public ScriptTreeCellRenderer() {
            folderIcon = UIManager.getIcon("Tree.closedIcon");
            scriptIcon = UIManager.getIcon("Tree.leafIcon");
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof FolderNode) {
                setIcon(expanded ? getOpenIcon() : getClosedIcon());
            } else if (userObject instanceof ScriptNode) {
                setIcon(scriptIcon);
            }

            return this;
        }
    }
}