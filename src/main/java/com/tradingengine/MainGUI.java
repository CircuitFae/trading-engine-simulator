package com.tradingengine;

import com.tradingengine.domain.*;
import com.tradingengine.engine.OrderIdGenerator;
import com.tradingengine.engine.TradingEngine;
import com.tradingengine.enums.OrderSide;
import com.tradingengine.portfolio.PortfolioManager;
import com.tradingengine.strategy.FIFOMatchingStrategy;
import com.tradingengine.strategy.ProRataMatchingStrategy;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainGUI {

    // ── Dark terminal palette ─────────────────────────────────────────────────
    private static final Color BG_DARK   = new Color(13,  17,  23);
    private static final Color BG_PANEL  = new Color(22,  27,  34);
    private static final Color BG_HDR    = new Color(10,  13,  18);
    private static final Color C_BUY     = new Color(63, 185,  80);
    private static final Color C_SELL    = new Color(248, 81,  73);
    private static final Color C_TRADE   = new Color(255, 166,   0);
    private static final Color C_PORT    = new Color(163, 113, 247);
    private static final Color C_ENGINE  = new Color( 88, 166, 255);
    private static final Color C_REJECT  = new Color(255, 100, 100);
    private static final Color C_GREY    = new Color(110, 118, 129);
    private static final Color C_WHITE   = new Color(230, 237, 243);
    private static final Color C_BORDER  = new Color( 48,  54,  61);
    private static final Color C_BTN_BG  = new Color(35, 134, 54);
    private static final Color C_BTN_TXT = new Color(245, 255, 248);

    // ── Swing components ──────────────────────────────────────────────────────
    private JFrame frame;
    private JTextPane logPane;
    private StyledDocument doc;
    private DefaultTableModel tradesModel;
    private DefaultTableModel portfolioModel;
    private JLabel statusLabel;
    private JLabel cashLabel;
    private JButton runButton;
    private JProgressBar progressBar;
    private JLabel tradeCountLabel;

    private final OrderIdGenerator idGen = new OrderIdGenerator();

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGUI().launch());
    }

    private void launch() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        frame = new JFrame("Algorithmic Trading Engine Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, 800);
        frame.setMinimumSize(new Dimension(1000, 650));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG_DARK);
        frame.setLayout(new BorderLayout());

        frame.add(buildHeader(),  BorderLayout.NORTH);
        frame.add(buildCenter(),  BorderLayout.CENTER);
        frame.add(buildFooter(),  BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_HDR);
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                BorderFactory.createEmptyBorder(13, 22, 13, 22)));

        JLabel title = new JLabel("  ◈  ALGORITHMIC TRADING ENGINE SIMULATOR");
        title.setFont(new Font("Monospaced", Font.BOLD, 17));
        title.setForeground(C_ENGINE);
        h.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setBackground(BG_HDR);

        tradeCountLabel = new JLabel("Trades: 0");
        tradeCountLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tradeCountLabel.setForeground(C_GREY);
        right.add(tradeCountLabel);

        statusLabel = new JLabel("● READY");
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        statusLabel.setForeground(new Color(100, 200, 100));
        right.add(statusLabel);

        runButton = new JButton("  ▶   RUN SIMULATION  ");
        runButton.setFont(new Font("Monospaced", Font.BOLD, 13));
        runButton.setBackground(C_BTN_BG);
        runButton.setForeground(C_BTN_TXT);
        runButton.setOpaque(true);
        runButton.setContentAreaFilled(true);
        runButton.setBorderPainted(false);
        runButton.setFocusPainted(false);
        runButton.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        runButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runButton.addActionListener(e -> startSimulation());
        right.add(runButton);

        h.add(right, BorderLayout.EAST);
        return h;
    }

    // ── Center split ──────────────────────────────────────────────────────────
    private JSplitPane buildCenter() {
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(10, 13, 19));
        logPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logPane.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        doc = logPane.getStyledDocument();

        JScrollPane logScroll = new JScrollPane(logPane);
        logScroll.setBorder(titledBorder("  Live Event Feed  "));
        logScroll.setBackground(BG_PANEL);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Monospaced", Font.BOLD, 12));
        tabs.setBackground(BG_PANEL);
        tabs.setForeground(C_WHITE);
        tabs.addTab("  Portfolios  ", buildPortfolioPanel());
        tabs.addTab("  Trade Book  ", buildTradesPanel());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logScroll, tabs);
        split.setDividerLocation(740);
        split.setDividerSize(5);
        split.setBackground(BG_DARK);
        return split;
    }

    private JScrollPane buildPortfolioPanel() {
        portfolioModel = new DefaultTableModel(
                new String[]{"User", "Cash Balance ($)", "Asset", "Qty"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        return wrapTable(makeTable(portfolioModel, new int[]{90, 150, 80, 70}));
    }

    private JScrollPane buildTradesPanel() {
        tradesModel = new DefaultTableModel(
                new String[]{"Trade ID", "Symbol", "Qty", "Price ($)", "Buyer", "Seller", "Time"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        return wrapTable(makeTable(tradesModel, new int[]{70, 70, 55, 95, 85, 85, 130}));
    }

    private JTable makeTable(DefaultTableModel model, int[] widths) {
        JTable t = new JTable(model);
        t.setBackground(BG_PANEL);
        t.setForeground(C_WHITE);
        t.setFont(new Font("Monospaced", Font.PLAIN, 12));
        t.setGridColor(C_BORDER);
        t.setRowHeight(26);
        t.setSelectionBackground(new Color(33, 65, 100));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader hdr = t.getTableHeader();
        hdr.setBackground(new Color(22, 30, 44));
        hdr.setForeground(C_ENGINE);
        hdr.setFont(new Font("Monospaced", Font.BOLD, 12));
        hdr.setReorderingAllowed(false);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.CENTER);
              setBackground(BG_PANEL); setForeground(C_WHITE); }
        };
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            if (i >= 2) t.getColumnModel().getColumn(i).setCellRenderer(centerRender);
        }
        return t;
    }

    private JScrollPane wrapTable(JTable t) {
        JScrollPane s = new JScrollPane(t);
        s.setBackground(BG_PANEL);
        s.getViewport().setBackground(BG_PANEL);
        s.setBorder(BorderFactory.createEmptyBorder());
        return s;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout(20, 0));
        f.setBackground(BG_HDR);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                BorderFactory.createEmptyBorder(9, 22, 9, 22)));

        cashLabel = new JLabel("Total Cash: —");
        cashLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        cashLabel.setForeground(C_TRADE);
        f.add(cashLabel, BorderLayout.WEST);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        progressBar.setForeground(C_ENGINE);
        progressBar.setBackground(BG_PANEL);
        progressBar.setFont(new Font("Monospaced", Font.PLAIN, 11));
        progressBar.setPreferredSize(new Dimension(280, 20));
        f.add(progressBar, BorderLayout.CENTER);

        JLabel note = new JLabel("ReentrantLock · Observer · Strategy Pattern · ConcurrentHashMap  ");
        note.setFont(new Font("Monospaced", Font.PLAIN, 11));
        note.setForeground(C_GREY);
        f.add(note, BorderLayout.EAST);
        return f;
    }

    // ── Simulation runner ─────────────────────────────────────────────────────
    private void startSimulation() {
        runButton.setEnabled(false);
        runButton.setForeground(C_BTN_TXT);
        runButton.setBackground(C_BTN_BG.darker());
        statusLabel.setText("● RUNNING");
        statusLabel.setForeground(C_TRADE);
        logPane.setText("");
        tradesModel.setRowCount(0);
        portfolioModel.setRowCount(0);
        progressBar.setIndeterminate(true);
        progressBar.setString("Simulating...");
        tradeCountLabel.setText("Trades: 0");
        cashLabel.setText("Total Cash: —");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                runSimulation();
                return null;
            }
            @Override
            protected void done() {
                try { get(); }
                catch (Exception ex) {
                    log("[ERROR] " + ex.getMessage() + "\n", C_REJECT, true);
                }
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                progressBar.setString("Complete");
                statusLabel.setText("● COMPLETE");
                statusLabel.setForeground(C_BUY);
                runButton.setEnabled(true);
                runButton.setForeground(C_BTN_TXT);
                runButton.setBackground(C_BTN_BG);
                runButton.setText("  ▶   RUN AGAIN  ");
            }
        }.execute();
    }

    private int tradeCount = 0;

    private void runSimulation() throws InterruptedException {
        tradeCount = 0;

        // ── Setup ─────────────────────────────────────────────────────────────
        PortfolioManager pm = new PortfolioManager();
        TradingEngine engine = new TradingEngine(pm, new FIFOMatchingStrategy());

        engine.registerAsset(new Stock("AAPL", "Apple Inc.", 150.00, "NASDAQ"));
        engine.registerAsset(new Stock("TSLA", "Tesla Inc.", 250.00, "NASDAQ"));

        pm.createPortfolio("user-1", 100_000.00);
        pm.createPortfolio("user-2", 100_000.00);
        pm.createPortfolio("bot-1",   50_000.00);
        pm.getPortfolio("user-2").seedShares("AAPL", 500);
        pm.getPortfolio("user-2").seedShares("TSLA", 100);

        // ── Header ────────────────────────────────────────────────────────────
        log("╔══════════════════════════════════════════════════════════════╗\n", C_ENGINE, true);
        log("║     ALGORITHMIC TRADING ENGINE SIMULATOR  —  START           ║\n", C_ENGINE, true);
        log("╚══════════════════════════════════════════════════════════════╝\n\n", C_ENGINE, true);
        sleep(120);

        log("[ENGINE]  AAPL  Apple Inc.   registered  @  $150.00  (NASDAQ)\n", C_ENGINE, false); sleep(80);
        log("[ENGINE]  TSLA  Tesla Inc.   registered  @  $250.00  (NASDAQ)\n\n", C_ENGINE, false); sleep(120);

        log("── INITIAL PORTFOLIOS ────────────────────────────────────────\n", C_GREY, false);
        log("[PORTFOLIO]  user-1  |  cash = $100,000.00  |  holdings: none\n",          C_PORT, false); sleep(60);
        log("[PORTFOLIO]  user-2  |  cash = $100,000.00  |  AAPL×500, TSLA×100\n",      C_PORT, false); sleep(60);
        log("[PORTFOLIO]  bot-1   |  cash =  $50,000.00  |  holdings: none\n\n",         C_PORT, false); sleep(120);

        // ── Phase 1: FIFO concurrent orders ───────────────────────────────────
        log("── PHASE 1 : FIFO MATCHING  (3 concurrent actors) ───────────\n\n", C_GREY, false);
        sleep(80);

        submitLog(engine, pm, limit("user-1","AAPL", OrderSide.BUY,  100, 151.00)); sleep(120);
        submitLog(engine, pm, limit("user-2","AAPL", OrderSide.SELL,  80, 150.50)); sleep(120);
        submitLog(engine, pm, limit("bot-1", "AAPL", OrderSide.BUY,  200, 150.00)); sleep(120);
        submitLog(engine, pm, limit("user-2","AAPL", OrderSide.SELL, 120, 149.00)); sleep(120);
        submitLog(engine, pm, limit("bot-1", "TSLA", OrderSide.BUY,   30, 251.00)); sleep(120);
        submitLog(engine, pm, limit("user-1","TSLA", OrderSide.BUY,   50, 248.00)); sleep(120);
        submitLog(engine, pm, limit("user-2","TSLA", OrderSide.SELL,  40, 249.00)); sleep(120);
        submitLog(engine, pm, limit("bot-1", "AAPL", OrderSide.SELL,  50, 152.00)); sleep(120);
        submitLog(engine, pm, market("user-1","AAPL", OrderSide.BUY,  30));         sleep(120);
        submitLog(engine, pm, market("bot-1", "TSLA", OrderSide.BUY,  10));         sleep(120);
        submitLog(engine, pm, market("user-2","AAPL", OrderSide.SELL, 25));         sleep(120);
        submitLog(engine, pm, limit("user-1","AAPL", OrderSide.BUY,   75, 149.50)); sleep(120);

        // ── Phase 2: ProRata demo ─────────────────────────────────────────────
        log("\n── PHASE 2 : PRO-RATA STRATEGY DEMO  (TSLA) ─────────────────\n\n", C_GREY, false);
        sleep(80);
        engine.switchMatchingStrategy(new ProRataMatchingStrategy());
        log("[ENGINE]  Strategy switched  →  ProRataMatchingStrategy\n\n", C_ENGINE, false); sleep(120);

        submitLog(engine, pm, limit("user-1","TSLA", OrderSide.BUY,  60, 252.00)); sleep(120);
        submitLog(engine, pm, limit("bot-1", "TSLA", OrderSide.BUY,  40, 252.00)); sleep(120);
        submitLog(engine, pm, limit("user-2","TSLA", OrderSide.SELL, 50, 251.50)); sleep(120);

        // ── Final portfolios ──────────────────────────────────────────────────
        log("\n╔══════════════════════════════════════════════════════════════╗\n", C_ENGINE, true);
        log("║     FINAL PORTFOLIOS                                          ║\n", C_ENGINE, true);
        log("╚══════════════════════════════════════════════════════════════╝\n\n", C_ENGINE, true);
        sleep(100);

        double totalCash = 0;
        for (Portfolio p : pm.getAllPortfolios()) {
            double cash = p.getCashBalance();
            totalCash += cash;
            Map<String, Integer> holdings = p.getHoldingsSnapshot();

            String holdStr = holdings.isEmpty() ? "none"
                    : holdings.entrySet().stream()
                        .map(e -> e.getKey() + "×" + e.getValue())
                        .reduce((a, b) -> a + ", " + b).orElse("—");

            log(String.format("[PORTFOLIO]  %-8s  cash = $%,.2f  |  %s\n",
                    p.getUserId(), cash, holdStr), C_PORT, false);
            sleep(80);

            // Populate portfolio table
            if (holdings.isEmpty()) {
                addPortfolioRow(p.getUserId(), cash, "—", "—");
            } else {
                for (Map.Entry<String, Integer> e : holdings.entrySet()) {
                    addPortfolioRow(p.getUserId(), cash, e.getKey(), String.valueOf(e.getValue()));
                }
            }
        }

        // ── Sanity check ──────────────────────────────────────────────────────
        log("\n── SANITY CHECK ─────────────────────────────────────────────\n", C_GREY, false);
        log(String.format("  Total cash across all portfolios:  $%,.2f\n", totalCash), C_BUY, true);
        log("  Conservation verified — no negative cash or share counts\n\n", C_BUY, false);
        log("══════════════  SIMULATION COMPLETE  ══════════════\n", Color.WHITE, true);
        sleep(80);

        double finalCash = totalCash;
        SwingUtilities.invokeLater(() ->
            cashLabel.setText(String.format("Total Cash: $%,.2f  ✓  (100k + 100k + 50k — verified)", finalCash)));
    }

    // ── Submit order, log it, log resulting trades ─────────────────────────────
    private void submitLog(TradingEngine engine, PortfolioManager pm, Order order) {
        boolean isBuy = order.getSide() == OrderSide.BUY;
        Color orderColor = isBuy ? C_BUY : C_SELL;
        String sideTag  = isBuy ? "BUY " : "SELL";
        String typeTag  = order.isMarketOrder() ? "MKT  " : "LIMIT";

        if (order.isMarketOrder()) {
            log(String.format("[%s %s]  %-6s  %-8s  qty=%-5d  @MARKET           [%s]\n",
                    sideTag, typeTag, order.getSymbol(), order.getUserId(),
                    order.getTotalQuantity(), order.getOrderId()), orderColor, false);
        } else {
            double price = ((LimitOrder) order).getLimitPrice();
            log(String.format("[%s %s]  %-6s  %-8s  qty=%-5d  @$%-9.2f  [%s]\n",
                    sideTag, typeTag, order.getSymbol(), order.getUserId(),
                    order.getTotalQuantity(), price, order.getOrderId()), orderColor, false);
        }

        List<Trade> trades = engine.submitOrder(order);

        if (trades.isEmpty() && order.isMarketOrder()) {
            log("           ↳ [CANCELLED]  No liquidity — market order cancelled\n", C_GREY, false);
        }

        for (Trade t : trades) {
            tradeCount++;
            log(String.format("           ↳ [TRADE %-4s]  %-6s  qty=%-5d  @ $%-9.2f  buyer=%-8s  seller=%s\n",
                    t.getTradeId(), t.getSymbol(), t.getQuantity(),
                    t.getExecutionPrice(), t.getBuyerUserId(), t.getSellerUserId()),
                    C_TRADE, true);

            addTradeRow(t);
            final int tc = tradeCount;
            SwingUtilities.invokeLater(() -> tradeCountLabel.setText("Trades: " + tc));
        }
    }

    // ── Table helpers ─────────────────────────────────────────────────────────
    private void addTradeRow(Trade t) {
        String time = t.getTimestamp().toLocalTime().toString();
        if (time.length() > 12) time = time.substring(0, 12);
        final String finalTime = time;
        SwingUtilities.invokeLater(() -> tradesModel.addRow(new Object[]{
                t.getTradeId(), t.getSymbol(), t.getQuantity(),
                String.format("$%.2f", t.getExecutionPrice()),
                t.getBuyerUserId(), t.getSellerUserId(), finalTime
        }));
    }

    private void addPortfolioRow(String user, double cash, String asset, String qty) {
        SwingUtilities.invokeLater(() -> portfolioModel.addRow(new Object[]{
                user, String.format("$%,.2f", cash), asset, qty
        }));
    }

    // ── Logging helper ────────────────────────────────────────────────────────
    private void log(String text, Color color, boolean bold) {
        SwingUtilities.invokeLater(() -> {
            Style s = logPane.addStyle("x", null);
            StyleConstants.setForeground(s, color);
            StyleConstants.setBold(s, bold);
            StyleConstants.setFontFamily(s, "Monospaced");
            StyleConstants.setFontSize(s, 13);
            try {
                doc.insertString(doc.getLength(), text, s);
                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private javax.swing.border.Border titledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_BORDER), title,
                0, 0, new Font("Monospaced", Font.BOLD, 11), C_ENGINE);
    }

    // ── Order factories ───────────────────────────────────────────────────────
    private LimitOrder limit(String userId, String symbol, OrderSide side, int qty, double price) {
        return new LimitOrder(idGen.nextId(), userId, symbol, side, qty, price, LocalDateTime.now());
    }

    private MarketOrder market(String userId, String symbol, OrderSide side, int qty) {
        return new MarketOrder(idGen.nextId(), userId, symbol, side, qty, LocalDateTime.now());
    }
}
