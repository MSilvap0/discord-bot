# OrbsAlarm

Bot de lembretes e pontuacao para Discord com comandos slash.

## Comandos

- `/recompensa-lembrete intervalo:<minutos>`
- `/status-lembrete`
- `/meus-pontos`
- `/top-pontos`
- `/parar-lembrete`

## Rodar localmente

```bash
cp .env.example .env
# edite o token no arquivo .env
mvn package
./run-bot.sh
```

Ou, se preferir um atalho no estilo Node:

```bash
npm run dev
```

Os pontos ficam salvos em `data/points.properties`.

## Rodar 24h com systemd

```bash
cp .env.example .env
# edite o token no arquivo .env
mvn package
chmod +x run-bot.sh
mkdir -p ~/.config/systemd/user
cp orbsalarm.service ~/.config/systemd/user/orbsalarm.service
systemctl --user daemon-reload
systemctl --user enable --now orbsalarm.service
```

Comandos uteis:

```bash
systemctl --user status orbsalarm.service
journalctl --user -u orbsalarm.service -f
systemctl --user restart orbsalarm.service
```

## Deploy no Render

O Render nao oferece Java como runtime nativo no formulario. Para este projeto, use `Docker`.

Arquivos prontos no repo:

- `Dockerfile`
- `render.yaml`

Passos:

1. Suba o projeto para o GitHub.
2. No Render, crie um `Background Worker`.
3. Escolha o repo.
4. Em `Language`, selecione `Docker`.
5. Adicione as variaveis:
   - `DISCORD_TOKEN`
   - `DISCORD_GUILD_ID`
6. Faca o deploy.

Observacao:

- `Background Worker` no Render e pago.
- O `render.yaml` deste projeto ja esta configurado para `worker` com plano `starter`.
