package me.kitakeyos.j2me.presentation.script.component;

import me.kitakeyos.j2me.domain.script.model.LuaScript;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * Script list component for displaying and managing the list of Lua scripts.
 * Provides script selection functionality through a listener interface.
 */
public class ScriptList extends JPanel {

    public interface ScriptSelectionListener {
        void onScriptSelected(String scriptName);
    }

    private DefaultTableModel scriptTableModel;
    private JTable scriptTable;

    public ScriptList(ScriptSelectionListener listener) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Scripts"));

        scriptTableModel = new DefaultTableModel(new String[]{"Script Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        scriptTable = new JTable(scriptTableModel);
        scriptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scriptTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listener != null) {
                int selectedRow = scriptTable.getSelectedRow();
                String scriptName = selectedRow != -1 ?
                        (String) scriptTableModel.getValueAt(selectedRow, 0) : null;
                listener.onScriptSelected(scriptName);
            }
        });

        add(new JScrollPane(scriptTable), BorderLayout.CENTER);
    }

    public void addScript(String scriptName) {
        scriptTableModel.addRow(new Object[]{scriptName});
    }

    public void removeScript(String scriptName) {
        for (int i = 0; i < scriptTableModel.getRowCount(); i++) {
            if (scriptName.equals(scriptTableModel.getValueAt(i, 0))) {
                scriptTableModel.removeRow(i);
                break;
            }
        }
    }

    public void loadScripts(Map<String, LuaScript> scripts) {
        scriptTableModel.setRowCount(0);
        scripts.keySet().forEach(this::addScript);
    }

    public String getSelectedScriptName() {
        int selectedRow = scriptTable.getSelectedRow();
        return selectedRow != -1 ? (String) scriptTableModel.getValueAt(selectedRow, 0) : null;
    }

    public void selectFirstScript() {
        if (scriptTableModel.getRowCount() > 0) {
            scriptTable.setRowSelectionInterval(0, 0);
        }
    }
}
