package me.kitakeyos.j2me.presentation.script.component;

import me.kitakeyos.j2me.domain.script.model.LuaScript;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Script list component using JTree for displaying Lua scripts in a tree
 * structure.
 * Supports nested folders and provides script selection functionality.
 */
public class ScriptList extends JPanel {

    public interface ScriptActionListener {
        void onScriptSelected(String scriptPath);

        void onNewScript(String folderPath);

        void onNewFolder(String parentPath);

        void onDelete(String path, boolean isFolder);

        void onRename(String path, boolean isFolder);
    }

    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JTree scriptTree;
    private ScriptActionListener listener;
    private JPopupMenu contextMenu;

    public ScriptList(ScriptActionListener listener) {
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
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) scriptTree
                        .getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.isLeaf()
                        && selectedNode.getUserObject() instanceof ScriptNode) {
                    ScriptNode scriptNode = (ScriptNode) selectedNode.getUserObject();
                    listener.onScriptSelected(scriptNode.getFullPath());
                } else {
                    listener.onScriptSelected(null);
                }
            }
        });

        setupContextMenu();

        add(new JScrollPane(scriptTree), BorderLayout.CENTER);
    }

    private void setupContextMenu() {
        contextMenu = new JPopupMenu();
        JMenuItem newScriptItem = new JMenuItem("New Script");
        JMenuItem newFolderItem = new JMenuItem("New Folder");
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem deleteItem = new JMenuItem("Delete");

        newScriptItem.addActionListener(e -> {
            String path = getSelectedFolderPath();
            if (listener != null)
                listener.onNewScript(path);
        });

        newFolderItem.addActionListener(e -> {
            String path = getSelectedFolderPath();
            if (listener != null)
                listener.onNewFolder(path);
        });

        renameItem.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) scriptTree.getLastSelectedPathComponent();
            if (node != null && listener != null) {
                Object userObject = node.getUserObject();
                if (userObject instanceof ScriptNode) {
                    listener.onRename(((ScriptNode) userObject).getFullPath(), false);
                } else if (userObject instanceof FolderNode) {
                    listener.onRename(getPathFromNode(node), true);
                }
            }
        });

        deleteItem.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) scriptTree.getLastSelectedPathComponent();
            if (node != null && listener != null) {
                Object userObject = node.getUserObject();
                if (userObject instanceof ScriptNode) {
                    listener.onDelete(((ScriptNode) userObject).getFullPath(), false);
                } else if (userObject instanceof FolderNode) {
                    listener.onDelete(getPathFromNode(node), true);
                }
            }
        });

        contextMenu.add(newScriptItem);
        contextMenu.add(newFolderItem);
        contextMenu.addSeparator();
        contextMenu.add(renameItem);
        contextMenu.add(deleteItem);

        scriptTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = scriptTree.getRowForLocation(e.getX(), e.getY());
                    if (row != -1) {
                        scriptTree.setSelectionRow(row);
                        contextMenu.show(scriptTree, e.getX(), e.getY());
                    } else {
                        // Deselect if clicking on empty space
                        scriptTree.clearSelection();
                        contextMenu.show(scriptTree, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    public String getSelectedFolderPath() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) scriptTree.getLastSelectedPathComponent();
        if (node == null)
            return "";

        Object userObject = node.getUserObject();
        if (userObject instanceof FolderNode) {
            return getPathFromNode(node);
        } else if (userObject instanceof ScriptNode) {
            // If a script is selected, return its parent folder
            TreeNode parent = node.getParent();
            if (parent instanceof DefaultMutableTreeNode) {
                return getPathFromNode((DefaultMutableTreeNode) parent);
            }
        }
        return "";
    }

    private String getPathFromNode(DefaultMutableTreeNode node) {
        if (node == null || node.isRoot())
            return "";

        Object userObject = node.getUserObject();
        if (userObject instanceof FolderNode) {
            String parentPath = getPathFromNode((DefaultMutableTreeNode) node.getParent());
            return parentPath.isEmpty() ? ((FolderNode) userObject).getName()
                    : parentPath + "/" + ((FolderNode) userObject).getName();
        }
        return "";
    }

    /**
     * Load scripts into tree structure
     */
    public void loadScripts(Map<String, LuaScript> scripts) {
        rootNode.removeAllChildren();

        for (String scriptPath : scripts.keySet()) {
            addScriptToTree(scriptPath);
        }

        sortTree(rootNode);
        treeModel.reload();
        expandAllNodes();
    }

    public void loadFolders(List<String> folders) {
        for (String folder : folders) {
            addFolderToTree(folder);
        }
        sortTree(rootNode);
        treeModel.reload();
        expandAllNodes();
    }

    /**
     * Add a script to the tree, creating folder nodes as needed
     */
    public void addScript(String scriptPath) {
        addScriptToTree(scriptPath);
        sortTree(rootNode);
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

    private void addFolderToTree(String folderPath) {
        String[] parts = folderPath.split("/");
        DefaultMutableTreeNode currentNode = rootNode;

        for (String part : parts) {
            DefaultMutableTreeNode childNode = findChild(currentNode, part);
            if (childNode == null) {
                FolderNode folderNode = new FolderNode(part);
                childNode = new DefaultMutableTreeNode(folderNode);
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
        sortTree(rootNode);
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
                    // Don't remove empty folders anymore as we support them explicitly
                    // node.remove(i);
                }
            }
        }
    }

    private void sortTree(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 0)
            return;

        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((DefaultMutableTreeNode) node.getChildAt(i));
        }

        children.sort((n1, n2) -> {
            boolean isFolder1 = n1.getUserObject() instanceof FolderNode;
            boolean isFolder2 = n2.getUserObject() instanceof FolderNode;

            if (isFolder1 && !isFolder2)
                return -1;
            if (!isFolder1 && isFolder2)
                return 1;

            String name1 = n1.getUserObject().toString();
            String name2 = n2.getUserObject().toString();
            return name1.compareToIgnoreCase(name2);
        });

        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            node.add(child);
            sortTree(child);
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

            if (name != null && name.equals(nodeName)) {
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
        private final Icon scriptIcon;

        public ScriptTreeCellRenderer() {
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