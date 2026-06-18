import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 하나의 수입/지출 내역을 표현하는 클래스.
 * 파일 저장을 위해 CSV 변환 기능도 함께 제공한다.
 */
public class Transaction {
    public enum Type {
        INCOME("수입"),
        EXPENSE("지출");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static Type fromLabel(String value) {
            if (value == null) {
                throw new IllegalArgumentException("구분 값이 비어 있습니다.");
            }
            String trimmed = value.trim();
            for (Type type : Type.values()) {
                if (type.name().equalsIgnoreCase(trimmed) || type.label.equals(trimmed)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("구분은 수입 또는 지출이어야 합니다: " + value);
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private int id;
    private LocalDate date;
    private Type type;
    private long amount;
    private String category;
    private String memo;

    public Transaction(int id, LocalDate date, Type type, long amount, String category, String memo) {
        setId(id);
        setDate(date);
        setType(type);
        setAmount(amount);
        setCategory(category);
        setMemo(memo);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID는 1 이상이어야 합니다.");
        }
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜를 입력해야 합니다.");
        }
        this.date = date;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("수입/지출 구분을 선택해야 합니다.");
        }
        this.type = type;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 1원 이상이어야 합니다.");
        }
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리를 입력해야 합니다.");
        }
        this.category = category.trim();
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo == null ? "" : memo.trim();
    }

    public String getDateText() {
        return date.format(DATE_FORMATTER);
    }

    public String[] toTableRow() {
        return new String[] {
                String.valueOf(id),
                getDateText(),
                type.getLabel(),
                String.valueOf(amount),
                category,
                memo
        };
    }

    public String toCsvLine() {
        return id + "," + getDateText() + "," + type.name() + "," + amount + ","
                + escapeCsv(category) + "," + escapeCsv(memo);
    }

    public static Transaction fromCsvLine(String line) {
        String[] fields = splitCsv(line);
        if (fields.length != 6) {
            throw new IllegalArgumentException("CSV 필드 개수가 올바르지 않습니다: " + line);
        }
        try {
            int id = Integer.parseInt(fields[0].trim());
            LocalDate date = LocalDate.parse(fields[1].trim(), DATE_FORMATTER);
            Type type = Type.fromLabel(fields[2].trim());
            long amount = Long.parseLong(fields[3].trim());
            String category = unescapeCsv(fields[4]);
            String memo = unescapeCsv(fields[5]);
            return new Transaction(id, date, type, amount, category, memo);
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new IllegalArgumentException("CSV 값을 읽을 수 없습니다: " + line, ex);
        }
    }

    private static String escapeCsv(String text) {
        if (text == null) {
            return "";
        }
        boolean needQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        String escaped = text.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static String unescapeCsv(String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        if (result.length() >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        return result.replace("\"\"", "\"");
    }

    private static String[] splitCsv(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append("\"\"");
                    i++;
                } else {
                    inQuotes = !inQuotes;
                    current.append(ch);
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
