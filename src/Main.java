import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("========== ЧАСТЬ 1: ИССЛЕДОВАНИЕ ХЕШ-ФУНКЦИЙ ==========\n");
        investigateDigests();

        System.out.println("\n========== ЧАСТЬ 2: ПРОСТОЙ БЛОКЧЕЙН ==========\n");
        demonstrateBlockchain();
    }

    // ---------- Часть 1: Исследование хеш-функций ----------
    private static void investigateDigests() {
        String[] algorithms = {"MD5", "SHA-1", "SHA-256"};
        String[] testStrings = {
                "",                     // пустая строка
                "Hello",                // короткая
                repeat("Hello", 1000),   // длинная (повтор)
                "Hello",                // дубликат короткой
                "World",                // другая строка
                "hello world",          // фраза
                "world hello",          // перестановка слов
                "listen",               // анаграмма 1
                "silent"                // анаграмма 2
        };

        for (String algo : algorithms) {
            System.out.println("--- Алгоритм: " + algo + " ---");
            try {
                MessageDigest md = MessageDigest.getInstance(algo);
                System.out.printf("%-20s | %-10s | %s%n", "Строка (первые 20 симв.)", "Длина хеша (байт)", "Хеш (hex)");
                System.out.println("--------------------------------------------------------");

                for (String s : testStrings) {
                    byte[] digest = md.digest(s.getBytes());
                    String hex = bytesToHex(digest);
                    // Обрезаем строку для вывода (первые 20 символов)
                    String shortStr = s.length() > 20 ? s.substring(0, 20) + "..." : s;
                    System.out.printf("%-20s | %-10d | %s%n", shortStr, digest.length, hex);
                }
                System.out.println();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Алгоритм " + algo + " не поддерживается: " + e.getMessage());
            }
        }
    }


    // Повторение строки n раз
    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    // Преобразование байтов в шестнадцатеричную строку с ведущими нулями
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ---------- Часть 2: Простой блокчейн ----------
    static class Block {
        private String previousHash;
        private String data;
        private long timestamp;
        private int nonce;
        private String hash;

        public Block(String data, String previousHash) {
            this.data = data;
            this.previousHash = previousHash;
            this.timestamp = System.currentTimeMillis();
            this.hash = calculateHash();
        }

        // Вычисление хеша блока (SHA-256)
        public String calculateHash() {
            String input = previousHash + data + timestamp + nonce;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(input.getBytes());
                return bytesToHex(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        // Майнинг блока: подбор nonce так, чтобы хеш начинался с заданного количества нулей
        public void mineBlock(int difficulty) {
            String target = new String(new char[difficulty]).replace('\0', '0'); // строка из difficulty нулей
            while (!hash.substring(0, difficulty).equals(target)) {
                nonce++;
                hash = calculateHash();
            }
            System.out.println("Блок добавлен (mined): nonce = " + nonce + ", hash = " + hash);
        }

        // Геттеры (необязательно, но для полноты)
        public String getHash() { return hash; }
        public String getPreviousHash() { return previousHash; }
        public String getData() { return data; }
    }

    static class SimpleBlockchain {
        private List<Block> chain;
        private int difficulty;

        public SimpleBlockchain(int difficulty) {
            this.difficulty = difficulty;
            chain = new ArrayList<>();
            // Создаём genesis блок (предыдущий хеш = "0")
            chain.add(new Block("Genesis Block", "0"));
        }

        public void addBlock(Block block) {
            block.previousHash = chain.get(chain.size() - 1).getHash();
            block.mineBlock(difficulty);
            chain.add(block);
        }

        public boolean isChainValid() {
            for (int i = 1; i < chain.size(); i++) {
                Block current = chain.get(i);
                Block previous = chain.get(i - 1);

                // Проверяем, что хеш текущего блока корректен
                if (!current.getHash().equals(current.calculateHash())) {
                    System.out.println("Неверный хеш блока " + i);
                    return false;
                }
                // Проверяем, что previousHash совпадает с хешем предыдущего блока
                if (!current.getPreviousHash().equals(previous.getHash())) {
                    System.out.println("Неверная ссылка на предыдущий блок " + i);
                    return false;
                }
            }
            return true;
        }

        public void printChain() {
            for (int i = 0; i < chain.size(); i++) {
                Block b = chain.get(i);
                System.out.printf("Блок %d:\n", i);
                System.out.println("  Data: " + b.getData());
                System.out.println("  PreviousHash: " + b.getPreviousHash());
                System.out.println("  Hash: " + b.getHash());
                System.out.println();
            }
        }
    }

    private static void demonstrateBlockchain() {
        // Создаём блокчейн со сложностью 4 (т.е. хеш должен начинаться с "0000")
        SimpleBlockchain blockchain = new SimpleBlockchain(4);

        System.out.println("Майним первый блок (данные: Перевод 100 BTC)...");
        blockchain.addBlock(new Block("Перевод 100 BTC", ""));

        System.out.println("Майним второй блок (данные: Перевод 50 BTC)...");
        blockchain.addBlock(new Block("Перевод 50 BTC", ""));

        System.out.println("\n--- Цепочка блоков ---");
        blockchain.printChain();

        System.out.println("--- Проверка целостности цепочки ---");
        boolean valid = blockchain.isChainValid();
        System.out.println("Цепочка валидна? " + valid);

        // Попытка подделки: изменяем данные во втором блоке
        System.out.println("\n--- Попытка подделки: изменяем данные блока 1 (индекс 1) ---");
        Block tamperedBlock = blockchain.chain.get(1);
        tamperedBlock.data = "Перевод 999 BTC";  // меняем данные
        // Хеш блока теперь не соответствует новым данным (если не пересчитать)
        System.out.println("Хеш блока после изменения (без пересчёта): " + tamperedBlock.getHash());
        System.out.println("Правильный хеш для новых данных: " + tamperedBlock.calculateHash());

        valid = blockchain.isChainValid();
        System.out.println("Цепочка после подделки валидна? " + valid);
    }
}