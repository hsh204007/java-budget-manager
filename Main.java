import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 개인 가계부 관리 프로그램의 실행 클래스.
 * Java Swing으로 GUI를 만들고 BudgetManager를 호출하여 실제 기능을 수행한다.
 */
public class Main extends JFrame {
    private static final String APP_TITLE = "개인 가계부 관리 프로그램";
    private static final Path DEFAULT_DATA_PATH = Paths.get("data", "transactions.csv");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final BudgetManager manager = new BudgetManager();
    private final DefaultTableModel tableModel;

    private final JTextField dateField = new JTextField(10);
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[] {"수입", "지출"});
    private final JComboBox<String> categoryCombo = new JComboBox<>(new String[] {
            "식비", "교통", "카페", "쇼핑", "통신", "문화", "월급", "용돈", "기타"
    });
    private final JTextField amountField = new JTextField(10);
    private final JTextField memoField = new JTextField(20);
    private final JComboBox<String> monthCombo = new JComboBox<>();
    private final JLabel incomeValueLabel = new JLabel("0원");
    private final JLabel expenseValueLabel = new JLabel("0원");
    private final JLabel balanceValueLabel = new JLabel("0원");
    private final JTextArea categorySummaryArea = new JTextArea(8, 24);
    private final JTable table;

    private Integer selectedId = null;

    public Main() {
        super(APP_TITLE);
        categoryCombo.setEditable(true);

        tableModel = new DefaultTableModel(new String[] {"ID", "날짜", "구분", "금액", "카테고리", "메모"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setCellRenderer(new MoneyCellRenderer());

        buildLayout();
        bindEvents();
        loadDefaultData();
        refreshAll();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        JLabel titleLabel = new JLabel(APP_TITLE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        root.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("내역 입력"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(formPanel, gbc, 0, "날짜", dateField, "예: 2026-06-17");
        addFormRow(formPanel, gbc, 1, "구분", typeCombo, "수입 또는 지출 선택");
        addFormRow(formPanel, gbc, 2, "카테고리", categoryCombo, "직접 입력 가능");
        addFormRow(formPanel, gbc, 3, "금액", amountField, "숫자만 입력");
        addFormRow(formPanel, gbc, 4, "메모", memoField, "선택 입력");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("추가");
        JButton updateButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        JButton clearButton = new JButton("입력 초기화");
        JButton saveButton = new JButton("저장");
        JButton loadButton = new JButton("불러오기");
        addButton.setName("addButton");
        updateButton.setName("updateButton");
        deleteButton.setName("deleteButton");
        clearButton.setName("clearButton");
        saveButton.setName("saveButton");
        loadButton.setName("loadButton");
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        formPanel.add(buttonPanel, gbc);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(formPanel, BorderLayout.NORTH);
        leftPanel.add(buildSummaryPanel(), BorderLayout.CENTER);
        root.add(leftPanel, BorderLayout.WEST);

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("전체 수입/지출 내역"));
        root.add(tableScrollPane, BorderLayout.CENTER);

        // 버튼 이벤트는 buildLayout 안에서 만든 버튼을 이름으로 찾아 바인딩한다.
        addButton.addActionListener(event -> addTransaction());
        updateButton.addActionListener(event -> updateTransaction());
        deleteButton.addActionListener(event -> deleteTransaction());
        clearButton.addActionListener(event -> clearInputFields());
        saveButton.addActionListener(event -> saveDefaultData());
        loadButton.addActionListener(event -> {
            loadDefaultData();
            refreshAll();
        });
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("월별 요약"));

        JPanel monthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monthPanel.add(new JLabel("조회 월"));
        monthCombo.setPreferredSize(new Dimension(140, 28));
        monthPanel.add(monthCombo);
        panel.add(monthPanel);

        panel.add(createSummaryLine("총수입", incomeValueLabel));
        panel.add(createSummaryLine("총지출", expenseValueLabel));
        panel.add(createSummaryLine("잔액", balanceValueLabel));

        categorySummaryArea.setEditable(false);
        categorySummaryArea.setLineWrap(true);
        categorySummaryArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new JLabel("  카테고리별 지출 합계"));
        panel.add(new JScrollPane(categorySummaryArea));
        return panel;
    }

    private JPanel createSummaryLine(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(valueLabel, BorderLayout.EAST);
        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component field, String helpText) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel help = new JLabel(helpText);
        help.setForeground(new Color(100, 100, 100));
        panel.add(help, gbc);
    }

    private void bindEvents() {
        table.getSelectionModel().addListSelectionListener(this::onTableSelectionChanged);
        monthCombo.addActionListener(event -> refreshSummary());
    }

    private void onTableSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || table.getSelectedRow() < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        selectedId = Integer.parseInt(tableModel.getValueAt(modelRow, 0).toString());
        manager.findById(selectedId).ifPresent(transaction -> {
            dateField.setText(transaction.getDateText());
            typeCombo.setSelectedItem(transaction.getType().getLabel());
            categoryCombo.setSelectedItem(transaction.getCategory());
            amountField.setText(String.valueOf(transaction.getAmount()));
            memoField.setText(transaction.getMemo());
        });
    }

    private void addTransaction() {
        try {
            TransactionInput input = readInput();
            manager.addTransaction(input.date, input.type, input.amount, input.category, input.memo);
            refreshAll();
            clearInputFields();
            showMessage("내역이 추가되었습니다.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateTransaction() {
        if (selectedId == null) {
            showError("수정할 내역을 표에서 먼저 선택하세요.");
            return;
        }
        try {
            TransactionInput input = readInput();
            manager.updateTransaction(selectedId, input.date, input.type, input.amount, input.category, input.memo);
            refreshAll();
            clearInputFields();
            showMessage("선택한 내역이 수정되었습니다.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteTransaction() {
        if (selectedId == null) {
            showError("삭제할 내역을 표에서 먼저 선택하세요.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "선택한 내역을 삭제할까요?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            manager.deleteTransaction(selectedId);
            refreshAll();
            clearInputFields();
            showMessage("선택한 내역이 삭제되었습니다.");
        }
    }

    private TransactionInput readInput() {
        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("날짜는 yyyy-MM-dd 형식으로 입력하세요.");
        }

        Transaction.Type type = Transaction.Type.fromLabel(String.valueOf(typeCombo.getSelectedItem()));

        String category = String.valueOf(categoryCombo.getSelectedItem()).trim();
        if (category.isEmpty()) {
            throw new IllegalArgumentException("카테고리를 입력하세요.");
        }

        long amount;
        try {
            amount = Long.parseLong(amountField.getText().trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("금액은 숫자로 입력하세요.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 1원 이상이어야 합니다.");
        }

        String memo = memoField.getText().trim();
        return new TransactionInput(date, type, amount, category, memo);
    }

    private void clearInputFields() {
        selectedId = null;
        table.clearSelection();
        dateField.setText(LocalDate.now().format(DATE_FORMATTER));
        typeCombo.setSelectedItem("지출");
        categoryCombo.setSelectedItem("식비");
        amountField.setText("");
        memoField.setText("");
    }

    private void refreshAll() {
        refreshMonthCombo();
        refreshTable();
        refreshSummary();
    }

    private void refreshMonthCombo() {
        String previous = (String) monthCombo.getSelectedItem();
        monthCombo.removeAllItems();
        List<YearMonth> months = manager.getAvailableMonths();
        if (months.isEmpty()) {
            monthCombo.addItem(YearMonth.now().toString());
        } else {
            for (YearMonth month : months) {
                monthCombo.addItem(month.toString());
            }
        }
        if (previous != null) {
            monthCombo.setSelectedItem(previous);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Transaction transaction : manager.getAllTransactions()) {
            tableModel.addRow(transaction.toTableRow());
        }
    }

    private void refreshSummary() {
        if (monthCombo.getSelectedItem() == null) {
            return;
        }
        YearMonth selectedMonth = YearMonth.parse(monthCombo.getSelectedItem().toString());
        long income = manager.getTotalIncome(selectedMonth);
        long expense = manager.getTotalExpense(selectedMonth);
        long balance = manager.getBalance(selectedMonth);

        incomeValueLabel.setText(formatWon(income));
        expenseValueLabel.setText(formatWon(expense));
        balanceValueLabel.setText(formatWon(balance));
        balanceValueLabel.setForeground(balance >= 0 ? new Color(0, 110, 55) : new Color(190, 40, 40));

        Map<String, Long> categorySummary = manager.getExpenseByCategory(selectedMonth);
        if (categorySummary.isEmpty()) {
            categorySummaryArea.setText("해당 월의 지출 내역이 없습니다.");
        } else {
            StringBuilder builder = new StringBuilder();
            categorySummary.forEach((category, amount) ->
                    builder.append("- ").append(category).append(" : ").append(formatWon(amount)).append("\n"));
            categorySummaryArea.setText(builder.toString());
        }
    }

    private void loadDefaultData() {
        try {
            manager.loadFromFile(DEFAULT_DATA_PATH);
            if (manager.getAllTransactions().isEmpty()) {
                createSampleData();
            }
            clearInputFields();
        } catch (IOException | IllegalArgumentException ex) {
            showError("파일을 불러오는 중 오류가 발생했습니다: " + ex.getMessage());
            createSampleData();
        }
    }

    private void createSampleData() {
        manager.clear();
        YearMonth currentMonth = YearMonth.now();
        manager.addTransaction(currentMonth.atDay(1), Transaction.Type.INCOME, 600000, "용돈", "이번 달 용돈");
        manager.addTransaction(currentMonth.atDay(3), Transaction.Type.EXPENSE, 7800, "식비", "학식");
        manager.addTransaction(currentMonth.atDay(5), Transaction.Type.EXPENSE, 4500, "카페", "아이스 아메리카노");
        manager.addTransaction(currentMonth.atDay(8), Transaction.Type.EXPENSE, 62000, "교통", "교통카드 충전");
        manager.addTransaction(currentMonth.atDay(12), Transaction.Type.EXPENSE, 25900, "쇼핑", "필기구와 노트");
    }

    private void saveDefaultData() {
        try {
            manager.saveToFile(DEFAULT_DATA_PATH);
            showMessage("transactions.csv 파일로 저장했습니다.");
        } catch (IOException ex) {
            showError("파일 저장 중 오류가 발생했습니다: " + ex.getMessage());
        }
    }

    private static String formatWon(long value) {
        return MONEY_FORMAT.format(value) + "원";
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "알림", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 기본 Look & Feel 사용
            }
            new Main().setVisible(true);
        });
    }

    private static class TransactionInput {
        private final LocalDate date;
        private final Transaction.Type type;
        private final long amount;
        private final String category;
        private final String memo;

        private TransactionInput(LocalDate date, Transaction.Type type, long amount, String category, String memo) {
            this.date = date;
            this.type = type;
            this.amount = amount;
            this.category = category;
            this.memo = memo;
        }
    }

    private static class MoneyCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            try {
                long amount = Long.parseLong(String.valueOf(value));
                setText(formatWon(amount));
                setHorizontalAlignment(SwingConstants.RIGHT);
            } catch (NumberFormatException ex) {
                super.setValue(value);
            }
        }
    }
}
