import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReminderCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        User user = event.getUser();

        try {
            switch (commandName) {
                case "recompensa-lembrete" -> handleStartReminder(event);
                case "parar-lembrete" -> handleStopReminder(event, user);
                case "status-lembrete" -> handleStatusReminder(event, user);
                case "meus-pontos" -> handlePoints(event, user);
                case "top-pontos" -> handleTopPoints(event);
                default -> event.reply("Comando desconhecido.").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            event.reply("Erro interno: " + e.getMessage()).setEphemeral(true).queue();
            System.err.println("Erro no comando " + commandName + " de " + user.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStartReminder(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("❌ Esse comando só pode ser usado dentro de um servidor.").setEphemeral(true).queue();
            return;
        }

        OptionMapping intervalOption = event.getOption("intervalo");
        if (intervalOption == null) {
            event.reply("❌ Intervalo é obrigatório.").setEphemeral(true).queue();
            return;
        }

        int interval = intervalOption.getAsInt();
        if (interval < 1 || interval > 1440) {
            event.reply("❌ Intervalo deve ser entre 1 e 1440 minutos (24h).").setEphemeral(true).queue();
            return;
        }

        ReminderManager.updateUserName(event.getUser().getIdLong(), event.getUser().getName());
        long guildId = event.getGuild().getIdLong();

        ReminderManager.startReminder(event.getUser().getIdLong(), interval, event.getUser(), () -> {
            int updatedPoints = ReminderManager.addPoints(guildId, event.getUser().getIdLong(), event.getUser().getName(), 10);
            event.getChannel().sendMessage(
                    "<@" + event.getUser().getId() + ">\n" +
                    ReminderManager.getReminderMessage(guildId, event.getUser().getIdLong()) +
                    "\n🏆 Você ganhou **10 pontos**. Total: **" + updatedPoints + "**"
            ).queue();
        });

        event.reply("✅ Lembrete iniciado! Avisarei você a cada **" + interval + " minutos**.").setEphemeral(true).queue();
    }

    private void handleStopReminder(SlashCommandInteractionEvent event, User user) {
        boolean stopped = ReminderManager.stopReminder(user.getIdLong());
        if (stopped) {
            event.reply("🛑 Lembretes cancelados com sucesso.").setEphemeral(true).queue();
        } else {
            event.reply("ℹ️ Nenhum lembrete ativo para você.").setEphemeral(true).queue();
        }
    }

    private void handleStatusReminder(SlashCommandInteractionEvent event, User user) {
        if (event.getGuild() == null) {
            event.reply("❌ Esse comando só pode ser usado dentro de um servidor.").setEphemeral(true).queue();
            return;
        }

        ReminderManager.updateUserName(user.getIdLong(), user.getName());
        boolean active = ReminderManager.hasReminder(user.getIdLong());
        String status = active ? "✅ **ATIVO**" : "❌ **INATIVO**";
        int points = ReminderManager.getPoints(event.getGuild().getIdLong(), user.getIdLong());
        event.reply("📊 **Status do seu lembrete:** " + status + "\n🏆 **Seus pontos:** " + points)
                .setEphemeral(true)
                .queue();
    }

    private void handlePoints(SlashCommandInteractionEvent event, User user) {
        if (event.getGuild() == null) {
            event.reply("❌ Esse comando só pode ser usado dentro de um servidor.").setEphemeral(true).queue();
            return;
        }

        ReminderManager.updateUserName(user.getIdLong(), user.getName());
        int points = ReminderManager.getPoints(event.getGuild().getIdLong(), user.getIdLong());
        event.reply("🏆 **Seus pontos atuais:** " + points).setEphemeral(true).queue();
    }

    private void handleTopPoints(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("❌ Esse comando só pode ser usado dentro de um servidor.").setEphemeral(true).queue();
            return;
        }

        List<ReminderManager.UserScore> topScores = ReminderManager.getTopScores(event.getGuild().getIdLong(), 10);
        if (topScores.isEmpty()) {
            event.reply("🏆 Ainda nao ha pontos registrados.").setEphemeral(true).queue();
            return;
        }

        StringBuilder ranking = new StringBuilder("🏆 **Ranking de Pontos**\n");
        for (int i = 0; i < topScores.size(); i++) {
            ReminderManager.UserScore score = topScores.get(i);
            ranking.append(i + 1)
                    .append(". ")
                    .append(score.userName())
                    .append(" - **")
                    .append(score.points())
                    .append("** pontos\n");
        }

        event.reply(ranking.toString().trim()).queue();
    }
}
