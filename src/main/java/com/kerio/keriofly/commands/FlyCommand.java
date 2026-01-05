package com.kerio.keriofly.commands;

import com.kerio.keriofly.KerioFly;
import com.kerio.keriofly.managers.FlyManager;
import com.kerio.keriofly.managers.PointsManager;
import com.kerio.keriofly.managers.TicketManager;
import com.kerio.keriofly.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FlyCommand implements CommandExecutor, TabCompleter {

    private final KerioFly plugin;
    private final FlyManager flyManager;
    private final TicketManager ticketManager;
    private final EconomyManager economyManager;
    private final PointsManager pointsManager;

    public FlyCommand(KerioFly plugin) {
        this.plugin = plugin;
        this.flyManager = plugin.getFlyManager();
        this.ticketManager = plugin.getTicketManager();
        this.economyManager = plugin.getEconomyManager();
        this.pointsManager = plugin.getPointsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /kfly - 切換飛行或顯示幫助
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendHelp(sender);
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("keriofly.use")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            toggleFly(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // /kfly help - 任何人都可以查看
        if (subCommand.equals("help")) {
            sendHelp(sender);
            return true;
        }

        // /kfly reload - 管理員專用
        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("keriofly.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            plugin.getConfigManager().reloadConfigs();
            ticketManager.loadTickets();
            sender.sendMessage(plugin.getConfigManager().getMessage("config-reloaded"));
            return true;
        }

        // /kfly true/false - 普通玩家可用
        if (subCommand.equals("true") || subCommand.equals("false")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("usage"));
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("keriofly.use")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            boolean enable = subCommand.equals("true");
            setFly(player, enable);
            return true;
        }

        // /kfly buy <hours> - 普通玩家可用
        if (subCommand.equals("buy")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c只有玩家可以使用此指令！");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("keriofly.use")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§c用法: /kfly buy <小時數>");
                return true;
            }

            return handleBuyCommand(player, args[1]);
        }

        // /kfly points - 查看自己的點數 (普通玩家) 或管理點數 (管理員)
        if (subCommand.equals("points")) {
            if (args.length == 1) {
                // 查看自己的點數 - 普通玩家可用
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此指令！");
                    return true;
                }

                Player player = (Player) sender;
                if (!player.hasPermission("keriofly.use")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                long points = pointsManager.getPoints(player.getUniqueId());
                String time = pointsManager.getConvertibleTime(points);
                player.sendMessage(plugin.getConfigManager().getMessage("points-info")
                        .replace("{points}", String.valueOf(points))
                        .replace("{time}", time));
                return true;
            }

            // /kfly points add/set/reduce <player> <amount> - 管理員專用
            if (!sender.hasPermission("keriofly.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage("§c用法: /kfly points <add|set|reduce> <玩家> <點數>");
                return true;
            }

            return handlePointsCommand(sender, args);
        }

        // /kfly config - 管理員專用
        if (subCommand.equals("config")) {
            if (!sender.hasPermission("keriofly.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§c用法: /kfly config <moneycost|money|points> <值>");
                return true;
            }

            return handleConfigCommand(sender, args);
        }

        // /kfly add/set/reduce/reset <player> [time] - 管理員專用
        if (Arrays.asList("add", "set", "reduce", "reset").contains(subCommand)) {
            if (!sender.hasPermission("keriofly.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            return handleTimeCommand(sender, subCommand, args);
        }

        // /kfly ticket - 根據子指令判斷權限
        if (subCommand.equals("ticket")) {
            return handleTicketCommand(sender, args);
        }

        // 未知指令
        sender.sendMessage(plugin.getConfigManager().getMessage("usage"));
        return true;
    }

    private boolean handleBuyCommand(Player player, String hoursStr) {
        try {
            int hours = Integer.parseInt(hoursStr);

            if (hours <= 0) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                return true;
            }

            // 優先使用飛行點數
            if (economyManager.isPointsExchangeEnabled()) {
                if (pointsManager.purchaseWithPoints(player.getUniqueId(), hours)) {
                    String time = flyManager.formatTime(hours * 3600L);
                    player.sendMessage(plugin.getConfigManager().getMessage("purchase-success")
                            .replace("{time}", time)
                            .replace("{cost}", hours + " 飛行點數"));
                    return true;
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("not-enough-points")
                            .replace("{points}", String.valueOf(hours))
                            .replace("{balance}", String.valueOf(pointsManager.getPoints(player.getUniqueId()))));
                    return true;
                }
            }

            // 使用金錢購買
            if (economyManager.isMoneyExchangeEnabled()) {
                if (!economyManager.isVaultEnabled()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("vault-not-found"));
                    return true;
                }

                if (economyManager.purchaseWithMoney(player, hours)) {
                    String time = flyManager.formatTime(hours * 3600L);
                    double cost = economyManager.getCostPerHour() * hours;
                    player.sendMessage(plugin.getConfigManager().getMessage("purchase-success")
                            .replace("{time}", time)
                            .replace("{cost}", economyManager.formatMoney(cost)));
                    return true;
                } else {
                    double cost = economyManager.getCostPerHour() * hours;
                    player.sendMessage(plugin.getConfigManager().getMessage("not-enough-money")
                            .replace("{cost}", economyManager.formatMoney(cost))
                            .replace("{balance}", economyManager.formatMoney(economyManager.getBalance(player))));
                    return true;
                }
            }

            player.sendMessage("§c金錢和點數兌換功能都未啟用！");
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
        }

        return true;
    }

    private boolean handlePointsCommand(CommandSender sender, String[] args) {
        String action = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }

        try {
            long points = Long.parseLong(args[3]);
            UUID uuid = target.getUniqueId();

            switch (action) {
                case "add":
                    pointsManager.addPoints(uuid, points);
                    sender.sendMessage(plugin.getConfigManager().getMessage("points-added")
                            .replace("{player}", target.getName())
                            .replace("{points}", String.valueOf(points)));
                    break;
                case "set":
                    pointsManager.setPoints(uuid, points);
                    sender.sendMessage(plugin.getConfigManager().getMessage("points-set")
                            .replace("{player}", target.getName())
                            .replace("{points}", String.valueOf(points)));
                    break;
                case "reduce":
                    pointsManager.reducePoints(uuid, points);
                    sender.sendMessage(plugin.getConfigManager().getMessage("points-reduced")
                            .replace("{player}", target.getName())
                            .replace("{points}", String.valueOf(points)));
                    break;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
        }

        return true;
    }

    private boolean handleConfigCommand(CommandSender sender, String[] args) {
        String option = args[1].toLowerCase();
        String value = args[2];

        switch (option) {
            case "moneycost":
                try {
                    double cost = Double.parseDouble(value);
                    economyManager.setCostPerHour(cost);
                    sender.sendMessage(plugin.getConfigManager().getMessage("money-cost-set")
                            .replace("{cost}", String.valueOf(cost)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                }
                break;
            case "money":
                boolean moneyEnabled = Boolean.parseBoolean(value);
                economyManager.setMoneyExchangeEnabled(moneyEnabled);
                String status = moneyEnabled ? plugin.getConfigManager().getMessageWithoutPrefix("enabled")
                        : plugin.getConfigManager().getMessageWithoutPrefix("disabled");
                sender.sendMessage(plugin.getConfigManager().getMessage("money-exchange-toggled")
                        .replace("{status}", status));
                break;
            case "points":
                boolean pointsEnabled = Boolean.parseBoolean(value);
                economyManager.setPointsExchangeEnabled(pointsEnabled);
                String pointsStatus = pointsEnabled ? plugin.getConfigManager().getMessageWithoutPrefix("enabled")
                        : plugin.getConfigManager().getMessageWithoutPrefix("disabled");
                sender.sendMessage(plugin.getConfigManager().getMessage("points-exchange-toggled")
                        .replace("{status}", pointsStatus));
                break;
            default:
                sender.sendMessage("§c未知的配置選項！");
        }

        return true;
    }

    private boolean handleTimeCommand(CommandSender sender, String subCommand, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }

        if (subCommand.equals("reset")) {
            flyManager.setFlyTime(target.getUniqueId(), 0);
            sender.sendMessage(plugin.getConfigManager().getMessage("time-reset")
                    .replace("{player}", target.getName()));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage"));
            return true;
        }

        long time = flyManager.parseTime(args[2]);
        if (time < 0) {
            sender.sendMessage(plugin.getConfigManager().getMessage("invalid-time"));
            return true;
        }

        UUID uuid = target.getUniqueId();

        switch (subCommand) {
            case "add":
                flyManager.addFlyTime(uuid, time);
                sender.sendMessage(plugin.getConfigManager().getMessage("time-added")
                        .replace("{player}", target.getName())
                        .replace("{time}", flyManager.formatTime(time)));
                break;
            case "set":
                flyManager.setFlyTime(uuid, time);
                sender.sendMessage(plugin.getConfigManager().getMessage("time-set")
                        .replace("{player}", target.getName())
                        .replace("{time}", flyManager.formatTime(time)));
                break;
            case "reduce":
                flyManager.reduceFlyTime(uuid, time);
                sender.sendMessage(plugin.getConfigManager().getMessage("time-reduced")
                        .replace("{player}", target.getName())
                        .replace("{time}", flyManager.formatTime(time)));
                break;
        }

        return true;
    }

    private boolean handleTicketCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /kfly ticket <create|remove|edit|give|open|list>");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                if (!sender.hasPermission("keriofly.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage("§c用法: /kfly ticket create <名稱> <時間>");
                    return true;
                }

                String createName = args[2];
                long createTime = flyManager.parseTime(args[3]);

                if (createTime < 0) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("invalid-time"));
                    return true;
                }

                if (ticketManager.createTicket(createName, createTime)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("ticket-created")
                            .replace("{name}", createName)
                            .replace("{time}", flyManager.formatTime(createTime)));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getMessage("ticket-already-exists")
                            .replace("{name}", createName));
                }
                break;

            case "remove":
                if (!sender.hasPermission("keriofly.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage("§c用法: /kfly ticket remove <名稱>");
                    return true;
                }

                String removeName = args[2];

                if (ticketManager.removeTicket(removeName)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("ticket-removed")
                            .replace("{name}", removeName));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getMessage("ticket-not-found")
                            .replace("{name}", removeName));
                }
                break;

            case "edit":
                if (!sender.hasPermission("keriofly.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage("§c用法: /kfly ticket edit <名稱> [time <時間>]");
                    return true;
                }

                String editName = args[2];

                if (!ticketManager.hasTicket(editName)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("ticket-not-found")
                            .replace("{name}", editName));
                    return true;
                }

                if (args.length >= 5 && args[3].equalsIgnoreCase("time")) {
                    long newTime = flyManager.parseTime(args[4]);
                    if (newTime < 0) {
                        sender.sendMessage(plugin.getConfigManager().getMessage("invalid-time"));
                        return true;
                    }

                    plugin.getConfigManager().getTicketConfig().set("tickets." + editName + ".time", newTime);
                    plugin.getConfigManager().saveConfigs();
                    ticketManager.loadTickets();

                    sender.sendMessage(plugin.getConfigManager().getMessage("ticket-edited")
                            .replace("{name}", editName));
                } else {
                    sender.sendMessage("§e提示: 在 ticket.yml 中編輯票券 '" + editName + "' 的樣式");
                    sender.sendMessage("§e然後使用 /kfly reload 重新載入配置");
                }
                break;

            case "give":
                if (!sender.hasPermission("keriofly.ticket.give")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage("§c用法: /kfly ticket give <名稱> <玩家> [數量]");
                    return true;
                }

                String giveName = args[2];
                Player giveTarget = Bukkit.getPlayer(args[3]);

                if (giveTarget == null) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                    return true;
                }

                if (!ticketManager.hasTicket(giveName)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("ticket-not-found")
                            .replace("{name}", giveName));
                    return true;
                }

                int amount = 1;
                if (args.length >= 5) {
                    try {
                        amount = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                        return true;
                    }
                }

                ticketManager.giveTicket(giveTarget, giveName, amount);
                sender.sendMessage(plugin.getConfigManager().getMessage("ticket-given")
                        .replace("{player}", giveTarget.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{ticket}", giveName));
                break;

            case "open":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此指令！");
                    return true;
                }

                if (!sender.hasPermission("keriofly.ticket.gui")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                ticketManager.openTicketGUI((Player) sender);
                break;

            case "list":
                if (!sender.hasPermission("keriofly.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                String tickets = String.join(", ", ticketManager.getTicketNames());
                sender.sendMessage(plugin.getConfigManager().getMessage("ticket-list")
                        .replace("{tickets}", tickets.isEmpty() ? "無" : tickets));
                break;

            default:
                sender.sendMessage("§c未知的票券指令!");
        }

        return true;
    }

    private void toggleFly(Player player) {
        UUID uuid = player.getUniqueId();
        boolean newState = !flyManager.isFlyEnabled(uuid);

        if (newState) {
            long time = flyManager.getFlyTime(uuid);
            if (time <= 0 && !player.hasPermission("keriofly.unlimited")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-time"));
                return;
            }

            flyManager.setFlyEnabled(player, true);
            player.sendMessage(plugin.getConfigManager().getMessage("fly-enabled")
                    .replace("{time}",
                            player.hasPermission("keriofly.unlimited") ? "無限" : flyManager.formatTime(time)));
        } else {
            flyManager.setFlyEnabled(player, false);
            player.sendMessage(plugin.getConfigManager().getMessage("fly-disabled"));
        }
    }

    private void setFly(Player player, boolean enable) {
        UUID uuid = player.getUniqueId();

        if (enable) {
            long time = flyManager.getFlyTime(uuid);
            if (time <= 0 && !player.hasPermission("keriofly.unlimited")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-time"));
                return;
            }

            flyManager.setFlyEnabled(player, true);
            player.sendMessage(plugin.getConfigManager().getMessage("fly-enabled")
                    .replace("{time}",
                            player.hasPermission("keriofly.unlimited") ? "無限" : flyManager.formatTime(time)));
        } else {
            flyManager.setFlyEnabled(player, false);
            player.sendMessage(plugin.getConfigManager().getMessage("fly-disabled"));
        }
    }

    private void sendHelp(CommandSender sender) {
        List<String> help = plugin.getConfigManager().getMessagesConfig().getStringList("help");
        for (String line : help) {
            sender.sendMessage(plugin.getConfigManager().colorize(line));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 普通玩家只能看到基本指令
            if (sender.hasPermission("keriofly.use")) {
                completions.addAll(Arrays.asList("true", "false", "help"));

                // 如果啟用了購買功能，顯示 buy
                if (economyManager.isMoneyExchangeEnabled() || economyManager.isPointsExchangeEnabled()) {
                    completions.add("buy");
                }

                // 如果啟用了點數系統，顯示 points (查看自己的)
                if (economyManager.isPointsExchangeEnabled()) {
                    completions.add("points");
                }
            }

            // 管理員可以看到所有指令
            if (sender.hasPermission("keriofly.admin")) {
                completions.addAll(Arrays.asList("add", "set", "reduce", "reset", "config", "reload"));

                // 確保 points 存在 (用於管理他人點數)
                if (!completions.contains("points")) {
                    completions.add("points");
                }
            }

            // 票券相關權限
            if (sender.hasPermission("keriofly.admin") ||
                    sender.hasPermission("keriofly.ticket.give") ||
                    sender.hasPermission("keriofly.ticket.gui")) {
                completions.add("ticket");
            }

        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "ticket":
                    // 根據權限顯示不同的子指令
                    if (sender.hasPermission("keriofly.admin")) {
                        completions.addAll(Arrays.asList("create", "remove", "edit", "list"));
                    }
                    if (sender.hasPermission("keriofly.ticket.give")) {
                        completions.add("give");
                    }
                    if (sender.hasPermission("keriofly.ticket.gui")) {
                        completions.add("open");
                    }
                    break;

                case "points":
                    // 管理員才能看到子指令
                    if (sender.hasPermission("keriofly.admin")) {
                        completions.addAll(Arrays.asList("add", "set", "reduce"));
                    }
                    break;

                case "config":
                    if (sender.hasPermission("keriofly.admin")) {
                        completions.addAll(Arrays.asList("moneycost", "money", "points"));
                    }
                    break;

                case "add":
                case "set":
                case "reduce":
                case "reset":
                    if (sender.hasPermission("keriofly.admin")) {
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList());
                    }
                    break;

                case "buy":
                    if (sender.hasPermission("keriofly.use")) {
                        completions.addAll(Arrays.asList("1", "2", "3", "5", "10", "24"));
                    }
                    break;
            }

        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("ticket")) {
                if (args[1].equalsIgnoreCase("give") && sender.hasPermission("keriofly.ticket.give")) {
                    return new ArrayList<>(ticketManager.getTicketNames());
                } else if ((args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("edit"))
                        && sender.hasPermission("keriofly.admin")) {
                    return new ArrayList<>(ticketManager.getTicketNames());
                } else if (args[1].equalsIgnoreCase("create") && sender.hasPermission("keriofly.admin")) {
                    return null; // 讓玩家自由輸入名稱
                }
            } else if (args[0].equalsIgnoreCase("points") && sender.hasPermission("keriofly.admin")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if (sender.hasPermission("keriofly.admin") &&
                    !args[0].equalsIgnoreCase("reset") &&
                    !args[0].equalsIgnoreCase("config")) {
                completions.addAll(Arrays.asList("1d", "1h", "1m", "1s"));
            }

        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("ticket") && args[1].equalsIgnoreCase("give")
                    && sender.hasPermission("keriofly.ticket.give")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("ticket") && args[1].equalsIgnoreCase("create")
                    && sender.hasPermission("keriofly.admin")) {
                completions.addAll(Arrays.asList("1d", "1h", "30m", "1m"));
            } else if (args[0].equalsIgnoreCase("points") && sender.hasPermission("keriofly.admin")) {
                completions.addAll(Arrays.asList("1", "5", "10", "50", "100"));
            }

        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("ticket") && args[1].equalsIgnoreCase("give")
                    && sender.hasPermission("keriofly.ticket.give")) {
                completions.addAll(Arrays.asList("1", "5", "10", "32", "64"));
            }
        }

        // 過濾並返回符合輸入的補全選項
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}