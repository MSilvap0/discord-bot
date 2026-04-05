import net.dv8tion.jda.api.entities.User;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ReminderManager {
    private static final Map<Long, ScheduledFuture<?>> userReminders = new ConcurrentHashMap<>();
    private static final Map<Long, Map<Long, Integer>> guildPoints = new ConcurrentHashMap<>();
    private static final Map<Long, String> userNames = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private static final Path dataFile = Paths.get("data", "points.properties");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    static {
        loadPoints();
    }

    public record UserScore(long userId, String userName, int points) {}

    /**
     * Inicia um lembrete para o usuário com intervalo especificado em minutos.
     */
    public static void startReminder(long userId, int intervalMinutes, User user, Runnable onReminder) {
        // Cancela lembrete anterior se existir
        stopReminder(userId);
        updateUserName(userId, user.getName());

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                onReminder.run();
            } catch (Exception e) {
                System.err.println("Erro no lembrete do usuário " + userId + ": " + e.getMessage());
            }
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);

        userReminders.put(userId, task);
        System.out.println("Lembrete iniciado para " + user.getName() + " (ID: " + userId + ") a cada " + intervalMinutes + " min");
    }

    /**
     * Para o lembrete do usuário.
     */
    public static boolean stopReminder(long userId) {
        ScheduledFuture<?> task = userReminders.remove(userId);
        if (task != null && !task.isDone()) {
            task.cancel(true);
            System.out.println("Lembrete cancelado para usuário " + userId);
            return true;
        }
        return false;
    }

    /**
     * Verifica se o usuário tem lembrete ativo.
     */
    public static boolean hasReminder(long userId) {
        ScheduledFuture<?> task = userReminders.get(userId);
        return task != null && !task.isDone() && !task.isCancelled();
    }

    /**
     * Adiciona pontos ao usuário.
     */
    public static int addPoints(long guildId, long userId, int points) {
        return addPoints(guildId, userId, getUserName(userId), points);
    }

    /**
     * Adiciona pontos ao usuário e salva no arquivo local.
     */
    public static synchronized int addPoints(long guildId, long userId, String userName, int points) {
        userNames.put(userId, userName);
        Map<Long, Integer> pointsByUser = guildPoints.computeIfAbsent(guildId, ignored -> new ConcurrentHashMap<>());
        int total = pointsByUser.merge(userId, points, Integer::sum);
        savePoints();
        return total;
    }

    /**
     * Retorna a pontuação atual do usuário.
     */
    public static int getPoints(long guildId, long userId) {
        Map<Long, Integer> pointsByUser = guildPoints.get(guildId);
        if (pointsByUser == null) {
            return 0;
        }
        return pointsByUser.getOrDefault(userId, 0);
    }

    /**
     * Retorna a mensagem de lembrete formatada.
     */
    public static String getReminderMessage(long guildId, long userId) {
        int currentPoints = getPoints(guildId, userId);
        return "⏰ **Lembrete de Recompensa!** ⏰\n" +
                "Hora atual: `" + formatter.format(Instant.now()) + "`\n" +
                "Seus pontos atuais: **" + currentPoints + "**\n" +
                "Verifique suas recompensas! ✅";
    }

    public static synchronized void updateUserName(long userId, String userName) {
        userNames.put(userId, userName);
        savePoints();
    }

    public static String getUserName(long userId) {
        return userNames.getOrDefault(userId, "Usuario " + userId);
    }

    public static List<UserScore> getTopScores(long guildId, int limit) {
        List<UserScore> scores = new ArrayList<>();
        Map<Long, Integer> pointsByUser = guildPoints.get(guildId);
        if (pointsByUser == null) {
            return scores;
        }

        for (Map.Entry<Long, Integer> entry : pointsByUser.entrySet()) {
            long userId = entry.getKey();
            scores.add(new UserScore(userId, getUserName(userId), entry.getValue()));
        }

        scores.sort(Comparator.comparingInt(UserScore::points).reversed()
                .thenComparing(UserScore::userName, String.CASE_INSENSITIVE_ORDER));

        if (scores.size() > limit) {
            return scores.subList(0, limit);
        }
        return scores;
    }

    private static synchronized void loadPoints() {
        if (!Files.exists(dataFile)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(dataFile)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Erro ao carregar pontuacoes: " + e.getMessage());
            return;
        }

        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("user.") || !key.endsWith(".points")) {
                continue;
            }

            String userIdText = key.substring("user.".length(), key.length() - ".points".length());
            try {
                long userId = Long.parseLong(userIdText);
                int points = Integer.parseInt(properties.getProperty(key, "0"));
                String userName = properties.getProperty("user." + userId + ".name", "Usuario " + userId);
                userNames.put(userId, userName);
            } catch (NumberFormatException e) {
                System.err.println("Registro de pontos invalido para chave: " + key);
            }
        }

        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("guild.") || !key.endsWith(".points")) {
                continue;
            }

            String[] parts = key.split("\\.");
            if (parts.length != 5 || !"guild".equals(parts[0]) || !"user".equals(parts[2]) || !"points".equals(parts[4])) {
                continue;
            }

            try {
                long guildId = Long.parseLong(parts[1]);
                long userId = Long.parseLong(parts[3]);
                int points = Integer.parseInt(properties.getProperty(key, "0"));
                guildPoints.computeIfAbsent(guildId, ignored -> new ConcurrentHashMap<>()).put(userId, points);
            } catch (NumberFormatException e) {
                System.err.println("Registro de pontos invalido para chave: " + key);
            }
        }
    }

    private static synchronized void savePoints() {
        Properties properties = new Properties();
        for (Map.Entry<Long, String> entry : userNames.entrySet()) {
            long userId = entry.getKey();
            properties.setProperty("user." + userId + ".name", getUserName(userId));
        }

        for (Map.Entry<Long, Map<Long, Integer>> guildEntry : guildPoints.entrySet()) {
            long guildId = guildEntry.getKey();
            for (Map.Entry<Long, Integer> userEntry : guildEntry.getValue().entrySet()) {
                long userId = userEntry.getKey();
                properties.setProperty("guild." + guildId + ".user." + userId + ".points", String.valueOf(userEntry.getValue()));
            }
        }

        try {
            Files.createDirectories(dataFile.getParent());
            try (OutputStream output = Files.newOutputStream(dataFile)) {
                properties.store(output, "OrbsAlarm points");
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar pontuacoes: " + e.getMessage());
        }
    }
}
