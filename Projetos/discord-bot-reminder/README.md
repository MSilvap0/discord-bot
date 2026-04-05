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
