import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class FeesReceiptSystem extends JFrame {

    private JTextField nameField, rollField, amountField, searchField;
    private JComboBox<String> paymentCombo;
    private JTextArea receiptArea;
    private JTable table;
    private DefaultTableModel model;

    private Connection conn;

    public FeesReceiptSystem() {
        setTitle("Java Fees Receipt System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initDB();
        initUI();
    }

    private void initDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:fees_receipts.db");

            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS receipts (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "roll TEXT, " +
                    "amount REAL, " +
                    "payment TEXT, " +
                    "notes TEXT, " +
                    "date TEXT)");
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();

        // === New Receipt Tab ===
        JPanel receiptPanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Enter Student Details"));

        formPanel.add(new JLabel("Student Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Roll Number:"));
        rollField = new JTextField();
        formPanel.add(rollField);

        formPanel.add(new JLabel("Amount:"));
        amountField = new JTextField();
        formPanel.add(amountField);

        formPanel.add(new JLabel("Payment Method:"));
        paymentCombo = new JComboBox<>(new String[]{"Cash", "Card", "Online"});
        formPanel.add(paymentCombo);

        formPanel.add(new JLabel("Notes:"));
        JTextField notesField = new JTextField();
        formPanel.add(notesField);

        JButton saveBtn = new JButton("Save Receipt");
        JButton printBtn = new JButton("Print Receipt");
        JButton clearBtn = new JButton("Clear");

        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn);
        btnPanel.add(printBtn);
        btnPanel.add(clearBtn);

        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        receiptArea.setBorder(BorderFactory.createTitledBorder("Generated Receipt"));

        receiptPanel.add(formPanel, BorderLayout.NORTH);
        receiptPanel.add(new JScrollPane(receiptArea), BorderLayout.CENTER);
        receiptPanel.add(btnPanel, BorderLayout.SOUTH);

        tabs.add("New Receipt", receiptPanel);

        // === History/Search Tab ===
        JPanel historyPanel = new JPanel(new BorderLayout());

        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        model = new DefaultTableModel(new String[]{"Receipt ID", "Name", "Roll", "Amount", "Payment", "Date"}, 0);
        table = new JTable(model);
        loadReceipts("");

        JPanel actionPanel = new JPanel();
        JButton printSelBtn = new JButton("Print Selected");
        JButton deleteSelBtn = new JButton("Delete Selected");
        actionPanel.add(printSelBtn);
        actionPanel.add(deleteSelBtn);

        historyPanel.add(searchPanel, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        historyPanel.add(actionPanel, BorderLayout.SOUTH);

        tabs.add("History / Search", historyPanel);

        add(tabs);

        // === Button Actions ===saveBtn.addActionListener(_ -> saveReceipt(notesField.getText()));

        saveBtn.addActionListener(_ -> saveReceipt(notesField.getText()));
        printBtn.addActionListener(_ -> printReceipt());
        clearBtn.addActionListener(_ -> receiptArea.setText(""));

        searchBtn.addActionListener(_ -> loadReceipts(searchField.getText()));
        printSelBtn.addActionListener(_ -> printSelected());
        deleteSelBtn.addActionListener(_ -> deleteSelected());
    }

    private void saveReceipt(String notes) {
        try {
            String id = "RCPT-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String name = nameField.getText();
            String roll = rollField.getText();
            String amount = amountField.getText();
            String payment = (String) paymentCombo.getSelectedItem();
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            if (name.isEmpty() || roll.isEmpty() || amount.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields.");
                return;
            }

            PreparedStatement pst = conn.prepareStatement("INSERT INTO receipts VALUES (?, ?, ?, ?, ?, ?, ?)");
            pst.setString(1, id);
            pst.setString(2, name);
            pst.setString(3, roll);
            pst.setDouble(4, Double.parseDouble(amount));
            pst.setString(5, payment);
            pst.setString(6, notes);
            pst.setString(7, date);
            pst.executeUpdate();

            receiptArea.setText("----- Student Fee Receipt -----\n");
            receiptArea.append("Receipt ID: " + id + "\n");
            receiptArea.append("Name: " + name + "\n");
            receiptArea.append("Roll No: " + roll + "\n");
            receiptArea.append("Amount: " + amount + "\n");
            receiptArea.append("Payment Method: " + payment + "\n");
            receiptArea.append("Notes: " + notes + "\n");
            receiptArea.append("Date: " + date + "\n");
            receiptArea.append("-------------------------------\n");

            loadReceipts("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving receipt: " + ex.getMessage());
        }
    }

    private void loadReceipts(String keyword) {
        try {
            model.setRowCount(0);
            String sql = "SELECT id, name, roll, amount, payment, date FROM receipts";
            if (!keyword.isEmpty()) {
                sql += " WHERE name LIKE ? OR roll LIKE ? OR id LIKE ?";
            }
            PreparedStatement pst = conn.prepareStatement(sql);
            if (!keyword.isEmpty()) {
                pst.setString(1, "%" + keyword + "%");
                pst.setString(2, "%" + keyword + "%");
                pst.setString(3, "%" + keyword + "%");
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= 6; i++) {
                    row.add(rs.getObject(i));
                }
                model.addRow(row);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading receipts: " + ex.getMessage());
        }
    }

    private void printReceipt() {
        try {
            receiptArea.print();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error printing: " + e.getMessage());
        }
    }

    private void printSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a record to print.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("----- Student Fee Receipt -----\n");
        sb.append("Receipt ID: ").append(model.getValueAt(row, 0)).append("\n");
        sb.append("Name: ").append(model.getValueAt(row, 1)).append("\n");
        sb.append("Roll No: ").append(model.getValueAt(row, 2)).append("\n");
        sb.append("Amount: ").append(model.getValueAt(row, 3)).append("\n");
        sb.append("Payment Method: ").append(model.getValueAt(row, 4)).append("\n");
        sb.append("Date: ").append(model.getValueAt(row, 5)).append("\n");
        sb.append("-------------------------------\n");

        JTextArea printArea = new JTextArea(sb.toString());
        try {
            printArea.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Error printing: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a record to delete.");
            return;
        }
        String id = (String) model.getValueAt(row, 0);
        try {
            PreparedStatement pst = conn.prepareStatement("DELETE FROM receipts WHERE id=?");
            pst.setString(1, id);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Receipt deleted.");
            loadReceipts("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FeesReceiptSystem().setVisible(true));
    }
}
