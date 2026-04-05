import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Main {
    public static void main(String[] args) {
        String token = System.getenv("DISCORD_TOKEN");
        String guildId = System.getenv("DISCORD_GUILD_ID");
        if (token == null || token.isEmpty()) {
            System.err.println("Erro: Defina a variável de ambiente DISCORD_TOKEN com o token do seu bot.");
            System.exit(1);
        }

        try {
            JDA jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.of(Activity.ActivityType.PLAYING, "Lembretes! /recompensa-lembrete"))
                    .addEventListeners(new ReminderCommand())
                    .build()
                    .awaitReady();

            if (guildId != null && !guildId.isBlank()) {
                Guild guild = jda.getGuildById(guildId);
                if (guild == null) {
                    System.err.println("❌ Não encontrei o servidor com DISCORD_GUILD_ID=" + guildId);
                    System.err.println("   Confirme se o bot está nesse servidor e se o ID está correto.");
                } else {
                    guild.updateCommands()
                            .addCommands(
                                    Commands.slash("recompensa-lembrete", "Inicia lembretes de recompensa em intervalos definidos")
                                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER, "intervalo", "Intervalo em minutos (1-1440)", true),
                                    Commands.slash("parar-lembrete", "Para seus lembretes de recompensa"),
                                    Commands.slash("status-lembrete", "Mostra se você tem lembretes ativos"),
                                    Commands.slash("meus-pontos", "Mostra quantos pontos voce acumulou"),
                                    Commands.slash("top-pontos", "Mostra o ranking de pontos do servidor")
                            )
                            .queue();

                    System.out.println("⚡ Comandos registrados no servidor " + guild.getName() + " (" + guildId + ").");
                }
            } else {
                jda.updateCommands()
                        .addCommands(
                                Commands.slash("recompensa-lembrete", "Inicia lembretes de recompensa em intervalos definidos")
                                        .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER, "intervalo", "Intervalo em minutos (1-1440)", true),
                                Commands.slash("parar-lembrete", "Para seus lembretes de recompensa"),
                                Commands.slash("status-lembrete", "Mostra se você tem lembretes ativos"),
                                Commands.slash("meus-pontos", "Mostra quantos pontos voce acumulou"),
                                Commands.slash("top-pontos", "Mostra o ranking de pontos do servidor")
                        )
                        .queue();

                System.out.println("📝 Comandos globais registrados. Aguarde sincronização (~1h para aparecerem).");
            }

            System.out.println("✅ Bot iniciado com sucesso! ID: " + jda.getSelfUser().getId());
            System.out.println("🤖 Bot online e pronto para receber slash commands.");

            // Keep alive
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("❌ Erro ao iniciar o bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
