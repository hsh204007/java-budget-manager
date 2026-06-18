import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 수입/지출 목록을 관리하고 통계 계산, 수정, 삭제, 파일 저장을 담당한다.
 */
public class BudgetManager {
    private final List<Transaction> transactions = new ArrayList<>();
    private int nextId = 1;

    public Transaction addTransaction(LocalDate date, Transaction.Type type, long amount, String category, String memo) {
        Transaction transaction = new Transaction(nextId++, date, type, amount, category, memo);
        transactions.add(transaction);
        sortTransactions();
        return transaction;
    }

    public void updateTransaction(int id, LocalDate date, Transaction.Type type, long amount, String category, String memo) {
        Transaction transaction = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 내역을 찾을 수 없습니다. ID=" + id));
        transaction.setDate(date);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setMemo(memo);
        sortTransactions();
    }

    public boolean deleteTransaction(int id) {
        return transactions.removeIf(transaction -> transaction.getId() == id);
    }

    public Optional<Transaction> findById(int id) {
        return transactions.stream()
                .filter(transaction -> transaction.getId() == id)
                .findFirst();
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction> getTransactionsByMonth(YearMonth month) {
        return transactions.stream()
                .filter(transaction -> YearMonth.from(transaction.getDate()).equals(month))
                .collect(Collectors.toList());
    }

    public long getTotalIncome(YearMonth month) {
        return getTransactionsByMonth(month).stream()
                .filter(transaction -> transaction.getType() == Transaction.Type.INCOME)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getTotalExpense(YearMonth month) {
        return getTransactionsByMonth(month).stream()
                .filter(transaction -> transaction.getType() == Transaction.Type.EXPENSE)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getBalance(YearMonth month) {
        return getTotalIncome(month) - getTotalExpense(month);
    }

    public Map<String, Long> getExpenseByCategory(YearMonth month) {
        Map<String, Long> summary = new LinkedHashMap<>();
        getTransactionsByMonth(month).stream()
                .filter(transaction -> transaction.getType() == Transaction.Type.EXPENSE)
                .sorted(Comparator.comparing(Transaction::getCategory))
                .forEach(transaction -> summary.merge(transaction.getCategory(), transaction.getAmount(), Long::sum));
        return summary;
    }

    public List<YearMonth> getAvailableMonths() {
        return transactions.stream()
                .map(transaction -> YearMonth.from(transaction.getDate()))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    public void saveToFile(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("id,date,type,amount,category,memo");
            writer.newLine();
            for (Transaction transaction : transactions) {
                writer.write(transaction.toCsvLine());
                writer.newLine();
            }
        }
    }

    public void loadFromFile(Path path) throws IOException {
        transactions.clear();
        nextId = 1;
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (firstLine && line.toLowerCase().startsWith("id,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                Transaction transaction = Transaction.fromCsvLine(line);
                transactions.add(transaction);
                nextId = Math.max(nextId, transaction.getId() + 1);
            }
        }
        sortTransactions();
    }

    public void clear() {
        transactions.clear();
        nextId = 1;
    }

    private void sortTransactions() {
        transactions.sort(Comparator
                .comparing(Transaction::getDate).reversed()
                .thenComparing(Transaction::getId));
    }
}
